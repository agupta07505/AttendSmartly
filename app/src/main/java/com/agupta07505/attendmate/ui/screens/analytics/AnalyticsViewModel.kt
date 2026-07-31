package com.agupta07505.attendmate.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agupta07505.attendmate.data.local.entity.SubjectEntity
import com.agupta07505.attendmate.data.preferences.UserPreferences
import com.agupta07505.attendmate.data.preferences.UserPreferencesRepository
import com.agupta07505.attendmate.data.repository.AttendMateRepository
import com.agupta07505.attendmate.domain.calculator.AttendanceCalculator
import com.agupta07505.attendmate.domain.calculator.AttendanceSummary
import com.agupta07505.attendmate.domain.model.AttendanceStatus
import com.agupta07505.attendmate.ui.screens.subjects.SubjectWithSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class AnalyticsState(
    val overallSummary: AttendanceSummary,
    val subjectSummaries: List<SubjectWithSummary>,
    val subjectsBelowTarget: List<SubjectWithSummary>,
    val subjectsNearTarget: List<SubjectWithSummary>,
    val totalSafeBunksAcrossAllSubjects: Int,
    val bestSubject: SubjectWithSummary?,
    val lowestSubject: SubjectWithSummary?
)

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val repository: AttendMateRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    val analyticsState: StateFlow<AnalyticsState> = combine(
        repository.activeSubjects,
        repository.allSessions,
        repository.allUnits,
        userPreferences
    ) { subjects, sessions, units, prefs ->
        val allStatuses = units.mapNotNull {
            try { AttendanceStatus.valueOf(it.status) } catch (e: Exception) { null }
        }
        val overallSummary = AttendanceCalculator.calculate(allStatuses, prefs.defaultTargetAttendance)

        val subjectSummaries = subjects.map { subject ->
            val subSessions = sessions.filter { it.subjectId == subject.id }
            val sessionIds = subSessions.map { it.id }.toSet()
            val subUnits = units.filter { sessionIds.contains(it.sessionId) }
            val subStatuses = subUnits.mapNotNull {
                try { AttendanceStatus.valueOf(it.status) } catch (e: Exception) { null }
            }
            val summary = AttendanceCalculator.calculate(subStatuses, subject.targetPercentage)
            SubjectWithSummary(subject, summary)
        }

        val belowTarget = subjectSummaries.filter { it.summary.percentage < it.subject.targetPercentage && it.summary.totalConductedUnits > 0 }
        val nearTarget = subjectSummaries.filter {
            val diff = it.summary.percentage - it.subject.targetPercentage
            diff in 0.0..5.0 && it.summary.totalConductedUnits > 0
        }

        val totalBunks = subjectSummaries.sumOf { it.summary.safeBunks }
        val sortedByPct = subjectSummaries.filter { it.summary.totalConductedUnits > 0 }.sortedByDescending { it.summary.percentage }

        AnalyticsState(
            overallSummary = overallSummary,
            subjectSummaries = subjectSummaries,
            subjectsBelowTarget = belowTarget,
            subjectsNearTarget = nearTarget,
            totalSafeBunksAcrossAllSubjects = totalBunks,
            bestSubject = sortedByPct.firstOrNull(),
            lowestSubject = sortedByPct.lastOrNull()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AnalyticsState(
            overallSummary = AttendanceCalculator.calculate(emptyList()),
            subjectSummaries = emptyList(),
            subjectsBelowTarget = emptyList(),
            subjectsNearTarget = emptyList(),
            totalSafeBunksAcrossAllSubjects = 0,
            bestSubject = null,
            lowestSubject = null
        )
    )
}
