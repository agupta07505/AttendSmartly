/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly

import com.agupta07505.attendsmartly.data.local.entity.SubjectEntity
import com.agupta07505.attendsmartly.data.local.entity.TimetableEntryEntity
import com.agupta07505.attendsmartly.data.preferences.UserPreferences
import com.agupta07505.attendsmartly.notification.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationAttendanceTest {

    @Test
    fun testSubjectLocationDefaults() {
        val subject = SubjectEntity(
            name = "Mathematics",
            code = "MATH101"
        )
        assertNull(subject.latitude)
        assertNull(subject.longitude)
        assertEquals(50, subject.locationRadiusMeters)
    }

    @Test
    fun testSubjectLocationCustom() {
        val subject = SubjectEntity(
            name = "Computer Networks",
            code = "CS302",
            latitude = 28.6139,
            longitude = 77.2090,
            locationRadiusMeters = 100
        )
        assertEquals(28.6139, subject.latitude!!, 0.0001)
        assertEquals(77.2090, subject.longitude!!, 0.0001)
        assertEquals(100, subject.locationRadiusMeters)
    }

    @Test
    fun testLocationInheritanceFromSubject() {
        val subject = SubjectEntity(
            id = 1,
            name = "Physics",
            latitude = 28.6139,
            longitude = 77.2090,
            locationRadiusMeters = 75
        )

        // Timetable entry without override: inherits subject location
        val entryWithoutOverride = TimetableEntryEntity(
            id = 10,
            subjectId = 1,
            dayOfWeek = 1,
            startTime = "09:00",
            endTime = "10:00"
        )

        val resolvedLat = entryWithoutOverride.latitude ?: subject.latitude
        val resolvedLng = entryWithoutOverride.longitude ?: subject.longitude
        val resolvedRadius = entryWithoutOverride.locationRadiusMeters.takeIf { it > 0 } ?: subject.locationRadiusMeters

        assertEquals(28.6139, resolvedLat!!, 0.0001)
        assertEquals(77.2090, resolvedLng!!, 0.0001)
        assertEquals(50, resolvedRadius) // Timetable entry has default 50 unless overridden

        // Timetable entry with override (different lab room)
        val entryWithOverride = TimetableEntryEntity(
            id = 11,
            subjectId = 1,
            dayOfWeek = 3,
            startTime = "14:00",
            endTime = "16:00",
            latitude = 28.6150,
            longitude = 77.2100,
            locationRadiusMeters = 30
        )

        val overrideLat = entryWithOverride.latitude ?: subject.latitude
        val overrideLng = entryWithOverride.longitude ?: subject.longitude
        val overrideRadius = entryWithOverride.locationRadiusMeters

        assertEquals(28.6150, overrideLat!!, 0.0001)
        assertEquals(77.2100, overrideLng!!, 0.0001)
        assertEquals(30, overrideRadius)
    }

    @Test
    fun testFiveMinuteDwellThresholdLogic() {
        val dwellTargetMinutes = 5
        val dwellTargetMillis = dwellTargetMinutes * 60 * 1000L // 300,000 ms

        val firstSeenTimestamp = 1000000L

        // After 2 minutes (120,000 ms)
        val checkTime2Min = firstSeenTimestamp + (2 * 60 * 1000L)
        val elapsed2Min = checkTime2Min - firstSeenTimestamp
        assertFalse("2 minutes should not satisfy 5-minute dwell", elapsed2Min >= dwellTargetMillis)

        // After 4 minutes 59 seconds (299,000 ms)
        val checkTimeJustBefore = firstSeenTimestamp + (4 * 60 * 1000L) + (59 * 1000L)
        val elapsedJustBefore = checkTimeJustBefore - firstSeenTimestamp
        assertFalse("4m59s should not satisfy 5-minute dwell", elapsedJustBefore >= dwellTargetMillis)

        // After exactly 5 minutes (300,000 ms)
        val checkTime5Min = firstSeenTimestamp + (5 * 60 * 1000L)
        val elapsed5Min = checkTime5Min - firstSeenTimestamp
        assertTrue("5 minutes exact should satisfy dwell requirement", elapsed5Min >= dwellTargetMillis)

        // After 6 minutes (360,000 ms)
        val checkTime6Min = firstSeenTimestamp + (6 * 60 * 1000L)
        val elapsed6Min = checkTime6Min - firstSeenTimestamp
        assertTrue("6 minutes should satisfy dwell requirement", elapsed6Min >= dwellTargetMillis)
    }

    @Test
    fun testAutoAttendanceUserPreferencesDefaults() {
        val prefs = UserPreferences()
        assertTrue("Auto-attendance should be enabled by default", prefs.autoAttendanceEnabled)
        assertEquals(5, prefs.autoAttendanceDwellMinutes)
        assertEquals(50, prefs.autoAttendanceRadiusMeters)
    }

    @Test
    fun testNotificationChannelConstants() {
        assertEquals("AttendSmartly_auto_attendance", NotificationHelper.CHANNEL_AUTO_ATTENDANCE_ID)
        assertEquals("Auto-Attendance Alerts", NotificationHelper.CHANNEL_AUTO_ATTENDANCE_NAME)
    }
}
