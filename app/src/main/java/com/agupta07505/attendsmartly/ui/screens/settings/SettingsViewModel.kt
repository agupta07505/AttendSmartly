/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agupta07505.attendsmartly.data.preferences.UserPreferences
import com.agupta07505.attendsmartly.data.preferences.UserPreferencesRepository
import com.agupta07505.attendsmartly.data.repository.AttendSmartlyRepository
import com.agupta07505.attendsmartly.util.DemoDataGenerator
import com.agupta07505.attendsmartly.util.ExportImportUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: AttendSmartlyRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    fun updateDefaultTargetAttendance(target: Double) {
        viewModelScope.launch {
            preferencesRepository.updateDefaultTargetAttendance(target)
        }
    }

    fun updateDefaultReminderMinutes(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.updateDefaultReminderMinutes(minutes)
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.updateThemeMode(mode)
        }
    }

    fun updateDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateDynamicColors(enabled)
        }
    }

    fun updateGeminiApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesRepository.updateGeminiApiKey(apiKey)
        }
    }

    fun exportBackup(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = ExportImportUtils.exportDataToJson(context, repository, uri)
            onResult(success)
        }
    }

    fun importBackup(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = ExportImportUtils.importDataFromJson(context, repository, uri)
            onResult(success)
        }
    }

    fun exportCsv(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = ExportImportUtils.exportAttendanceCsv(context, repository, uri)
            onResult(success)
        }
    }

    fun loadDemoData(onComplete: () -> Unit) {
        viewModelScope.launch {
            DemoDataGenerator.generateDemoData(repository)
            onComplete()
        }
    }

    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearAllData()
            onComplete()
        }
    }
}
