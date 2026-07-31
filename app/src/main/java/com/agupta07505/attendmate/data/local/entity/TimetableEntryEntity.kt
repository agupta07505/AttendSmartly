package com.agupta07505.attendmate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timetable_entries",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class TimetableEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val dayOfWeek: Int, // 1 = Monday, 7 = Sunday
    val startTime: String, // e.g. "09:00"
    val endTime: String,   // e.g. "11:00"
    val roomOverride: String = "",
    val teacherOverride: String = "",
    val attendanceUnitCount: Int = 1,
    val reminderMinutes: Int = 10,
    val startDate: String = "", // YYYY-MM-DD
    val endDate: String = "",   // YYYY-MM-DD
    val repeatType: String = "WEEKLY",
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
