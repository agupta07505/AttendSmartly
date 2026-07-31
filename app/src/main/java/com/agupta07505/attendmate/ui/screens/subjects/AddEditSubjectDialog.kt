package com.agupta07505.attendmate.ui.screens.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.attendmate.data.local.entity.SubjectEntity
import com.agupta07505.attendmate.domain.calculator.AttendanceCalculator
import com.agupta07505.attendmate.domain.model.SubjectType
import com.agupta07505.attendmate.ui.theme.SubjectPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubjectDialog(
    initialSubject: SubjectEntity? = null,
    onSave: (SubjectEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialSubject?.name ?: "") }
    var code by remember { mutableStateOf(initialSubject?.code ?: "") }
    var type by remember { mutableStateOf(initialSubject?.type ?: SubjectType.LECTURE.displayName) }
    var teacherName by remember { mutableStateOf(initialSubject?.teacherName ?: "") }
    var room by remember { mutableStateOf(initialSubject?.room ?: "") }
    var selectedColor by remember { mutableStateOf(initialSubject?.colorValue ?: SubjectPalette.first().toArgb().toLong()) }

    var durationMinutesText by remember { mutableStateOf((initialSubject?.defaultSessionDurationMinutes ?: 120).toString()) }
    var unitMinutesText by remember { mutableStateOf((initialSubject?.attendanceUnitMinutes ?: 60).toString()) }
    var targetText by remember { mutableStateOf((initialSubject?.targetPercentage ?: 75.0).toString()) }

    var showTypeDropdown by remember { mutableStateOf(false) }

    val typesList = SubjectType.values().map { it.displayName }

    val computedUnits = remember(durationMinutesText, unitMinutesText) {
        val dur = durationMinutesText.toIntOrNull() ?: 60
        val unitMins = unitMinutesText.toIntOrNull() ?: 60
        AttendanceCalculator.calculateAttendanceUnits(dur, unitMins)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialSubject == null) "Add New Subject" else "Edit Subject",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name*") },
                    placeholder = { Text("Database Management Systems") },
                    leadingIcon = { Icon(Icons.Default.Book, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("subject_name_input")
                )

                // Code Input
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Subject Code") },
                    placeholder = { Text("CS301") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Subject Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = showTypeDropdown,
                    onExpandedChange = { showTypeDropdown = it }
                ) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = showTypeDropdown,
                        onDismissRequest = { showTypeDropdown = false }
                    ) {
                        typesList.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    type = t
                                    showTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                // Color Selection Row
                Text(
                    text = "Subject Color Theme",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SubjectPalette.forEach { color ->
                        val colorVal = color.toArgb().toLong()
                        val isSelected = selectedColor == colorVal
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorVal }
                        )
                    }
                }

                // Teacher & Room
                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { teacherName = it },
                    label = { Text("Teacher Name") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Default Classroom") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Text(
                    text = "Attendance Counting Rule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = durationMinutesText,
                        onValueChange = { durationMinutesText = it },
                        label = { Text("Class Duration (mins)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = unitMinutesText,
                        onValueChange = { unitMinutesText = it },
                        label = { Text("1 Unit = (mins)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Calculated Attendance Units: $computedUnits per class session",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Target Attendance Percentage (%)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val durationMins = durationMinutesText.toIntOrNull() ?: 60
                    val unitMins = unitMinutesText.toIntOrNull() ?: 60
                    val target = targetText.toDoubleOrNull() ?: 75.0

                    val updated = (initialSubject ?: SubjectEntity(name = name.trim())).copy(
                        name = name.trim(),
                        code = code.trim(),
                        type = type,
                        teacherName = teacherName.trim(),
                        room = room.trim(),
                        colorValue = selectedColor,
                        defaultSessionDurationMinutes = durationMins,
                        attendanceUnitMinutes = unitMins,
                        defaultAttendanceUnits = computedUnits,
                        targetPercentage = target,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(updated)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_subject_btn")
            ) {
                Text("Save Subject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
