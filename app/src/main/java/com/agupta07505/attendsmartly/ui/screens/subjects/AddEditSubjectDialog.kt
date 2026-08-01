/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.ui.screens.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.attendsmartly.data.local.entity.SubjectEntity
import com.agupta07505.attendsmartly.domain.calculator.AttendanceCalculator
import com.agupta07505.attendsmartly.domain.model.SubjectType
import com.agupta07505.attendsmartly.ui.theme.SubjectPalette

private fun formatColorToHex(colorVal: Long): String {
    val argb = colorVal.toInt()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return String.format("#%02X%02X%02X", r, g, b)
}

private fun parseHexToColor(hex: String): Long? {
    val cleaned = hex.trim().removePrefix("#")
    return try {
        when (cleaned.length) {
            6 -> {
                val rgb = cleaned.toLong(16)
                0xFF000000L or rgb
            }
            8 -> {
                cleaned.toLong(16)
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

private fun colorFromHue(hue: Float): Color {
    val hsv = floatArrayOf(hue.coerceIn(0f, 360f), 0.85f, 0.90f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

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

    var hexInputText by remember { mutableStateOf(formatColorToHex(selectedColor)) }
    var currentHue by remember {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(selectedColor.toInt(), hsv)
        mutableFloatStateOf(hsv[0])
    }

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

                // Color Selection Section
                Text(
                    text = "Subject Color Theme",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubjectPalette.forEach { color ->
                        val colorVal = color.toArgb().toLong()
                        val isSelected = selectedColor == colorVal
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedColor = colorVal
                                    hexInputText = formatColorToHex(colorVal)
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(colorVal.toInt(), hsv)
                                    currentHue = hsv[0]
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Custom Color Selector Bar & Hex Input
                Text(
                    text = "Custom Color Selector Bar",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val rainbowGradient = remember {
                    Brush.horizontalGradient(
                        listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(rainbowGradient)
                )

                Slider(
                    value = currentHue,
                    onValueChange = { newHue ->
                        currentHue = newHue
                        val newColor = colorFromHue(newHue)
                        val colorVal = newColor.toArgb().toLong()
                        selectedColor = colorVal
                        hexInputText = formatColorToHex(colorVal)
                    },
                    valueRange = 0f..360f,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(selectedColor.toInt()))
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )

                    OutlinedTextField(
                        value = hexInputText,
                        onValueChange = { newText ->
                            hexInputText = newText
                            val parsed = parseHexToColor(newText)
                            if (parsed != null) {
                                selectedColor = parsed
                                val hsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(parsed.toInt(), hsv)
                                currentHue = hsv[0]
                            }
                        },
                        label = { Text("Custom Color (HEX Code)") },
                        placeholder = { Text("#1E88E5") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
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
