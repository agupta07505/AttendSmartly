/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.ui.screens.timetable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.attendsmartly.data.local.entity.SubjectEntity
import com.agupta07505.attendsmartly.data.local.entity.TimetableEntryEntity
import com.agupta07505.attendsmartly.domain.calculator.AttendanceCalculator
import com.agupta07505.attendsmartly.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTimetableDialog(
    initialEntry: TimetableEntryEntity? = null,
    subjects: List<SubjectEntity>,
    defaultDayOfWeek: Int = 1,
    onSave: (TimetableEntryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSubject by remember {
        mutableStateOf(subjects.find { it.id == initialEntry?.subjectId } ?: subjects.firstOrNull())
    }
    var dayOfWeek by remember { mutableIntStateOf(initialEntry?.dayOfWeek ?: defaultDayOfWeek) }
    var startTime by remember { mutableStateOf(initialEntry?.startTime ?: "10:00") }
    var endTime by remember { mutableStateOf(initialEntry?.endTime ?: "12:00") }
    var roomOverride by remember { mutableStateOf(initialEntry?.roomOverride ?: "") }
    var teacherOverride by remember { mutableStateOf(initialEntry?.teacherOverride ?: "") }

    var unitCount by remember(startTime, endTime, selectedSubject) {
        val calculatedDuration = DateUtils.calculateDurationMinutes(startTime, endTime)
        val unitMins = selectedSubject?.attendanceUnitMinutes ?: 60
        mutableIntStateOf(
            initialEntry?.attendanceUnitCount ?: AttendanceCalculator.calculateAttendanceUnits(calculatedDuration, unitMins)
        )
    }

    var reminderMinutes by remember {
        mutableIntStateOf(initialEntry?.reminderMinutes ?: selectedSubject?.defaultReminderMinutes ?: 10)
    }

    var showSubjectDropdown by remember { mutableStateOf(false) }

    val daysList = listOf(
        1 to "Monday",
        2 to "Tuesday",
        3 to "Wednesday",
        4 to "Thursday",
        5 to "Friday",
        6 to "Saturday",
        7 to "Sunday"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialEntry == null) "Add Class to Timetable" else "Edit Timetable Entry",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Subject Dropdown
                ExposedDropdownMenuBox(
                    expanded = showSubjectDropdown,
                    onExpandedChange = { showSubjectDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedSubject?.name ?: "Select Subject",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        leadingIcon = { Icon(Icons.Default.Book, null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSubjectDropdown) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                            .testTag("timetable_subject_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = showSubjectDropdown,
                        onDismissRequest = { showSubjectDropdown = false }
                    ) {
                        subjects.forEach { subject ->
                            DropdownMenuItem(
                                text = { Text("${subject.name} (${subject.type})") },
                                onClick = {
                                    selectedSubject = subject
                                    showSubjectDropdown = false
                                }
                            )
                        }
                    }
                }

                // Day of Week
                Text(
                    text = "Day of Week",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    daysList.forEach { (dInt, dName) ->
                        FilterChip(
                            selected = dayOfWeek == dInt,
                            onClick = { dayOfWeek = dInt },
                            label = {
                                Text(
                                    text = dName.take(3),
                                    maxLines = 1,
                                    softWrap = false,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }

                // Start & End Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time (HH:mm)") },
                        leadingIcon = { Icon(Icons.Default.Schedule, null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_start_time")
                    )

                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_end_time")
                    )
                }

                // Attendance Units count override
                OutlinedTextField(
                    value = unitCount.toString(),
                    onValueChange = { unitCount = it.toIntOrNull() ?: 1 },
                    label = { Text("Attendance Units Count") },
                    supportingText = {
                        val duration = DateUtils.calculateDurationMinutes(startTime, endTime)
                        val ruleMins = selectedSubject?.attendanceUnitMinutes ?: 60
                        Text("Class duration: ${duration}m • Rule: ${ruleMins}m = 1 unit")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Room Override
                OutlinedTextField(
                    value = roomOverride,
                    onValueChange = { roomOverride = it },
                    label = { Text("Room / Location (Optional)") },
                    placeholder = { Text(selectedSubject?.room ?: "LH-101") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Teacher Override
                OutlinedTextField(
                    value = teacherOverride,
                    onValueChange = { teacherOverride = it },
                    label = { Text("Teacher Name (Optional)") },
                    placeholder = { Text(selectedSubject?.teacherName ?: "") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sub = selectedSubject ?: return@Button
                    val entry = (initialEntry ?: TimetableEntryEntity(
                        subjectId = sub.id,
                        dayOfWeek = dayOfWeek,
                        startTime = startTime,
                        endTime = endTime
                    )).copy(
                        subjectId = sub.id,
                        dayOfWeek = dayOfWeek,
                        startTime = startTime.trim(),
                        endTime = endTime.trim(),
                        roomOverride = roomOverride.trim(),
                        teacherOverride = teacherOverride.trim(),
                        attendanceUnitCount = unitCount,
                        reminderMinutes = reminderMinutes,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(entry)
                },
                enabled = selectedSubject != null && startTime.isNotBlank() && endTime.isNotBlank(),
                modifier = Modifier.testTag("save_timetable_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
