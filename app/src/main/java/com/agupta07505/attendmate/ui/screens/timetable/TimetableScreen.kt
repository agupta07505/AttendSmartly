package com.agupta07505.attendmate.ui.screens.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.agupta07505.attendmate.data.local.entity.TimetableEntryEntity
import com.agupta07505.attendmate.domain.model.TimetableWithSubject
import com.agupta07505.attendmate.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel
) {
    val selectedDayOfWeek by viewModel.selectedDayOfWeek.collectAsState()
    val entriesForSelectedDay by viewModel.entriesForSelectedDay.collectAsState()
    val activeSubjects by viewModel.activeSubjects.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<TimetableEntryEntity?>(null) }

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
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("timetable_add_icon")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Class")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("timetable_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Class")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Day Selector Row
            TabRow(
                selectedTabIndex = selectedDayOfWeek - 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                daysMap.forEach { (dInt, dName) ->
                    Tab(
                        selected = selectedDayOfWeek == dInt,
                        onClick = { viewModel.selectDayOfWeek(dInt) },
                        text = {
                            Text(
                                text = dName,
                                fontWeight = if (selectedDayOfWeek == dInt) FontWeight.Bold else FontWeight.Normal
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
                            text = "Tap the '+' button to schedule classes for this day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Add Class")
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
                    items(entriesForSelectedDay) { item ->
                        val subjectColor = Color(item.subject.colorValue.toULong())
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
}
