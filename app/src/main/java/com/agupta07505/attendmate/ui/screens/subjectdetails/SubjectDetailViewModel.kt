package com.agupta07505.attendmate.ui.screens.subjectdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agupta07505.attendmate.data.local.entity.AttendanceSessionEntity
import com.agupta07505.attendmate.data.local.entity.AttendanceUnitEntity
import com.agupta07505.attendmate.data.local.entity.SubjectEntity
import com.agupta07505.attendmate.data.repository.AttendMateRepository
import com.agupta07505.attendmate.domain.calculator.AttendanceCalculator
import com.agupta07505.attendmate.domain.calculator.AttendanceSummary
import com.agupta07505.attendmate.domain.model.AttendanceStatus
import com.agupta07505.attendmate.domain.model.SessionWithUnits
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SubjectDetailViewModel(
    private val repository: AttendMateRepository,
    private val subjectId: Long
) : ViewModel() {

    val subject: StateFlow<SubjectEntity?> = repository.getSubjectByIdFlow(subjectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val subjectSessions: StateFlow<List<SessionWithUnits>> = combine(
        repository.getSessionsForSubject(subjectId),
        repository.getAllUnitsForSubject(subjectId)
    ) { sessions, units ->
        sessions.map { session ->
            val sessionUnits = units.filter { it.sessionId == session.id }
            SessionWithUnits(session = session, units = sessionUnits)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<AttendanceSummary> = combine(
        subject,
        repository.getAllUnitsForSubject(subjectId)
    ) { sub, units ->
        val target = sub?.targetPercentage ?: 75.0
        val statuses = units.mapNotNull {
            try { AttendanceStatus.valueOf(it.status) } catch (e: Exception) { null }
        }
        AttendanceCalculator.calculate(statuses, target)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AttendanceCalculator.calculate(emptyList()))

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

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }
}
