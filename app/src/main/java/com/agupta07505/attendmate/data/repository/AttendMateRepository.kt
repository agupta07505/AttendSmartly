package com.agupta07505.attendmate.data.repository

import com.agupta07505.attendmate.data.local.dao.AttendanceDao
import com.agupta07505.attendmate.data.local.dao.HolidayDao
import com.agupta07505.attendmate.data.local.dao.SubjectDao
import com.agupta07505.attendmate.data.local.dao.TimetableDao
import com.agupta07505.attendmate.data.local.entity.*
import com.agupta07505.attendmate.domain.model.AttendanceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate

class AttendMateRepository(
    private val subjectDao: SubjectDao,
    private val timetableDao: TimetableDao,
    private val attendanceDao: AttendanceDao,
    private val holidayDao: HolidayDao
) {
    // Subjects
    val activeSubjects: Flow<List<SubjectEntity>> = subjectDao.getActiveSubjects()
    val allSubjects: Flow<List<SubjectEntity>> = subjectDao.getAllSubjects()

    fun getSubjectByIdFlow(id: Long): Flow<SubjectEntity?> = subjectDao.getSubjectByIdFlow(id)
    suspend fun getSubjectById(id: Long): SubjectEntity? = subjectDao.getSubjectById(id)
    suspend fun insertSubject(subject: SubjectEntity): Long = subjectDao.insertSubject(subject)
    suspend fun updateSubject(subject: SubjectEntity) = subjectDao.updateSubject(subject)
    suspend fun setSubjectArchived(id: Long, archived: Boolean) = subjectDao.setArchivedStatus(id, archived)
    suspend fun deleteSubject(subject: SubjectEntity) = subjectDao.deleteSubject(subject)
    suspend fun deleteSubjectById(id: Long) = subjectDao.deleteSubjectById(id)

    // Timetable
    val allActiveTimetableEntries: Flow<List<TimetableEntryEntity>> = timetableDao.getAllActiveEntries()
    fun getTimetableForDay(dayOfWeek: Int): Flow<List<TimetableEntryEntity>> = timetableDao.getEntriesForDay(dayOfWeek)
    fun getTimetableForSubject(subjectId: Long): Flow<List<TimetableEntryEntity>> = timetableDao.getEntriesForSubject(subjectId)
    suspend fun getTimetableEntryById(id: Long): TimetableEntryEntity? = timetableDao.getEntryById(id)
    suspend fun insertTimetableEntry(entry: TimetableEntryEntity): Long = timetableDao.insertEntry(entry)
    suspend fun updateTimetableEntry(entry: TimetableEntryEntity) = timetableDao.updateEntry(entry)
    suspend fun deleteTimetableEntry(id: Long) = timetableDao.deleteEntryById(id)

    // Attendance
    fun getSessionsForDate(dateStr: String): Flow<List<AttendanceSessionEntity>> = attendanceDao.getSessionsForDate(dateStr)
    fun getSessionsForSubject(subjectId: Long): Flow<List<AttendanceSessionEntity>> = attendanceDao.getSessionsForSubject(subjectId)
    val allSessions: Flow<List<AttendanceSessionEntity>> = attendanceDao.getAllSessions()

    fun getUnitsForSession(sessionId: Long): Flow<List<AttendanceUnitEntity>> = attendanceDao.getUnitsForSessionFlow(sessionId)
    fun getAllUnitsForSubject(subjectId: Long): Flow<List<AttendanceUnitEntity>> = attendanceDao.getAllUnitsForSubject(subjectId)
    val allUnits: Flow<List<AttendanceUnitEntity>> = attendanceDao.getAllUnits()

    suspend fun getSessionForSubjectAndDate(subjectId: Long, dateStr: String) =
        attendanceDao.getSessionForSubjectAndDate(subjectId, dateStr)

    suspend fun getSessionForTimetableAndDate(timetableEntryId: Long, dateStr: String) =
        attendanceDao.getSessionForTimetableAndDate(timetableEntryId, dateStr)

    suspend fun getSessionById(sessionId: Long): AttendanceSessionEntity? = attendanceDao.getSessionById(sessionId)
    suspend fun getUnitById(unitId: Long): AttendanceUnitEntity? = attendanceDao.getUnitById(unitId)

    suspend fun createOrUpdateSessionWithUnits(
        session: AttendanceSessionEntity,
        units: List<AttendanceUnitEntity>
    ): Long = attendanceDao.createOrUpdateSessionWithUnits(session, units)

    suspend fun updateUnitStatus(unitId: Long, newStatus: AttendanceStatus) {
        val target = attendanceDao.getUnitById(unitId) ?: return
        attendanceDao.updateUnit(
            target.copy(
                status = newStatus.name,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markOneUnitStatus(
        sessionId: Long,
        status: AttendanceStatus
    ) {
        val units = attendanceDao.getUnitsForSession(sessionId)
        val unmarked = units.firstOrNull { it.status == AttendanceStatus.UNMARKED.name }
        if (unmarked != null) {
            attendanceDao.updateUnit(
                unmarked.copy(
                    status = status.name,
                    updatedAt = System.currentTimeMillis()
                )
            )
            return
        }
        val firstDifferent = units.firstOrNull { it.status != status.name }
        if (firstDifferent != null) {
            attendanceDao.updateUnit(
                firstDifferent.copy(
                    status = status.name,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun markNextUnitStatus(
        sessionId: Long,
        status: AttendanceStatus
    ): Boolean {
        val units = attendanceDao.getUnitsForSession(sessionId)
        val unmarked = units.firstOrNull { it.status == AttendanceStatus.UNMARKED.name }
        if (unmarked != null) {
            attendanceDao.updateUnit(
                unmarked.copy(
                    status = status.name,
                    updatedAt = System.currentTimeMillis()
                )
            )
            return true
        }
        return false
    }

    suspend fun markCompleteSessionStatus(
        sessionId: Long,
        status: AttendanceStatus
    ) {
        val units = attendanceDao.getUnitsForSession(sessionId)
        val updated = units.map {
            it.copy(
                status = status.name,
                updatedAt = System.currentTimeMillis()
            )
        }
        attendanceDao.updateUnits(updated)
    }

    suspend fun resetSessionAttendance(sessionId: Long) {
        markCompleteSessionStatus(sessionId, AttendanceStatus.UNMARKED)
    }

    suspend fun deleteSession(sessionId: Long) {
        attendanceDao.deleteSessionById(sessionId)
    }

    // Holidays
    val allHolidays: Flow<List<HolidayEntity>> = holidayDao.getAllHolidays()
    suspend fun getHolidayByDate(dateStr: String): HolidayEntity? = holidayDao.getHolidayByDate(dateStr)
    suspend fun insertHoliday(holiday: HolidayEntity): Long = holidayDao.insertHoliday(holiday)
    suspend fun deleteHoliday(id: Long) = holidayDao.deleteHolidayById(id)

    suspend fun clearAllData() {
        val subs = allSubjects.first()
        for (s in subs) subjectDao.deleteSubject(s)
        val tts = allActiveTimetableEntries.first()
        for (t in tts) timetableDao.deleteEntryById(t.id)
        val sesss = allSessions.first()
        for (se in sesss) attendanceDao.deleteSessionById(se.id)
    }
}
