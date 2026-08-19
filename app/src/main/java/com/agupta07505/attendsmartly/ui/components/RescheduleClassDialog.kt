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
import com.agupta07505.attendsmartly.data.local.entity.SubjectEntity
import com.agupta07505.attendsmartly.domain.model.ClassScheduleItem
import com.agupta07505.attendsmartly.util.DateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleClassDialog(
    item: ClassScheduleItem? = null,
    availableSubjects: List<SubjectEntity> = emptyList(),
    currentDateIso: String = DateUtils.todayIso(),
    onConfirm: (
        subjectId: Long,
        originalDate: String,
        originalTime: String,
        newDate: String,
        newStartTime: String,
        newEndTime: String,
        unitCount: Int,
        reason: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSubject by remember {
        mutableStateOf(item?.subject ?: availableSubjects.firstOrNull())
    }
    var showSubjectDropdown by remember { mutableStateOf(false) }

    val origDate = item?.session?.sessionDate ?: currentDateIso
    val origStartTime = item?.timetableEntry?.startTime ?: "10:00"
    val origEndTime = item?.timetableEntry?.endTime ?: "11:00"

    // Default target date: tomorrow or current date
    val defaultTargetDate = remember(origDate) {
        try {
            val local = LocalDate.parse(origDate, DateUtils.isoDateFormatter)
            local.plusDays(1).format(DateUtils.isoDateFormatter)
        } catch (e: Exception) {
            DateUtils.todayIso()
        }
    }

    var targetDateIso by remember { mutableStateOf(defaultTargetDate) }
    var newStartTime by remember { mutableStateOf(origStartTime) }
    var newEndTime by remember { mutableStateOf(origEndTime) }
    var unitCount by remember { mutableIntStateOf(item?.timetableEntry?.attendanceUnitCount ?: 1) }
    var reason by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }

    val targetLocalDate = remember(targetDateIso) {
        try {
            LocalDate.parse(targetDateIso, DateUtils.isoDateFormatter)
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    val subjectColor = selectedSubject?.let { Color(it.colorValue.toInt()) } ?: MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EditCalendar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Reschedule Class",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Move class to another date/time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // If item was null, allow choosing subject
                if (item == null && availableSubjects.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = showSubjectDropdown,
                        onExpandedChange = { showSubjectDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedSubject?.name ?: "Select Subject",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subject to Reschedule") },
                            leadingIcon = { Icon(Icons.Default.Book, null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSubjectDropdown) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
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
                                        showSubjectDropdown = false
                                    }
                                )
                            }
                        }
                    }
                } else if (selectedSubject != null) {
                    // Original Slot Info Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(subjectColor)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedSubject?.name ?: "Subject",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Original: ${DateUtils.formatDateToHuman(origDate)} • ${DateUtils.formatTime(origStartTime)} - ${DateUtils.formatTime(origEndTime)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    text = "New Rescheduled Slot",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Target Date Selector Field
                OutlinedTextField(
                    value = DateUtils.formatDateToHuman(targetDateIso),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("New Date") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .testTag("reschedule_target_date_input")
                )

                // Start & End Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newStartTime,
                        onValueChange = { newStartTime = it },
                        label = { Text("Start (HH:mm)") },
                        leadingIcon = { Icon(Icons.Default.Schedule, null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("reschedule_new_start_time")
                    )

                    OutlinedTextField(
                        value = newEndTime,
                        onValueChange = { newEndTime = it },
                        label = { Text("End (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("reschedule_new_end_time")
                    )
                }

                // Attendance Units Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Attendance Units",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Units to credit when attended",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { if (unitCount > 1) unitCount-- },
                            enabled = unitCount > 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(
                            text = "$unitCount",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        FilledTonalIconButton(
                            onClick = { if (unitCount < 10) unitCount++ },
                            enabled = unitCount < 10,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }

                // Optional Reason / Note
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason / Note (Optional)") },
                    placeholder = { Text("e.g. Faculty requested extra class") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("reschedule_reason_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sub = selectedSubject ?: return@Button
                    onConfirm(
                        sub.id,
                        origDate,
                        origStartTime,
                        targetDateIso,
                        newStartTime.trim(),
                        newEndTime.trim(),
                        unitCount,
                        reason.trim()
                    )
                    onDismiss()
                },
                enabled = selectedSubject != null && newStartTime.isNotBlank() && newEndTime.isNotBlank() && targetDateIso.isNotBlank(),
                modifier = Modifier.testTag("reschedule_confirm_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirm Reschedule")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("reschedule_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        val initialMillis = remember(targetLocalDate) {
            targetLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
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
                            targetDateIso = pickedDate.format(DateUtils.isoDateFormatter)
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
