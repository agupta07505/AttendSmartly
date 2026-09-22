/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly

import com.agupta07505.attendsmartly.data.local.entity.AttendanceSessionEntity
import com.agupta07505.attendsmartly.data.local.entity.AttendanceUnitEntity
import com.agupta07505.attendsmartly.data.local.entity.HolidayEntity
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
    fun testSanitizeHolidayWithNulls() {
        val rawJson = """
            {
                "id": 1,
                "notes": null
            }
        """.trimIndent()

        val gson = Gson()
        val deserialized = gson.fromJson(rawJson, com.agupta07505.attendsmartly.data.local.entity.HolidayEntity::class.java)
        val sanitized = ExportImportUtils.sanitizeHoliday(deserialized)

        assertEquals("Holiday", sanitized.title)
        assertEquals("", sanitized.notes)
        assertTrue(sanitized.date.isNotBlank())
    }

    @Test
    fun testSanitizeExtendedUnitStatuses() {
        val u1 = AttendanceUnitEntity(sessionId = 1, unitIndex = 0, status = "ATTENDED")
        val u2 = AttendanceUnitEntity(sessionId = 1, unitIndex = 1, status = "MISSED")
        val u3 = AttendanceUnitEntity(sessionId = 1, unitIndex = 2, status = "CANCELED")
        val u4 = AttendanceUnitEntity(sessionId = 1, unitIndex = 3, status = "YES")
        val u5 = AttendanceUnitEntity(sessionId = 1, unitIndex = 4, status = "NO")

        assertEquals("PRESENT", ExportImportUtils.sanitizeUnit(u1, 10).status)
        assertEquals("ABSENT", ExportImportUtils.sanitizeUnit(u2, 10).status)
        assertEquals("CANCELLED", ExportImportUtils.sanitizeUnit(u3, 10).status)
        assertEquals("PRESENT", ExportImportUtils.sanitizeUnit(u4, 10).status)
        assertEquals("ABSENT", ExportImportUtils.sanitizeUnit(u5, 10).status)
    }

    @Test
    fun testV12BackupWithMissingFieldsAndNullsDeserialization() {
        // Simulating a realistic v1.2 backup JSON where newer fields are missing
        val v12Json = """
            {
                "version": 1,
                "exportedAt": 1724140000000,
                "subjects": [
                    {
                        "id": 1,
                        "name": "Machine Learning",
                        "code": "CS401",
                        "type": "Lecture",
                        "colorValue": 4280391411,
                        "targetPercentage": 75.0
                    }
                ],
                "timetableEntries": [
                    {
                        "id": 101,
                        "subjectId": 1,
                        "dayOfWeek": 1,
                        "startTime": "09:00",
                        "endTime": "10:00",
                        "attendanceUnitCount": 1
                    }
                ],
                "sessions": [
                    {
                        "id": 201,
                        "subjectId": 1,
                        "timetableEntryId": 101,
                        "sessionDate": "2026-08-18",
                        "startTime": "09:00",
                        "endTime": "10:00",
                        "expectedUnitCount": 1
                    }
                ],
                "units": [
                    {
                        "id": 301,
                        "sessionId": 201,
                        "unitIndex": 0,
                        "status": "PRESENT"
                    }
                ],
                "holidays": [
                    {
                        "id": 401,
                        "date": "2026-08-15",
                        "title": "Independence Day"
                    }
                ]
            }
        """.trimIndent()

        val jsonElement = com.google.gson.JsonParser.parseString(v12Json)
        assertTrue(jsonElement.isJsonObject)
        val obj = jsonElement.asJsonObject

        val subjectsJson = obj.get("subjects").asJsonArray
        val parsedSubjects = ExportImportUtils.parseJsonArray(subjectsJson, SubjectEntity::class.java)

        assertEquals(1, parsedSubjects.size)
        val sanitizedSub = ExportImportUtils.sanitizeSubject(parsedSubjects[0])
        assertEquals("Machine Learning", sanitizedSub.name)
        assertEquals("CS401", sanitizedSub.code)
        assertEquals("Lecture", sanitizedSub.type)
        assertEquals("Book", sanitizedSub.iconName)
        assertEquals(60, sanitizedSub.defaultSessionDurationMinutes)

        val timetableJson = obj.get("timetableEntries").asJsonArray
        val parsedEntries = ExportImportUtils.parseJsonArray(timetableJson, TimetableEntryEntity::class.java)
        assertEquals(1, parsedEntries.size)
        val sanitizedEntry = ExportImportUtils.sanitizeTimetableEntry(parsedEntries[0])
        assertTrue(sanitizedEntry.isActive)
        assertEquals("09:00", sanitizedEntry.startTime)
        assertEquals("10:00", sanitizedEntry.endTime)

        val sessionsJson = obj.get("sessions").asJsonArray
        val parsedSessions = ExportImportUtils.parseJsonArray(sessionsJson, AttendanceSessionEntity::class.java)
        assertEquals(1, parsedSessions.size)
        val sanitizedSession = ExportImportUtils.sanitizeSession(parsedSessions[0])
        assertEquals("2026-08-18", sanitizedSession.sessionDate)
        assertEquals(false, sanitizedSession.isRescheduled)
        assertEquals("", sanitizedSession.rescheduledReason)

        val unitsJson = obj.get("units").asJsonArray
        val parsedUnits = ExportImportUtils.parseJsonArray(unitsJson, AttendanceUnitEntity::class.java)
        assertEquals(1, parsedUnits.size)
        val sanitizedUnit = ExportImportUtils.sanitizeUnit(parsedUnits[0], 201)
        assertEquals("PRESENT", sanitizedUnit.status)

        val holidaysJson = obj.get("holidays").asJsonArray
        val parsedHolidays = ExportImportUtils.parseJsonArray(holidaysJson, com.agupta07505.attendsmartly.data.local.entity.HolidayEntity::class.java)
        assertEquals(1, parsedHolidays.size)
        val sanitizedHoliday = ExportImportUtils.sanitizeHoliday(parsedHolidays[0])
        assertEquals("Independence Day", sanitizedHoliday.title)
        assertEquals("2026-08-15", sanitizedHoliday.date)
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

    @Test
    fun testImportActualHelperBackupFile() {
        val backupFile = java.io.File("A:\\AttendSmartly\\helper\\AttendSmartly_backup.json")
        if (backupFile.exists()) {
            val jsonContent = backupFile.readText()
            val gson = Gson()
            val backup = gson.fromJson(jsonContent, com.agupta07505.attendsmartly.util.AttendSmartlyBackup::class.java)

            org.junit.Assert.assertNotNull(backup)
            assertTrue("Should contain subjects", backup.subjects.isNotEmpty())
            assertTrue("Should contain timetable entries", backup.timetableEntries.isNotEmpty())
            assertTrue("Should contain sessions", backup.sessions.isNotEmpty())
            assertTrue("Should contain units", backup.units.isNotEmpty())

            // Sanitize all entities to ensure no NPE or SQLite constraints
            val sanitizedSubjects = backup.subjects.map { ExportImportUtils.sanitizeSubject(it) }
            val sanitizedTimetable = backup.timetableEntries.map { ExportImportUtils.sanitizeTimetableEntry(it) }
            val sanitizedSessions = backup.sessions.map { ExportImportUtils.sanitizeSession(it) }
            val sanitizedUnits = backup.units.map { ExportImportUtils.sanitizeUnit(it, it.sessionId) }

            assertEquals(backup.subjects.size, sanitizedSubjects.size)
            assertEquals(backup.timetableEntries.size, sanitizedTimetable.size)
            assertEquals(backup.sessions.size, sanitizedSessions.size)
            assertEquals(backup.units.size, sanitizedUnits.size)
        }
    }

    @Test
    fun testMalformedJsonWithCorruptedHeaderRecovery() {
        val corruptedJson = """
            {
              "exportedAt": 1787195259305,
              "holidays": [],
              "sessions": [],
              "subjects": [],
              "timetableEntries": [],
              "units": [],
              "version": 1
            }ctedUnitCount": 1,
                  "id": 177,
                  "notes": "",
                  "sessionDate": "2026-10-08",
                  "startTime": "10:00",
                  "subjectId": 75,
                  "timetableEntryId": 204,
                  "updatedAt": 1786595641840
                }
              ],
              "subjects": [
                {
                  "id": 75,
                  "name": "COA",
                  "targetPercentage": 75.0
                }
              ],
              "timetableEntries": [
                {
                  "id": 204,
                  "subjectId": 75,
                  "dayOfWeek": 4,
                  "startTime": "10:00",
                  "endTime": "11:00"
                }
              ],
              "units": [
                {
                  "id": 1,
                  "sessionId": 177,
                  "unitIndex": 0,
                  "status": "PRESENT"
                }
              ]
            }
        """.trimIndent()

        val repaired = ExportImportUtils.autoRepairJson(corruptedJson)
        val subjects = mutableListOf<SubjectEntity>()
        val timetable = mutableListOf<TimetableEntryEntity>()
        val sessions = mutableListOf<AttendanceSessionEntity>()
        val units = mutableListOf<AttendanceUnitEntity>()
        val holidays = mutableListOf<HolidayEntity>()

        ExportImportUtils.extractEntitiesFromMalformedJson(repaired, subjects, timetable, sessions, units, holidays)

        assertEquals(1, subjects.size)
        assertEquals("COA", subjects[0].name)
        assertEquals(1, timetable.size)
        assertEquals(204L, timetable[0].id)
        assertEquals(1, sessions.size)
        assertEquals("2026-10-08", sessions[0].sessionDate)
        assertEquals(1, units.size)
        assertEquals("PRESENT", units[0].status)
    }

    @Test
    fun testTimetableEntryRescheduleAndEffectiveDateSanitization() {
        val originalEntry = TimetableEntryEntity(
            id = 10,
            subjectId = 1,
            dayOfWeek = 1, // Monday
            startTime = "10:00",
            endTime = "11:00",
            startDate = "2026-08-01",
            endDate = ""
        )

        // Rescheduled / Moved to Friday starting from 2026-08-22
        val movedEntry = originalEntry.copy(
            id = 0,
            dayOfWeek = 5, // Friday
            startDate = "2026-08-22",
            endDate = ""
        )

        val sanitizedOriginal = ExportImportUtils.sanitizeTimetableEntry(originalEntry.copy(endDate = "2026-08-21"))
        val sanitizedMoved = ExportImportUtils.sanitizeTimetableEntry(movedEntry)

        assertEquals("2026-08-21", sanitizedOriginal.endDate)
        assertEquals(1, sanitizedOriginal.dayOfWeek)

        assertEquals("2026-08-22", sanitizedMoved.startDate)
        assertEquals("", sanitizedMoved.endDate)
        assertEquals(5, sanitizedMoved.dayOfWeek)
        assertTrue(sanitizedMoved.isActive)
    }

    @Test
    fun testSanitizeLocationOnSubjectAndTimetableEntry() {
        val validSubject = SubjectEntity(
            id = 1,
            name = "Robotics",
            latitude = 28.6139,
            longitude = 77.2090,
            locationRadiusMeters = 100
        )
        val sanitizedSub = ExportImportUtils.sanitizeSubject(validSubject)
        assertEquals(28.6139, sanitizedSub.latitude!!, 0.0001)
        assertEquals(77.2090, sanitizedSub.longitude!!, 0.0001)
        assertEquals(100, sanitizedSub.locationRadiusMeters)

        // Invalid coordinates and negative radius
        val invalidSubject = SubjectEntity(
            id = 2,
            name = "Physics",
            latitude = 195.0, // Out of bounds
            longitude = -250.0, // Out of bounds
            locationRadiusMeters = -10
        )
        val sanitizedInvalid = ExportImportUtils.sanitizeSubject(invalidSubject)
        org.junit.Assert.assertNull(sanitizedInvalid.latitude)
        org.junit.Assert.assertNull(sanitizedInvalid.longitude)
        assertEquals(50, sanitizedInvalid.locationRadiusMeters)

        val entry = TimetableEntryEntity(
            id = 10,
            subjectId = 1,
            dayOfWeek = 1,
            startTime = "09:00",
            endTime = "10:00",
            latitude = 12.9716,
            longitude = 77.5946,
            locationRadiusMeters = 75
        )
        val sanitizedEntry = ExportImportUtils.sanitizeTimetableEntry(entry)
        assertEquals(12.9716, sanitizedEntry.latitude!!, 0.0001)
        assertEquals(77.5946, sanitizedEntry.longitude!!, 0.0001)
        assertEquals(75, sanitizedEntry.locationRadiusMeters)
    }

    @Test
    fun testSanitizeSessionAutoMarked() {
        val session = AttendanceSessionEntity(
            id = 1,
            subjectId = 1,
            sessionDate = "2026-09-22",
            startTime = "09:00",
            endTime = "10:00",
            autoMarked = true
        )
        val sanitized = ExportImportUtils.sanitizeSession(session)
        assertTrue(sanitized.autoMarked)
    }

    @Test
    fun testLocationBackupRoundTripSerialization() {
        val subject = SubjectEntity(
            id = 10,
            name = "Computer Architecture",
            latitude = 28.5450,
            longitude = 77.1926,
            locationRadiusMeters = 60
        )
        val entry = TimetableEntryEntity(
            id = 20,
            subjectId = 10,
            dayOfWeek = 3,
            startTime = "11:00",
            endTime = "12:00",
            latitude = 28.5452,
            longitude = 77.1928,
            locationRadiusMeters = 40
        )
        val session = AttendanceSessionEntity(
            id = 30,
            subjectId = 10,
            timetableEntryId = 20,
            sessionDate = "2026-09-22",
            startTime = "11:00",
            endTime = "12:00",
            autoMarked = true
        )

        val backup = com.agupta07505.attendsmartly.util.AttendSmartlyBackup(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            subjects = listOf(subject),
            timetableEntries = listOf(entry),
            sessions = listOf(session)
        )

        val gson = Gson()
        val json = gson.toJson(backup)
        val deserialized = gson.fromJson(json, com.agupta07505.attendsmartly.util.AttendSmartlyBackup::class.java)

        assertEquals(1, deserialized.subjects.size)
        assertEquals(28.5450, deserialized.subjects[0].latitude!!, 0.0001)
        assertEquals(77.1926, deserialized.subjects[0].longitude!!, 0.0001)
        assertEquals(60, deserialized.subjects[0].locationRadiusMeters)

        assertEquals(1, deserialized.timetableEntries.size)
        assertEquals(28.5452, deserialized.timetableEntries[0].latitude!!, 0.0001)
        assertEquals(77.1928, deserialized.timetableEntries[0].longitude!!, 0.0001)
        assertEquals(40, deserialized.timetableEntries[0].locationRadiusMeters)

        assertEquals(1, deserialized.sessions.size)
        assertTrue(deserialized.sessions[0].autoMarked)
    }

    @Test
    fun testOlderBackupWithoutLocationDeserializesGracefullyWithDefaults() {
        val legacyJson = """
            {
                "version": 1,
                "exportedAt": 1724140000000,
                "subjects": [
                    {
                        "id": 1,
                        "name": "Linear Algebra"
                    }
                ],
                "timetableEntries": [
                    {
                        "id": 101,
                        "subjectId": 1,
                        "dayOfWeek": 2,
                        "startTime": "09:00",
                        "endTime": "10:00"
                    }
                ],
                "sessions": [
                    {
                        "id": 201,
                        "subjectId": 1,
                        "sessionDate": "2026-08-20",
                        "startTime": "09:00",
                        "endTime": "10:00"
                    }
                ]
            }
        """.trimIndent()

        val gson = Gson()
        val deserialized = gson.fromJson(legacyJson, com.agupta07505.attendsmartly.util.AttendSmartlyBackup::class.java)
        val sanitizedSub = ExportImportUtils.sanitizeSubject(deserialized.subjects[0])
        val sanitizedEntry = ExportImportUtils.sanitizeTimetableEntry(deserialized.timetableEntries[0])
        val sanitizedSession = ExportImportUtils.sanitizeSession(deserialized.sessions[0])

        org.junit.Assert.assertNull(sanitizedSub.latitude)
        org.junit.Assert.assertNull(sanitizedSub.longitude)
        assertEquals(50, sanitizedSub.locationRadiusMeters)

        org.junit.Assert.assertNull(sanitizedEntry.latitude)
        org.junit.Assert.assertNull(sanitizedEntry.longitude)
        assertEquals(50, sanitizedEntry.locationRadiusMeters)

        org.junit.Assert.assertFalse(sanitizedSession.autoMarked)
    }
}
