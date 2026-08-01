package com.agupta07505.attendsmartly.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.agupta07505.attendsmartly.AttendSmartlyApplication
import com.agupta07505.attendsmartly.domain.model.AttendanceStatus
import com.agupta07505.attendsmartly.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, sessionId.toInt())

        val app = context.applicationContext as AttendSmartlyApplication
        val repository = app.repository

        when (action) {
            ACTION_MARK_PRESENT -> {
                if (sessionId != -1L) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            repository.markNextUnitStatus(sessionId, AttendanceStatus.PRESENT)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
                cancelNotification(context, notificationId)
            }
            ACTION_MARK_ABSENT -> {
                if (sessionId != -1L) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            repository.markNextUnitStatus(sessionId, AttendanceStatus.ABSENT)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
                cancelNotification(context, notificationId)
            }
            ACTION_TRIGGER_REMINDER -> {
                val subjectName = intent.getStringExtra(EXTRA_SUBJECT_NAME) ?: "Class"
                val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
                
                NotificationHelper.showClassReminderNotification(
                    context = context,
                    notificationId = notificationId,
                    sessionId = sessionId,
                    subjectName = subjectName,
                    startTime = startTime,
                    room = "",
                    teacher = "",
                    durationMinutes = 60,
                    unitCount = 1,
                    minutesBefore = 10
                )
            }
        }
    }

    private fun cancelNotification(context: Context, notificationId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
    }

    companion object {
        const val ACTION_MARK_PRESENT = "com.agupta07505.attendsmartly.ACTION_MARK_PRESENT"
        const val ACTION_MARK_ABSENT = "com.agupta07505.attendsmartly.ACTION_MARK_ABSENT"
        const val ACTION_TRIGGER_REMINDER = "com.agupta07505.attendsmartly.ACTION_TRIGGER_REMINDER"

        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_SUBJECT_NAME = "extra_subject_name"
        const val EXTRA_START_TIME = "extra_start_time"
    }
}
