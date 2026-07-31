package com.agupta07505.attendmate.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agupta07505.attendmate.data.local.entity.AttendanceSessionEntity
import com.agupta07505.attendmate.data.local.entity.AttendanceUnitEntity
import com.agupta07505.attendmate.data.local.entity.SubjectEntity
import com.agupta07505.attendmate.data.repository.AttendMateRepository
import com.agupta07505.attendmate.domain.model.AttendanceStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HistoryRecord(
    val session: AttendanceSessionEntity,
    val subject: SubjectEntity,
    val units: List<AttendanceUnitEntity>
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val repository: AttendMateRepository
) : ViewModel() {

    private val _selectedSubjectId = MutableStateFlow<Long?>(null)
    val selectedSubjectId: StateFlow<Long?> = _selectedSubjectId.asStateFlow()

    private val _statusFilter = MutableStateFlow<AttendanceStatus?>(null)
    val statusFilter: StateFlow<AttendanceStatus?> = _statusFilter.asStateFlow()

    val subjects: StateFlow<List<SubjectEntity>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyRecords: StateFlow<List<HistoryRecord>> = combine(
        repository.allSessions,
        repository.allSubjects,
        repository.allUnits,
        _selectedSubjectId,
        _statusFilter
    ) { sessions, subjects, units, subIdFilter, statusFilter ->
        sessions
            .filter { subIdFilter == null || it.subjectId == subIdFilter }
            .mapNotNull { session ->
                val subject = subjects.find { it.id == session.subjectId } ?: return@mapNotNull null
                val sessionUnits = units.filter { it.sessionId == session.id }

                if (statusFilter != null) {
                    val hasMatchingUnit = sessionUnits.any { it.status == statusFilter.name }
                    if (!hasMatchingUnit) return@mapNotNull null
                }

                HistoryRecord(session = session, subject = subject, units = sessionUnits)
            }
            .sortedByDescending { it.session.sessionDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectSubjectFilter(id: Long?) {
        _selectedSubjectId.value = id
    }

    fun selectStatusFilter(status: AttendanceStatus?) {
        _statusFilter.value = status
    }

    fun updateUnitStatus(unitId: Long, newStatus: AttendanceStatus) {
        viewModelScope.launch {
            repository.updateUnitStatus(unitId, newStatus)
        }
    }

    fun markAllSessionStatus(sessionId: Long, status: AttendanceStatus) {
        viewModelScope.launch {
            repository.markCompleteSessionStatus(sessionId, status)
        }
    }

    fun resetSession(sessionId: Long) {
        viewModelScope.launch {
            repository.resetSessionAttendance(sessionId)
        }
    }
}
