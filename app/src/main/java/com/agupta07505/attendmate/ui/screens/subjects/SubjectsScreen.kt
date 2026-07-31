package com.agupta07505.attendmate.ui.screens.subjects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.agupta07505.attendmate.data.local.entity.SubjectEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    viewModel: SubjectsViewModel,
    onNavigateToSubjectDetail: (subjectId: Long) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val subjectsWithSummary by viewModel.subjectsWithSummary.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showArchived by viewModel.showArchived.collectAsState()

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subjects", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleShowArchived(!showArchived) },
                        modifier = Modifier.testTag("toggle_archive_btn")
                    ) {
                        Icon(
                            imageVector = if (showArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = "Toggle Archived"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSubjectDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("subjects_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search & Filter Header
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search subjects, codes, teachers...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { viewModel.updateSearchQuery("") }) { Icon(Icons.Default.Clear, null) } }
                } else null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("subjects_search_input")
            )

            if (showArchived) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Viewing Archived Subjects",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (subjectsWithSummary.isEmpty()) {
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
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Text(
                            text = if (showArchived) "No Archived Subjects" else "No Subjects Created Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap the '+' button below to add your college subjects.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showAddSubjectDialog = true },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Add Subject")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(subjectsWithSummary) { item ->
                        val subject = item.subject
                        val summary = item.summary
                        val subjectColor = Color(subject.colorValue.toInt())

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("subject_card_${subject.id}")
                                .clickable { onNavigateToSubjectDetail(subject.id) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(subjectColor)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = subject.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${subject.code} • ${subject.type} • Target: ${subject.targetPercentage.toInt()}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(onClick = { editingSubject = subject }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }

                                    IconButton(onClick = { viewModel.setArchived(subject.id, !subject.isArchived) }) {
                                        Icon(
                                            imageVector = if (subject.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                            contentDescription = "Archive"
                                        )
                                    }

                                    IconButton(onClick = { subjectToDelete = subject }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${String.format("%.1f", summary.percentage)}%",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (summary.percentage >= subject.targetPercentage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "${summary.presentUnits}/${summary.totalConductedUnits} Units Attended",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    val statusBadgeText = when {
                                        summary.totalConductedUnits == 0 -> "No Records"
                                        summary.percentage >= subject.targetPercentage -> {
                                            if (summary.safeBunks > 0) "Safe: ${summary.safeBunks} ${if (summary.safeBunks == 1) "unit" else "units"}"
                                            else "At Target"
                                        }
                                        else -> "Need ${summary.requiredUnitsToTarget} ${if (summary.requiredUnitsToTarget == 1) "unit" else "units"}"
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (summary.percentage >= subject.targetPercentage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            text = statusBadgeText,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (summary.percentage >= subject.targetPercentage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }

                                LinearProgressIndicator(
                                    progress = { (summary.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = if (summary.percentage >= subject.targetPercentage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = summary.statusMessage,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSubjectDialog || editingSubject != null) {
        AddEditSubjectDialog(
            initialSubject = editingSubject,
            onSave = { updated ->
                if (editingSubject != null) {
                    viewModel.updateSubject(updated)
                } else {
                    viewModel.addSubject(updated)
                }
                showAddSubjectDialog = false
                editingSubject = null
            },
            onDismiss = {
                showAddSubjectDialog = false
                editingSubject = null
            }
        )
    }

    subjectToDelete?.let { sub ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("Delete Subject?") },
            text = { Text("Are you sure you want to delete '${sub.name}'? This will delete all associated timetable entries and attendance records.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubject(sub)
                        subjectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
