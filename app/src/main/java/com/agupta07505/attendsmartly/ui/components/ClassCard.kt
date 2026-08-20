/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.attendsmartly.data.local.entity.AttendanceSessionEntity
import com.agupta07505.attendsmartly.data.local.entity.AttendanceUnitEntity
import com.agupta07505.attendsmartly.data.local.entity.SubjectEntity
import com.agupta07505.attendsmartly.data.local.entity.TimetableEntryEntity
import com.agupta07505.attendsmartly.domain.model.AttendanceStatus
import com.agupta07505.attendsmartly.util.DateUtils

@Composable
fun ClassCard(
    timetableEntry: TimetableEntryEntity,
    subject: SubjectEntity,
    session: AttendanceSessionEntity?,
    units: List<AttendanceUnitEntity>,
    onMarkPresent: () -> Unit,
    onMarkAbsent: () -> Unit,
    onMarkBunked: () -> Unit = {},
    onMarkCancelled: () -> Unit = {},
    onResetSession: () -> Unit = {},
    onMoreOptions: () -> Unit = {},
    onReschedule: () -> Unit = {},
    onCancelReschedule: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val subjectColor = Color(subject.colorValue.toInt())
    val expectedUnits = timetableEntry.attendanceUnitCount

    val isRescheduledAway = session?.rescheduledToDate != null
    val isRescheduledIncoming = session?.isRescheduled == true || session?.originalDate != null

    val presentCount = units.count { it.status == AttendanceStatus.PRESENT.name }
    val absentCount = units.count { it.status == AttendanceStatus.ABSENT.name }
    val bunkedCount = units.count { it.status == AttendanceStatus.BUNKED.name }
    val cancelledCount = units.count { it.status == AttendanceStatus.CANCELLED.name }
    val markedCount = units.count { it.status != AttendanceStatus.UNMARKED.name }
    val isMarked = markedCount > 0

    val primaryStatusStr = when {
        isRescheduledAway -> "Rescheduled"
        presentCount == expectedUnits -> "Present"
        absentCount == expectedUnits -> "Absent"
        bunkedCount == expectedUnits -> "Bunked"
        cancelledCount == expectedUnits -> "Cancelled"
        markedCount > 0 -> "Partial ($presentCount/$expectedUnits)"
        else -> "Unmarked"
    }

    val formattedStartTime = DateUtils.formatTime(timetableEntry.startTime)
    val formattedEndTime = DateUtils.formatTime(timetableEntry.endTime)
    val roomStr = if (timetableEntry.roomOverride.isNotBlank()) timetableEntry.roomOverride else subject.room
    val teacherStr = if (timetableEntry.teacherOverride.isNotBlank()) timetableEntry.teacherOverride else subject.teacherName

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("class_card_${timetableEntry.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRescheduledAway) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isRescheduledIncoming) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Subject Icon Container, Name, Room/Time, Type Badge, Reschedule Button, Options
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bento Icon Container
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(subjectColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (subject.type.lowercase()) {
                            "lab", "practical" -> Icons.Default.Terminal
                            "tutorial" -> Icons.Default.Group
                            else -> Icons.AutoMirrored.Filled.MenuBook
                        },
                        contentDescription = null,
                        tint = subjectColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        val isExtraClass = session != null && session.timetableEntryId == null && !isRescheduledIncoming && !isRescheduledAway
                        if (isRescheduledIncoming) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "RESCHEDULED",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        } else if (isExtraClass) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "EXTRA CLASS",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    Text(
                        text = "$formattedStartTime - $formattedEndTime • ${if (roomStr.isNotBlank()) roomStr else subject.code}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Type Pill Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = subject.type.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                // Reschedule Quick Action Button
                if (!isRescheduledAway) {
                    IconButton(
                        onClick = onReschedule,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_reschedule_${timetableEntry.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = "Reschedule Class",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // If class was rescheduled to another date, show informative reschedule banner
            if (isRescheduledAway) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(14.dp)
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventRepeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Rescheduled to ${DateUtils.formatDateToHuman(session?.rescheduledToDate ?: "")}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "New Time: ${DateUtils.formatTime(session?.rescheduledToTime ?: timetableEntry.startTime)}${if (session?.rescheduledReason?.isNotBlank() == true) " • ${session.rescheduledReason}" else ""}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(
                            onClick = onCancelReschedule,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("btn_revert_reschedule_${timetableEntry.id}")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Revert", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Rescheduled incoming metadata notice if applicable
                if (isRescheduledIncoming && session?.originalDate != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Rescheduled from ${DateUtils.formatDateToHuman(session.originalDate)} (${DateUtils.formatTime(session.originalTime ?: "")})${if (session.rescheduledReason.isNotBlank()) " • ${session.rescheduledReason}" else ""}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            TextButton(
                                onClick = onCancelReschedule,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("btn_revert_incoming_reschedule_${timetableEntry.id}")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Revert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Unit Progress & Segmented Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Segmented pills for units
                        if (units.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                units.forEach { unit ->
                                    val status = try {
                                        AttendanceStatus.valueOf(unit.status)
                                    } catch (e: Exception) {
                                        AttendanceStatus.UNMARKED
                                    }
                                    val pillColor = when (status) {
                                        AttendanceStatus.PRESENT -> com.agupta07505.attendsmartly.ui.theme.StatusPresent
                                        AttendanceStatus.ABSENT -> com.agupta07505.attendsmartly.ui.theme.StatusAbsent
                                        AttendanceStatus.BUNKED -> com.agupta07505.attendsmartly.ui.theme.StatusAbsent
                                        AttendanceStatus.CANCELLED -> com.agupta07505.attendsmartly.ui.theme.StatusCancelled
                                        AttendanceStatus.UNMARKED -> MaterialTheme.colorScheme.outlineVariant
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .height(7.dp)
                                            .clip(CircleShape)
                                            .background(pillColor)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "$markedCount/$expectedUnits marked ($primaryStatusStr)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isMarked) {
                        TextButton(
                            onClick = onResetSession,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Clear Status",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Quick Status Actions Row: Present, Absent, Bunked, Cancelled
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Present
                    Button(
                        onClick = onMarkPresent,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_present_${timetableEntry.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (presentCount == expectedUnits) com.agupta07505.attendsmartly.ui.theme.StatusPresent else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (presentCount == expectedUnits) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Present",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Absent
                    Button(
                        onClick = onMarkAbsent,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_absent_${timetableEntry.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (absentCount == expectedUnits) com.agupta07505.attendsmartly.ui.theme.StatusAbsent else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (absentCount == expectedUnits) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Absent",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Bunked
                    Button(
                        onClick = onMarkBunked,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_bunked_${timetableEntry.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (bunkedCount == expectedUnits) com.agupta07505.attendsmartly.ui.theme.StatusAbsent.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (bunkedCount == expectedUnits) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Bunked",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Cancelled
                    Button(
                        onClick = onMarkCancelled,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_cancelled_${timetableEntry.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (cancelledCount == expectedUnits) com.agupta07505.attendsmartly.ui.theme.StatusCancelled else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (cancelledCount == expectedUnits) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Cancelled",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
