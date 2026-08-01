/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.ui.screens.timetable

import android.net.Uri
import com.agupta07505.attendsmartly.domain.model.ParsedTimetableItem

sealed interface AiTimetableOcrState {
    object Idle : AiTimetableOcrState
    data class Processing(val imageUri: Uri?, val stepMessage: String) : AiTimetableOcrState
    data class Preview(
        val imageUri: Uri?,
        val parsedItems: List<ParsedTimetableItem>,
        val selectedDay: Int = 1,
        val replaceExisting: Boolean = false
    ) : AiTimetableOcrState
    data class Error(val message: String) : AiTimetableOcrState
    data class Success(val message: String) : AiTimetableOcrState
}
