/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

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
    val allActiveTimetableEntries: Flow<List<TimetableEntryEntity>> = timetableDao.getAllActiveEntries(DateUtils.todayIso())
    val allTimetableEntries: Flow<List<TimetableEntryEntity>> = timetableDao.getAllEntries()
    fun getTimetableForDay(dayOfWeek: Int, dateStr: String = DateUtils.todayIso()): Flow<List<TimetableEntryEntity>> = timetableDao.getEntriesForDay(dayOfWeek, dateStr)
    fun getTimetableForDayAndDate(dayOfWeek: Int, dateStr: String): Flow<List<TimetableEntryEntity>> = timetableDao.getEntriesForDayAndDate(dayOfWeek, dateStr)
    fun getTimetableForSubject(subjectId: Long): Flow<List<TimetableEntryEntity>> = timetableDao.getEntriesForSubject(subjectId)
    suspend fun getTimetableEntryById(id: Long): TimetableEntryEntity? = timetableDao.getEntryById(id)
    suspend fun insertTimetableEntry(entry: TimetableEntryEntity): Long = timetableDao.insertEntry(entry)

    suspend fun updateTimetableEntry(entry: TimetableEntryEntity) {
        val oldEntry = timetableDao.getEntryById(entry.id)
        timetableDao.updateEntry(entry)

        if (oldEntry != null) {
            // If day of week or timing changed, clean up or update any future/today unmarked sessions linked to this entry
            val linkedSessions = attendanceDao.getSessionsForTimetableEntry(entry.id)
            for (session in linkedSessions) {
                val sessionUnits = attendanceDao.getUnitsForSession(session.id)
                val isMarked = sessionUnits.any { it.status != com.agupta07505.attendsmartly.domain.model.AttendanceStatus.UNMARKED.name }

                if (!isMarked && !session.isRescheduled) {
                    val sessionDayOfWeek = DateUtils.getDayOfWeekInt(session.sessionDate)
                    if (sessionDayOfWeek != entry.dayOfWeek) {
                        // Session was created on a day that is no longer scheduled
                        attendanceDao.deleteSessionById(session.id)
                    } else {
                        // Update session timing and expected units
                        val updatedSession = session.copy(
                            subjectId = entry.subjectId,
                            startTime = entry.startTime,
                            endTime = entry.endTime,
                            expectedUnitCount = entry.attendanceUnitCount,
                            updatedAt = System.currentTimeMillis()
                        )
                        attendanceDao.updateSession(updatedSession)
                    }
                }
            }
        }
    }

    suspend fun deleteTimetableEntry(id: Long) {
        val sessions = attendanceDao.getSessionsForTimetableEntry(id)
        for (session in sessions) {
            val sessionUnits = attendanceDao.getUnitsForSession(session.id)
            val isMarked = sessionUnits.any { it.status != com.agupta07505.attendsmartly.domain.model.AttendanceStatus.UNMARKED.name }
            if (!isMarked && !session.isRescheduled) {
                attendanceDao.deleteSessionById(session.id)
            } else {
                attendanceDao.updateSession(session.copy(timetableEntryId = null))
            }
        }
        timetableDao.deleteEntryById(id)
    }

    suspend fun deleteAllTimetableEntries() = timetableDao.deleteAllEntries()

    suspend fun updateTimetableEntryFromDate(
        oldEntryId: Long,
        updatedEntry: TimetableEntryEntity,
        effectiveStartDate: String = DateUtils.todayIso()
    ): Long {
        val oldEntry = timetableDao.getEntryById(oldEntryId)
        if (oldEntry != null) {
            val yesterdayStr = try {
                LocalDate.parse(effectiveStartDate, DateUtils.isoDateFormatter).minusDays(1).format(DateUtils.isoDateFormatter)
            } catch (e: Exception) { "" }

            if (yesterdayStr.isNotBlank() && (oldEntry.startDate.isBlank() || oldEntry.startDate <= yesterdayStr)) {
                timetableDao.setEntryEndDate(oldEntryId, yesterdayStr)

                // Clean up any future or today UNMARKED sessions for the old entry
                val futureSessions = attendanceDao.getSessionsForTimetableEntryFromDate(oldEntryId, effectiveStartDate)
                for (session in futureSessions) {
                    val sessionUnits = attendanceDao.getUnitsForSession(session.id)
                    val isMarked = sessionUnits.any { it.status != com.agupta07505.attendsmartly.domain.model.AttendanceStatus.UNMARKED.name }
                    if (!isMarked && !session.isRescheduled) {
                        attendanceDao.deleteSessionById(session.id)
                    }
                }

                return timetableDao.insertEntry(
                    updatedEntry.copy(
                        id = 0,
                        startDate = effectiveStartDate,
                        endDate = "",
                        isActive = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                updateTimetableEntry(updatedEntry)
                return oldEntryId
            }
        }
        return timetableDao.insertEntry(updatedEntry)
    }

    suspend fun retireTimetableEntry(id: Long, effectiveDate: String = DateUtils.todayIso()) {
        val yesterdayStr = try {
            LocalDate.parse(effectiveDate, DateUtils.isoDateFormatter).minusDays(1).format(DateUtils.isoDateFormatter)
        } catch (e: Exception) { "" }
        if (yesterdayStr.isNotBlank()) {
            timetableDao.setEntryEndDate(id, yesterdayStr)
            val futureSessions = attendanceDao.getSessionsForTimetableEntryFromDate(id, effectiveDate)
            for (session in futureSessions) {
                val sessionUnits = attendanceDao.getUnitsForSession(session.id)
                val isMarked = sessionUnits.any { it.status != com.agupta07505.attendsmartly.domain.model.AttendanceStatus.UNMARKED.name }
                if (!isMarked && !session.isRescheduled) {
                    attendanceDao.deleteSessionById(session.id)
                }
            }
        } else {
            deleteTimetableEntry(id)
        }
    }

    suspend fun importParsedTimetable(
        items: List<com.agupta07505.attendsmartly.domain.model.ParsedTimetableItem>,
        replaceExisting: Boolean = false,
        effectiveStartDate: String = DateUtils.todayIso()
    ) {
        if (replaceExisting) {
            val yesterdayStr = try {
                LocalDate.parse(effectiveStartDate, DateUtils.isoDateFormatter).minusDays(1).format(DateUtils.isoDateFormatter)
            } catch (e: Exception) { "" }
            if (yesterdayStr.isNotBlank()) {
                timetableDao.endActiveTimetableEntries(yesterdayStr)
            } else {
                timetableDao.deleteAllEntries()
            }
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
                attendanceUnitCount = item.attendanceUnitCount.coerceAtLeast(1),
                startDate = effectiveStartDate
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

    suspend fun addExtraClassSession(
        subjectId: Long,
        dateIso: String,
        startTime: String,
        endTime: String,
        unitCount: Int,
        notes: String = ""
    ): Long {
        val session = AttendanceSessionEntity(
            subjectId = subjectId,
            timetableEntryId = null,
            sessionDate = dateIso,
            startTime = startTime,
            endTime = endTime,
            expectedUnitCount = unitCount,
            notes = if (notes.isNotBlank()) notes else "Extra class",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val units = (0 until unitCount).map { index ->
            AttendanceUnitEntity(
                sessionId = 0,
                unitIndex = index,
                status = AttendanceStatus.UNMARKED.name
            )
        }
        return attendanceDao.createOrUpdateSessionWithUnits(session, units)
    }

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

    suspend fun rescheduleClassSession(
        originalDate: String,
        timetableEntryId: Long?,
        subjectId: Long,
        originalTime: String,
        newDate: String,
        newStartTime: String,
        newEndTime: String,
        unitCount: Int,
        reason: String = ""
    ): Pair<Long, Long> {
        val existingOriginal = if (timetableEntryId != null) {
            attendanceDao.getSessionForTimetableAndDate(timetableEntryId, originalDate)
        } else {
            attendanceDao.getSessionForSubjectAndDate(subjectId, originalDate)
        }

        val originalSession = existingOriginal?.copy(
            rescheduledToDate = newDate,
            rescheduledToTime = newStartTime,
            rescheduledReason = reason,
            notes = if (reason.isNotBlank()) "Rescheduled to $newDate $newStartTime: $reason" else "Rescheduled to $newDate $newStartTime",
            updatedAt = System.currentTimeMillis()
        ) ?: AttendanceSessionEntity(
            subjectId = subjectId,
            timetableEntryId = timetableEntryId,
            sessionDate = originalDate,
            startTime = originalTime,
            endTime = originalTime,
            expectedUnitCount = unitCount,
            rescheduledToDate = newDate,
            rescheduledToTime = newStartTime,
            rescheduledReason = reason,
            notes = if (reason.isNotBlank()) "Rescheduled to $newDate $newStartTime: $reason" else "Rescheduled to $newDate $newStartTime"
        )

        val origUnits = (0 until unitCount).map { idx ->
            AttendanceUnitEntity(
                sessionId = originalSession.id,
                unitIndex = idx,
                status = AttendanceStatus.CANCELLED.name
            )
        }
        val origSessionId = attendanceDao.createOrUpdateSessionWithUnits(originalSession, origUnits)

        val targetSession = AttendanceSessionEntity(
            subjectId = subjectId,
            timetableEntryId = null,
            sessionDate = newDate,
            startTime = newStartTime,
            endTime = newEndTime,
            expectedUnitCount = unitCount,
            isRescheduled = true,
            originalDate = originalDate,
            originalTime = originalTime,
            rescheduledReason = reason,
            notes = if (reason.isNotBlank()) "Rescheduled from $originalDate $originalTime: $reason" else "Rescheduled from $originalDate $originalTime"
        )

        val targetUnits = (0 until unitCount).map { idx ->
            AttendanceUnitEntity(
                sessionId = 0,
                unitIndex = idx,
                status = AttendanceStatus.UNMARKED.name
            )
        }
        val targetSessionId = attendanceDao.createOrUpdateSessionWithUnits(targetSession, targetUnits)

        return Pair(origSessionId, targetSessionId)
    }

    suspend fun cancelReschedule(session: AttendanceSessionEntity) {
        if (session.isRescheduled) {
            // Case 1: Called on the newly rescheduled target session (e.g. on newDate)
            val origDate = session.originalDate
            if (origDate != null) {
                val origSessions = attendanceDao.getSessionsForSubject(session.subjectId).first()
                val originalSession = origSessions.find {
                    it.sessionDate == origDate &&
                    (it.rescheduledToDate == session.sessionDate || (session.timetableEntryId != null && it.timetableEntryId == session.timetableEntryId))
                } ?: (if (session.timetableEntryId != null) {
                    attendanceDao.getSessionForTimetableAndDate(session.timetableEntryId, origDate)
                } else {
                    attendanceDao.getSessionForSubjectAndDate(session.subjectId, origDate)
                })

                if (originalSession != null) {
                    val updatedOriginal = originalSession.copy(
                        rescheduledToDate = null,
                        rescheduledToTime = null,
                        rescheduledReason = "",
                        notes = "",
                        updatedAt = System.currentTimeMillis()
                    )
                    val units = (0 until originalSession.expectedUnitCount).map { idx ->
                        AttendanceUnitEntity(
                            sessionId = originalSession.id,
                            unitIndex = idx,
                            status = AttendanceStatus.UNMARKED.name
                        )
                    }
                    attendanceDao.createOrUpdateSessionWithUnits(updatedOriginal, units)
                }
            }
            attendanceDao.deleteSessionById(session.id)
        } else if (session.rescheduledToDate != null) {
            // Case 2: Called on the original session (e.g. on originalDate where card shows 'Rescheduled to newDate')
            val targetDate = session.rescheduledToDate
            if (targetDate != null) {
                val subjectSessions = attendanceDao.getSessionsForSubject(session.subjectId).first()
                val targetSession = subjectSessions.find {
                    it.sessionDate == targetDate && it.isRescheduled && it.originalDate == session.sessionDate
                }
                if (targetSession != null) {
                    attendanceDao.deleteSessionById(targetSession.id)
                }
            }

            val updatedOriginal = session.copy(
                rescheduledToDate = null,
                rescheduledToTime = null,
                rescheduledReason = "",
                notes = "",
                updatedAt = System.currentTimeMillis()
            )
            val units = (0 until session.expectedUnitCount).map { idx ->
                AttendanceUnitEntity(
                    sessionId = session.id,
                    unitIndex = idx,
                    status = AttendanceStatus.UNMARKED.name
                )
            }
            attendanceDao.createOrUpdateSessionWithUnits(updatedOriginal, units)
        }
    }

    // Holidays
    val allHolidays: Flow<List<HolidayEntity>> = holidayDao.getAllHolidays()
    suspend fun getHolidayByDate(dateStr: String): HolidayEntity? = holidayDao.getHolidayByDate(dateStr)
    suspend fun insertHoliday(holiday: HolidayEntity): Long = holidayDao.insertHoliday(holiday)
    suspend fun deleteHoliday(id: Long) = holidayDao.deleteHolidayById(id)

    suspend fun clearAllData() {
        val subs = subjectDao.getAllSubjects().first()
        for (s in subs) subjectDao.deleteSubject(s)
        timetableDao.deleteAllEntries()
        val sesss = attendanceDao.getAllSessions().first()
        for (se in sesss) attendanceDao.deleteSessionById(se.id)
        val holidays = holidayDao.getAllHolidays().first()
        for (h in holidays) holidayDao.deleteHolidayById(h.id)
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
