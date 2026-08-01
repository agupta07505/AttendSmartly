package com.agupta07505.attendsmartly.data.repository

import com.agupta07505.attendsmartly.data.local.dao.AttendanceDao
import com.agupta07505.attendsmartly.data.local.dao.HolidayDao
import com.agupta07505.attendsmartly.data.local.dao.SubjectDao
import com.agupta07505.attendsmartly.data.local.dao.TimetableDao
import com.agupta07505.attendsmartly.data.local.entity.*
import com.agupta07505.attendsmartly.domain.model.AttendanceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import com.agupta07505.attendsmartly.util.DateUtils
import java.time.DayOfWeek
import java.time.LocalDate

class AttendSmartlyRepository(
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
    suspend fun deleteAllTimetableEntries() = timetableDao.deleteAllEntries()

    suspend fun importParsedTimetable(
        items: List<com.agupta07505.attendsmartly.domain.model.ParsedTimetableItem>,
        replaceExisting: Boolean = false
    ) {
        if (replaceExisting) {
            timetableDao.deleteAllEntries()
        }

        val existingSubjects = subjectDao.getAllSubjects().first().toMutableList()
        val colors = listOf(
            0xFF2196F3L, 0xFF4CAF50L, 0xFFFF9800L, 0xFF9C27B0L,
            0xFFE91E63L, 0xFF00BCD4L, 0xFF3F51B5L, 0xFF009688L, 0xFFFF5722L
        )

        for (item in items) {
            val normName = item.subjectName.trim()
            val normCode = item.subjectCode.trim()
            if (normName.isEmpty() && normCode.isEmpty()) continue

            val isLabItem = item.isPractical ||
                    normName.contains("lab", ignoreCase = true) ||
                    normName.contains("practical", ignoreCase = true) ||
                    normName.contains("workshop", ignoreCase = true) ||
                    normCode.contains("lab", ignoreCase = true)

            val targetType = if (isLabItem) "Lab" else "Lecture"

            // Match subject by BOTH name/code AND subject category (Lab vs Lecture)
            var subject = existingSubjects.find { sub ->
                val subIsLab = sub.type.equals("Lab", ignoreCase = true) ||
                        sub.type.equals("Practical", ignoreCase = true) ||
                        sub.name.contains("lab", ignoreCase = true)
                
                val sameTypeCategory = (subIsLab == isLabItem)

                val nameMatches = (normName.isNotEmpty() && (sub.name.equals(normName, ignoreCase = true) || sub.code.equals(normName, ignoreCase = true))) ||
                        (normCode.isNotEmpty() && (sub.code.equals(normCode, ignoreCase = true) || sub.name.equals(normCode, ignoreCase = true)))

                sameTypeCategory && nameMatches
            }

            val subjectId = if (subject != null) {
                // If existing subject doesn't have a teacher name, update it with imported teacherName
                if (subject.teacherName.isBlank() && item.teacherName.isNotBlank()) {
                    val updatedSubject = subject.copy(teacherName = item.teacherName.trim())
                    subjectDao.updateSubject(updatedSubject)
                    val idx = existingSubjects.indexOfFirst { it.id == subject.id }
                    if (idx >= 0) existingSubjects[idx] = updatedSubject
                }
                subject.id
            } else {
                val newColor = colors[existingSubjects.size % colors.size]
                var displayName = if (normName.isNotBlank()) normName else normCode
                if (isLabItem && !displayName.contains("lab", ignoreCase = true) && !displayName.contains("practical", ignoreCase = true)) {
                    displayName = "$displayName Lab"
                }
                val displayCode = if (normCode.isNotBlank()) normCode else ""
                val newSub = SubjectEntity(
                    name = displayName,
                    code = displayCode,
                    type = targetType,
                    teacherName = item.teacherName.trim(),
                    room = item.roomLocation,
                    colorValue = newColor,
                    iconName = if (isLabItem) "Science" else "Book",
                    notes = if (normCode.isNotBlank() && normCode != displayName) "Code: $normCode" else ""
                )
                val newId = subjectDao.insertSubject(newSub)
                val createdSubject = newSub.copy(id = newId)
                existingSubjects.add(createdSubject)
                newId
            }

            val entry = TimetableEntryEntity(
                subjectId = subjectId,
                dayOfWeek = item.dayOfWeek.coerceIn(1, 7),
                startTime = item.startTime,
                endTime = item.endTime,
                roomOverride = item.roomLocation,
                teacherOverride = item.teacherName.trim(),
                attendanceUnitCount = item.attendanceUnitCount.coerceAtLeast(1)
            )
            timetableDao.insertEntry(entry)
        }
    }

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

    suspend fun markPastAttendanceForSubject(
        subjectId: Long,
        attendedCount: Int
    ): Pair<Boolean, String> {
        if (attendedCount <= 0) {
            return Pair(false, "Please enter a valid count greater than 0.")
        }

        val allEntries = timetableDao.getEntriesForSubject(subjectId).first()
        if (allEntries.isEmpty()) {
            return Pair(false, "No timetable schedule entries found for this subject. Please add timetable entries first.")
        }

        val holidays = holidayDao.getAllHolidays().first()
        val today = LocalDate.now()
        var currentDate = today.minusDays(1) // EXCLUDING CURRENT DATE (TODAY)

        var remainingToMark = attendedCount
        var sessionsCreatedOrUpdated = 0
        var unitsMarkedPresent = 0

        var daysChecked = 0
        val maxDays = 365

        while (remainingToMark > 0 && daysChecked < maxDays) {
            val dateStr = currentDate.format(DateUtils.isoDateFormatter)

            val isHoliday = holidays.any { it.date == dateStr }
            if (!isHoliday) {
                val dayOfWeek = currentDate.dayOfWeek.value // 1=Mon, 7=Sun
                val dayEntries = allEntries.filter { it.dayOfWeek == dayOfWeek }
                    .sortedByDescending { it.startTime } // fill latest past entries first

                if (dayEntries.isNotEmpty()) {
                    for (entry in dayEntries) {
                        if (remainingToMark <= 0) break

                        val unitsToMarkForThisEntry = minOf(entry.attendanceUnitCount, remainingToMark)

                        var session = attendanceDao.getSessionForTimetableAndDate(entry.id, dateStr)
                        val sessionId = if (session != null) {
                            session.id
                        } else {
                            val newSession = AttendanceSessionEntity(
                                subjectId = subjectId,
                                timetableEntryId = entry.id,
                                sessionDate = dateStr,
                                startTime = entry.startTime,
                                endTime = entry.endTime,
                                expectedUnitCount = entry.attendanceUnitCount
                            )
                            attendanceDao.insertSession(newSession)
                        }

                        val existingUnits = attendanceDao.getUnitsForSession(sessionId)
                        val newUnits = mutableListOf<AttendanceUnitEntity>()

                        for (uIdx in 0 until entry.attendanceUnitCount) {
                            val existingUnit = existingUnits.find { it.unitIndex == uIdx }
                            val status = if (uIdx < unitsToMarkForThisEntry) {
                                AttendanceStatus.PRESENT.name
                            } else {
                                existingUnit?.status ?: AttendanceStatus.UNMARKED.name
                            }

                            newUnits.add(
                                AttendanceUnitEntity(
                                    id = existingUnit?.id ?: 0,
                                    sessionId = sessionId,
                                    unitIndex = uIdx,
                                    status = status,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }

                        attendanceDao.insertUnits(newUnits)
                        remainingToMark -= unitsToMarkForThisEntry
                        unitsMarkedPresent += unitsToMarkForThisEntry
                        sessionsCreatedOrUpdated++
                    }
                }
            }

            currentDate = currentDate.minusDays(1)
            daysChecked++
        }

        return Pair(
            true,
            "Successfully marked $unitsMarkedPresent class unit(s) as Present across $sessionsCreatedOrUpdated past session(s) (Excluding today)."
        )
    }
}
