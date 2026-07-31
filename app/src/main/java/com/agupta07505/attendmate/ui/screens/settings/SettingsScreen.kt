package com.agupta07505.attendmate.ui.screens.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                        onClick = { exportBackupLauncher.launch("attendmate_backup.json") },
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
                        onClick = { exportCsvLauncher.launch("attendmate_attendance.csv") },
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
