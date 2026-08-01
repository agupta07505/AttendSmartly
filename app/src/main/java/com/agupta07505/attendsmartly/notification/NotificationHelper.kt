/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.agupta07505.attendsmartly.MainActivity
import com.agupta07505.attendsmartly.R
import com.agupta07505.attendsmartly.receiver.ReminderNotificationReceiver

object NotificationHelper {

    const val CHANNEL_ID = "AttendSmartly_class_reminders"
    const val CHANNEL_NAME = "Class Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming college classes"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showClassReminderNotification(
        context: Context,
        notificationId: Int,
        sessionId: Long,
        subjectName: String,
        startTime: String,
        room: String,
        teacher: String,
        durationMinutes: Int,
        unitCount: Int,
        minutesBefore: Int
    ) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Present action
        val presentIntent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ReminderNotificationReceiver.ACTION_MARK_PRESENT
            putExtra(ReminderNotificationReceiver.EXTRA_SESSION_ID, sessionId)
            putExtra(ReminderNotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val presentPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            presentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Absent action
        val absentIntent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ReminderNotificationReceiver.ACTION_MARK_ABSENT
            putExtra(ReminderNotificationReceiver.EXTRA_SESSION_ID, sessionId)
            putExtra(ReminderNotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val absentPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            absentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val locationInfo = if (room.isNotBlank()) " in $room" else ""
        val teacherInfo = if (teacher.isNotBlank()) " with $teacher" else ""
        val timeMessage = if (minutesBefore > 0) "starts in $minutesBefore mins at $startTime" else "starts now at $startTime"
        
        val contentText = "$subjectName$locationInfo$teacherInfo ($durationMinutes mins • $unitCount ${if (unitCount == 1) "unit" else "units"}) $timeMessage."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Upcoming Class: $subjectName")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, "Mark Present", presentPendingIntent)
            .addAction(0, "Mark Absent", absentPendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    fun scheduleAlarm(context: Context, triggerAtMillis: Long, sessionId: Long, subjectName: String, startTime: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ReminderNotificationReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderNotificationReceiver.EXTRA_SESSION_ID, sessionId)
            putExtra(ReminderNotificationReceiver.EXTRA_SUBJECT_NAME, subjectName)
            putExtra(ReminderNotificationReceiver.EXTRA_START_TIME, startTime)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
