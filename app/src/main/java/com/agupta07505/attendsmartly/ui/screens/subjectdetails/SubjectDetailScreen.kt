/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.ui.screens.subjectdetails

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.attendsmartly.domain.model.AttendanceStatus
import com.agupta07505.attendsmartly.domain.model.SessionWithUnits
import com.agupta07505.attendsmartly.ui.components.AttendanceDonutChart
import com.agupta07505.attendsmartly.ui.components.EditUnitBottomSheet
import com.agupta07505.attendsmartly.ui.components.UnitBadgeChip
import com.agupta07505.attendsmartly.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    viewModel: SubjectDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val subject by viewModel.subject.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val sessions by viewModel.subjectSessions.collectAsState()

    val context = LocalContext.current
    var activeEditingSession by remember { mutableStateOf<SessionWithUnits?>(null) }
    var showMarkPastDialog by remember { mutableStateOf(false) }
    var pastClassesInput by remember { mutableStateOf("") }
    var isProcessingPast by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subject?.name ?: "Subject Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMarkPastDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = "Mark Past Attendance",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (subject == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val sub = subject!!
            val subjectColor = Color(sub.colorValue.toInt())

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Header Stats Card with Donut Chart
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("subject_detail_stats_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AttendanceDonutChart(
                                    presentUnits = summary.presentUnits,
                                    absentUnits = summary.absentUnits,
                                    cancelledUnits = summary.cancelledUnits
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${sub.code} • ${sub.type}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Target: ${sub.targetPercentage.toInt()}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Conducted: ${summary.totalConductedUnits} Units",
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Present: ${summary.presentUnits} Units",
                                        fontSize = 13.sp,
                                        color = com.agupta07505.attendsmartly.ui.theme.StatusPresent
                                    )
                                    Text(
                                        text = "Absent: ${summary.absentUnits} Units",
                                        fontSize = 13.sp,
                                        color = com.agupta07505.attendsmartly.ui.theme.StatusAbsent
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (summary.percentage >= sub.targetPercentage) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = summary.statusMessage,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (summary.percentage >= sub.targetPercentage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Info Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Subject Configuration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "Teacher: ${sub.teacherName.ifBlank { "Not set" }}", fontSize = 13.sp)
                            Text(text = "Classroom: ${sub.room.ifBlank { "Not set" }}", fontSize = 13.sp)
                            Text(text = "Class Duration: ${sub.defaultSessionDurationMinutes} mins", fontSize = 13.sp)
                            Text(text = "Attendance Unit Rule: ${sub.attendanceUnitMinutes} mins = 1 Unit", fontSize = 13.sp)
                        }
                    }
                }

                // Quick Action: Mark Past Attendance
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mark Past Attendance",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Auto-fill attended classes for past dates (Excludes today)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { showMarkPastDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark")
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Attendance History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (sessions.isEmpty()) {
                    item {
                        Text(
                            text = "No attendance sessions recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(sessions) { sessionWithUnits ->
                        val sess = sessionWithUnits.session
                        val units = sessionWithUnits.units

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = DateUtils.formatDateToHuman(sess.sessionDate),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${DateUtils.formatTime(sess.startTime)} - ${DateUtils.formatTime(sess.endTime)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        units.forEachIndexed { idx, u ->
                                            val st = try {
                                                AttendanceStatus.valueOf(u.status)
                                            } catch (e: Exception) {
                                                AttendanceStatus.UNMARKED
                                            }
                                            UnitBadgeChip(
                                                unitIndex = idx,
                                                status = st,
                                                onClick = { activeEditingSession = sessionWithUnits }
                                            )
                                        }
                                    }
                                }

                                Row {
                                    IconButton(onClick = { activeEditingSession = sessionWithUnits }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Session")
                                    }
                                    IconButton(onClick = { viewModel.deleteSession(sess.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    activeEditingSession?.let { item ->
        EditUnitBottomSheet(
            subjectName = subject?.name ?: "Subject",
            units = item.units,
            onUnitStatusChange = { unitId, newStatus ->
                viewModel.updateUnitStatus(unitId, newStatus)
            },
            onMarkAllStatus = { status ->
                viewModel.markAllSessionStatus(item.session.id, status)
            },
            onResetSession = {
                viewModel.resetSession(item.session.id)
            },
            onDismiss = { activeEditingSession = null }
        )
    }

    if (showMarkPastDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isProcessingPast) {
                    showMarkPastDialog = false
                    pastClassesInput = ""
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.EventAvailable,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Mark Past Attendance", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Auto-fills past classes as Present based on your timetable schedule, working backward from yesterday (Excludes today).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    OutlinedTextField(
                        value = pastClassesInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) {
                                pastClassesInput = input
                            }
                        },
                        label = { Text("Classes Already Attended") },
                        placeholder = { Text("e.g. 12") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = pastClassesInput.toIntOrNull() ?: 0
                        if (count <= 0) {
                            Toast.makeText(context, "Please enter a valid count > 0", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isProcessingPast = true
                        viewModel.markPastAttendance(count) { success, msg ->
                            isProcessingPast = false
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            if (success) {
                                showMarkPastDialog = false
                                pastClassesInput = ""
                            }
                        }
                    },
                    enabled = !isProcessingPast && (pastClassesInput.toIntOrNull() ?: 0) > 0
                ) {
                    if (isProcessingPast) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Apply Attendance")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMarkPastDialog = false
                        pastClassesInput = ""
                    },
                    enabled = !isProcessingPast
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
