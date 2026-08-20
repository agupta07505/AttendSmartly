/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly

import com.agupta07505.attendsmartly.data.local.entity.AttendanceSessionEntity
import com.agupta07505.attendsmartly.data.local.entity.AttendanceUnitEntity
import com.agupta07505.attendsmartly.data.preferences.UserPreferences
import com.agupta07505.attendsmartly.domain.calculator.AttendanceCalculator
import com.agupta07505.attendsmartly.domain.model.AttendanceStatus
import com.agupta07505.attendsmartly.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SemesterTrackingTest {

    private fun isDateInSemester(dateIso: String, prefs: UserPreferences): Boolean {
        if (!prefs.trackBySemester) return true
        if (prefs.semesterStartDate.isNotBlank() && dateIso < prefs.semesterStartDate) return false
        if (prefs.semesterEndDate.isNotBlank() && dateIso > prefs.semesterEndDate) return false
        return true
    }

    @Test
    fun testDateContainmentWhenSemesterTrackingDisabled() {
        val prefs = UserPreferences(
            trackBySemester = false,
            semesterStartDate = "2026-08-01",
            semesterEndDate = "2026-12-15"
        )

        // All dates are allowed when trackBySemester is false
        assertTrue(isDateInSemester("2026-07-15", prefs))
        assertTrue(isDateInSemester("2026-09-01", prefs))
        assertTrue(isDateInSemester("2027-01-10", prefs))
    }

    @Test
    fun testDateContainmentWhenSemesterTrackingEnabled() {
        val prefs = UserPreferences(
            trackBySemester = true,
            semesterStartDate = "2026-08-01",
            semesterEndDate = "2026-12-15"
        )

        // Before start
        assertFalse(isDateInSemester("2026-07-31", prefs))
        assertFalse(isDateInSemester("2026-01-01", prefs))

        // On boundaries
        assertTrue(isDateInSemester("2026-08-01", prefs))
        assertTrue(isDateInSemester("2026-12-15", prefs))

        // Inside range
        assertTrue(isDateInSemester("2026-09-20", prefs))
        assertTrue(isDateInSemester("2026-11-30", prefs))

        // After end
        assertFalse(isDateInSemester("2026-12-16", prefs))
        assertFalse(isDateInSemester("2027-02-01", prefs))
    }

    @Test
    fun testSemesterAttendanceFiltering() {
        val prefs = UserPreferences(
            trackBySemester = true,
            semesterStartDate = "2026-08-01",
            semesterEndDate = "2026-12-15"
        )

        // Session 1: Before semester (Absent)
        val s1 = AttendanceSessionEntity(id = 1, subjectId = 10, sessionDate = "2026-07-20", startTime = "09:00", endTime = "10:00")
        val u1 = AttendanceUnitEntity(id = 101, sessionId = 1, unitIndex = 0, status = AttendanceStatus.ABSENT.name)

        // Session 2: In semester (Present)
        val s2 = AttendanceSessionEntity(id = 2, subjectId = 10, sessionDate = "2026-08-10", startTime = "09:00", endTime = "10:00")
        val u2 = AttendanceUnitEntity(id = 102, sessionId = 2, unitIndex = 0, status = AttendanceStatus.PRESENT.name)

        // Session 3: In semester (Present)
        val s3 = AttendanceSessionEntity(id = 3, subjectId = 10, sessionDate = "2026-09-15", startTime = "09:00", endTime = "10:00")
        val u3 = AttendanceUnitEntity(id = 103, sessionId = 3, unitIndex = 0, status = AttendanceStatus.PRESENT.name)

        // Session 4: After semester (Absent)
        val s4 = AttendanceSessionEntity(id = 4, subjectId = 10, sessionDate = "2026-12-20", startTime = "09:00", endTime = "10:00")
        val u4 = AttendanceUnitEntity(id = 104, sessionId = 4, unitIndex = 0, status = AttendanceStatus.ABSENT.name)

        val allSessions = listOf(s1, s2, s3, s4)
        val allUnits = listOf(u1, u2, u3, u4)

        // Filter sessions by semester
        val semesterSessions = allSessions.filter { isDateInSemester(it.sessionDate, prefs) }
        val semesterSessionIds = semesterSessions.map { it.id }.toSet()
        val semesterUnits = allUnits.filter { semesterSessionIds.contains(it.sessionId) }

        val statuses = semesterUnits.mapNotNull {
            try { AttendanceStatus.valueOf(it.status) } catch (e: Exception) { null }
        }

        val summary = AttendanceCalculator.calculate(statuses, targetPercentage = 75.0)

        // Should only count Session 2 and 3 (both Present) -> 2 conducted, 2 present -> 100%
        assertEquals(2, summary.totalConductedUnits)
        assertEquals(2, summary.presentUnits)
        assertEquals(0, summary.absentUnits)
        assertEquals(100.0, summary.percentage, 0.01)
    }

    @Test
    fun testSemesterDateRangeGeneration() {
        val startIso = "2026-08-01"
        val endIso = "2026-08-05"

        val start = LocalDate.parse(startIso, DateUtils.isoDateFormatter)
        val end = LocalDate.parse(endIso, DateUtils.isoDateFormatter)

        val days = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt()
        val dates = (0..days).map { start.plusDays(it.toLong()).format(DateUtils.isoDateFormatter) }

        assertEquals(5, dates.size)
        assertEquals("2026-08-01", dates.first())
        assertEquals("2026-08-05", dates.last())
    }
}
