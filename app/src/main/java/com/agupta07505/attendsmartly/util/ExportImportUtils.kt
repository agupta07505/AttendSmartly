/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.util

import android.content.Context
import android.net.Uri
import com.agupta07505.attendsmartly.data.local.entity.*
import com.agupta07505.attendsmartly.data.repository.AttendSmartlyRepository
import com.agupta07505.attendsmartly.domain.calculator.AttendanceCalculator
import com.agupta07505.attendsmartly.domain.model.AttendanceStatus
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate

data class AttendSmartlyBackup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val subjects: List<SubjectEntity> = emptyList(),
    val timetableEntries: List<TimetableEntryEntity> = emptyList(),
    val sessions: List<AttendanceSessionEntity> = emptyList(),
    val units: List<AttendanceUnitEntity> = emptyList(),
    val holidays: List<HolidayEntity> = emptyList()
)

data class TimetableSharePackage(
    val format: String = "AttendSmartly_TIMETABLE_V1",
    val exportedAt: Long = System.currentTimeMillis(),
    val subjects: List<SubjectEntity> = emptyList(),
    val timetableEntries: List<TimetableEntryEntity> = emptyList()
)

object ExportImportUtils {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create()

    fun sanitizeSubject(sub: SubjectEntity): SubjectEntity {
        val name = sub.name.trim()
        val code = sub.code?.trim() ?: ""
        val type = if (!sub.type.isNullOrBlank()) sub.type.trim() else "Lecture"
        val teacher = sub.teacherName?.trim() ?: ""
        val room = sub.room?.trim() ?: ""
        val color = if (sub.colorValue != 0L) sub.colorValue else 0xFF2196F3L
        val icon = if (!sub.iconName.isNullOrBlank()) sub.iconName.trim() else "Book"
        val dur = if (sub.defaultSessionDurationMinutes > 0) sub.defaultSessionDurationMinutes else 60
        val unitMins = if (sub.attendanceUnitMinutes > 0) sub.attendanceUnitMinutes else 60
        val defaultUnits = if (sub.defaultAttendanceUnits > 0) sub.defaultAttendanceUnits else 1
        val target = if (sub.targetPercentage > 0.0) sub.targetPercentage else 75.0
        val reminder = if (sub.defaultReminderMinutes >= 0) sub.defaultReminderMinutes else 10
        val notes = sub.notes?.trim() ?: ""

        return sub.copy(
            id = sub.id,
            name = name,
            code = code,
            type = type,
            teacherName = teacher,
            room = room,
            colorValue = color,
            iconName = icon,
            defaultSessionDurationMinutes = dur,
            attendanceUnitMinutes = unitMins,
            defaultAttendanceUnits = defaultUnits,
            targetPercentage = target,
            defaultReminderMinutes = reminder,
            notes = notes,
            isArchived = sub.isArchived,
            createdAt = if (sub.createdAt > 0) sub.createdAt else System.currentTimeMillis(),
            updatedAt = if (sub.updatedAt > 0) sub.updatedAt else System.currentTimeMillis()
        )
    }

    fun sanitizeTimetableEntry(entry: TimetableEntryEntity, defaultStartDate: String = ""): TimetableEntryEntity {
        return entry.copy(
            id = entry.id,
            subjectId = entry.subjectId,
            dayOfWeek = entry.dayOfWeek.coerceIn(1, 7),
            startTime = if (!entry.startTime.isNullOrBlank()) entry.startTime.trim() else "09:00",
            endTime = if (!entry.endTime.isNullOrBlank()) entry.endTime.trim() else "10:00",
            roomOverride = entry.roomOverride?.trim() ?: "",
            teacherOverride = entry.teacherOverride?.trim() ?: "",
            attendanceUnitCount = if (entry.attendanceUnitCount > 0) entry.attendanceUnitCount else 1,
            reminderMinutes = if (entry.reminderMinutes >= 0) entry.reminderMinutes else 10,
            startDate = if (!entry.startDate.isNullOrBlank()) entry.startDate.trim() else defaultStartDate,
            endDate = entry.endDate?.trim() ?: "",
            repeatType = if (!entry.repeatType.isNullOrBlank()) entry.repeatType.trim() else "WEEKLY",
            notes = entry.notes?.trim() ?: "",
            isActive = true, // Ensure timetable entry is active
            createdAt = if (entry.createdAt > 0) entry.createdAt else System.currentTimeMillis(),
            updatedAt = if (entry.updatedAt > 0) entry.updatedAt else System.currentTimeMillis()
        )
    }

