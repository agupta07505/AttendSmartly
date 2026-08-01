/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {

    val isoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val timeFormatter24: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val timeFormatter12: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    fun todayIso(): String = LocalDate.now().format(isoDateFormatter)

    fun formatDateToHuman(dateIso: String): String {
        return try {
            val date = LocalDate.parse(dateIso, isoDateFormatter)
            val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val month = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            "$dayOfWeek, ${date.dayOfMonth} $month ${date.year}"
        } catch (e: Exception) {
            dateIso
        }
    }

    fun formatTime(timeStr: String, use24Hr: Boolean = false): String {
        return try {
            val time = LocalTime.parse(timeStr, timeFormatter24)
            if (use24Hr) {
                time.format(timeFormatter24)
            } else {
                time.format(timeFormatter12)
            }
        } catch (e: Exception) {
            timeStr
        }
    }

    fun getDayOfWeekInt(dateIso: String): Int {
        return try {
            val date = LocalDate.parse(dateIso, isoDateFormatter)
            date.dayOfWeek.value // 1 = Monday, 7 = Sunday
        } catch (e: Exception) {
            1
        }
    }

    fun getDayOfWeekName(dayOfWeek: Int): String {
        return try {
            java.time.DayOfWeek.of(dayOfWeek.coerceIn(1, 7)).getDisplayName(TextStyle.FULL, Locale.getDefault())
        } catch (e: Exception) {
            "Monday"
        }
    }

    fun calculateDurationMinutes(startTime: String, endTime: String): Int {
        return try {
            val start = LocalTime.parse(startTime, timeFormatter24)
            val end = LocalTime.parse(endTime, timeFormatter24)
            var minutes = (end.toSecondOfDay() - start.toSecondOfDay()) / 60
            if (minutes <= 0) minutes += 24 * 60 // Handle overnight class
            minutes
        } catch (e: Exception) {
            60
        }
    }
}
