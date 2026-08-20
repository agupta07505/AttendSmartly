/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly

import com.agupta07505.attendsmartly.data.local.entity.AttendanceSessionEntity
import com.agupta07505.attendsmartly.data.local.entity.AttendanceUnitEntity
import com.agupta07505.attendsmartly.data.local.entity.SubjectEntity
import com.agupta07505.attendsmartly.data.local.entity.TimetableEntryEntity
import com.agupta07505.attendsmartly.util.ExportImportUtils
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportImportUtilsTest {

    @Test
    fun testSanitizeOlderTimetableEntryWithNulls() {
        // Simulating an entity deserialized by Gson from older JSON where isActive and startDate were missing
        val rawJson = """
            {
                "id": 1,
                "subjectId": 2,
                "dayOfWeek": 1,
                "startTime": "09:00",
                "endTime": "10:00"
            }
        """.trimIndent()

        val gson = Gson()
        val deserialized = gson.fromJson(rawJson, TimetableEntryEntity::class.java)

        val sanitized = ExportImportUtils.sanitizeTimetableEntry(deserialized, defaultStartDate = "2026-08-20")

        assertTrue(sanitized.isActive)
        assertEquals("2026-08-20", sanitized.startDate)
        assertEquals("", sanitized.endDate)
        assertEquals("", sanitized.notes)
        assertEquals("", sanitized.roomOverride)
        assertEquals("", sanitized.teacherOverride)
        assertEquals(1, sanitized.attendanceUnitCount)
    }

    @Test
    fun testSanitizeOlderAttendanceSessionWithNulls() {
        val rawJson = """
            {
                "id": 5,
                "subjectId": 2,
                "sessionDate": "2026-08-15",
                "startTime": "10:00",
                "endTime": "11:00",
                "expectedUnitCount": 1
            }
        """.trimIndent()

        val gson = Gson()
        val deserialized = gson.fromJson(rawJson, AttendanceSessionEntity::class.java)

        val sanitized = ExportImportUtils.sanitizeSession(deserialized)

        assertEquals("", sanitized.notes)
        assertEquals("", sanitized.rescheduledReason)
        assertEquals(false, sanitized.isRescheduled)
        assertEquals("2026-08-15", sanitized.sessionDate)
    }

    @Test
    fun testSanitizeOlderSubjectWithNulls() {
        val rawJson = """
            {
                "id": 2,
                "name": "Database Systems"
            }
        """.trimIndent()

        val gson = Gson()
        val deserialized = gson.fromJson(rawJson, SubjectEntity::class.java)

        val sanitized = ExportImportUtils.sanitizeSubject(deserialized)

        assertEquals("Database Systems", sanitized.name)
        assertEquals("", sanitized.code)
        assertEquals("Lecture", sanitized.type)
        assertEquals("", sanitized.teacherName)
        assertEquals("", sanitized.room)
        assertEquals(75.0, sanitized.targetPercentage, 0.01)
        assertEquals(60, sanitized.attendanceUnitMinutes)
        assertEquals(1, sanitized.defaultAttendanceUnits)
    }

    @Test
    fun testSanitizeUnitStatus() {
        val u1 = AttendanceUnitEntity(sessionId = 1, unitIndex = 0, status = "PRESENT")
        val u2 = AttendanceUnitEntity(sessionId = 1, unitIndex = 1, status = "bunked")
        val u3 = AttendanceUnitEntity(sessionId = 1, unitIndex = 2, status = "")

        assertEquals("PRESENT", ExportImportUtils.sanitizeUnit(u1, 10).status)
        assertEquals("ABSENT", ExportImportUtils.sanitizeUnit(u2, 10).status)
        assertEquals("UNMARKED", ExportImportUtils.sanitizeUnit(u3, 10).status)
    }

    @Test
    fun testFullBackupRoundTripSerialization() {
        val subject = SubjectEntity(
            id = 10,
            name = "Advanced Algorithms",
            code = "CS501",
            type = "Lecture",
            teacherName = "Prof. Turing",
            room = "Room-404",
            colorValue = 0xFF4CAF50L,
            targetPercentage = 80.0
        )
        val entry = TimetableEntryEntity(
            id = 20,
            subjectId = 10,
            dayOfWeek = 2,
            startTime = "10:00",
            endTime = "12:00",
            attendanceUnitCount = 2
        )
        val session = AttendanceSessionEntity(
            id = 30,
            subjectId = 10,
            timetableEntryId = 20,
            sessionDate = "2026-08-20",
            startTime = "10:00",
            endTime = "12:00",
            expectedUnitCount = 2
        )
        val unit1 = AttendanceUnitEntity(id = 40, sessionId = 30, unitIndex = 0, status = "PRESENT")
        val unit2 = AttendanceUnitEntity(id = 41, sessionId = 30, unitIndex = 1, status = "ABSENT")
        val holiday = com.agupta07505.attendsmartly.data.local.entity.HolidayEntity(id = 50, date = "2026-08-15", title = "Independence Day")

        val backup = com.agupta07505.attendsmartly.util.AttendSmartlyBackup(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            subjects = listOf(subject),
            timetableEntries = listOf(entry),
            sessions = listOf(session),
            units = listOf(unit1, unit2),
            holidays = listOf(holiday)
        )

        val gson = Gson()
        val json = gson.toJson(backup)
        val deserialized = gson.fromJson(json, com.agupta07505.attendsmartly.util.AttendSmartlyBackup::class.java)

        assertEquals(1, deserialized.subjects.size)
        assertEquals("Advanced Algorithms", deserialized.subjects[0].name)
        assertEquals(1, deserialized.timetableEntries.size)
        assertEquals(2, deserialized.timetableEntries[0].attendanceUnitCount)
        assertEquals(1, deserialized.sessions.size)
        assertEquals("2026-08-20", deserialized.sessions[0].sessionDate)
        assertEquals(2, deserialized.units.size)
        assertEquals("PRESENT", deserialized.units[0].status)
        assertEquals("ABSENT", deserialized.units[1].status)
        assertEquals(1, deserialized.holidays.size)
        assertEquals("Independence Day", deserialized.holidays[0].title)
    }
}
