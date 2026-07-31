package com.agupta07505.attendmate.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.agupta07505.attendmate.data.local.entity.*
import com.agupta07505.attendmate.data.repository.AttendMateRepository
import com.agupta07505.attendmate.domain.calculator.AttendanceCalculator
import com.agupta07505.attendmate.domain.model.AttendanceStatus
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.InputStreamReader

data class AttendMateBackup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val subjects: List<SubjectEntity> = emptyList(),
    val timetableEntries: List<TimetableEntryEntity> = emptyList(),
    val sessions: List<AttendanceSessionEntity> = emptyList(),
    val units: List<AttendanceUnitEntity> = emptyList(),
    val holidays: List<HolidayEntity> = emptyList()
)

data class TimetableSharePackage(
    val format: String = "ATTENDMATE_TIMETABLE_V1",
    val exportedAt: Long = System.currentTimeMillis(),
    val subjects: List<SubjectEntity> = emptyList(),
    val timetableEntries: List<TimetableEntryEntity> = emptyList()
)

object ExportImportUtils {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportTimetableToJson(repository: AttendMateRepository): String {
        val entries = repository.allActiveTimetableEntries.first()
        val subjectIds = entries.map { it.subjectId }.toSet()
        val allSubjects = repository.allSubjects.first()
        val relevantSubjects = allSubjects.filter { subjectIds.contains(it.id) }

        val pkg = TimetableSharePackage(
            format = "ATTENDMATE_TIMETABLE_V1",
            exportedAt = System.currentTimeMillis(),
            subjects = relevantSubjects,
            timetableEntries = entries
        )
        return gson.toJson(pkg)
    }

    suspend fun importTimetableFromJson(jsonString: String, repository: AttendMateRepository): Pair<Boolean, String> {
        return try {
            val pkg = gson.fromJson(jsonString, TimetableSharePackage::class.java)
            if (pkg != null && pkg.timetableEntries.isNotEmpty()) {
                val existingSubjects = repository.allSubjects.first().associateBy { it.name.trim().lowercase() }
                val subjectIdMap = mutableMapOf<Long, Long>()

                for (sub in pkg.subjects) {
                    val existing = existingSubjects[sub.name.trim().lowercase()]
                    if (existing != null) {
                        subjectIdMap[sub.id] = existing.id
                    } else {
                        val newId = repository.insertSubject(sub.copy(id = 0))
                        subjectIdMap[sub.id] = newId
                    }
                }

                var importedCount = 0
                for (entry in pkg.timetableEntries) {
                    val newSubjectId = subjectIdMap[entry.subjectId] ?: entry.subjectId
                    repository.insertTimetableEntry(
                        entry.copy(
                            id = 0,
                            subjectId = newSubjectId,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    importedCount++
                }
                Pair(true, "Successfully imported $importedCount timetable entries!")
            } else {
                val backup = gson.fromJson(jsonString, AttendMateBackup::class.java)
                if (backup != null && backup.timetableEntries.isNotEmpty()) {
                    restoreFromJson(jsonString, repository)
                    Pair(true, "Imported ${backup.timetableEntries.size} timetable entries!")
                } else {
                    Pair(false, "Invalid timetable JSON or empty entries.")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Failed to import timetable: ${e.message}")
        }
    }

    suspend fun exportToJson(repository: AttendMateRepository): String {
        val backup = AttendMateBackup(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            subjects = repository.allSubjects.first(),
            timetableEntries = repository.allActiveTimetableEntries.first(),
            sessions = repository.allSessions.first(),
            units = repository.allUnits.first(),
            holidays = repository.allHolidays.first()
        )
        return gson.toJson(backup)
    }

    suspend fun restoreFromJson(
        jsonString: String,
        repository: AttendMateRepository
    ): Boolean {
        return try {
            val backup = gson.fromJson(jsonString, AttendMateBackup::class.java) ?: return false

            // Restore subjects
            for (sub in backup.subjects) {
                repository.insertSubject(sub)
            }
            // Restore timetable entries
            for (entry in backup.timetableEntries) {
                repository.insertTimetableEntry(entry)
            }
            // Restore sessions and units
            for (session in backup.sessions) {
                val sessionUnits = backup.units.filter { it.sessionId == session.id }
                repository.createOrUpdateSessionWithUnits(session, sessionUnits)
            }
            // Restore holidays
            for (holiday in backup.holidays) {
                repository.insertHoliday(holiday)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportToCsv(repository: AttendMateRepository): String {
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

    suspend fun exportDataToJson(context: Context, repository: AttendMateRepository, uri: Uri): Boolean {
        val json = exportToJson(repository)
        return writeTextToUri(context, uri, json)
    }

    suspend fun importDataFromJson(context: Context, repository: AttendMateRepository, uri: Uri): Boolean {
        val json = readTextFromUri(context, uri) ?: return false
        return restoreFromJson(json, repository)
    }

    suspend fun exportAttendanceCsv(context: Context, repository: AttendMateRepository, uri: Uri): Boolean {
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
