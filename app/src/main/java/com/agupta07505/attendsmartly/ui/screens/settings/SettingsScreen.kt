/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.attendsmartly.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val context = LocalContext.current

    var targetText by remember(prefs.defaultTargetAttendance) {
        mutableStateOf(prefs.defaultTargetAttendance.toInt().toString())
    }
    var reminderText by remember(prefs.defaultReminderMinutes) {
        mutableStateOf(prefs.defaultReminderMinutes.toString())
    }
    var apiKeyText by remember(prefs.geminiApiKey) {
        mutableStateOf(prefs.geminiApiKey)
    }
    var apiKeyVisible by remember { mutableStateOf(false) }

    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showDemoConfirm by remember { mutableStateOf(false) }

    // SAF Activity Launchers
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportBackup(context, it) { success ->
                val msg = if (success) "Backup exported successfully" else "Failed to export backup"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importBackup(context, it) { success ->
                val msg = if (success) "Backup restored successfully" else "Failed to restore backup"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            viewModel.exportCsv(context, it) { success ->
                val msg = if (success) "CSV exported successfully" else "Failed to export CSV"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Target & Reminders
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
                        onValueChange = {
                            targetText = it
                            it.toDoubleOrNull()?.let { t -> viewModel.updateDefaultTargetAttendance(t) }
                        },
                        label = { Text("Default Target Attendance (%)") },
                        leadingIcon = { Icon(Icons.Default.Percent, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_target_input")
                    )

                    OutlinedTextField(
                        value = reminderText,
                        onValueChange = {
                            reminderText = it
                            it.toIntOrNull()?.let { r -> viewModel.updateDefaultReminderMinutes(r) }
                        },
                        label = { Text("Class Reminder Minutes Before") },
                        leadingIcon = { Icon(Icons.Default.Notifications, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_reminder_input")
                    )
                }
            }

            // Notifications Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Class Reminder Notifications", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Receive notifications before scheduled classes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.notificationsEnabled,
                            onCheckedChange = { viewModel.updateNotificationsEnabled(it) },
                            modifier = Modifier.testTag("settings_notifications_switch")
                        )
                    }

                    if (prefs.notificationsEnabled) {
                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Notification Sound")
                            Switch(
                                checked = prefs.notificationSound,
                                onCheckedChange = { viewModel.updateNotificationSound(it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Notification Vibration")
                            Switch(
                                checked = prefs.notificationVibrate,
                                onCheckedChange = { viewModel.updateNotificationVibrate(it) }
                            )
                        }
                    }
                }
            }

            // Theme Options
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Text("Theme Mode", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = prefs.themeMode == "SYSTEM",
                            onClick = { viewModel.updateThemeMode("SYSTEM") },
                            label = { Text("System") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = prefs.themeMode == "LIGHT",
                            onClick = { viewModel.updateThemeMode("LIGHT") },
                            label = { Text("Light") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = prefs.themeMode == "DARK",
                            onClick = { viewModel.updateThemeMode("DARK") },
                            label = { Text("Dark") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dynamic Material 3 Colors")
                        Switch(
                            checked = prefs.dynamicColors,
                            onCheckedChange = { viewModel.updateDynamicColors(it) }
                        )
                    }
                }
            }

            // Gemini API Key & AI Settings
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
                            "Timetable OCR & Gemini API Key",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "AttendSmartly automatically extracts timetables from images using smart OCR. Enter your personal Gemini API key below to avoid rate limits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = {
                            apiKeyText = it
                            viewModel.updateGeminiApiKey(it)
                        },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (apiKeyVisible) "Hide API key" else "Show API key"
                                )
                            }
                        },
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_gemini_api_key_input")
                    )

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Get Free Gemini API Key (Google AI Studio)", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Backup & Export Data
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Data & Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Button(
                        onClick = { exportBackupLauncher.launch("AttendSmartly_backup.json") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Upload, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export JSON Backup")
                    }

                    OutlinedButton(
                        onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore JSON Backup")
                    }

                    OutlinedButton(
                        onClick = { exportCsvLauncher.launch("AttendSmartly_attendance.csv") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.TableChart, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export CSV Report")
                    }
                }
            }

            // Demo & Reset Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Developer & Reset Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Button(
                        onClick = { showDemoConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Load Sample College Demo Data")
                    }

                    Button(
                        onClick = { showClearDataConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All App Data")
                    }
                }
            }

            // Developer Contact Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Contact Developer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // GitHub
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_github_btn"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_github),
                                contentDescription = "GitHub",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GitHub", fontWeight = FontWeight.SemiBold)
                        }

                        // LinkedIn
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/agupta07505"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_linkedin_btn"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_linkedin),
                                contentDescription = "LinkedIn",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("LinkedIn", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Instagram
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/agupta07505"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_instagram_btn"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_instagram),
                                contentDescription = "Instagram",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Instagram", fontWeight = FontWeight.SemiBold)
                        }

                        // Email
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:agupta.07505@gmail.com"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_email_btn"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Email", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // App & Legal Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "About & Legal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Row 1: Review & Bug Report
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/AttendSmartly/issues/new?template=app_review.md"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Review", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Review", fontWeight = FontWeight.SemiBold)
                        }

                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/AttendSmartly/issues/new?template=bug_report.md"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = "Report", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Report Bug", fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }

                    // Row 2: Feature Request & License
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/AttendSmartly/issues/new?template=feature_request.md"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = "Feature Request", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Feature Req.", fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }

                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/AttendSmartly/blob/main/LICENSE"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "License", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("License", fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }

                    // Row 3: Privacy & Terms
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/AttendSmartly/blob/main/PRIVACY.md"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = "Privacy Policy", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Privacy", fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }

                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/AttendSmartly/blob/main/TERMS.md"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = "Terms", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Terms", fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }

            // Developer Footer Branding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Made with ❤️ by Animesh Gupta",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDemoConfirm) {
        AlertDialog(
            onDismissRequest = { showDemoConfirm = false },
            title = { Text("Load Sample Demo Data?") },
            text = { Text("This will add sample college subjects (DBMS, Operating Systems, Computer Networks) and a sample weekly timetable.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.loadDemoData {
                        Toast.makeText(context, "Demo data loaded successfully!", Toast.LENGTH_SHORT).show()
                    }
                    showDemoConfirm = false
                }) { Text("Load Demo Data") }
            },
            dismissButton = {
                TextButton(onClick = { showDemoConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text("Clear All App Data?") },
            text = { Text("Are you sure? This will delete all subjects, timetables, and attendance records permanently.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData {
                            Toast.makeText(context, "All data cleared", Toast.LENGTH_SHORT).show()
                        }
                        showClearDataConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
