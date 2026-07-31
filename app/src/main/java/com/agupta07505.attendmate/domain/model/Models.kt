package com.agupta07505.attendmate.domain.model

import com.agupta07505.attendmate.data.local.entity.AttendanceSessionEntity
import com.agupta07505.attendmate.data.local.entity.AttendanceUnitEntity
import com.agupta07505.attendmate.data.local.entity.SubjectEntity
import com.agupta07505.attendmate.data.local.entity.TimetableEntryEntity

data class SessionWithUnits(
    val session: AttendanceSessionEntity,
    val units: List<AttendanceUnitEntity>
)

data class TimetableWithSubject(
    val entry: TimetableEntryEntity,
    val subject: SubjectEntity
)

data class ClassScheduleItem(
    val session: AttendanceSessionEntity?,
    val timetableEntry: TimetableEntryEntity,
    val subject: SubjectEntity,
    val units: List<AttendanceUnitEntity>,
    val isHoliday: Boolean = false,
    val holidayTitle: String? = null
)
