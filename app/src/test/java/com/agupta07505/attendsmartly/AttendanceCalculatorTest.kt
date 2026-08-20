/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly

import com.agupta07505.attendsmartly.domain.calculator.AttendanceCalculator
import com.agupta07505.attendsmartly.domain.model.AttendanceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceCalculatorTest {

    @Test
    fun testEmptyAttendance() {
        val summary = AttendanceCalculator.calculate(emptyList(), targetPercentage = 75.0)
        assertEquals(0, summary.totalConductedUnits)
        assertEquals(0, summary.presentUnits)
        assertEquals(0.0, summary.percentage, 0.01)
        assertEquals(0, summary.safeBunks)
        assertEquals(0, summary.requiredUnitsToTarget)
    }

    @Test
    fun testSafeBunksCalculation() {
        // 8 Present out of 10 Conducted = 80% (Target 75%)
        val statuses = listOf(
            AttendanceStatus.PRESENT, AttendanceStatus.PRESENT, AttendanceStatus.PRESENT, AttendanceStatus.PRESENT,
            AttendanceStatus.PRESENT, AttendanceStatus.PRESENT, AttendanceStatus.PRESENT, AttendanceStatus.PRESENT,
            AttendanceStatus.ABSENT, AttendanceStatus.ABSENT
        )
        val summary = AttendanceCalculator.calculate(statuses, targetPercentage = 75.0)
        assertEquals(10, summary.totalConductedUnits)
        assertEquals(8, summary.presentUnits)
        assertEquals(80.0, summary.percentage, 0.01)

        // 9 Present out of 10 Conducted = 90% (Target 75%)
        val statuses2 = listOf(
            AttendanceStatus.PRESENT, AttendanceStatus.PRESENT, AttendanceStatus.PRESENT, AttendanceStatus.PRESENT,
            AttendanceStatus.PRESENT, AttendanceStatus.PRESENT, AttendanceStatus.PRESENT, AttendanceStatus.PRESENT,
            AttendanceStatus.PRESENT, AttendanceStatus.ABSENT
        )
        val summary2 = AttendanceCalculator.calculate(statuses2, targetPercentage = 75.0)
        assertEquals(10, summary2.totalConductedUnits)
        assertEquals(9, summary2.presentUnits)
        assertEquals(90.0, summary2.percentage, 0.01)
        assertEquals(2, summary2.safeBunks)
    }

    @Test
    fun testPrecisionSafeBunks() {
        // 14 Present out of 19 Conducted = 73.68% (Target 70%)
        // If 1 miss: 14 / 20 = 70.0% >= 70% -> Exactly 1 safe bunk!
        val summary = AttendanceCalculator.calculate(presentUnits = 14, absentUnits = 5, targetPercentage = 70.0)
        assertEquals(1, summary.safeBunks)
    }

    @Test
    fun testRequiredClassesCalculation() {
        // 5 Present out of 10 Conducted = 50% (Target 75%)
        val statuses = List(5) { AttendanceStatus.PRESENT } + List(5) { AttendanceStatus.ABSENT }
        val summary = AttendanceCalculator.calculate(statuses, targetPercentage = 75.0)
        assertEquals(10, summary.totalConductedUnits)
        assertEquals(5, summary.presentUnits)
        assertEquals(50.0, summary.percentage, 0.01)
        assertEquals(10, summary.requiredUnitsToTarget)
    }

    @Test
    fun testTimetableChangePastAttendanceIntegrity() {
        // Old timetable had 4 classes attended in the past, new timetable has 2 classes attended
        val pastSessionsUnits = listOf(
            AttendanceStatus.PRESENT,
            AttendanceStatus.PRESENT,
            AttendanceStatus.PRESENT,
            AttendanceStatus.ABSENT
        )
        val newTimetableUnits = listOf(
            AttendanceStatus.PRESENT,
            AttendanceStatus.PRESENT
        )
        val combinedAllHistory = pastSessionsUnits + newTimetableUnits
        val summary = AttendanceCalculator.calculate(combinedAllHistory, targetPercentage = 75.0)

        // 5 Present out of 6 Conducted = 83.33%
        assertEquals(6, summary.totalConductedUnits)
        assertEquals(5, summary.presentUnits)
        assertEquals(1, summary.absentUnits)
        assertEquals(83.33, summary.percentage, 0.01)
        assertTrue(summary.percentage >= 75.0)
    }
}

