/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.ui.screens.subjectdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agupta07505.attendsmartly.data.local.entity.AttendanceSessionEntity
import com.agupta07505.attendsmartly.data.local.entity.AttendanceUnitEntity
import com.agupta07505.attendsmartly.data.local.entity.SubjectEntity
import com.agupta07505.attendsmartly.data.repository.AttendSmartlyRepository
import com.agupta07505.attendsmartly.domain.calculator.AttendanceCalculator
import com.agupta07505.attendsmartly.domain.calculator.AttendanceSummary
import com.agupta07505.attendsmartly.domain.model.AttendanceStatus
import com.agupta07505.attendsmartly.domain.model.SessionWithUnits
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SubjectDetailViewModel(
    private val repository: AttendSmartlyRepository,
    private val subjectId: Long
) : ViewModel() {

    val subject: StateFlow<SubjectEntity?> = repository.getSubjectByIdFlow(subjectId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val subjectSessions: StateFlow<List<SessionWithUnits>> = combine(
        repository.getSessionsForSubject(subjectId),
        repository.getAllUnitsForSubject(subjectId)
    ) { sessions, units ->
        sessions.map { session ->
            val sessionUnits = units.filter { it.sessionId == session.id }
            SessionWithUnits(session = session, units = sessionUnits)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val summary: StateFlow<AttendanceSummary> = combine(
        subject,
        repository.getAllUnitsForSubject(subjectId)
    ) { sub, units ->
        val target = sub?.targetPercentage ?: 75.0
        val statuses = units.mapNotNull {
            try { AttendanceStatus.valueOf(it.status) } catch (e: Exception) { null }
        }
        AttendanceCalculator.calculate(statuses, target)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AttendanceCalculator.calculate(emptyList()))

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

    fun markPastAttendance(count: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (success, message) = repository.markPastAttendanceForSubject(subjectId, count)
            onResult(success, message)
        }
    }
}
