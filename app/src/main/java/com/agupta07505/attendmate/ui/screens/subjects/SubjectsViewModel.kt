package com.agupta07505.attendmate.ui.screens.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agupta07505.attendmate.data.local.entity.SubjectEntity
import com.agupta07505.attendmate.data.repository.AttendMateRepository
import com.agupta07505.attendmate.domain.calculator.AttendanceCalculator
import com.agupta07505.attendmate.domain.calculator.AttendanceSummary
import com.agupta07505.attendmate.domain.model.AttendanceStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SubjectWithSummary(
    val subject: SubjectEntity,
    val summary: AttendanceSummary
)

@OptIn(ExperimentalCoroutinesApi::class)
class SubjectsViewModel(
    private val repository: AttendMateRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    val subjectsWithSummary: StateFlow<List<SubjectWithSummary>> = combine(
        repository.allSubjects,
        repository.allSessions,
        repository.allUnits,
        _searchQuery,
        _showArchived
    ) { subjects, sessions, units, query, showArchived ->
        subjects
            .filter { it.isArchived == showArchived }
            .filter {
                it.name.contains(query, ignoreCase = true) ||
                it.code.contains(query, ignoreCase = true) ||
                it.teacherName.contains(query, ignoreCase = true)
            }
            .map { subject ->
                val subjectSessions = sessions.filter { it.subjectId == subject.id }
                val sessionIds = subjectSessions.map { it.id }.toSet()
                val subjectUnits = units.filter { sessionIds.contains(it.sessionId) }
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
