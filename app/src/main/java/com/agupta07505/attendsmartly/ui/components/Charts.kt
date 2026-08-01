/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.attendsmartly.ui.theme.StatusAbsent
import com.agupta07505.attendsmartly.ui.theme.StatusCancelled
import com.agupta07505.attendsmartly.ui.theme.StatusPresent

@Composable
fun AttendanceDonutChart(
    presentUnits: Int,
    absentUnits: Int,
    cancelledUnits: Int,
    bunkedUnits: Int = 0,
    modifier: Modifier = Modifier.size(160.dp)
) {
    val total = (presentUnits + absentUnits + bunkedUnits + cancelledUnits).toFloat()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 24.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            if (total == 0f) {
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                val presentSweep = (presentUnits / total) * 360f
                val absentSweep = (absentUnits / total) * 360f
                val bunkedSweep = (bunkedUnits / total) * 360f
                val cancelledSweep = (cancelledUnits / total) * 360f

                var startAngle = -90f

                if (presentSweep > 0) {
                    drawArc(
                        color = StatusPresent,
                        startAngle = startAngle,
                        sweepAngle = presentSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )
                    startAngle += presentSweep
                }

                if (absentSweep > 0) {
                    drawArc(
                        color = StatusAbsent,
                        startAngle = startAngle,
                        sweepAngle = absentSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )
                    startAngle += absentSweep
                }

                if (bunkedSweep > 0) {
                    drawArc(
                        color = Color(0xFFFF9800), // Bunked Amber/Orange
                        startAngle = startAngle,
                        sweepAngle = bunkedSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )
                    startAngle += bunkedSweep
                }

                if (cancelledSweep > 0) {
                    drawArc(
                        color = StatusCancelled,
                        startAngle = startAngle,
                        sweepAngle = cancelledSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }

        val conducted = presentUnits + absentUnits + bunkedUnits
        val percentage = if (conducted > 0) (presentUnits.toDouble() / conducted * 100) else 0.0

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${String.format("%.0f", percentage)}%",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Present",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
