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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.attendsmartly.BuildConfig
import com.agupta07505.attendsmartly.R
import com.agupta07505.attendsmartly.util.DateUtils
import com.agupta07505.attendsmartly.util.GitHubReleaseInfo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

enum class SettingsSection(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String? = null
) {
    ATTENDANCE(
        title = "Attendance Rules & Goals",
        subtitle = "Target percentage, reminder lead time & semester calendar",
        icon = Icons.Default.TrackChanges
    ),
    NOTIFICATIONS(
        title = "Notifications & Alerts",
        subtitle = "Class reminders, alert sound & vibration toggles",
        icon = Icons.Default.NotificationsActive
    ),
    APPEARANCE(
        title = "Appearance & Theme",
        subtitle = "System, light or dark mode & dynamic Material 3 colors",
        icon = Icons.Default.Palette
    ),
    AI_OCR(
        title = "AI & Timetable Scanner",
        subtitle = "Personal Gemini API key & image OCR timetable extractor",
        icon = Icons.Default.AutoAwesome
    ),
    DATA_BACKUP(
        title = "Data Management & Backup",
        subtitle = "Export/Restore JSON backup, CSV reports & data reset",
        icon = Icons.Default.Storage
    ),
    ABOUT(
        title = "About & Updates",
        subtitle = "App version v${BuildConfig.VERSION_NAME}, check updates, developer & legal",
        icon = Icons.Default.School,
        badge = "v${BuildConfig.VERSION_NAME}"
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val context = LocalContext.current

    var currentSection by remember { mutableStateOf<SettingsSection?>(null) }

    var targetText by remember(prefs.defaultTargetAttendance) {
        mutableStateOf(prefs.defaultTargetAttendance.toInt().toString())
    }
    var reminderText by remember(prefs.defaultReminderMinutes) {
        mutableStateOf(prefs.defaultReminderMinutes.toString())
    }
    var startDateText by remember(prefs.semesterStartDate) {
        mutableStateOf(prefs.semesterStartDate)
    }
    var endDateText by remember(prefs.semesterEndDate) {
        mutableStateOf(prefs.semesterEndDate)
    }
    var apiKeyText by remember(prefs.geminiApiKey) {
        mutableStateOf(prefs.geminiApiKey)
    }
    var apiKeyVisible by remember { mutableStateOf(false) }

    var showClearDataConfirm by remember { mutableStateOf(false) }
    var activeDatePickerTarget by remember { mutableStateOf<String?>(null) } // "START" or "END"

    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    var updateReleaseInfo by remember { mutableStateOf<GitHubReleaseInfo?>(null) }

    // Intercept system back press when in a sub-section
    BackHandler(enabled = currentSection != null) {
        currentSection = null
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.updateNotificationsEnabled(true)
        }
    }

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
                title = {
                    Text(
                        text = currentSection?.title ?: "Settings",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentSection != null) {
                                currentSection = null
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentSection,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut()
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "settings_navigation"
        ) { section ->
            if (section == null) {
                // ==========================================
                // MAIN SETTINGS OVERVIEW MENU
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header App Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.School,
                                        contentDescription = "AttendSmartly",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "AttendSmartly",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "v${BuildConfig.VERSION_NAME}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Smart attendance tracking & safe bunks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = "Preferences & Sections",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                    )

                    // 6 Interactive Section Items
                    SettingsSection.values().forEach { sec ->
                        SettingsSectionCard(
                            section = sec,
                            onClick = { currentSection = sec }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Footer branding
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Made with ❤️ by Animesh Gupta • GNU GPL-3.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                // ==========================================
                // DEDICATED SUB-WINDOW FOR EACH SECTION
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (section) {
                        SettingsSection.ATTENDANCE -> {
                            // 1. ATTENDANCE & ACADEMIC GOALS
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.TrackChanges,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text("Target Attendance Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Target % used for safe bunks and catch-up alerts",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = targetText,
                                        onValueChange = {
                                            targetText = it
                                            it.toDoubleOrNull()?.let { t -> viewModel.updateDefaultTargetAttendance(t) }
                                        },
                                        label = { Text("Target Attendance Percentage") },
                                        leadingIcon = { Icon(Icons.Default.Percent, null) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("settings_target_input")
                                    )

                                    // Quick Preset Chips for Target %
                                    Text("Quick Presets", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("70", "75", "80", "85", "90").forEach { preset ->
                                            val isSelected = targetText == preset
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    targetText = preset
                                                    preset.toDoubleOrNull()?.let { t -> viewModel.updateDefaultTargetAttendance(t) }
                                                },
                                                label = { Text("$preset%") },
                                                leadingIcon = if (isSelected) {
                                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Notifications,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text("Class Reminder Lead Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Minutes before class to trigger reminders",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = reminderText,
                                        onValueChange = {
                                            reminderText = it
                                            it.toIntOrNull()?.let { r -> viewModel.updateDefaultReminderMinutes(r) }
                                        },
                                        label = { Text("Reminder Lead Time (Minutes)") },
                                        leadingIcon = { Icon(Icons.Default.AccessTime, null) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("settings_reminder_input")
                                    )

                                    // Quick Preset Chips for Reminder Minutes
                                    Text("Quick Presets", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("5", "10", "15", "30").forEach { preset ->
                                            val isSelected = reminderText == preset
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    reminderText = preset
                                                    preset.toIntOrNull()?.let { r -> viewModel.updateDefaultReminderMinutes(r) }
                                                },
                                                label = { Text("$preset min") },
                                                leadingIcon = if (isSelected) {
                                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.CalendarMonth,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text("Semester Calendar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Optional date range for timetable and stats",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = startDateText,
                                        onValueChange = {
                                            startDateText = it
                                            viewModel.updateSemesterDates(it, endDateText)
                                        },
                                        label = { Text("Semester Start Date") },
                                        placeholder = { Text("YYYY-MM-DD") },
                                        leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                                        trailingIcon = {
                                            IconButton(onClick = { activeDatePickerTarget = "START" }) {
                                                Icon(Icons.Default.EditCalendar, contentDescription = "Pick start date")
                                            }
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = endDateText,
                                        onValueChange = {
                                            endDateText = it
                                            viewModel.updateSemesterDates(startDateText, it)
                                        },
                                        label = { Text("Semester End Date") },
                                        placeholder = { Text("YYYY-MM-DD") },
                                        leadingIcon = { Icon(Icons.Default.Event, null) },
                                        trailingIcon = {
                                            IconButton(onClick = { activeDatePickerTarget = "END" }) {
                                                Icon(Icons.Default.EditCalendar, contentDescription = "Pick end date")
                                            }
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        SettingsSection.NOTIFICATIONS -> {
                            // 2. NOTIFICATIONS & ALERTS
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.NotificationsActive,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Column {
                                                Text("Class Reminders", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                                Text(
                                                    "Receive alerts before upcoming classes",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = prefs.notificationsEnabled,
                                            onCheckedChange = { enabled ->
                                                if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                                                            context,
                                                            android.Manifest.permission.POST_NOTIFICATIONS
                                                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                                    ) {
                                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                                    } else {
                                                        viewModel.updateNotificationsEnabled(true)
                                                    }
                                                } else {
                                                    viewModel.updateNotificationsEnabled(enabled)
                                                }
                                            },
                                            modifier = Modifier.testTag("settings_notifications_switch")
                                        )
                                    }

                                    AnimatedVisibility(visible = prefs.notificationsEnabled) {
                                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (prefs.notificationSound) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                        modifier = Modifier.size(38.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                if (prefs.notificationSound) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                                                                contentDescription = null,
                                                                tint = if (prefs.notificationSound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                    Column {
                                                        Text("Alert Sound", fontWeight = FontWeight.SemiBold)
                                                        Text("Play notification audio chime", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
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
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (prefs.notificationVibrate) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                        modifier = Modifier.size(38.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                Icons.Default.Vibration,
                                                                contentDescription = null,
                                                                tint = if (prefs.notificationVibrate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                    Column {
                                                        Text("Alert Vibration", fontWeight = FontWeight.SemiBold)
                                                        Text("Vibrate on reminder alert", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                                Switch(
                                                    checked = prefs.notificationVibrate,
                                                    onCheckedChange = { viewModel.updateNotificationVibrate(it) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SettingsSection.APPEARANCE -> {
                            // 3. APPEARANCE & THEME
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Palette,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text("Theme Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Choose your preferred visual appearance",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Responsive 3-option Theme Cards
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            Triple("SYSTEM", "System", Icons.Default.BrightnessAuto),
                                            Triple("LIGHT", "Light", Icons.Default.LightMode),
                                            Triple("DARK", "Dark", Icons.Default.DarkMode)
                                        ).forEach { (mode, label, icon) ->
                                            val isSelected = (prefs.themeMode == mode)
                                            Card(
                                                onClick = { viewModel.updateThemeMode(mode) },
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                ),
                                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        icon,
                                                        contentDescription = null,
                                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                    Text(
                                                        label,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (prefs.dynamicColors) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.ColorLens,
                                                        contentDescription = null,
                                                        tint = if (prefs.dynamicColors) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Column {
                                                Text("Dynamic Material 3 Colors", fontWeight = FontWeight.SemiBold)
                                                Text("Wallpaper-based theme (Android 12+)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Switch(
                                            checked = prefs.dynamicColors,
                                            onCheckedChange = { viewModel.updateDynamicColors(it) }
                                        )
                                    }
                                }
                            }
                        }

                        SettingsSection.AI_OCR -> {
                            // 4. AI & TIMETABLE OCR
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text("Gemini AI OCR Scanner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Extracts schedule data from timetable photos",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = "AttendSmartly uses Google Gemini to read college timetable charts from your gallery or camera. Provide your free Gemini API key below.",
                                        style = MaterialTheme.typography.bodyMedium,
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

                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                            try { context.startActivity(intent) } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Get Free API Key (Google AI Studio)", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        SettingsSection.DATA_BACKUP -> {
                            // 5. DATA MANAGEMENT & BACKUPS
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Storage,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text("Backup & Export", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Manage local data files, snapshots and reports",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { exportBackupLauncher.launch("AttendSmartly_backup.json") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Export JSON Backup", fontWeight = FontWeight.SemiBold)
                                    }

                                    OutlinedButton(
                                        onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Restore JSON Backup", fontWeight = FontWeight.SemiBold)
                                    }

                                    OutlinedButton(
                                        onClick = { exportCsvLauncher.launch("AttendSmartly_attendance.csv") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.TableChart, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Export Attendance CSV", fontWeight = FontWeight.SemiBold)
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                                    // Reset Data options
                                    Text("Reset Data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                                    Button(
                                        onClick = { showClearDataConfirm = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Clear All App Data", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        SettingsSection.ABOUT -> {
                            // 6. ABOUT & UPDATES
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // App Header & Version Banner
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(54.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.School,
                                                    contentDescription = "AttendSmartly",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(30.dp)
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "AttendSmartly",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Licensed under GNU GPL-3.0",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "Open Source",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Track classes, calculate safe bunks, and manage college timetables with 100% offline privacy.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                                    // Update Checker & Releases Section
                                    Text("App Updates & Releases", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.checkForUpdates(BuildConfig.VERSION_NAME) { result ->
                                                    result.onSuccess { info ->
                                                        if (info != null && info.isUpdateAvailable) {
                                                            updateReleaseInfo = info
                                                        } else {
                                                            Toast.makeText(context, "You are using the latest version (v${BuildConfig.VERSION_NAME})! ✨", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }.onFailure { err ->
                                                        Toast.makeText(context, "Failed to check for updates: ${err.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            enabled = !isCheckingUpdate,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("settings_check_update_btn"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            if (isCheckingUpdate) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Checking...")
                                            } else {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Check Update", maxLines = 1)
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/AttendSmartly/releases"))
                                                try { context.startActivity(intent) } catch (_: Exception) {}
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Releases", maxLines = 1)
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                                    // Contact Developer Section
                                    Text("Developer & Connect", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505"))
                                                try { context.startActivity(intent) } catch (_: Exception) {}
                                            },
                                            modifier = Modifier.weight(1f).testTag("settings_github_btn"),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                                        ) {
                                            Icon(painter = painterResource(R.drawable.ic_github), contentDescription = "GitHub", modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("GitHub", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/agupta07505"))
                                                try { context.startActivity(intent) } catch (_: Exception) {}
                                            },
                                            modifier = Modifier.weight(1f).testTag("settings_linkedin_btn"),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                                        ) {
                                            Icon(painter = painterResource(R.drawable.ic_linkedin), contentDescription = "LinkedIn", modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("LinkedIn", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/agupta07505"))
                                                try { context.startActivity(intent) } catch (_: Exception) {}
                                            },
                                            modifier = Modifier.weight(1f).testTag("settings_instagram_btn"),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                                        ) {
                                            Icon(painter = painterResource(R.drawable.ic_instagram), contentDescription = "Instagram", modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Instagram", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:agupta.07505@gmail.com"))
                                                try { context.startActivity(intent) } catch (_: Exception) {}
                                            },
                                            modifier = Modifier.weight(1f).testTag("settings_email_btn"),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Email, contentDescription = "Email", modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Email", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                                    // Community & Legal
                                    Text("Community & Legal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/AttendSmartly/issues/new?template=app_review.md"))
                                                try { context.startActivity(intent) } catch (_: Exception) {}
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = "Review", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Review", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/AttendSmartly/issues/new?template=bug_report.md"))
                                                try { context.startActivity(intent) } catch (_: Exception) {}
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                                        ) {
                                            Icon(Icons.Default.BugReport, contentDescription = "Report", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Bug Report", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/AttendSmartly/issues/new?template=feature_request.md"))
                                                try { context.startActivity(intent) } catch (_: Exception) {}
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Lightbulb, contentDescription = "Feature", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Feature", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/AttendSmartly/blob/main/LICENSE"))
                                                try { context.startActivity(intent) } catch (_: Exception) {}
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = "License", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("License", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/AttendSmartly/blob/main/PRIVACY.md"))
                                                try { context.startActivity(intent) } catch (_: Exception) {}
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Security, contentDescription = "Privacy", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Privacy", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/AttendSmartly/blob/main/TERMS.md"))
                                                try { context.startActivity(intent) } catch (_: Exception) {}
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Description, contentDescription = "Terms", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Terms", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
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

    // Native DatePicker Modal for Semester Start & End Dates
    activeDatePickerTarget?.let { target ->
        val currentStr = if (target == "START") startDateText else endDateText
        val initialLocalDate = try {
            LocalDate.parse(currentStr, DateUtils.isoDateFormatter)
        } catch (e: Exception) {
            LocalDate.now()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { activeDatePickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        val formatted = picked.format(DateUtils.isoDateFormatter)
                        if (target == "START") {
                            startDateText = formatted
                            viewModel.updateSemesterDates(formatted, endDateText)
                        } else {
                            endDateText = formatted
                            viewModel.updateSemesterDates(startDateText, formatted)
                        }
                    }
                    activeDatePickerTarget = null
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeDatePickerTarget = null }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    updateReleaseInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { updateReleaseInfo = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.NewReleases, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("New Version Available!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Latest: ${info.tagName} • Installed: v${BuildConfig.VERSION_NAME}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (info.body.isNotBlank()) {
                        Text("Release Notes & Changelog:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = info.body,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val downloadTarget = info.apkAsset?.downloadUrl?.ifBlank { null } ?: info.htmlUrl
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadTarget))
                        try { context.startActivity(intent) } catch (_: Exception) {}
                        updateReleaseInfo = null
                    }
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (info.apkAsset != null) "Download APK" else "View Release")
                }
            },
            dismissButton = {
                TextButton(onClick = { updateReleaseInfo = null }) {
                    Text("Later")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    section: SettingsSection,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("settings_section_${section.name.lowercase()}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = section.icon,
                        contentDescription = section.title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (section.badge != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = section.badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = section.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
