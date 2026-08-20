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
    availableClasses: List<ClassScheduleItem> = emptyList(),
    currentDateIso: String = DateUtils.todayIso(),
    onConfirm: (
        item: ClassScheduleItem,
        newDate: String,
        newStartTime: String,
        newEndTime: String,
        unitCount: Int,
        reason: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedItem by remember {
        mutableStateOf(item ?: availableClasses.firstOrNull())
    }
    var showClassDropdown by remember { mutableStateOf(false) }

    val origDate = selectedItem?.session?.sessionDate ?: currentDateIso
    val origStartTime = selectedItem?.timetableEntry?.startTime ?: "10:00"
    val origEndTime = selectedItem?.timetableEntry?.endTime ?: "11:00"

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
    var newStartTime by remember(selectedItem) { mutableStateOf(selectedItem?.timetableEntry?.startTime ?: "10:00") }
    var newEndTime by remember(selectedItem) { mutableStateOf(selectedItem?.timetableEntry?.endTime ?: "11:00") }
    var unitCount by remember(selectedItem) { mutableIntStateOf(selectedItem?.timetableEntry?.attendanceUnitCount ?: 1) }
    var reason by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }

    val targetLocalDate = remember(targetDateIso) {
        try {
            LocalDate.parse(targetDateIso, DateUtils.isoDateFormatter)
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    val subjectColor = selectedItem?.subject?.let { Color(it.colorValue.toInt()) } ?: MaterialTheme.colorScheme.primary

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
                                Icons.Default.EditCalendar,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                text = "Reschedule Class",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Move class to another date and time",
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
                    // When choosing from today's classes
                    if (item == null) {
                        if (availableClasses.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.EventBusy,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "No Classes Scheduled Today",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "There are no classes present on ${DateUtils.formatDateToHuman(currentDateIso)} to reschedule.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Select Today's Class to Reschedule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            ExposedDropdownMenuBox(
                                expanded = showClassDropdown,
                                onExpandedChange = { showClassDropdown = it }
                            ) {
                                val currentClassText = selectedItem?.let {
                                    "${it.subject.name} (${DateUtils.formatTime(it.timetableEntry.startTime)} - ${DateUtils.formatTime(it.timetableEntry.endTime)})"
                                } ?: "Select Class"

                                OutlinedTextField(
                                    value = currentClassText,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Class to Reschedule*") },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(subjectColor)
                                        )
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showClassDropdown) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = showClassDropdown,
                                    onDismissRequest = { showClassDropdown = false }
                                ) {
                                    availableClasses.forEach { classItem ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = classItem.subject.name,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "${DateUtils.formatTime(classItem.timetableEntry.startTime)} - ${DateUtils.formatTime(classItem.timetableEntry.endTime)} • ${classItem.subject.type}",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedItem = classItem
                                                showClassDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else if (selectedItem != null) {
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
                                        text = selectedItem?.subject?.name ?: "Subject",
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

                    if (selectedItem != null) {
                        Text(
                            text = "New Rescheduled Slot",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Target Date Field
                        OutlinedTextField(
                            value = DateUtils.formatDateToHuman(targetDateIso),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Rescheduled Date*") },
                            leadingIcon = { Icon(Icons.Default.Event, null) },
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

                        // Start and End Time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = newStartTime,
                                onValueChange = { newStartTime = it },
                                label = { Text("Start Time (HH:mm)") },
                                leadingIcon = { Icon(Icons.Default.Schedule, null) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("reschedule_new_start_time")
                            )

                            OutlinedTextField(
                                value = newEndTime,
                                onValueChange = { newEndTime = it },
                                label = { Text("End Time (HH:mm)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("reschedule_new_end_time")
                            )
                        }

                        // Unit Count
                        OutlinedTextField(
                            value = unitCount.toString(),
                            onValueChange = { unitCount = it.toIntOrNull() ?: 1 },
                            label = { Text("Attendance Units Count") },
                            leadingIcon = { Icon(Icons.Default.ConfirmationNumber, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Reason / Note
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("Reason / Note (Optional)") },
                            placeholder = { Text("e.g. Extra lecture, Lab swapped, Teacher request") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("reschedule_reason_input")
                        )
                    }
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
                        modifier = Modifier.testTag("reschedule_cancel_btn")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val curItem = selectedItem ?: return@Button
                            onConfirm(
                                curItem,
                                targetDateIso,
                                newStartTime.trim(),
                                newEndTime.trim(),
                                unitCount,
                                reason.trim()
                            )
                            onDismiss()
                        },
                        enabled = selectedItem != null && newStartTime.isNotBlank() && newEndTime.isNotBlank() && targetDateIso.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("reschedule_confirm_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirm Reschedule")
                    }
                }
            }
        }
    }

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
