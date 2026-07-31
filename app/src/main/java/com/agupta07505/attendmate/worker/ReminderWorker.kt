package com.agupta07505.attendmate.worker

import android.content.Context
import androidx.work.*
import com.agupta07505.attendmate.AttendMateApplication
import com.agupta07505.attendmate.notification.NotificationHelper
import com.agupta07505.attendmate.util.DateUtils
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = context.applicationContext as AttendMateApplication
            val repository = app.repository
            val todayIso = DateUtils.todayIso()
            val dayOfWeek = DateUtils.getDayOfWeekInt(todayIso)

            val activeTimetable = repository.getTimetableForDay(dayOfWeek).first()
            val now = LocalTime.now()

            for (entry in activeTimetable) {
                val subject = repository.getSubjectById(entry.subjectId) ?: continue
                if (subject.isArchived) continue

                val reminderMins = if (entry.reminderMinutes > 0) {
                    entry.reminderMinutes
                } else if (subject.defaultReminderMinutes > 0) {
                    subject.defaultReminderMinutes
                } else {
                    0
                }

                if (reminderMins <= 0) continue

                val classStartTime = LocalTime.parse(entry.startTime, DateUtils.timeFormatter24)
                val reminderTime = classStartTime.minusMinutes(reminderMins.toLong())

                // Check if we are within 15 minutes of the reminder time
                val diffMinutes = java.time.Duration.between(now, reminderTime).toMinutes()
                if (diffMinutes in 0..15) {
                    val durationMins = DateUtils.calculateDurationMinutes(entry.startTime, entry.endTime)
                    
                    NotificationHelper.showClassReminderNotification(
                        context = context,
                        notificationId = entry.id.toInt(),
                        sessionId = entry.id,
                        subjectName = subject.name,
                        startTime = DateUtils.formatTime(entry.startTime),
                        room = if (entry.roomOverride.isNotBlank()) entry.roomOverride else subject.room,
                        teacher = if (entry.teacherOverride.isNotBlank()) entry.teacherOverride else subject.teacherName,
                        durationMinutes = durationMins,
                        unitCount = entry.attendanceUnitCount,
                        minutesBefore = reminderMins
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "attendmate_reminder_work"

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
