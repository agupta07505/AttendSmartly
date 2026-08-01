package com.agupta07505.attendsmartly.ui.screens.timetable

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.attendsmartly.data.local.entity.TimetableEntryEntity
import com.agupta07505.attendsmartly.domain.model.TimetableWithSubject
import com.agupta07505.attendsmartly.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val selectedDayOfWeek by viewModel.selectedDayOfWeek.collectAsState()
    val entriesForSelectedDay by viewModel.entriesForSelectedDay.collectAsState()
    val activeSubjects by viewModel.activeSubjects.collectAsState()
    val ocrState by viewModel.ocrState.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<TimetableEntryEntity?>(null) }
    var showOcrDialog by remember { mutableStateOf(false) }
    var showMarkPastDialog by remember { mutableStateOf(false) }

    var showShareDialog by remember { mutableStateOf(false) }
    var shareJsonText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var isFabMenuExpanded by remember { mutableStateOf(false) }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importTimetableFromUri(context, it) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                if (success) {
                    showImportDialog = false
                }
            }
        }
    }

    val daysMap = listOf(
        1 to "Mon",
        2 to "Tue",
        3 to "Wed",
        4 to "Thu",
        5 to "Fri",
        6 to "Sat",
        7 to "Sun"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Timetable", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("timetable_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isFabMenuExpanded) {
                    // Option 1: Add Class Manually
                    ExtendedFloatingActionButton(
                        onClick = {
                            isFabMenuExpanded = false
                            showAddDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Add Class Manually", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("timetable_fab_add_manual")
                    )

                    // Option 2: AI Timetable Scan
                    ExtendedFloatingActionButton(
                        onClick = {
                            isFabMenuExpanded = false
                            showOcrDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                        text = { Text("AI Timetable Scan", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("timetable_ai_scan_btn")
                    )

                    // Option 3: Mark Past Attendance
                    ExtendedFloatingActionButton(
                        onClick = {
                            isFabMenuExpanded = false
                            showMarkPastDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        icon = { Icon(Icons.Default.EventAvailable, contentDescription = null) },
                        text = { Text("Mark Past Attendance", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("timetable_mark_past_btn")
                    )

                    // Option 4: Share Timetable
                    ExtendedFloatingActionButton(
                        onClick = {
                            isFabMenuExpanded = false
                            viewModel.exportTimetable { json ->
                                shareJsonText = json
                                showShareDialog = true
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        icon = { Icon(Icons.Default.Share, contentDescription = null) },
                        text = { Text("Share Timetable", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("timetable_share_btn")
                    )

                    // Option 5: Import Timetable
                    ExtendedFloatingActionButton(
                        onClick = {
                            isFabMenuExpanded = false
                            showImportDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        icon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                        text = { Text("Import Timetable", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("timetable_import_btn")
                    )
                }

                // Main Plus FAB
                FloatingActionButton(
                    onClick = { isFabMenuExpanded = !isFabMenuExpanded },
                    containerColor = if (isFabMenuExpanded) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary,
                    contentColor = if (isFabMenuExpanded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("timetable_fab_add")
                ) {
                    Icon(
                        imageVector = if (isFabMenuExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (isFabMenuExpanded) "Close timetable menu" else "Open timetable menu"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
            // Day Selector Row
            ScrollableTabRow(
                selectedTabIndex = selectedDayOfWeek - 1,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                daysMap.forEach { (dInt, dName) ->
                    Tab(
                        selected = selectedDayOfWeek == dInt,
                        onClick = { viewModel.selectDayOfWeek(dInt) },
                        text = {
                            Text(
                                text = dName,
                                fontWeight = if (selectedDayOfWeek == dInt) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }

            if (entriesForSelectedDay.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "No Classes on ${daysMap.find { it.first == selectedDayOfWeek }?.second}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap below to add classes manually or scan an image of your timetable using AI.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            ElevatedButton(
                                onClick = { showOcrDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .testTag("timetable_empty_scan_btn"),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan Timetable Image", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { showAddDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .testTag("timetable_empty_add_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Class Manually")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMarkPastDialog = true }
                                .testTag("timetable_mark_past_banner"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.EventAvailable,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Mark Past Attendance",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Auto-fill classes by subject (Excluding current date)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(entriesForSelectedDay) { item ->
                        val subjectColor = Color(item.subject.colorValue.toInt())
                        val durationMins = DateUtils.calculateDurationMinutes(item.entry.startTime, item.entry.endTime)

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("timetable_item_${item.entry.id}"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(subjectColor)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.subject.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${DateUtils.formatTime(item.entry.startTime)} - ${DateUtils.formatTime(item.entry.endTime)} (${durationMins}m)",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    val room = if (item.entry.roomOverride.isNotBlank()) item.entry.roomOverride else item.subject.room
                                    val teacher = if (item.entry.teacherOverride.isNotBlank()) item.entry.teacherOverride else item.subject.teacherName
                                    Text(
                                        text = "Room: ${room.ifBlank { "N/A" }} • Teacher: ${teacher.ifBlank { "N/A" }} • ${item.entry.attendanceUnitCount} Units",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = { editingEntry = item.entry }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteTimetableEntry(item.entry.id) }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isFabMenuExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { isFabMenuExpanded = false }
                )
            }
        }
    }

    if (showAddDialog || editingEntry != null) {
        AddEditTimetableDialog(
            initialEntry = editingEntry,
            subjects = activeSubjects,
            defaultDayOfWeek = selectedDayOfWeek,
            onSave = { entry ->
                if (editingEntry != null) {
                    viewModel.updateTimetableEntry(entry)
                } else {
                    viewModel.addTimetableEntry(entry)
                }
                showAddDialog = false
                editingEntry = null
            },
            onDismiss = {
                showAddDialog = false
                editingEntry = null
            }
        )
    }

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Share Timetable", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Your timetable and associated subjects are packaged into a JSON format. Share this directly or copy the code to send to another device.")
                    OutlinedTextField(
                        value = shareJsonText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Timetable JSON Format") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareJsonText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Timetable JSON"))
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share via App")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(shareJsonText))
                            Toast.makeText(context, "Copied JSON code to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }
                    TextButton(onClick = { showShareDialog = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Timetable", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Import timetable entries and subjects from a JSON file or paste the JSON code below:")

                    Button(
                        onClick = {
                            importFileLauncher.launch(arrayOf("application/json", "*/*", "text/plain"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick JSON File")
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        label = { Text("Paste JSON Code") },
                        placeholder = { Text("{\"format\":\"AttendSmartly_TIMETABLE_V1\",...}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 160.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isBlank()) {
                            Toast.makeText(context, "Please paste JSON code or pick a file", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.importTimetable(importJsonText) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (success) {
                                    showImportDialog = false
                                    importJsonText = ""
                                }
                            }
                        }
                    }
                ) {
                    Text("Import Code")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showOcrDialog || ocrState !is AiTimetableOcrState.Idle) {
        TimetableOcrDialog(
            state = ocrState,
            currentApiKey = geminiApiKey,
            onSaveApiKey = { key -> viewModel.saveGeminiApiKey(key) },
            onPickImage = { uri -> viewModel.startOcrFromUri(context, uri) },
            onProcessSampleImage = { bitmap -> viewModel.startOcrFromBitmap(bitmap) },
            onConfirmImport = { items, replace -> viewModel.confirmOcrImport(items, replace) },
            onDismiss = {
                viewModel.resetOcrState()
                showOcrDialog = false
            }
        )
    }

    if (showMarkPastDialog) {
        MarkPastAttendanceDialog(
            subjects = activeSubjects,
            onMarkPast = { subjectId, count, onResult ->
                viewModel.markPastAttendance(subjectId, count, onResult)
            },
            onDismiss = { showMarkPastDialog = false }
        )
    }
}
}
