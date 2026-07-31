package com.agupta07505.attendmate.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.agupta07505.attendmate.domain.model.ClassScheduleItem
import com.agupta07505.attendmate.ui.components.AttendanceProgressCard
import com.agupta07505.attendmate.ui.components.ClassCard
import com.agupta07505.attendmate.ui.components.EditUnitBottomSheet
import com.agupta07505.attendmate.util.DateUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAddSubject: () -> Unit,
    onNavigateToAddTimetable: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val selectedDateIso by viewModel.selectedDateIso.collectAsState()
    val todaySchedules by viewModel.todaySchedules.collectAsState()
    val overallSummary by viewModel.overallSummary.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var activeSheetItem by remember { mutableStateOf<ClassScheduleItem?>(null) }
    var showFabMenu by remember { mutableStateOf(false) }

    val greeting = remember {
        val hour = LocalTime.now().hour
        when (hour) {
            in 4..11 -> "Good Morning ☀️"
            in 12..16 -> "Good Afternoon 🌤️"
            in 17..22 -> "Good Evening 🌙"
            else -> "Night Owl 🦉"
        }
    }

    val selectedDate = remember(selectedDateIso) {
        try { LocalDate.parse(selectedDateIso, DateUtils.isoDateFormatter) } catch (e: Exception) { LocalDate.now() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = DateUtils.formatDateToHuman(selectedDateIso),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_icon_btn")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (showFabMenu) {
                    Column(
                        modifier = Modifier.padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                onNavigateToAddSubject()
                            },
                            icon = { Icon(Icons.Default.Book, null) },
                            text = { Text("Add Subject") }
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                onNavigateToAddTimetable()
                            },
                            icon = { Icon(Icons.Default.CalendarToday, null) },
                            text = { Text("Add Timetable Entry") }
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("home_fab_add")
                ) {
                    Icon(
                        imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Quick Add"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Date Selector (-3 days to +7 days)
            val dates = remember {
                (-3..7).map { LocalDate.now().plusDays(it.toLong()) }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dates) { date ->
                    val dateIso = date.format(DateUtils.isoDateFormatter)
                    val isSelected = dateIso == selectedDateIso
                    val isToday = date == LocalDate.now()

                    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    val dayNum = date.dayOfMonth

                    Card(
                        modifier = Modifier
                            .testTag("date_chip_$dateIso")
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.selectDate(dateIso) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                            else if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dayNum.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overall Progress Summary Card
                item {
                    AttendanceProgressCard(summary = overallSummary)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Classes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (todaySchedules.isNotEmpty()) {
                            Text(
                                text = "${todaySchedules.size} scheduled",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (todaySchedules.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "No Classes Scheduled Today",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Enjoy your day off or tap the '+' button to add subjects and build your timetable.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(todaySchedules) { item ->
                        ClassCard(
                            timetableEntry = item.timetableEntry,
                            subject = item.subject,
                            session = item.session,
                            units = item.units,
                            onMarkPresent = {
                                viewModel.markPresent(item)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Marked ${item.subject.name} Present",
                                        actionLabel = "Undo"
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoLastAction()
                                    }
                                }
                            },
                            onMarkAbsent = {
                                viewModel.markAbsent(item)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Marked ${item.subject.name} Absent",
                                        actionLabel = "Undo"
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoLastAction()
                                    }
                                }
                            },
                            onMoreOptions = {
                                activeSheetItem = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Bottom Sheet for Editing Units
    activeSheetItem?.let { item ->
        EditUnitBottomSheet(
            subjectName = item.subject.name,
            units = item.units,
            onUnitStatusChange = { unitId, newStatus ->
                if (item.session != null) {
                    viewModel.updateUnitStatus(item.session.id, unitId, newStatus)
                }
            },
            onMarkAllStatus = { status ->
                viewModel.markAllSessionStatus(item, status)
            },
            onResetSession = {
                viewModel.resetSession(item)
            },
            onDismiss = { activeSheetItem = null }
        )
    }
}
