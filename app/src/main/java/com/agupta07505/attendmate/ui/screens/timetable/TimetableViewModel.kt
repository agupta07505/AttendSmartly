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

@OptIn(ExperimentalCoroutinesApi::class)
class TimetableViewModel(
    private val repository: AttendMateRepository
) : ViewModel() {

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
