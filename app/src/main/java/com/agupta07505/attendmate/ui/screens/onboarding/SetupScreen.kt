package com.agupta07505.attendmate.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.attendmate.data.local.entity.SubjectEntity
import com.agupta07505.attendmate.util.DateUtils
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onSaveSetup: (targetPercent: Double, startDate: String, endDate: String, firstSubject: SubjectEntity?) -> Unit,
    onSkipSetup: () -> Unit
) {
    var targetText by remember { mutableStateOf("75") }
    var startDateText by remember { mutableStateOf(LocalDate.now().format(DateUtils.isoDateFormatter)) }
    var endDateText by remember { mutableStateOf(LocalDate.now().plusMonths(4).format(DateUtils.isoDateFormatter)) }

    var addSubjectNow by remember { mutableStateOf(true) }
    var subjectName by remember { mutableStateOf("") }
    var subjectCode by remember { mutableStateOf("") }
    var subjectTeacher by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Initial Setup",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onSkipSetup,
                    modifier = Modifier.testTag("skip_setup_btn")
                ) {
                    Text("Skip for Now")
                }
            }

            Text(
                text = "Configure your target attendance and current semester dates.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Target Attendance Input
            OutlinedTextField(
                value = targetText,
                onValueChange = { targetText = it },
                label = { Text("Default Target Attendance (%)") },
                leadingIcon = { Icon(Icons.Default.Percent, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("setup_target_input")
            )

            // Semester Start Date
            OutlinedTextField(
                value = startDateText,
                onValueChange = { startDateText = it },
                label = { Text("Semester Start Date (YYYY-MM-DD)") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Semester End Date
            OutlinedTextField(
                value = endDateText,
                onValueChange = { endDateText = it },
                label = { Text("Semester End Date (YYYY-MM-DD)") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = addSubjectNow,
                    onCheckedChange = { addSubjectNow = it }
                )
                Text(
                    text = "Add First Subject Now",
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (addSubjectNow) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = subjectName,
                            onValueChange = { subjectName = it },
                            label = { Text("Subject Name (e.g. DBMS)") },
                            leadingIcon = { Icon(Icons.Default.Book, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("setup_subject_name_input")
                        )

                        OutlinedTextField(
                            value = subjectCode,
                            onValueChange = { subjectCode = it },
                            label = { Text("Subject Code (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = subjectTeacher,
                            onValueChange = { subjectTeacher = it },
                            label = { Text("Teacher Name (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val target = targetText.toDoubleOrNull() ?: 75.0
                    val firstSubject = if (addSubjectNow && subjectName.isNotBlank()) {
                        SubjectEntity(
                            name = subjectName.trim(),
                            code = subjectCode.trim(),
                            teacherName = subjectTeacher.trim(),
                            targetPercentage = target
                        )
                    } else null

                    onSaveSetup(target, startDateText, endDateText, firstSubject)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_setup_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Complete Setup & Launch",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
