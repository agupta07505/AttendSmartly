/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agupta07505.attendsmartly.data.local.entity.SubjectEntity
import com.agupta07505.attendsmartly.data.preferences.UserPreferences
import com.agupta07505.attendsmartly.data.preferences.UserPreferencesRepository
import com.agupta07505.attendsmartly.data.repository.AttendSmartlyRepository
import com.agupta07505.attendsmartly.domain.calculator.AttendanceCalculator
import com.agupta07505.attendsmartly.domain.calculator.AttendanceSummary
import com.agupta07505.attendsmartly.domain.model.AttendanceStatus
import com.agupta07505.attendsmartly.ui.screens.subjects.SubjectWithSummary
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
    private val repository: AttendSmartlyRepository,
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
        val validSessions = if (prefs.trackBySemester && (prefs.semesterStartDate.isNotBlank() || prefs.semesterEndDate.isNotBlank())) {
            sessions.filter { session ->
                val afterStart = prefs.semesterStartDate.isBlank() || session.sessionDate >= prefs.semesterStartDate
                val beforeEnd = prefs.semesterEndDate.isBlank() || session.sessionDate <= prefs.semesterEndDate
                afterStart && beforeEnd
            }
        } else {
            sessions
        }

        val validSessionIds = validSessions.map { it.id }.toSet()
        val validUnits = units.filter { validSessionIds.contains(it.sessionId) }

        val allStatuses = validUnits.mapNotNull {
            try { AttendanceStatus.valueOf(it.status) } catch (e: Exception) { null }
        }
        val overallSummary = AttendanceCalculator.calculate(allStatuses, prefs.defaultTargetAttendance)

        val subjectSummaries = subjects.map { subject ->
            val subSessions = validSessions.filter { it.subjectId == subject.id }
            val sessionIds = subSessions.map { it.id }.toSet()
            val subUnits = validUnits.filter { sessionIds.contains(it.sessionId) }
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
