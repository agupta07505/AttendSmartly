/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agupta07505.attendsmartly.data.local.entity.SubjectEntity
import com.agupta07505.attendsmartly.domain.calculator.AttendanceCalculator
import com.agupta07505.attendsmartly.util.DateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExtraClassDialog(
    availableSubjects: List<SubjectEntity>,
    currentDateIso: String = DateUtils.todayIso(),
    onConfirm: (
        subjectId: Long,
        dateIso: String,
        startTime: String,
        endTime: String,
        unitCount: Int,
        room: String,
        teacher: String,
        notes: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSubject by remember {
        mutableStateOf(availableSubjects.firstOrNull())
    }
    var showSubjectDropdown by remember { mutableStateOf(false) }

    var dateIso by remember { mutableStateOf(currentDateIso) }
    var startTime by remember { mutableStateOf("14:00") }
    var endTime by remember { mutableStateOf("15:00") }
    var room by remember(selectedSubject) { mutableStateOf(selectedSubject?.room ?: "") }
    var teacher by remember(selectedSubject) { mutableStateOf(selectedSubject?.teacherName ?: "") }
    var notes by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }

    var unitCount by remember(startTime, endTime, selectedSubject) {
        val calculatedDuration = DateUtils.calculateDurationMinutes(startTime, endTime)
        val unitMins = selectedSubject?.attendanceUnitMinutes ?: 60
        mutableIntStateOf(
            AttendanceCalculator.calculateAttendanceUnits(calculatedDuration, unitMins)
        )
    }

    val subjectColor = selectedSubject?.let { Color(it.colorValue.toInt()) } ?: MaterialTheme.colorScheme.primary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 12.dp)
                .systemBarsPadding(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                Icons.Default.MoreTime,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                text = "Add Extra Class",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Schedule an extra class session for any date",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .weight(1f)
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
                            label = { Text("Subject*") },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(subjectColor)
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSubjectDropdown) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                                .testTag("extra_class_subject_dropdown")
                        )

                        ExposedDropdownMenu(
                            expanded = showSubjectDropdown,
                            onDismissRequest = { showSubjectDropdown = false }
                        ) {
                            availableSubjects.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text("${sub.name} (${sub.type})") },
                                    onClick = {
                                        selectedSubject = sub
                                        room = sub.room
                                        teacher = sub.teacherName
                                        showSubjectDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Date Selector
                    OutlinedTextField(
                        value = DateUtils.formatDateToHuman(dateIso),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Class Date*") },
                        leadingIcon = { Icon(Icons.Default.Event, null) },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .testTag("extra_class_date_input")
                    )

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
                            modifier = Modifier.weight(1f).testTag("extra_class_start_time")
                        )

                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("End Time (HH:mm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("extra_class_end_time")
                        )
                    }

                    // Attendance Units Count
                    OutlinedTextField(
                        value = unitCount.toString(),
                        onValueChange = { unitCount = it.toIntOrNull() ?: 1 },
                        label = { Text("Attendance Units Count") },
                        supportingText = {
                            val duration = DateUtils.calculateDurationMinutes(startTime, endTime)
                            val ruleMins = selectedSubject?.attendanceUnitMinutes ?: 60
                            Text("Class duration: ${duration}m • Rule: ${ruleMins}m = 1 unit")
                        },
                        leadingIcon = { Icon(Icons.Default.ConfirmationNumber, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Classroom / Room (Optional)
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Room / Location (Optional)") },
                        placeholder = { Text(selectedSubject?.room ?: "LH-101") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Teacher Name (Optional)
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("Teacher Name (Optional)") },
                        placeholder = { Text(selectedSubject?.teacherName ?: "") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Notes / Reason
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Topic / Reason / Notes (Optional)") },
                        placeholder = { Text("e.g. Extra lecture, Remedial session, Exam revision") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("extra_class_notes_input")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("extra_class_cancel_btn")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val sub = selectedSubject ?: return@Button
                            onConfirm(
                                sub.id,
                                dateIso,
                                startTime.trim(),
                                endTime.trim(),
                                unitCount,
                                room.trim(),
                                teacher.trim(),
                                notes.trim()
                            )
                            onDismiss()
                        },
                        enabled = selectedSubject != null && startTime.isNotBlank() && endTime.isNotBlank() && dateIso.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("extra_class_confirm_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Extra Class")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialLocalDate = try {
            LocalDate.parse(dateIso, DateUtils.isoDateFormatter)
        } catch (e: Exception) {
            LocalDate.now()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val pickedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            dateIso = pickedDate.format(DateUtils.isoDateFormatter)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