    fun sanitizeSession(session: AttendanceSessionEntity): AttendanceSessionEntity {
        return session.copy(
            id = session.id,
            subjectId = session.subjectId,
            timetableEntryId = session.timetableEntryId,
            sessionDate = if (!session.sessionDate.isNullOrBlank()) session.sessionDate.trim() else DateUtils.todayIso(),
            startTime = if (!session.startTime.isNullOrBlank()) session.startTime.trim() else "09:00",
            endTime = if (!session.endTime.isNullOrBlank()) session.endTime.trim() else "10:00",
            expectedUnitCount = if (session.expectedUnitCount > 0) session.expectedUnitCount else 1,
            notes = session.notes?.trim() ?: "",
            isRescheduled = session.isRescheduled,
            originalDate = session.originalDate,
            originalTime = session.originalTime,
            rescheduledToDate = session.rescheduledToDate,
            rescheduledToTime = session.rescheduledToTime,
            rescheduledReason = session.rescheduledReason?.trim() ?: "",
            createdAt = if (session.createdAt > 0) session.createdAt else System.currentTimeMillis(),
            updatedAt = if (session.updatedAt > 0) session.updatedAt else System.currentTimeMillis()
        )
    }

    fun sanitizeUnit(unit: AttendanceUnitEntity, sessionId: Long): AttendanceUnitEntity {
        val rawStatus = unit.status?.trim()?.uppercase() ?: ""
        val status = when (rawStatus) {
            "PRESENT" -> "PRESENT"
            "ABSENT", "BUNKED" -> "ABSENT"
            "CANCELLED" -> "CANCELLED"
            else -> "UNMARKED"
        }
        return unit.copy(
            id = 0,
            sessionId = sessionId,
            unitIndex = unit.unitIndex.coerceAtLeast(0),
            status = status,
            markedAt = if (unit.markedAt > 0) unit.markedAt else System.currentTimeMillis(),
            updatedAt = if (unit.updatedAt > 0) unit.updatedAt else System.currentTimeMillis()
        )
    }

    suspend fun exportTimetableToJson(repository: AttendSmartlyRepository): String {
        val entries = repository.allActiveTimetableEntries.first()
        val subjectIds = entries.map { it.subjectId }.toSet()
        val allSubjects = repository.allSubjects.first()
        val relevantSubjects = allSubjects.filter { subjectIds.contains(it.id) }

        val pkg = TimetableSharePackage(
            format = "AttendSmartly_TIMETABLE_V1",
            exportedAt = System.currentTimeMillis(),
            subjects = relevantSubjects,
            timetableEntries = entries
        )
        return gson.toJson(pkg)
    }

