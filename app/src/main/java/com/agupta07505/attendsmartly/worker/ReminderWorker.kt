/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.worker

import android.content.Context
import androidx.work.*
import com.agupta07505.attendsmartly.AttendSmartlyApplication
import com.agupta07505.attendsmartly.notification.NotificationHelper
import com.agupta07505.attendsmartly.util.DateUtils
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class ReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = context.applicationContext as AttendSmartlyApplication
            val repository = app.repository
            val prefsRepo = app.userPreferencesRepository
            val userPrefs = prefsRepo.userPreferencesFlow.first()

            if (!userPrefs.notificationsEnabled) {
                return Result.success()
            }

            val todayIso = DateUtils.todayIso()
            val dayOfWeek = DateUtils.getDayOfWeekInt(todayIso)

            val activeTimetable = repository.getTimetableForDay(dayOfWeek).first()
            val now = LocalTime.now()
            val today = LocalDate.now()

            for (entry in activeTimetable) {
                val subject = repository.getSubjectById(entry.subjectId) ?: continue
                if (subject.isArchived) continue

                val reminderMins = if (entry.reminderMinutes > 0) {
                    entry.reminderMinutes
                } else if (subject.defaultReminderMinutes > 0) {
                    subject.defaultReminderMinutes
                } else if (userPrefs.defaultReminderMinutes > 0) {
                    userPrefs.defaultReminderMinutes
                } else {
                    10
                }

                if (reminderMins <= 0) continue

                val classStartTime = LocalTime.parse(entry.startTime, DateUtils.timeFormatter24)
                val reminderTime = classStartTime.minusMinutes(reminderMins.toLong())
                val durationMins = DateUtils.calculateDurationMinutes(entry.startTime, entry.endTime)
                val room = if (entry.roomOverride.isNotBlank()) entry.roomOverride else subject.room
                val teacher = if (entry.teacherOverride.isNotBlank()) entry.teacherOverride else subject.teacherName

                if (now.isBefore(reminderTime)) {
                    // Schedule exact alarm for reminderTime
                    val reminderDateTime = LocalDateTime.of(today, reminderTime)
                    val triggerAtMillis = reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    NotificationHelper.scheduleAlarm(
                        context = context,
                        triggerAtMillis = triggerAtMillis,
                        sessionId = entry.id,
                        subjectName = subject.name,
                        startTime = DateUtils.formatTime(entry.startTime),
                        room = room,
                        teacher = teacher,
                        durationMinutes = durationMins,
                        unitCount = entry.attendanceUnitCount,
                        minutesBefore = reminderMins
                    )
                } else if (!now.isBefore(reminderTime) && now.isBefore(classStartTime)) {
                    // Fallback: If now is within 0..2 minutes past reminderTime, show notification directly
                    val minutesPastReminder = java.time.Duration.between(reminderTime, now).toMinutes()
                    if (minutesPastReminder in 0..2) {
                        NotificationHelper.showClassReminderNotification(
                            context = context,
                            notificationId = entry.id.toInt(),
                            sessionId = entry.id,
                            subjectName = subject.name,
                            startTime = DateUtils.formatTime(entry.startTime),
                            room = room,
                            teacher = teacher,
                            durationMinutes = durationMins,
                            unitCount = entry.attendanceUnitCount,
                            minutesBefore = reminderMins
                        )
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "AttendSmartly_reminder_work"

        fun schedulePeriodicReminderCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
