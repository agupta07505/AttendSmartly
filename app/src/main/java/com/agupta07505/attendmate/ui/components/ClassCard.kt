package com.agupta07505.attendmate.ui.components

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
import com.agupta07505.attendmate.data.local.entity.AttendanceSessionEntity
import com.agupta07505.attendmate.data.local.entity.AttendanceUnitEntity
import com.agupta07505.attendmate.data.local.entity.SubjectEntity
import com.agupta07505.attendmate.data.local.entity.TimetableEntryEntity
import com.agupta07505.attendmate.domain.model.AttendanceStatus
import com.agupta07505.attendmate.util.DateUtils

@Composable
fun ClassCard(
    timetableEntry: TimetableEntryEntity,
    subject: SubjectEntity,
    session: AttendanceSessionEntity?,
    units: List<AttendanceUnitEntity>,
    onMarkPresent: () -> Unit,
    onMarkAbsent: () -> Unit,
    onMoreOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subjectColor = Color(subject.colorValue.toULong())
    val durationMins = DateUtils.calculateDurationMinutes(timetableEntry.startTime, timetableEntry.endTime)
    val expectedUnits = timetableEntry.attendanceUnitCount

    val presentCount = units.count { it.status == AttendanceStatus.PRESENT.name }
    val markedCount = units.count { it.status != AttendanceStatus.UNMARKED.name }
    val allMarked = units.isNotEmpty() && markedCount >= expectedUnits

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

                IconButton(
                    onClick = onMoreOptions,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("class_more_button_${timetableEntry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            units.forEach { unit ->
                                val status = try {
                                    AttendanceStatus.valueOf(unit.status)
                                } catch (e: Exception) {
                                    AttendanceStatus.UNMARKED
                                }
                                val pillColor = when (status) {
                                    AttendanceStatus.PRESENT -> com.agupta07505.attendmate.ui.theme.StatusPresent
                                    AttendanceStatus.ABSENT -> com.agupta07505.attendmate.ui.theme.StatusAbsent
                                    AttendanceStatus.CANCELLED -> com.agupta07505.attendmate.ui.theme.StatusCancelled
                                    AttendanceStatus.UNMARKED -> MaterialTheme.colorScheme.outlineVariant
                                }
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(pillColor)
                                )
                            }
                        }
                    }
                    Text(
                        text = "$markedCount/$expectedUnits marked",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = onMoreOptions,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Details",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Action Buttons Row: Present & Absent
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onMarkPresent,
                    enabled = !allMarked,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("btn_present_${timetableEntry.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Present",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Present",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = onMarkAbsent,
                    enabled = !allMarked,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("btn_absent_${timetableEntry.id}"),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Absent",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Absent",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
