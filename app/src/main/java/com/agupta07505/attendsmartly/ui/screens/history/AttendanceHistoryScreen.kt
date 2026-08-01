package com.agupta07505.attendsmartly.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
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
import com.agupta07505.attendsmartly.domain.model.AttendanceStatus
import com.agupta07505.attendsmartly.ui.components.EditUnitBottomSheet
import com.agupta07505.attendsmartly.ui.components.UnitBadgeChip
import com.agupta07505.attendsmartly.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val records by viewModel.historyRecords.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsState()
    val selectedStatusFilter by viewModel.statusFilter.collectAsState()

    var activeEditingRecord by remember { mutableStateOf<HistoryRecord?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Chips Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status Filter Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == null,
                            onClick = { viewModel.selectStatusFilter(null) },
                            label = { Text("All Statuses") }
                        )
                    }
                    items(AttendanceStatus.values().filter { it != AttendanceStatus.UNMARKED }) { st ->
                        FilterChip(
                            selected = selectedStatusFilter == st,
                            onClick = { viewModel.selectStatusFilter(st) },
                            label = { Text(st.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                // Subject Filter Chips
                if (subjects.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedSubjectId == null,
                                onClick = { viewModel.selectSubjectFilter(null) },
                                label = { Text("All Subjects") }
                            )
                        }
                        items(subjects) { sub ->
                            FilterChip(
                                selected = selectedSubjectId == sub.id,
                                onClick = { viewModel.selectSubjectFilter(sub.id) },
                                label = { Text(sub.name) }
                            )
                        }
                    }
                }
            }

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "No History Records Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mark attendance on the Home screen to see records here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(records) { record ->
                        val subColor = Color(record.subject.colorValue.toInt())

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("history_record_${record.session.id}"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(subColor)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = record.subject.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "${DateUtils.formatDateToHuman(record.session.sessionDate)} • ${DateUtils.formatTime(record.session.startTime)} - ${DateUtils.formatTime(record.session.endTime)}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(onClick = { activeEditingRecord = record }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    record.units.forEachIndexed { idx, u ->
                                        val st = try {
                                            AttendanceStatus.valueOf(u.status)
                                        } catch (e: Exception) {
                                            AttendanceStatus.UNMARKED
                                        }
                                        UnitBadgeChip(
                                            unitIndex = idx,
                                            status = st,
                                            onClick = { activeEditingRecord = record }
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

    activeEditingRecord?.let { record ->
        EditUnitBottomSheet(
            subjectName = record.subject.name,
            units = record.units,
            onUnitStatusChange = { unitId, newStatus ->
                viewModel.updateUnitStatus(unitId, newStatus)
            },
            onMarkAllStatus = { status ->
                viewModel.markAllSessionStatus(record.session.id, status)
            },
            onResetSession = {
                viewModel.resetSession(record.session.id)
            },
            onDismiss = { activeEditingRecord = null }
        )
    }
}
