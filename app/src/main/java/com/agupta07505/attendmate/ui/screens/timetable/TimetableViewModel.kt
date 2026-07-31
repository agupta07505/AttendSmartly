package com.agupta07505.attendmate.ui.screens.timetable

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agupta07505.attendmate.data.local.entity.SubjectEntity
import com.agupta07505.attendmate.data.local.entity.TimetableEntryEntity
import com.agupta07505.attendmate.data.repository.AttendMateRepository
import com.agupta07505.attendmate.domain.model.TimetableWithSubject
import com.agupta07505.attendmate.util.DateUtils
import com.agupta07505.attendmate.util.ExportImportUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.agupta07505.attendmate.data.remote.gemini.TimetableOcrService
import com.agupta07505.attendmate.domain.model.ParsedTimetableItem

@OptIn(ExperimentalCoroutinesApi::class)
class TimetableViewModel(
    private val repository: AttendMateRepository
) : ViewModel() {

    private val ocrService = TimetableOcrService()

    private val _ocrState = MutableStateFlow<AiTimetableOcrState>(AiTimetableOcrState.Idle)
    val ocrState: StateFlow<AiTimetableOcrState> = _ocrState.asStateFlow()

    private val _selectedDayOfWeek = MutableStateFlow(LocalDate.now().dayOfWeek.value)
    val selectedDayOfWeek: StateFlow<Int> = _selectedDayOfWeek.asStateFlow()

    val activeSubjects: StateFlow<List<SubjectEntity>> = repository.activeSubjects
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

    fun importTimetable(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (success, message) = ExportImportUtils.importTimetableFromJson(jsonString, repository)
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
                _ocrState.value = AiTimetableOcrState.Processing(imageUri = uri, stepMessage = "Extracting classes & timings using Gemini AI...")
                val items = ocrService.extractTimetableFromImage(bitmap, isSampleImage = false)
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
            _ocrState.value = AiTimetableOcrState.Processing(imageUri = null, stepMessage = "Analyzing sample timetable using Gemini AI...")
            try {
                val items = ocrService.extractTimetableFromImage(bitmap, isSampleImage = true)
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

    fun confirmOcrImport(items: List<ParsedTimetableItem>, replaceExisting: Boolean) {
        viewModelScope.launch {
            try {
                repository.importParsedTimetable(items, replaceExisting)
                _ocrState.value = AiTimetableOcrState.Success("Successfully imported ${items.size} class schedule entries into your timetable!")
            } catch (e: Exception) {
                _ocrState.value = AiTimetableOcrState.Error("Failed to save schedule: ${e.localizedMessage}")
            }
        }
    }

    fun resetOcrState() {
        _ocrState.value = AiTimetableOcrState.Idle
    }

    fun importTimetableFromUri(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val json = ExportImportUtils.readTextFromUri(context, uri)
            if (json != null) {
                val (success, message) = ExportImportUtils.importTimetableFromJson(json, repository)
                onResult(success, message)
            } else {
                onResult(false, "Failed to read JSON file.")
            }
        }
    }
}
