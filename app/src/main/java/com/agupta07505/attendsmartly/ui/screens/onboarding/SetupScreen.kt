/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.ui.screens.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.attendsmartly.data.local.entity.SubjectEntity
import com.agupta07505.attendsmartly.util.DateUtils
import java.time.LocalDate

enum class SetupMethod {
    UPLOAD_TIMETABLE,
    DEMO_DATA,
    MANUAL_SUBJECT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    currentApiKey: String = "",
    onSaveApiKey: (String) -> Unit = {},
    onSaveSetup: (targetPercent: Double, startDate: String, endDate: String, firstSubject: SubjectEntity?, loadDemo: Boolean) -> Unit,
    onNavigateToTimetableOcr: () -> Unit = {},
    onSkipSetup: () -> Unit
) {
    val context = LocalContext.current

    var targetText by remember { mutableStateOf("75") }
    var startDateText by remember { mutableStateOf(LocalDate.now().format(DateUtils.isoDateFormatter)) }
    var endDateText by remember { mutableStateOf(LocalDate.now().plusMonths(4).format(DateUtils.isoDateFormatter)) }

    var apiKeyText by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var apiKeyVisible by remember { mutableStateOf(false) }

    var selectedSetupMethod by remember { mutableStateOf(SetupMethod.MANUAL_SUBJECT) }

    var subjectName by remember { mutableStateOf("") }
    var subjectCode by remember { mutableStateOf("") }
    var subjectTeacher by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Initial Setup",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Configure rules, API key, and your schedule",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onSkipSetup,
                    modifier = Modifier.testTag("skip_setup_btn")
                ) {
                    Text("Skip")
                }
            }

            // Card 1: Attendance Rules & Semester Dates
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Attendance Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = { Text("Default Target Attendance (%)") },
                        leadingIcon = { Icon(Icons.Default.Percent, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("setup_target_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = startDateText,
                            onValueChange = { startDateText = it },
                            label = { Text("Semester Start") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = endDateText,
                            onValueChange = { endDateText = it },
                            label = { Text("Semester End") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Card 2: Gemini API Key & AI Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Gemini API Key (Optional for AI OCR)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Add your Gemini API key to upload timetable photos and parse classes automatically with smart OCR.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = {
                            apiKeyText = it
                            onSaveApiKey(it)
                        },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (apiKeyVisible) "Hide Key" else "Show Key"
                                )
                            }
                        },
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("setup_gemini_api_key_input")
                    )

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Get Free Gemini API Key", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Card 3: Setup Method Selection (Upload Timetable, Demo Data, or Manual Subject)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "How would you like to set up your classes?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Option 1: Upload Timetable Photo
                    FilterChip(
                        selected = selectedSetupMethod == SetupMethod.UPLOAD_TIMETABLE,
                        onClick = { selectedSetupMethod = SetupMethod.UPLOAD_TIMETABLE },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.CameraEnhance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Upload Timetable Photo (Smart AI OCR)", fontWeight = FontWeight.Bold)
                                    Text("Auto-scan timetable image to extract all subjects & times", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Option 2: Load Sample Demo Data
                    FilterChip(
                        selected = selectedSetupMethod == SetupMethod.DEMO_DATA,
                        onClick = { selectedSetupMethod = SetupMethod.DEMO_DATA },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Column {
                                    Text("Load Sample College Demo Data", fontWeight = FontWeight.Bold)
                                    Text("Instantly prefill sample subjects (DBMS, OS, Networks) & timetable", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Option 3: Manual First Subject
                    FilterChip(
                        selected = selectedSetupMethod == SetupMethod.MANUAL_SUBJECT,
                        onClick = { selectedSetupMethod = SetupMethod.MANUAL_SUBJECT },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Book, contentDescription = null)
                                Column {
                                    Text("Add First Subject Manually", fontWeight = FontWeight.Bold)
                                    Text("Enter your first subject details now and add others later", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dynamic Fields based on selection
                    AnimatedVisibility(visible = selectedSetupMethod == SetupMethod.MANUAL_SUBJECT) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = subjectName,
                                onValueChange = { subjectName = it },
                                label = { Text("Subject Name (e.g. DBMS)") },
                                leadingIcon = { Icon(Icons.Default.Book, null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("setup_subject_name_input")
                            )

                            OutlinedTextField(
                                value = subjectCode,
                                onValueChange = { subjectCode = it },
                                label = { Text("Subject Code (Optional, e.g. CS101)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = subjectTeacher,
                                onValueChange = { subjectTeacher = it },
                                label = { Text("Teacher Name (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Main Action Button
            Button(
                onClick = {
                    val target = targetText.toDoubleOrNull() ?: 75.0
                    onSaveApiKey(apiKeyText.trim())

                    when (selectedSetupMethod) {
                        SetupMethod.UPLOAD_TIMETABLE -> {
                            onSaveSetup(target, startDateText, endDateText, null, false)
                            onNavigateToTimetableOcr()
                        }
                        SetupMethod.DEMO_DATA -> {
                            onSaveSetup(target, startDateText, endDateText, null, true)
                        }
                        SetupMethod.MANUAL_SUBJECT -> {
                            val firstSubject = if (subjectName.isNotBlank()) {
                                SubjectEntity(
                                    name = subjectName.trim(),
                                    code = subjectCode.trim(),
                                    teacherName = subjectTeacher.trim(),
                                    targetPercentage = target
                                )
                            } else null
                            onSaveSetup(target, startDateText, endDateText, firstSubject, false)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_setup_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = when (selectedSetupMethod) {
                        SetupMethod.UPLOAD_TIMETABLE -> Icons.Default.CameraEnhance
                        SetupMethod.DEMO_DATA -> Icons.Default.AutoFixHigh
                        SetupMethod.MANUAL_SUBJECT -> Icons.Default.Check
                    },
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (selectedSetupMethod) {
                        SetupMethod.UPLOAD_TIMETABLE -> "Scan Timetable Photo & Launch"
                        SetupMethod.DEMO_DATA -> "Load Demo Data & Launch"
                        SetupMethod.MANUAL_SUBJECT -> "Complete Setup & Launch"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
