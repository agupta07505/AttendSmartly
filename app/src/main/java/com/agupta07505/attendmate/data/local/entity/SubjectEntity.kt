package com.agupta07505.attendmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String = "",
    val type: String = "Lecture",
    val teacherName: String = "",
    val room: String = "",
    val colorValue: Long = 0xFF2196F3L,
    val iconName: String = "Book",
    val defaultSessionDurationMinutes: Int = 60,
    val attendanceUnitMinutes: Int = 60,
    val defaultAttendanceUnits: Int = 1,
    val targetPercentage: Double = 75.0,
    val defaultReminderMinutes: Int = 10,
    val notes: String = "",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
