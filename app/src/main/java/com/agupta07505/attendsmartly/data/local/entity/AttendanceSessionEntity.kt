/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_sessions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["sessionDate"])
    ]
)
data class AttendanceSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val timetableEntryId: Long? = null,
    val sessionDate: String, // YYYY-MM-DD
    val startTime: String,   // e.g. "10:00"
    val endTime: String,     // e.g. "12:00"
    val expectedUnitCount: Int = 1,
    val notes: String = "",
    val isRescheduled: Boolean = false,
    val originalDate: String? = null, // e.g. "2026-08-19" if this was rescheduled from another date
    val originalTime: String? = null, // e.g. "10:00"
    val rescheduledToDate: String? = null, // e.g. "2026-08-20" if this session was rescheduled to another date
    val rescheduledToTime: String? = null, // e.g. "14:00"
    val rescheduledReason: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
