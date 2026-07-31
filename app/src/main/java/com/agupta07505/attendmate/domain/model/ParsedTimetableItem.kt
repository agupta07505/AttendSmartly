package com.agupta07505.attendmate.domain.model

import java.util.UUID

data class ParsedTimetableItem(
    val id: String = UUID.randomUUID().toString(),
    var subjectName: String,
    var subjectCode: String = "",
    var startTime: String,
    var endTime: String,
    var roomLocation: String = "",
    var dayOfWeek: Int // 1 = Monday, 7 = Sunday
)