    suspend fun importTimetableFromJson(
        jsonString: String,
        repository: AttendSmartlyRepository,
        effectiveStartDate: String = DateUtils.todayIso(),
        replaceExisting: Boolean = false
    ): Pair<Boolean, String> {
        return try {
            val jsonElement = JsonParser.parseString(jsonString)
            if (!jsonElement.isJsonObject && !jsonElement.isJsonArray) {
                return Pair(false, "Invalid JSON format.")
            }

            val subjects = mutableListOf<SubjectEntity>()
            val timetableEntries = mutableListOf<TimetableEntryEntity>()
            val sessions = mutableListOf<AttendanceSessionEntity>()
            val units = mutableListOf<AttendanceUnitEntity>()
            val holidays = mutableListOf<HolidayEntity>()

            if (jsonElement.isJsonObject) {
                val obj = jsonElement.asJsonObject

                // Parse subjects
                val subjectsJson = obj.get("subjects") ?: obj.get("subjectList")
                if (subjectsJson != null && subjectsJson.isJsonArray) {
                    val listType = object : TypeToken<List<SubjectEntity>>() {}.type
                    val parsedSubjects: List<SubjectEntity>? = gson.fromJson(subjectsJson, listType)
                    parsedSubjects?.let { subjects.addAll(it) }
                }

                // Parse timetable entries
                val timetableJson = obj.get("timetableEntries") ?: obj.get("timetable") ?: obj.get("entries")
                if (timetableJson != null && timetableJson.isJsonArray) {
                    val listType = object : TypeToken<List<TimetableEntryEntity>>() {}.type
                    val parsedEntries: List<TimetableEntryEntity>? = gson.fromJson(timetableJson, listType)
                    parsedEntries?.let { timetableEntries.addAll(it) }
                }

                // Parse sessions (if importing a full data package or legacy backup)
                val sessionsJson = obj.get("sessions") ?: obj.get("attendanceSessions") ?: obj.get("attendance")
                if (sessionsJson != null && sessionsJson.isJsonArray) {
                    val listType = object : TypeToken<List<AttendanceSessionEntity>>() {}.type
                    val parsedSessions: List<AttendanceSessionEntity>? = gson.fromJson(sessionsJson, listType)
                    parsedSessions?.let { sessions.addAll(it) }
                }

                // Parse units
                val unitsJson = obj.get("units") ?: obj.get("attendanceUnits")
                if (unitsJson != null && unitsJson.isJsonArray) {
                    val listType = object : TypeToken<List<AttendanceUnitEntity>>() {}.type
                    val parsedUnits: List<AttendanceUnitEntity>? = gson.fromJson(unitsJson, listType)
                    parsedUnits?.let { units.addAll(it) }
                }

                // Parse holidays
                val holidaysJson = obj.get("holidays")
                if (holidaysJson != null && holidaysJson.isJsonArray) {
                    val listType = object : TypeToken<List<HolidayEntity>>() {}.type
                    val parsedHolidays: List<HolidayEntity>? = gson.fromJson(holidaysJson, listType)
                    parsedHolidays?.let { holidays.addAll(it) }
                }
            } else if (jsonElement.isJsonArray) {
                // If pure array of timetable entries
                val listType = object : TypeToken<List<TimetableEntryEntity>>() {}.type
                val parsedEntries: List<TimetableEntryEntity>? = gson.fromJson(jsonElement, listType)
                parsedEntries?.let { timetableEntries.addAll(it) }
            }

            if (timetableEntries.isEmpty() && subjects.isEmpty() && sessions.isEmpty()) {
                return Pair(false, "No subjects, timetable entries, or attendance data found in JSON.")
            }

            if (replaceExisting) {
                val yesterdayStr = try {
                    LocalDate.parse(effectiveStartDate, DateUtils.isoDateFormatter).minusDays(1).format(DateUtils.isoDateFormatter)
                } catch (e: Exception) { "" }
                if (yesterdayStr.isNotBlank()) {
                    repository.allActiveTimetableEntries.first().forEach {
                        repository.retireTimetableEntry(it.id, effectiveStartDate)
                    }
                }
            }

            // 1. Map and insert subjects
            val existingSubjects = repository.allSubjects.first().associateBy { it.name.trim().lowercase() }
            val subjectIdMap = mutableMapOf<Long, Long>()

            for (rawSub in subjects) {
                val sub = sanitizeSubject(rawSub)
                val existing = existingSubjects[sub.name.lowercase()]
                if (existing != null) {
                    subjectIdMap[rawSub.id] = existing.id
                    // Update any empty fields on existing subject if backup has values
                    if ((existing.teacherName.isBlank() && sub.teacherName.isNotBlank()) ||
                        (existing.room.isBlank() && sub.room.isNotBlank()) ||
                        (existing.code.isBlank() && sub.code.isNotBlank())
                    ) {
                        repository.updateSubject(
                            existing.copy(
                                teacherName = existing.teacherName.ifBlank { sub.teacherName },
                                room = existing.room.ifBlank { sub.room },
                                code = existing.code.ifBlank { sub.code }
                            )
                        )
                    }
                } else {
                    val newId = repository.insertSubject(sub.copy(id = 0))
                    subjectIdMap[rawSub.id] = newId
                }
            }

            // 2. Map and insert timetable entries
            val timetableIdMap = mutableMapOf<Long, Long>()
            var importedTimetableCount = 0

            for (rawEntry in timetableEntries) {
                val mappedSubjectId = subjectIdMap[rawEntry.subjectId] ?: rawEntry.subjectId
                val entryStartDate = if (rawEntry.startDate.isNotBlank()) rawEntry.startDate.trim()
                else if (replaceExisting) effectiveStartDate
                else ""
                val entry = sanitizeTimetableEntry(rawEntry, entryStartDate).copy(
                    id = 0,
                    subjectId = mappedSubjectId,
                    startDate = entryStartDate,
                    endDate = rawEntry.endDate?.trim() ?: "",
                    createdAt = if (rawEntry.createdAt > 0) rawEntry.createdAt else System.currentTimeMillis()
                )
                val newId = repository.insertTimetableEntry(entry)
                timetableIdMap[rawEntry.id] = newId
                importedTimetableCount++
            }

            // 3. Map and insert sessions & units
            var importedSessionCount = 0
            for (rawSession in sessions) {
                val mappedSubjectId = subjectIdMap[rawSession.subjectId] ?: rawSession.subjectId
                val mappedTimetableId = if (rawSession.timetableEntryId != null) {
                    timetableIdMap[rawSession.timetableEntryId] ?: rawSession.timetableEntryId
                } else null

                val session = sanitizeSession(rawSession).copy(
                    id = 0,
                    subjectId = mappedSubjectId,
                    timetableEntryId = mappedTimetableId
                )

                val sessionUnits = units.filter { it.sessionId == rawSession.id }
                val preparedUnits = if (sessionUnits.isNotEmpty()) {
                    sessionUnits.map { sanitizeUnit(it, 0) }
                } else {
                    List(session.expectedUnitCount.coerceAtLeast(1)) { idx ->
                        AttendanceUnitEntity(
                            sessionId = 0,
                            unitIndex = idx,
                            status = AttendanceStatus.PRESENT.name
                        )
                    }
                }

                repository.createOrUpdateSessionWithUnits(session, preparedUnits)
                importedSessionCount++
            }

            // 4. Insert holidays
            for (rawHoliday in holidays) {
                val holiday = rawHoliday.copy(id = 0, notes = rawHoliday.notes ?: "")
                repository.insertHoliday(holiday)
            }

            val msg = buildString {
                if (importedTimetableCount > 0 && importedSessionCount > 0) {
                    append("Successfully imported $importedTimetableCount timetable entries and $importedSessionCount attendance sessions!")
                } else if (importedTimetableCount > 0) {
                    append("Successfully imported $importedTimetableCount timetable entries!")
                } else if (importedSessionCount > 0) {
                    append("Successfully imported $importedSessionCount attendance sessions!")
                } else {
                    append("Successfully imported ${subjects.size} subjects!")
                }
            }

            Pair(true, msg)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Failed to import timetable: ${e.message}")
        }
    }

