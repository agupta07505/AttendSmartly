/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.ui.screens.timetable

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agupta07505.attendsmartly.data.local.entity.SubjectEntity
import com.agupta07505.attendsmartly.data.local.entity.TimetableEntryEntity
import com.agupta07505.attendsmartly.data.preferences.UserPreferencesRepository
import com.agupta07505.attendsmartly.data.remote.gemini.TimetableOcrService
import com.agupta07505.attendsmartly.data.repository.AttendSmartlyRepository
import com.agupta07505.attendsmartly.domain.model.ParsedTimetableItem
import com.agupta07505.attendsmartly.domain.model.TimetableWithSubject
import com.agupta07505.attendsmartly.util.DateUtils
import com.agupta07505.attendsmartly.util.ExportImportUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TimetableViewModel(
    private val repository: AttendSmartlyRepository,
    private val preferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    private val ocrService = TimetableOcrService()

    private val _ocrState = MutableStateFlow<AiTimetableOcrState>(AiTimetableOcrState.Idle)
    val ocrState: StateFlow<AiTimetableOcrState> = _ocrState.asStateFlow()

    private val _selectedDayOfWeek = MutableStateFlow(LocalDate.now().dayOfWeek.value)
    val selectedDayOfWeek: StateFlow<Int> = _selectedDayOfWeek.asStateFlow()

    val activeSubjects: StateFlow<List<SubjectEntity>> = repository.activeSubjects
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val geminiApiKey: StateFlow<String> = (preferencesRepository?.userPreferencesFlow
        ?.map { it.geminiApiKey }
        ?: flowOf(""))
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun saveGeminiApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesRepository?.updateGeminiApiKey(apiKey)
        }
    }

    val entriesForSelectedDay: StateFlow<List<TimetableWithSubject>> = combine(
        _selectedDayOfWeek,
        repository.allActiveTimetableEntries,
        repository.activeSubjects
    ) { day, entries, subjects ->
        entries.filter { it.dayOfWeek == day }
            .mapNotNull { entry ->
                val subject = subjects.find { it.id == entry.subjectId } ?: return@mapNotNull null
                TimetableWithSubject(entry = entry, subject = subject)
            }
            .sortedBy { it.entry.startTime }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allTimetableWithSubjects: StateFlow<List<TimetableWithSubject>> = combine(
        repository.allActiveTimetableEntries,
        repository.activeSubjects
    ) { entries, subjects ->
        entries.mapNotNull { entry ->
            val subject = subjects.find { it.id == entry.subjectId } ?: return@mapNotNull null
            TimetableWithSubject(entry = entry, subject = subject)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun selectDayOfWeek(day: Int) {
        _selectedDayOfWeek.value = day
    }

    fun addTimetableEntry(entry: TimetableEntryEntity) {
        viewModelScope.launch {
            repository.insertTimetableEntry(entry)
        }
    }

    fun updateTimetableEntry(entry: TimetableEntryEntity) {
        viewModelScope.launch {
            repository.updateTimetableEntry(entry)
        }
    }

    fun updateTimetableEntryFromDate(
        oldEntryId: Long,
        updatedEntry: TimetableEntryEntity,
        effectiveStartDate: String = DateUtils.todayIso()
    ) {
        viewModelScope.launch {
            repository.updateTimetableEntryFromDate(oldEntryId, updatedEntry, effectiveStartDate)
        }
    }

    fun retireTimetableEntry(id: Long, effectiveDate: String = DateUtils.todayIso()) {
        viewModelScope.launch {
            repository.retireTimetableEntry(id, effectiveDate)
        }
    }

    fun deleteTimetableEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteTimetableEntry(id)
        }
    }

    fun copyDayEntriesToOtherDays(sourceDay: Int, targetDays: List<Int>) {
        viewModelScope.launch {
            val sourceEntries = entriesForSelectedDay.value
            for (targetDay in targetDays) {
                for (item in sourceEntries) {
                    val copy = item.entry.copy(
                        id = 0,
                        dayOfWeek = targetDay,
                        createdAt = System.currentTimeMillis()
                    )
                    repository.insertTimetableEntry(copy)
                }
            }
        }
    }

    fun exportTimetable(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = ExportImportUtils.exportTimetableToJson(repository)
            onResult(json)
        }
    }

    fun importTimetable(
        jsonString: String,
        effectiveStartDate: String = DateUtils.todayIso(),
        replaceExisting: Boolean = false,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val (success, message) = ExportImportUtils.importTimetableFromJson(
                jsonString = jsonString,
                repository = repository,
                effectiveStartDate = effectiveStartDate,
                replaceExisting = replaceExisting
            )
            onResult(success, message)
        }
    }

    fun startOcrFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _ocrState.value = AiTimetableOcrState.Processing(imageUri = uri, stepMessage = "Reading timetable image...")
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap == null) {
                    _ocrState.value = AiTimetableOcrState.Error("Failed to decode image from Uri")
                    return@launch
                }
                _ocrState.value = AiTimetableOcrState.Processing(imageUri = uri, stepMessage = "Extracting classes & timings from timetable...")
                val customKey = preferencesRepository?.userPreferencesFlow?.first()?.geminiApiKey
                val items = ocrService.extractTimetableFromImage(bitmap, isSampleImage = false, customApiKey = customKey)
                if (items.isEmpty()) {
                    _ocrState.value = AiTimetableOcrState.Error("No valid class schedule detected. Please ensure the timetable is clearly visible.")
                } else {
                    _ocrState.value = AiTimetableOcrState.Preview(imageUri = uri, parsedItems = items)
                }
            } catch (e: Exception) {
                _ocrState.value = AiTimetableOcrState.Error(e.localizedMessage ?: "Detection error occurred")
            }
        }
    }

    fun startOcrFromBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _ocrState.value = AiTimetableOcrState.Processing(imageUri = null, stepMessage = "Analyzing sample timetable image...")
            try {
                val customKey = preferencesRepository?.userPreferencesFlow?.first()?.geminiApiKey
                val items = ocrService.extractTimetableFromImage(bitmap, isSampleImage = true, customApiKey = customKey)
                if (items.isEmpty()) {
                    _ocrState.value = AiTimetableOcrState.Error("No valid class schedule detected.")
                } else {
                    _ocrState.value = AiTimetableOcrState.Preview(imageUri = null, parsedItems = items)
                }
            } catch (e: Exception) {
                _ocrState.value = AiTimetableOcrState.Error(e.localizedMessage ?: "Detection error occurred")
            }
        }
    }

    fun confirmOcrImport(
        items: List<ParsedTimetableItem>,
        replaceExisting: Boolean,
        effectiveStartDate: String = DateUtils.todayIso()
    ) {
        viewModelScope.launch {
            try {
                repository.importParsedTimetable(items, replaceExisting, effectiveStartDate)
                _ocrState.value = AiTimetableOcrState.Success("Successfully imported ${items.size} class schedule entries into your timetable starting from ${DateUtils.formatDateToHuman(effectiveStartDate)}!")
            } catch (e: Exception) {
                _ocrState.value = AiTimetableOcrState.Error("Failed to save schedule: ${e.localizedMessage}")
            }
        }
    }

    fun resetOcrState() {
        _ocrState.value = AiTimetableOcrState.Idle
    }

    fun importTimetableFromUri(
        context: Context,
        uri: Uri,
        effectiveStartDate: String = DateUtils.todayIso(),
        replaceExisting: Boolean = false,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val json = ExportImportUtils.readTextFromUri(context, uri)
            if (json != null) {
                val (success, message) = ExportImportUtils.importTimetableFromJson(
                    jsonString = json,
                    repository = repository,
                    effectiveStartDate = effectiveStartDate,
                    replaceExisting = replaceExisting
                )
                onResult(success, message)
            } else {
                onResult(false, "Failed to read JSON file.")
            }
        }
    }

    fun markPastAttendance(subjectId: Long, attendedCount: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (success, message) = repository.markPastAttendanceForSubject(subjectId, attendedCount)
            onResult(success, message)
        }
    }
}
