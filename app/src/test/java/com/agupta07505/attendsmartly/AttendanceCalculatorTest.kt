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
        // Safe bunks formula: floor((8 - 0.75 * 10) / 0.75) = floor(0.5 / 0.75) = 0
        // Wait: floor((8 - 7.5)/0.75) = 0.
        // Let's test with 9 Present out of 10 Conducted = 90% (Target 75%)
        val statuses2 = listOf(
            AttendanceStatus.PRESENT, AttendanceStatus.PRESENT, AttendanceStatus.PRESENT, AttendanceStatus.PRESENT,
            AttendanceStatus.PRESENT, AttendanceStatus.PRESENT, AttendanceStatus.PRESENT, AttendanceStatus.PRESENT,
            AttendanceStatus.PRESENT, AttendanceStatus.ABSENT
        )
        val summary2 = AttendanceCalculator.calculate(statuses2, targetPercentage = 75.0)
        assertEquals(10, summary2.totalConductedUnits)
        assertEquals(9, summary2.presentUnits)
        assertEquals(90.0, summary2.percentage, 0.01)
        // floor((9 - 0.75 * 10) / 0.75) = floor(1.5 / 0.75) = 2 safe bunks!
        assertEquals(2, summary2.safeBunks)
    }

    @Test
    fun testRequiredClassesCalculation() {
        // 5 Present out of 10 Conducted = 50% (Target 75%)
        val statuses = List(5) { AttendanceStatus.PRESENT } + List(5) { AttendanceStatus.ABSENT }
        val summary = AttendanceCalculator.calculate(statuses, targetPercentage = 75.0)
        assertEquals(10, summary.totalConductedUnits)
        assertEquals(5, summary.presentUnits)
        assertEquals(50.0, summary.percentage, 0.01)
        // Required formula: ceil((0.75 * 10 - 5) / (1 - 0.75)) = ceil((7.5 - 5) / 0.25) = ceil(2.5 / 0.25) = 10 classes
        assertEquals(10, summary.requiredUnitsToTarget)
    }

    @Test
    fun testCancelledClassesExcludedFromConducted() {
        val statuses = listOf(
            AttendanceStatus.PRESENT,
            AttendanceStatus.PRESENT,
            AttendanceStatus.CANCELLED,
            AttendanceStatus.ABSENT
        )
        val summary = AttendanceCalculator.calculate(statuses, targetPercentage = 75.0)
        assertEquals(3, summary.totalConductedUnits)
        assertEquals(2, summary.presentUnits)
        assertEquals(1, summary.absentUnits)
        assertEquals(1, summary.cancelledUnits)
        assertEquals(66.67, summary.percentage, 0.01)
    }
}
