package com.agupta07505.attendsmartly.util

import com.agupta07505.attendsmartly.data.local.entity.AttendanceSessionEntity
import com.agupta07505.attendsmartly.data.local.entity.AttendanceUnitEntity
import com.agupta07505.attendsmartly.data.local.entity.SubjectEntity
import com.agupta07505.attendsmartly.data.local.entity.TimetableEntryEntity
import com.agupta07505.attendsmartly.data.repository.AttendSmartlyRepository
import com.agupta07505.attendsmartly.domain.model.AttendanceStatus
import java.time.LocalDate

object DemoDataGenerator {

    suspend fun generateDemoData(repository: AttendSmartlyRepository) {
        // 1. Subjects
        val dbms = SubjectEntity(
            name = "Database Management Systems",
            code = "CS301",
            type = "Lecture",
            teacherName = "Dr. Sharma",
            room = "LH-101",
            colorValue = 0xFF1E88E5L, // Blue
            iconName = "Storage",
            defaultSessionDurationMinutes = 120,
            attendanceUnitMinutes = 60,
            defaultAttendanceUnits = 2,
            targetPercentage = 75.0,
            defaultReminderMinutes = 10
        )
        val dbmsId = repository.insertSubject(dbms)

        val lab = SubjectEntity(
            name = "DBMS Practical Lab",
            code = "CS301P",
            type = "Practical Lab",
            teacherName = "Prof. Verma",
            room = "Lab-3",
            colorValue = 0xFF00897BL, // Teal
            iconName = "Computer",
            defaultSessionDurationMinutes = 120,
            attendanceUnitMinutes = 120,
            defaultAttendanceUnits = 1,
            targetPercentage = 75.0,
            defaultReminderMinutes = 15
        )
        val labId = repository.insertSubject(lab)

        val os = SubjectEntity(
            name = "Operating Systems",
            code = "CS302",
            type = "Lecture",
            teacherName = "Dr. Gupta",
            room = "LH-102",
            colorValue = 0xFFD81B60L, // Pink
            iconName = "Memory",
            defaultSessionDurationMinutes = 60,
            attendanceUnitMinutes = 60,
            defaultAttendanceUnits = 1,
            targetPercentage = 80.0,
            defaultReminderMinutes = 10
        )
        val osId = repository.insertSubject(os)

        val dsa = SubjectEntity(
            name = "Data Structures & Algorithms",
            code = "CS303",
            type = "Lecture",
            teacherName = "Prof. Roy",
            room = "LH-103",
            colorValue = 0xFF8E24AAL, // Purple
            iconName = "AccountTree",
            defaultSessionDurationMinutes = 120,
            attendanceUnitMinutes = 60,
            defaultAttendanceUnits = 2,
            targetPercentage = 75.0,
            defaultReminderMinutes = 10
        )
        val dsaId = repository.insertSubject(dsa)

        val web = SubjectEntity(
            name = "Web Development",
            code = "CS304",
            type = "Workshop",
            teacherName = "Er. Kapoor",
            room = "Seminar Hall",
            colorValue = 0xFFFB8C00L, // Orange
            iconName = "Language",
            defaultSessionDurationMinutes = 90,
            attendanceUnitMinutes = 45,
            defaultAttendanceUnits = 2,
            targetPercentage = 75.0,
            defaultReminderMinutes = 10
        )
        val webId = repository.insertSubject(web)

        // 2. Timetable Entries
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value // 1..7

        // Timetable entries for Today and other days
        repository.insertTimetableEntry(
            TimetableEntryEntity(
                subjectId = dbmsId,
                dayOfWeek = dayOfWeek,
                startTime = "10:00",
                endTime = "12:00",
                roomOverride = "LH-101",
                attendanceUnitCount = 2,
                reminderMinutes = 10
            )
        )

        repository.insertTimetableEntry(
            TimetableEntryEntity(
                subjectId = labId,
                dayOfWeek = dayOfWeek,
                startTime = "14:00",
                endTime = "16:00",
                roomOverride = "Lab-3",
                attendanceUnitCount = 1,
                reminderMinutes = 15
            )
        )

        val otherDay1 = if (dayOfWeek % 7 + 1 == 7) 1 else dayOfWeek % 7 + 1
        val otherDay2 = if ((dayOfWeek + 1) % 7 + 1 == 7) 2 else (dayOfWeek + 1) % 7 + 1

        repository.insertTimetableEntry(
            TimetableEntryEntity(
                subjectId = osId,
                dayOfWeek = otherDay1,
                startTime = "09:00",
                endTime = "10:00",
                attendanceUnitCount = 1,
                reminderMinutes = 10
            )
        )

        repository.insertTimetableEntry(
            TimetableEntryEntity(
                subjectId = dsaId,
                dayOfWeek = otherDay1,
                startTime = "11:00",
                endTime = "13:00",
                attendanceUnitCount = 2,
                reminderMinutes = 10
            )
        )

        repository.insertTimetableEntry(
            TimetableEntryEntity(
                subjectId = webId,
                dayOfWeek = otherDay2,
                startTime = "15:00",
                endTime = "16:30",
                attendanceUnitCount = 2,
                reminderMinutes = 10
            )
        )

        // 3. Past Sessions and History for Past Days (to show realistic stats)
        val pastDays = listOf(1L, 2L, 3L, 4L, 5L, 7L)
        for (daysAgo in pastDays) {
            val pastDate = today.minusDays(daysAgo)
            val pastDateIso = pastDate.format(DateUtils.isoDateFormatter)

            // Past DBMS Session (2 units: 1 Present, 1 Present)
            val dbmsSession = AttendanceSessionEntity(
                subjectId = dbmsId,
                sessionDate = pastDateIso,
                startTime = "10:00",
                endTime = "12:00",
                expectedUnitCount = 2
            )
            val u1 = AttendanceUnitEntity(sessionId = 0, unitIndex = 0, status = AttendanceStatus.PRESENT.name)
            val u2 = AttendanceUnitEntity(sessionId = 0, unitIndex = 1, status = if (daysAgo == 5L) AttendanceStatus.ABSENT.name else AttendanceStatus.PRESENT.name)
            repository.createOrUpdateSessionWithUnits(dbmsSession, listOf(u1, u2))

            // Past Lab Session (1 unit: 1 Present)
            val labSession = AttendanceSessionEntity(
                subjectId = labId,
                sessionDate = pastDateIso,
                startTime = "14:00",
                endTime = "16:00",
                expectedUnitCount = 1
            )
            val labUnit = AttendanceUnitEntity(sessionId = 0, unitIndex = 0, status = AttendanceStatus.PRESENT.name)
            repository.createOrUpdateSessionWithUnits(labSession, listOf(labUnit))
        }
    }
}
