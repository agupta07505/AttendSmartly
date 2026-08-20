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
        .setLenient()
        .setPrettyPrinting()
        .serializeNulls()
        .create()

    fun sanitizeSubject(sub: SubjectEntity): SubjectEntity {
        val name = if (!sub.name.isNullOrBlank()) sub.name.trim() else "Unnamed Subject"
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
        val startTime = if (!entry.startTime.isNullOrBlank()) entry.startTime.trim() else "09:00"
        val endTime = if (!entry.endTime.isNullOrBlank()) entry.endTime.trim() else "10:00"
        val startDate = if (!entry.startDate.isNullOrBlank()) entry.startDate.trim() else defaultStartDate
        val endDate = entry.endDate?.trim() ?: ""
        val repeatType = if (!entry.repeatType.isNullOrBlank()) entry.repeatType.trim() else "WEEKLY"
        val roomOverride = entry.roomOverride?.trim() ?: ""
        val teacherOverride = entry.teacherOverride?.trim() ?: ""
        val notes = entry.notes?.trim() ?: ""
        val units = if (entry.attendanceUnitCount > 0) entry.attendanceUnitCount else 1
        val reminder = if (entry.reminderMinutes >= 0) entry.reminderMinutes else 10
        val day = if (entry.dayOfWeek in 1..7) entry.dayOfWeek else 1
        val isActive = entry.isActive || (endDate.isBlank() || endDate >= DateUtils.todayIso())

        return entry.copy(
            id = entry.id,
            subjectId = entry.subjectId,
            dayOfWeek = day,
            startTime = startTime,
            endTime = endTime,
            roomOverride = roomOverride,
            teacherOverride = teacherOverride,
            attendanceUnitCount = units,
            reminderMinutes = reminder,
            startDate = startDate,
            endDate = endDate,
            repeatType = repeatType,
            notes = notes,
            isActive = isActive,
            createdAt = if (entry.createdAt > 0) entry.createdAt else System.currentTimeMillis(),
            updatedAt = if (entry.updatedAt > 0) entry.updatedAt else System.currentTimeMillis()
        )
    }

    fun sanitizeSession(session: AttendanceSessionEntity): AttendanceSessionEntity {
        val sessionDate = if (!session.sessionDate.isNullOrBlank()) session.sessionDate.trim() else DateUtils.todayIso()
        val startTime = if (!session.startTime.isNullOrBlank()) session.startTime.trim() else "09:00"
        val endTime = if (!session.endTime.isNullOrBlank()) session.endTime.trim() else "10:00"
        val expectedUnits = if (session.expectedUnitCount > 0) session.expectedUnitCount else 1
        val notes = session.notes?.trim() ?: ""
        val rescheduledReason = session.rescheduledReason?.trim() ?: ""
        val originalDate = session.originalDate?.trim()?.ifBlank { null }
        val originalTime = session.originalTime?.trim()?.ifBlank { null }
        val rescheduledToDate = session.rescheduledToDate?.trim()?.ifBlank { null }
        val rescheduledToTime = session.rescheduledToTime?.trim()?.ifBlank { null }

        return session.copy(
            id = session.id,
            subjectId = session.subjectId,
            timetableEntryId = session.timetableEntryId,
            sessionDate = sessionDate,
            startTime = startTime,
            endTime = endTime,
            expectedUnitCount = expectedUnits,
            notes = notes,
            isRescheduled = session.isRescheduled,
            originalDate = originalDate,
            originalTime = originalTime,
            rescheduledToDate = rescheduledToDate,
            rescheduledToTime = rescheduledToTime,
            rescheduledReason = rescheduledReason,
            createdAt = if (session.createdAt > 0) session.createdAt else System.currentTimeMillis(),
            updatedAt = if (session.updatedAt > 0) session.updatedAt else System.currentTimeMillis()
        )
    }

    fun sanitizeUnit(unit: AttendanceUnitEntity, sessionId: Long): AttendanceUnitEntity {
        val rawStatus = unit.status?.trim()?.uppercase() ?: ""
        val status = when (rawStatus) {
            "PRESENT", "ATTENDED", "YES" -> "PRESENT"
            "ABSENT", "BUNKED", "MISSED", "NO" -> "ABSENT"
            "CANCELLED", "CANCELED", "HOLIDAY" -> "CANCELLED"
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

    fun sanitizeHoliday(holiday: HolidayEntity): HolidayEntity {
        val date = if (!holiday.date.isNullOrBlank()) holiday.date.trim() else DateUtils.todayIso()
        val title = if (!holiday.title.isNullOrBlank()) holiday.title.trim() else "Holiday"
        val notes = holiday.notes?.trim() ?: ""
        return holiday.copy(
            id = 0,
            date = date,
            title = title,
            notes = notes
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

    fun autoRepairJson(raw: String): String {
        var text = raw.trim().removePrefix("\uFEFF")

        // Pattern 1: Empty envelope pasted on top of "expectedUnitCount": 1 or similar session field
        // E.g.: "}\s*ctedUnitCount": 1  or  "}\s*\"expectedUnitCount"
        if (text.contains(Regex("""\}\s*\"?c?t?e?d?UnitCount\"?""", RegexOption.IGNORE_CASE))) {
            text = text.replace(
                Regex("""\{\s*\"exportedAt\"[\s\S]*?\}\s*\"?c?t?e?d?UnitCount\"?""", RegexOption.IGNORE_CASE),
                "{\n  \"exportedAt\": ${System.currentTimeMillis()},\n  \"holidays\": [],\n  \"sessions\": [\n    {\n      \"expectedUnitCount\""
            )
            text = text.replace(
                Regex("""\}\s*\"?c?t?e?d?UnitCount\"?""", RegexOption.IGNORE_CASE),
                ",\n  \"sessions\": [\n    {\n      \"expectedUnitCount\""
            )
        }

        // Pattern 2: Multiple root objects or envelope followed by session id:
        if (text.contains(Regex("""\}\s*\"id\":\s*\d+,\s*\"notes\"""", RegexOption.IGNORE_CASE))) {
            text = text.replace(
                Regex("""\{\s*\"exportedAt\"[\s\S]*?\}\s*\"id\":\s*(\d+)""", RegexOption.IGNORE_CASE),
                "{\n  \"exportedAt\": ${System.currentTimeMillis()},\n  \"holidays\": [],\n  \"sessions\": [\n    {\n      \"id\": $1"
            )
        }

        return text
    }

    fun extractBalancedSection(text: String, startIdx: Int, openChar: Char, closeChar: Char): String? {
        if (startIdx < 0 || startIdx >= text.length || text[startIdx] != openChar) return null
        var depth = 0
        var inString = false
        var isEscaped = false

        for (i in startIdx until text.length) {
            val c = text[i]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (c == '\\') {
                isEscaped = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == openChar) depth++
                else if (c == closeChar) {
                    depth--
                    if (depth == 0) {
                        return text.substring(startIdx, i + 1)
                    }
                }
            }
        }
        return text.substring(startIdx) + closeChar
    }

    fun extractObjectsFromBlock(block: String): List<String> {
        val list = mutableListOf<String>()
        var i = 0
        while (i < block.length) {
            val start = block.indexOf('{', i)
            if (start == -1) break
            val obj = extractBalancedSection(block, start, '{', '}')
            if (obj != null) {
                list.add(obj)
                i = start + obj.length
            } else {
                i = start + 1
            }
        }
        return list
    }

    fun extractObjectsContaining(text: String, requiredKeys: List<String>): List<String> {
        val allObjs = extractObjectsFromBlock(text)
        return allObjs.filter { obj ->
            requiredKeys.all { key -> obj.contains("\"$key\"") }
        }
    }

    private inline fun <reified T> tryParseArray(arrayJson: String): List<T> {
        val listType = object : TypeToken<List<T>>() {}.type
        return try {
            val parsed: List<T>? = gson.fromJson(arrayJson, listType)
            parsed?.filterNotNull() ?: emptyList()
        } catch (e: Exception) {
            val results = mutableListOf<T>()
            val objects = extractObjectsFromBlock(arrayJson)
            for (objStr in objects) {
                try {
                    val obj = gson.fromJson(objStr, T::class.java)
                    if (obj != null) results.add(obj)
                } catch (_: Exception) {}
            }
            results
        }
    }

    fun extractEntitiesFromMalformedJson(
        rawText: String,
        outSubjects: MutableList<SubjectEntity>,
        outTimetable: MutableList<TimetableEntryEntity>,
        outSessions: MutableList<AttendanceSessionEntity>,
        outUnits: MutableList<AttendanceUnitEntity>,
        outHolidays: MutableList<HolidayEntity>
    ) {
        val arrayPatterns = listOf(
            listOf("subjects", "subjectList", "allSubjects", "subjectsList") to "subjects",
            listOf("timetableEntries", "timetable_entries", "timetable", "entries", "allTimetableEntries", "schedule", "timetableList") to "timetable",
            listOf("sessions", "attendanceSessions", "attendance_sessions", "attendance", "allSessions", "sessionList", "session_list") to "sessions",
            listOf("units", "attendanceUnits", "attendance_units", "allUnits", "unitList", "unit_list") to "units",
            listOf("holidays", "holidayList", "holiday_list", "allHolidays") to "holidays"
        )

        for ((keys, type) in arrayPatterns) {
            val keyPattern = Regex("\"(?:${keys.joinToString("|")})\"\\s*:\\s*\\[", RegexOption.IGNORE_CASE)
            val match = keyPattern.find(rawText)
            if (match != null) {
                val arrayStart = match.range.last
                val arrayContent = extractBalancedSection(rawText, arrayStart, '[', ']')
                if (arrayContent != null) {
                    when (type) {
                        "subjects" -> outSubjects.addAll(tryParseArray<SubjectEntity>(arrayContent))
                        "timetable" -> outTimetable.addAll(tryParseArray<TimetableEntryEntity>(arrayContent))
                        "sessions" -> outSessions.addAll(tryParseArray<AttendanceSessionEntity>(arrayContent))
                        "units" -> outUnits.addAll(tryParseArray<AttendanceUnitEntity>(arrayContent))
                        "holidays" -> outHolidays.addAll(tryParseArray<HolidayEntity>(arrayContent))
                    }
                }
            }
        }

        if (outSessions.isEmpty()) {
            val sessionObjects = extractObjectsContaining(rawText, listOf("sessionDate", "subjectId"))
            for (objStr in sessionObjects) {
                try {
                    val s = gson.fromJson(objStr, AttendanceSessionEntity::class.java)
                    if (s != null && s.sessionDate.isNotBlank()) outSessions.add(s)
                } catch (_: Exception) {}
            }
        }
        if (outSubjects.isEmpty()) {
            val subjectObjects = extractObjectsContaining(rawText, listOf("targetPercentage", "name"))
            for (objStr in subjectObjects) {
                try {
                    val s = gson.fromJson(objStr, SubjectEntity::class.java)
                    if (s != null && s.name.isNotBlank()) outSubjects.add(s)
                } catch (_: Exception) {}
            }
        }
        if (outTimetable.isEmpty()) {
            val ttObjects = extractObjectsContaining(rawText, listOf("dayOfWeek", "subjectId", "startTime"))
            for (objStr in ttObjects) {
                try {
                    val tt = gson.fromJson(objStr, TimetableEntryEntity::class.java)
                    if (tt != null && tt.dayOfWeek in 1..7) outTimetable.add(tt)
                } catch (_: Exception) {}
            }
        }
        if (outUnits.isEmpty()) {
            val unitObjects = extractObjectsContaining(rawText, listOf("sessionId", "unitIndex", "status"))
            for (objStr in unitObjects) {
                try {
                    val u = gson.fromJson(objStr, AttendanceUnitEntity::class.java)
                    if (u != null) outUnits.add(u)
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun importTimetableFromJson(
        jsonString: String,
        repository: AttendSmartlyRepository,
        effectiveStartDate: String = DateUtils.todayIso(),
        replaceExisting: Boolean = false
    ): Pair<Boolean, String> {
        return try {
            val raw = jsonString.trim().removePrefix("\uFEFF")
            if (raw.isBlank()) {
                return Pair(false, "Backup file is empty.")
            }

            val cleanJson = autoRepairJson(raw)

            val subjects = mutableListOf<SubjectEntity>()
            val timetableEntries = mutableListOf<TimetableEntryEntity>()
            val sessions = mutableListOf<AttendanceSessionEntity>()
            val units = mutableListOf<AttendanceUnitEntity>()
            val holidays = mutableListOf<HolidayEntity>()

            try {
                val jsonElement = try {
                    JsonParser.parseString(cleanJson)
                } catch (_: Exception) {
                    val reader = com.google.gson.stream.JsonReader(java.io.StringReader(cleanJson))
                    reader.isLenient = true
                    JsonParser.parseReader(reader)
                }

                if (jsonElement.isJsonObject) {
                    var obj = jsonElement.asJsonObject
                    // Unwrap top-level envelope if present
                    if (obj.has("data") && obj.get("data").isJsonObject) {
                        obj = obj.getAsJsonObject("data")
                    } else if (obj.has("backup") && obj.get("backup").isJsonObject) {
                        obj = obj.getAsJsonObject("backup")
                    } else if (obj.has("payload") && obj.get("payload").isJsonObject) {
                        obj = obj.getAsJsonObject("payload")
                    }

                    fun findArray(vararg keys: String): JsonArray? {
                        for (k in keys) {
                            val elem = obj.get(k)
                            if (elem != null && elem.isJsonArray) {
                                return elem.asJsonArray
                            }
                        }
                        return null
                    }

                    // Parse subjects
                    val subjectsJson = findArray("subjects", "subjectList", "subject_list", "allSubjects", "subjectsList")
                    if (subjectsJson != null) {
                        val listType = object : TypeToken<List<SubjectEntity>>() {}.type
                        val parsedSubjects: List<SubjectEntity>? = gson.fromJson(subjectsJson, listType)
                        parsedSubjects?.let { subjects.addAll(it) }
                    }

                    // Parse timetable entries
                    val timetableJson = findArray("timetableEntries", "timetable_entries", "timetable", "entries", "allTimetableEntries", "schedule", "timetableList")
                    if (timetableJson != null) {
                        val listType = object : TypeToken<List<TimetableEntryEntity>>() {}.type
                        val parsedEntries: List<TimetableEntryEntity>? = gson.fromJson(timetableJson, listType)
                        parsedEntries?.let { timetableEntries.addAll(it) }
                    }

                    // Parse sessions
                    val sessionsJson = findArray("sessions", "attendanceSessions", "attendance_sessions", "attendance", "allSessions", "sessionList", "session_list")
                    if (sessionsJson != null) {
                        val listType = object : TypeToken<List<AttendanceSessionEntity>>() {}.type
                        val parsedSessions: List<AttendanceSessionEntity>? = gson.fromJson(sessionsJson, listType)
                        parsedSessions?.let { sessions.addAll(it) }
                    }

                    // Parse units
                    val unitsJson = findArray("units", "attendanceUnits", "attendance_units", "allUnits", "unitList", "unit_list")
                    if (unitsJson != null) {
                        val listType = object : TypeToken<List<AttendanceUnitEntity>>() {}.type
                        val parsedUnits: List<AttendanceUnitEntity>? = gson.fromJson(unitsJson, listType)
                        parsedUnits?.let { units.addAll(it) }
                    }

                    // Parse holidays
                    val holidaysJson = findArray("holidays", "holidayList", "holiday_list", "allHolidays")
                    if (holidaysJson != null) {
                        val listType = object : TypeToken<List<HolidayEntity>>() {}.type
                        val parsedHolidays: List<HolidayEntity>? = gson.fromJson(holidaysJson, listType)
                        parsedHolidays?.let { holidays.addAll(it) }
                    }
                } else if (jsonElement.isJsonArray) {
                    val array = jsonElement.asJsonArray
                    if (array.size() > 0 && array.get(0).isJsonObject) {
                        val firstObj = array.get(0).asJsonObject
                        if (firstObj.has("dayOfWeek") || (firstObj.has("startTime") && !firstObj.has("sessionDate"))) {
                            val listType = object : TypeToken<List<TimetableEntryEntity>>() {}.type
                            val parsedEntries: List<TimetableEntryEntity>? = gson.fromJson(array, listType)
                            parsedEntries?.let { timetableEntries.addAll(it) }
                        } else if (firstObj.has("sessionDate")) {
                            val listType = object : TypeToken<List<AttendanceSessionEntity>>() {}.type
                            val parsedSessions: List<AttendanceSessionEntity>? = gson.fromJson(array, listType)
                            parsedSessions?.let { sessions.addAll(it) }
                        } else if (firstObj.has("targetPercentage") || firstObj.has("colorValue") || firstObj.has("teacherName")) {
                            val listType = object : TypeToken<List<SubjectEntity>>() {}.type
                            val parsedSubjects: List<SubjectEntity>? = gson.fromJson(array, listType)
                            parsedSubjects?.let { subjects.addAll(it) }
                        } else {
                            val listType = object : TypeToken<List<TimetableEntryEntity>>() {}.type
                            val parsedEntries: List<TimetableEntryEntity>? = gson.fromJson(array, listType)
                            parsedEntries?.let { timetableEntries.addAll(it) }
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore and fall back to regex/bracket extractor below
            }

            // If standard JSON parsing didn't find data or failed, extract with resilient scanner
            if (subjects.isEmpty() && timetableEntries.isEmpty() && sessions.isEmpty() && holidays.isEmpty()) {
                extractEntitiesFromMalformedJson(cleanJson, subjects, timetableEntries, sessions, units, holidays)
            }

            if (timetableEntries.isEmpty() && subjects.isEmpty() && sessions.isEmpty() && holidays.isEmpty()) {
                return Pair(false, "No valid subjects, timetable entries, or attendance data found in file.")
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
            val existingSubjectsList = repository.allSubjects.first()
            val existingSubjects = existingSubjectsList.associateBy { it.name.trim().lowercase() }
            val existingSubjectIds = existingSubjectsList.map { it.id }.toSet()
            val subjectIdMap = mutableMapOf<Long, Long>()

            for (rawSub in subjects) {
                val sub = sanitizeSubject(rawSub)
                val existing = existingSubjects[sub.name.trim().lowercase()]
                if (existing != null) {
                    if (rawSub.id != 0L) {
                        subjectIdMap[rawSub.id] = existing.id
                    }
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
                    if (rawSub.id != 0L) {
                        subjectIdMap[rawSub.id] = newId
                    }
                }
            }

            // Helper to get or create valid subject ID for foreign key integrity
            suspend fun resolveSubjectId(rawSubjectId: Long): Long {
                // If mapped in subjectIdMap
                val mappedId = subjectIdMap[rawSubjectId]
                if (mappedId != null) return mappedId

                // If rawSubjectId exists in database
                if (existingSubjectIds.contains(rawSubjectId) || repository.getSubjectById(rawSubjectId) != null) {
                    subjectIdMap[rawSubjectId] = rawSubjectId
                    return rawSubjectId
                }

                // If existing subjects has any subject, fallback to first subject
                val currentSubjects = repository.allSubjects.first()
                if (currentSubjects.isNotEmpty()) {
                    val fallbackId = currentSubjects.first().id
                    subjectIdMap[rawSubjectId] = fallbackId
                    return fallbackId
                }

                // Otherwise, create a placeholder subject to satisfy foreign key constraints
                val placeholder = SubjectEntity(
                    name = "Imported Subject",
                    type = "Lecture"
                )
                val createdId = repository.insertSubject(placeholder)
                subjectIdMap[rawSubjectId] = createdId
                return createdId
            }

            // 2. Map and insert timetable entries
            val timetableIdMap = mutableMapOf<Long, Long>()
            var importedTimetableCount = 0

            for (rawEntry in timetableEntries) {
                val resolvedSubId = resolveSubjectId(rawEntry.subjectId)
                val entryStartDate = if (!rawEntry.startDate.isNullOrBlank()) rawEntry.startDate.trim()
                else if (replaceExisting) effectiveStartDate
                else ""
                val entry = sanitizeTimetableEntry(rawEntry, entryStartDate).copy(
                    id = 0,
                    subjectId = resolvedSubId,
                    startDate = entryStartDate,
                    endDate = rawEntry.endDate?.trim() ?: "",
                    createdAt = if (rawEntry.createdAt > 0) rawEntry.createdAt else System.currentTimeMillis()
                )
                val newId = repository.insertTimetableEntry(entry)
                if (rawEntry.id != 0L) {
                    timetableIdMap[rawEntry.id] = newId
                }
                importedTimetableCount++
            }

            // 3. Map and insert sessions & units
            var importedSessionCount = 0
            for (rawSession in sessions) {
                val resolvedSubId = resolveSubjectId(rawSession.subjectId)
                val mappedTimetableId = if (rawSession.timetableEntryId != null) {
                    timetableIdMap[rawSession.timetableEntryId]
                } else null

                val session = sanitizeSession(rawSession).copy(
                    id = 0,
                    subjectId = resolvedSubId,
                    timetableEntryId = mappedTimetableId
                )

                val sessionUnits = if (rawSession.id != 0L) {
                    units.filter { it.sessionId == rawSession.id }
                } else emptyList()

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
            var importedHolidayCount = 0
            for (rawHoliday in holidays) {
                val holiday = sanitizeHoliday(rawHoliday)
                repository.insertHoliday(holiday)
                importedHolidayCount++
            }

            val msg = buildString {
                val parts = mutableListOf<String>()
                if (importedTimetableCount > 0) parts.add("$importedTimetableCount timetable entries")
                if (importedSessionCount > 0) parts.add("$importedSessionCount attendance sessions")
                if (subjects.isNotEmpty() && importedTimetableCount == 0 && importedSessionCount == 0) parts.add("${subjects.size} subjects")
                if (importedHolidayCount > 0 && parts.isEmpty()) parts.add("$importedHolidayCount holidays")
                if (parts.isEmpty()) {
                    append("Backup restored successfully!")
                } else {
                    append("Successfully imported ${parts.joinToString(" and ")}!")
                }
            }

            Pair(true, msg)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Failed to import backup: ${e.message}")
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
    ): Pair<Boolean, String> {
        return try {
            importTimetableFromJson(
                jsonString = jsonString,
                repository = repository,
                effectiveStartDate = DateUtils.todayIso(),
                replaceExisting = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Failed to import backup: ${e.localizedMessage ?: e.message}")
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

    suspend fun importDataFromJson(context: Context, repository: AttendSmartlyRepository, uri: Uri): Pair<Boolean, String> {
        val json = readTextFromUri(context, uri) ?: return Pair(false, "Could not open or read the selected file.")
        return restoreFromJson(json, repository)
    }

    suspend fun exportAttendanceCsv(context: Context, repository: AttendSmartlyRepository, uri: Uri): Boolean {
        val csv = exportToCsv(repository)
        return writeTextToUri(context, uri, csv)
    }

    fun writeTextToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
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
                BufferedReader(InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8)).use { reader ->
                    reader.readText().trim().removePrefix("\uFEFF")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
