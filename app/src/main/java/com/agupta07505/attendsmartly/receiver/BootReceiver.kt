package com.agupta07505.attendsmartly.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.agupta07505.attendsmartly.worker.ReminderWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ReminderWorker.schedulePeriodicReminderCheck(context)
        }
    }
}
