/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "AttendSmartly_prefs")

data class UserPreferences(
    val notificationsEnabled: Boolean = true,
    val defaultTargetAttendance: Double = 75.0,
    val defaultReminderMinutes: Int = 10,
    val semesterStartDate: String = "",
    val semesterEndDate: String = "",
    val firstDayOfWeek: Int = 1, // 1 = Monday
    val timeFormat24Hr: Boolean = false,
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val dynamicColors: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val confirmMarkingAbsent: Boolean = false,
    val notificationSound: Boolean = true,
    val notificationVibrate: Boolean = true,
    val geminiApiKey: String = ""
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DEFAULT_TARGET_ATTENDANCE = doublePreferencesKey("default_target_attendance")
        val DEFAULT_REMINDER_MINUTES = intPreferencesKey("default_reminder_minutes")
        val SEMESTER_START_DATE = stringPreferencesKey("semester_start_date")
        val SEMESTER_END_DATE = stringPreferencesKey("semester_end_date")
        val FIRST_DAY_OF_WEEK = intPreferencesKey("first_day_of_week")
        val TIME_FORMAT_24HR = booleanPreferencesKey("time_format_24hr")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val CONFIRM_MARKING_ABSENT = booleanPreferencesKey("confirm_marking_absent")
        val NOTIFICATION_SOUND = booleanPreferencesKey("notification_sound")
        val NOTIFICATION_VIBRATE = booleanPreferencesKey("notification_vibrate")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                defaultTargetAttendance = preferences[PreferencesKeys.DEFAULT_TARGET_ATTENDANCE] ?: 75.0,
                defaultReminderMinutes = preferences[PreferencesKeys.DEFAULT_REMINDER_MINUTES] ?: 10,
                semesterStartDate = preferences[PreferencesKeys.SEMESTER_START_DATE] ?: "",
                semesterEndDate = preferences[PreferencesKeys.SEMESTER_END_DATE] ?: "",
                firstDayOfWeek = preferences[PreferencesKeys.FIRST_DAY_OF_WEEK] ?: 1,
                timeFormat24Hr = preferences[PreferencesKeys.TIME_FORMAT_24HR] ?: false,
                themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
                dynamicColors = preferences[PreferencesKeys.DYNAMIC_COLORS] ?: true,
                onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
                confirmMarkingAbsent = preferences[PreferencesKeys.CONFIRM_MARKING_ABSENT] ?: false,
                notificationSound = preferences[PreferencesKeys.NOTIFICATION_SOUND] ?: true,
                notificationVibrate = preferences[PreferencesKeys.NOTIFICATION_VIBRATE] ?: true,
                geminiApiKey = preferences[PreferencesKeys.GEMINI_API_KEY] ?: ""
            )
        }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateDefaultTargetAttendance(target: Double) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_TARGET_ATTENDANCE] = target
        }
    }

    suspend fun updateDefaultReminderMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_REMINDER_MINUTES] = minutes
        }
    }

    suspend fun updateSemesterDates(startDate: String, endDate: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SEMESTER_START_DATE] = startDate
            preferences[PreferencesKeys.SEMESTER_END_DATE] = endDate
        }
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun updateDynamicColors(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLORS] = enabled
        }
    }

    suspend fun updateOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun updateConfirmMarkingAbsent(confirm: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONFIRM_MARKING_ABSENT] = confirm
        }
    }

    suspend fun updateNotificationSound(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_SOUND] = enabled
        }
    }

    suspend fun updateNotificationVibrate(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_VIBRATE] = enabled
        }
    }

    suspend fun updateGeminiApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GEMINI_API_KEY] = apiKey.trim()
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
