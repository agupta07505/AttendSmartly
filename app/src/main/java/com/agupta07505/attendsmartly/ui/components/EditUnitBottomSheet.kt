package com.agupta07505.attendsmartly.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.attendsmartly.data.local.entity.AttendanceUnitEntity
import com.agupta07505.attendsmartly.domain.model.AttendanceStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUnitBottomSheet(
    subjectName: String,
    units: List<AttendanceUnitEntity>,
    onUnitStatusChange: (unitId: Long, newStatus: AttendanceStatus) -> Unit,
    onMarkAllStatus: (status: AttendanceStatus) -> Unit,
    onResetSession: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
                .testTag("edit_unit_bottom_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Edit Attendance: $subjectName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Individual Unit Statuses",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Individual unit controls
            units.forEachIndexed { index, unit ->
                val currentStatus = try {
                    AttendanceStatus.valueOf(unit.status)
                } catch (e: Exception) {
                    AttendanceStatus.UNMARKED
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                        Text(
                            text = "Unit ${index + 1}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())
                        ) {
                            FilterChip(
                                selected = currentStatus == AttendanceStatus.PRESENT,
                                onClick = { onUnitStatusChange(unit.id, AttendanceStatus.PRESENT) },
                                label = { Text("Present") },
                                leadingIcon = if (currentStatus == AttendanceStatus.PRESENT) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )

                            FilterChip(
                                selected = currentStatus == AttendanceStatus.ABSENT,
                                onClick = { onUnitStatusChange(unit.id, AttendanceStatus.ABSENT) },
                                label = { Text("Absent") },
                                leadingIcon = if (currentStatus == AttendanceStatus.ABSENT) {
                                    { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )

                            FilterChip(
                                selected = currentStatus == AttendanceStatus.BUNKED,
                                onClick = { onUnitStatusChange(unit.id, AttendanceStatus.BUNKED) },
                                label = { Text("Bunked") },
                                leadingIcon = if (currentStatus == AttendanceStatus.BUNKED) {
                                    { Icon(Icons.Default.DirectionsRun, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )

                            FilterChip(
                                selected = currentStatus == AttendanceStatus.CANCELLED,
                                onClick = { onUnitStatusChange(unit.id, AttendanceStatus.CANCELLED) },
                                label = { Text("Cancelled") },
                                leadingIcon = if (currentStatus == AttendanceStatus.CANCELLED) {
                                    { Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "Quick Complete Session Actions",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onMarkAllStatus(AttendanceStatus.PRESENT)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.agupta07505.attendsmartly.ui.theme.StatusPresent
                    )
                ) {
                    Text("All Present")
                }

                Button(
                    onClick = {
                        onMarkAllStatus(AttendanceStatus.ABSENT)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.agupta07505.attendsmartly.ui.theme.StatusAbsent
                    )
                ) {
                    Text("All Absent")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onMarkAllStatus(AttendanceStatus.BUNKED)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DirectionsRun, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bunked")
                }

                OutlinedButton(
                    onClick = {
                        onMarkAllStatus(AttendanceStatus.CANCELLED)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelled")
                }

                OutlinedButton(
                    onClick = {
                        onResetSession()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }
        }
    }
}