    suspend fun exportToJson(repository: AttendSmartlyRepository): String {
        val backup = AttendSmartlyBackup(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            subjects = repository.allSubjects.first(),
            timetableEntries = repository.allTimetableEntries.first(),
            sessions = repository.allSessions.first(),
            units = repository.allUnits.first(),
            holidays = repository.allHolidays.first()
        )
        return gson.toJson(backup)
    }

    suspend fun restoreFromJson(
        jsonString: String,
        repository: AttendSmartlyRepository
    ): Boolean {
        return try {
            val result = importTimetableFromJson(
                jsonString = jsonString,
                repository = repository,
                effectiveStartDate = DateUtils.todayIso(),
                replaceExisting = false
            )
            result.first
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportToCsv(repository: AttendSmartlyRepository): String {
        val subjects = repository.allSubjects.first()
        val allSessions = repository.allSessions.first()
        val allUnits = repository.allUnits.first()

        val sb = StringBuilder()
        sb.append("Subject Name,Subject Code,Subject Type,Conducted Units,Present Units,Absent Units,Cancelled Units,Current Attendance %,Target %,Status\n")

        for (subject in subjects) {
            val subjectSessions = allSessions.filter { it.subjectId == subject.id }
            val sessionIds = subjectSessions.map { it.id }.toSet()
            val subjectUnits = allUnits.filter { sessionIds.contains(it.sessionId) }
            val unitStatuses = subjectUnits.mapNotNull {
                try { AttendanceStatus.valueOf(it.status) } catch (e: Exception) { null }
            }

            val summary = AttendanceCalculator.calculate(unitStatuses, subject.targetPercentage)

            sb.append("\"${subject.name}\",")
            sb.append("\"${subject.code}\",")
            sb.append("\"${subject.type}\",")
            sb.append("${summary.totalConductedUnits},")
            sb.append("${summary.presentUnits},")
            sb.append("${summary.absentUnits},")
            sb.append("${summary.cancelledUnits},")
            sb.append("${String.format("%.2f", summary.percentage)}%,")
            sb.append("${subject.targetPercentage}%,")
            sb.append("\"${summary.statusMessage}\"\n")
        }

        return sb.toString()
    }

    suspend fun exportDataToJson(context: Context, repository: AttendSmartlyRepository, uri: Uri): Boolean {
        val json = exportToJson(repository)
        return writeTextToUri(context, uri, json)
    }

    suspend fun importDataFromJson(context: Context, repository: AttendSmartlyRepository, uri: Uri): Boolean {
        val json = readTextFromUri(context, uri) ?: return false
        return restoreFromJson(json, repository)
    }

    suspend fun exportAttendanceCsv(context: Context, repository: AttendSmartlyRepository, uri: Uri): Boolean {
        val csv = exportToCsv(repository)
        return writeTextToUri(context, uri, csv)
    }

    fun writeTextToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun readTextFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
