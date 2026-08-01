/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly

import android.app.Application
import com.agupta07505.attendsmartly.data.local.database.AppDatabase
import com.agupta07505.attendsmartly.data.preferences.UserPreferencesRepository
import com.agupta07505.attendsmartly.data.repository.AttendSmartlyRepository
import com.agupta07505.attendsmartly.notification.NotificationHelper
import com.agupta07505.attendsmartly.worker.ReminderWorker

class AttendSmartlyApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: AttendSmartlyRepository
        private set

    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        
        database = AppDatabase.getInstance(this)
        userPreferencesRepository = UserPreferencesRepository(this)
        
        repository = AttendSmartlyRepository(
            subjectDao = database.subjectDao(),
            timetableDao = database.timetableDao(),
            attendanceDao = database.attendanceDao(),
            holidayDao = database.holidayDao()
        )

        NotificationHelper.createNotificationChannel(this)
        ReminderWorker.schedulePeriodicReminderCheck(this)
    }
}
