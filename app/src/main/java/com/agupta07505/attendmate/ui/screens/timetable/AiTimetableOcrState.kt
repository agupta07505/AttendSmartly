package com.agupta07505.attendmate.ui.screens.timetable

import android.net.Uri
import com.agupta07505.attendmate.domain.model.ParsedTimetableItem

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
