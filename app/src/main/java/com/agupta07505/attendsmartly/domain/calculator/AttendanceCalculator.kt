/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.domain.calculator

import com.agupta07505.attendsmartly.domain.model.AttendanceStatus
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

data class AttendanceSummary(
    val totalConductedUnits: Int,
    val presentUnits: Int,
    val absentUnits: Int,
    val cancelledUnits: Int,
    val bunkedUnits: Int = 0,
    val percentage: Double,
    val targetPercentage: Double,
    val safeBunks: Int,
    val requiredUnitsToTarget: Int,
    val statusMessage: String
)

object AttendanceCalculator {

    /**
     * Calculates the attendance summary for given unit statuses and a target percentage.
     */
    fun calculate(
        statuses: List<AttendanceStatus>,
        targetPercentage: Double = 75.0
    ): AttendanceSummary {
        val presentCount = statuses.count { it == AttendanceStatus.PRESENT }
        val absentCount = statuses.count { it == AttendanceStatus.ABSENT }
        val bunkedCount = statuses.count { it == AttendanceStatus.BUNKED }
        val cancelledCount = statuses.count { it == AttendanceStatus.CANCELLED }
        
        return calculate(
            presentUnits = presentCount,
            absentUnits = absentCount + bunkedCount,
            cancelledUnits = cancelledCount,
            bunkedUnits = bunkedCount,
            targetPercentage = targetPercentage
        )
    }

    /**
     * Calculates the attendance summary given counts directly.
     */
    fun calculate(
        presentUnits: Int,
        absentUnits: Int,
        cancelledUnits: Int = 0,
        bunkedUnits: Int = 0,
        targetPercentage: Double = 75.0
    ): AttendanceSummary {
        val safeTarget = targetPercentage.coerceIn(1.0, 100.0)
        val P = presentUnits.toDouble()
        val A = absentUnits.toDouble()
        val T = P + A // Total conducted units excluding cancelled

        val currentPercentage = if (T > 0) (P / T) * 100.0 else 0.0

        // Exact safe bunks calculation with epsilon precision tolerance
        val safeBunks: Int = if (T > 0 && P > 0) {
            val calc = floor(((100.0 * P - safeTarget * T) / safeTarget) + 1e-9)
            if (calc > 0 && !calc.isNaN() && !calc.isInfinite()) calc.toInt() else 0
        } else {
            0
        }

        // Exact required classes calculation with epsilon precision tolerance
        val requiredUnits: Int = if (T == 0.0) {
            0
        } else if (currentPercentage >= safeTarget - 1e-9) {
            0
        } else {
            if (safeTarget >= 100.0) {
                if (absentUnits > 0) Int.MAX_VALUE else 0
            } else {
                val num = (safeTarget * T) - (100.0 * P)
                val den = 100.0 - safeTarget
                val calc = ceil((num / den) - 1e-9)
                if (calc > 0 && !calc.isNaN() && !calc.isInfinite()) calc.toInt() else 0
            }
        }

        val statusMessage = buildStatusMessage(
            totalConducted = T.toInt(),
            presentUnits = presentUnits,
            currentPercentage = currentPercentage,
            targetPercentage = safeTarget,
            safeBunks = safeBunks,
            requiredUnits = requiredUnits
        )

        return AttendanceSummary(
            totalConductedUnits = T.toInt(),
            presentUnits = presentUnits,
            absentUnits = absentUnits,
            cancelledUnits = cancelledUnits,
            bunkedUnits = bunkedUnits,
            percentage = currentPercentage,
            targetPercentage = safeTarget,
            safeBunks = safeBunks,
            requiredUnitsToTarget = requiredUnits,
            statusMessage = statusMessage
        )
    }

    /**
     * Calculates attendance unit count from session duration and unit duration.
     * e.g., 120 mins duration with 60 mins unit = 2 units
     * e.g., 120 mins duration with 120 mins unit = 1 unit
     */
    fun calculateAttendanceUnits(
        durationMinutes: Int,
        unitDurationMinutes: Int
    ): Int {
        if (durationMinutes <= 0 || unitDurationMinutes <= 0) return 1
        val units = (durationMinutes.toDouble() / unitDurationMinutes.toDouble()).roundToInt()
        return units.coerceAtLeast(1)
    }

    private fun buildStatusMessage(
        totalConducted: Int,
        presentUnits: Int,
        currentPercentage: Double,
        targetPercentage: Double,
        safeBunks: Int,
        requiredUnits: Int
    ): String {
        if (totalConducted == 0) {
            return "No attendance has been recorded yet."
        }
        
        val roundedTarget = targetPercentage.roundToInt()
        
        return when {
            requiredUnits == Int.MAX_VALUE -> "Target is 100%. Cannot reach target after an absence."
            currentPercentage < targetPercentage - 1e-9 -> {
                "Attend the next $requiredUnits ${if (requiredUnits == 1) "unit" else "units"} to reach $roundedTarget%."
            }
            safeBunks > 0 -> {
                "You can safely miss $safeBunks ${if (safeBunks == 1) "attendance unit" else "attendance units"} and remain above $roundedTarget%."
            }
            else -> {
                "You are on the margin of your $roundedTarget% target. Avoid missing the next class."
            }
        }
    }
}
