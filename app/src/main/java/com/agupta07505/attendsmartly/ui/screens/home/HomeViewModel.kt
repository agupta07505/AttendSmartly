/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agupta07505.attendsmartly.data.local.entity.AttendanceSessionEntity
import com.agupta07505.attendsmartly.data.local.entity.AttendanceUnitEntity
import com.agupta07505.attendsmartly.data.preferences.UserPreferences
import com.agupta07505.attendsmartly.data.preferences.UserPreferencesRepository
import com.agupta07505.attendsmartly.data.repository.AttendSmartlyRepository
import com.agupta07505.attendsmartly.domain.calculator.AttendanceCalculator
import com.agupta07505.attendsmartly.domain.calculator.AttendanceSummary
import com.agupta07505.attendsmartly.domain.model.AttendanceStatus
import com.agupta07505.attendsmartly.domain.model.ClassScheduleItem
import com.agupta07505.attendsmartly.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: AttendSmartlyRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedDateIso = MutableStateFlow(DateUtils.todayIso())
    val selectedDateIso: StateFlow<String> = _selectedDateIso.asStateFlow()

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    // Last action for Undo
    private var lastSessionIdForUndo: Long? = null
    private var lastUnitsStateForUndo: List<AttendanceUnitEntity>? = null

    val todaySchedules: StateFlow<List<ClassScheduleItem>> = _selectedDateIso
        .flatMapLatest { dateIso ->
            val dayOfWeek = DateUtils.getDayOfWeekInt(dateIso)
            combine(
                repository.getTimetableForDay(dayOfWeek),
                repository.activeSubjects,
                repository.getSessionsForDate(dateIso),
                repository.allUnits,
                repository.allHolidays
            ) { timetable, subjects, sessions, allUnits, holidays ->
                val holiday = holidays.find { it.date == dateIso }
                val isHoliday = holiday != null

                timetable.mapNotNull { entry ->
                    val subject = subjects.find { it.id == entry.subjectId } ?: return@mapNotNull null
                    val session = sessions.find { it.timetableEntryId == entry.id }
                    val units = if (session != null) {
                        allUnits.filter { it.sessionId == session.id }
                    } else {
                        emptyList()
                    }

                    ClassScheduleItem(
                        session = session,
                        timetableEntry = entry,
                        subject = subject,
                        units = units,
                        isHoliday = isHoliday,
                        holidayTitle = holiday?.title
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val overallSummary: StateFlow<AttendanceSummary> = combine(
        repository.allUnits,
        userPreferences
    ) { units, prefs ->
        val statuses = units.mapNotNull {
            try { AttendanceStatus.valueOf(it.status) } catch (e: Exception) { null }
        }
        AttendanceCalculator.calculate(statuses, prefs.defaultTargetAttendance)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AttendanceCalculator.calculate(emptyList()))

    fun selectDate(dateIso: String) {
        _selectedDateIso.value = dateIso
    }

    private suspend fun getOrCreateSession(item: ClassScheduleItem): Long {
        if (item.session != null) return item.session.id

        val newSession = AttendanceSessionEntity(
            subjectId = item.subject.id,
            timetableEntryId = item.timetableEntry.id,
            sessionDate = _selectedDateIso.value,
            startTime = item.timetableEntry.startTime,
            endTime = item.timetableEntry.endTime,
            expectedUnitCount = item.timetableEntry.attendanceUnitCount
        )

        val units = (0 until item.timetableEntry.attendanceUnitCount).map { index ->
            AttendanceUnitEntity(
                sessionId = 0,
                unitIndex = index,
                status = AttendanceStatus.UNMARKED.name
            )
        }

        return repository.createOrUpdateSessionWithUnits(newSession, units)
    }

    fun markPresent(item: ClassScheduleItem) {
        viewModelScope.launch {
            val sessionId = getOrCreateSession(item)
            saveUndoState(sessionId)
            repository.markOneUnitStatus(sessionId, AttendanceStatus.PRESENT)
        }
    }

    fun markAbsent(item: ClassScheduleItem) {
        viewModelScope.launch {
            val sessionId = getOrCreateSession(item)
            saveUndoState(sessionId)
            repository.markOneUnitStatus(sessionId, AttendanceStatus.ABSENT)
        }
    }

    fun markBunked(item: ClassScheduleItem) {
        viewModelScope.launch {
            val sessionId = getOrCreateSession(item)
            saveUndoState(sessionId)
            repository.markOneUnitStatus(sessionId, AttendanceStatus.BUNKED)
        }
    }

    fun markCancelled(item: ClassScheduleItem) {
        viewModelScope.launch {
            val sessionId = getOrCreateSession(item)
            saveUndoState(sessionId)
            repository.markOneUnitStatus(sessionId, AttendanceStatus.CANCELLED)
        }
    }

    fun updateUnitStatus(sessionId: Long, unitId: Long, newStatus: AttendanceStatus) {
        viewModelScope.launch {
            saveUndoState(sessionId)
            repository.updateUnitStatus(unitId, newStatus)
        }
    }

    fun markAllSessionStatus(item: ClassScheduleItem, status: AttendanceStatus) {
        viewModelScope.launch {
            val sessionId = getOrCreateSession(item)
            saveUndoState(sessionId)
            repository.markCompleteSessionStatus(sessionId, status)
        }
    }

    fun resetSession(item: ClassScheduleItem) {
        viewModelScope.launch {
            val sessionId = getOrCreateSession(item)
            saveUndoState(sessionId)
            repository.resetSessionAttendance(sessionId)
        }
    }

    private suspend fun saveUndoState(sessionId: Long) {
        lastSessionIdForUndo = sessionId
        lastUnitsStateForUndo = repository.getUnitsForSession(sessionId).first()
    }

    fun undoLastAction() {
        val sessionId = lastSessionIdForUndo ?: return
        val previousUnits = lastUnitsStateForUndo ?: return
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId) ?: return@launch
            repository.createOrUpdateSessionWithUnits(session, previousUnits)
            lastSessionIdForUndo = null
            lastUnitsStateForUndo = null
        }
    }
}
