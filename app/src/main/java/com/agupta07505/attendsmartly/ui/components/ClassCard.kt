package com.agupta07505.attendsmartly.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    modifier: Modifier = Modifier
) {
    val subjectColor = Color(subject.colorValue.toInt())
    val durationMins = DateUtils.calculateDurationMinutes(timetableEntry.startTime, timetableEntry.endTime)
    val expectedUnits = timetableEntry.attendanceUnitCount

    val presentCount = units.count { it.status == AttendanceStatus.PRESENT.name }
    val absentCount = units.count { it.status == AttendanceStatus.ABSENT.name }
    val bunkedCount = units.count { it.status == AttendanceStatus.BUNKED.name }
    val cancelledCount = units.count { it.status == AttendanceStatus.CANCELLED.name }
    val markedCount = units.count { it.status != AttendanceStatus.UNMARKED.name }
    val isMarked = markedCount > 0

    val primaryStatusStr = when {
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Subject Icon Container, Name, Room/Time, Type Badge, Options
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
                            else -> Icons.Default.MenuBook
                        },
                        contentDescription = null,
                        tint = subjectColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$formattedStartTime • ${if (roomStr.isNotBlank()) roomStr else subject.code}",
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
                        containerColor = if (presentCount == expectedUnits) com.agupta07505.attendsmartly.ui.theme.StatusPresent else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (presentCount == expectedUnits) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
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
                    Icon(Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.size(14.dp))
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
