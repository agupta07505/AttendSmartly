package com.agupta07505.attendmate

import android.app.Application
import com.agupta07505.attendmate.data.local.database.AppDatabase
import com.agupta07505.attendmate.data.preferences.UserPreferencesRepository
import com.agupta07505.attendmate.data.repository.AttendMateRepository
import com.agupta07505.attendmate.notification.NotificationHelper
import com.agupta07505.attendmate.worker.ReminderWorker

class AttendMateApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: AttendMateRepository
        private set

    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        
        database = AppDatabase.getInstance(this)
        userPreferencesRepository = UserPreferencesRepository(this)
        
        repository = AttendMateRepository(
            subjectDao = database.subjectDao(),
            timetableDao = database.timetableDao(),
            attendanceDao = database.attendanceDao(),
            holidayDao = database.holidayDao()
        )

        NotificationHelper.createNotificationChannel(this)
        ReminderWorker.schedulePeriodicReminderCheck(this)
    }
}
