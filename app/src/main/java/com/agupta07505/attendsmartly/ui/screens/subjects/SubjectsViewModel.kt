/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.ui.screens.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agupta07505.attendsmartly.data.local.entity.SubjectEntity
import com.agupta07505.attendsmartly.data.preferences.UserPreferences
import com.agupta07505.attendsmartly.data.preferences.UserPreferencesRepository
import com.agupta07505.attendsmartly.data.repository.AttendSmartlyRepository
import com.agupta07505.attendsmartly.domain.calculator.AttendanceCalculator
import com.agupta07505.attendsmartly.domain.calculator.AttendanceSummary
import com.agupta07505.attendsmartly.domain.model.AttendanceStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SubjectWithSummary(
    val subject: SubjectEntity,
    val summary: AttendanceSummary
)

private data class SubjectFilter(
    val query: String,
    val showArchived: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
class SubjectsViewModel(
    private val repository: AttendSmartlyRepository,
    private val preferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = (preferencesRepository?.userPreferencesFlow ?: flowOf(UserPreferences()))
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    private val _filterFlow = combine(_searchQuery, _showArchived) { query, showArchived ->
        SubjectFilter(query, showArchived)
    }

    val subjectsWithSummary: StateFlow<List<SubjectWithSummary>> = combine(
        repository.allSubjects,
        repository.allSessions,
        repository.allUnits,
        _filterFlow,
        userPreferences
    ) { subjects, sessions, units, filter, prefs ->
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

        subjects
            .filter { it.isArchived == filter.showArchived }
            .filter {
                it.name.contains(filter.query, ignoreCase = true) ||
                it.code.contains(filter.query, ignoreCase = true) ||
                it.teacherName.contains(filter.query, ignoreCase = true)
            }
            .map { subject ->
                val subjectSessions = validSessions.filter { it.subjectId == subject.id }
                val sessionIds = subjectSessions.map { it.id }.toSet()
                val subjectUnits = validUnits.filter { sessionIds.contains(it.sessionId) }
                val statuses = subjectUnits.mapNotNull {
                    try { AttendanceStatus.valueOf(it.status) } catch (e: Exception) { null }
                }

                val summary = AttendanceCalculator.calculate(statuses, subject.targetPercentage)
                SubjectWithSummary(subject = subject, summary = summary)
            }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleShowArchived(show: Boolean) {
        _showArchived.value = show
    }

    fun addSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.insertSubject(subject)
        }
    }

    fun updateSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.updateSubject(subject)
        }
    }

    fun setArchived(id: Long, isArchived: Boolean) {
        viewModelScope.launch {
            repository.setSubjectArchived(id, isArchived)
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
        }
    }
}
