package com.agupta07505.attendsmartly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.attendsmartly.domain.model.AttendanceStatus
import com.agupta07505.attendsmartly.ui.theme.*

@Composable
fun UnitBadgeChip(
    unitIndex: Int,
    status: AttendanceStatus,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon) = when (status) {
        AttendanceStatus.PRESENT -> Triple(StatusPresent.copy(alpha = 0.15f), StatusPresent, Icons.Default.CheckCircle)
        AttendanceStatus.ABSENT -> Triple(StatusAbsent.copy(alpha = 0.15f), StatusAbsent, Icons.Default.RemoveCircle)
        AttendanceStatus.BUNKED -> Triple(StatusAbsent.copy(alpha = 0.25f), StatusAbsent, Icons.Default.DirectionsRun)
        AttendanceStatus.CANCELLED -> Triple(StatusCancelled.copy(alpha = 0.15f), StatusCancelled, Icons.Default.Cancel)
        AttendanceStatus.UNMARKED -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.HourglassEmpty)
    }

    Row(
        modifier = modifier
            .testTag("unit_chip_${unitIndex}")
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = status.name,
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "Unit ${unitIndex + 1}: ${status.name.lowercase().replaceFirstChar { it.uppercase() }}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
