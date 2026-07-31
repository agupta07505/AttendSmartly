package com.agupta07505.attendmate.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
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
    var showDatePickerDialog by remember { mutableStateOf(false) }

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
                        onClick = { showDatePickerDialog = true },
                        modifier = Modifier.testTag("date_picker_icon_btn")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                    }
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
            // Horizontal Date Selector (Wide Range: -30 days to +14 days relative to selectedDate/today)
            val dates = remember(selectedDateIso) {
                val base = selectedDate.minusDays(20)
                (0..35).map { base.plusDays(it.toLong()) }
            }
            val dateRowState = rememberLazyListState()

            LaunchedEffect(selectedDateIso) {
                val index = dates.indexOfFirst { it.format(DateUtils.isoDateFormatter) == selectedDateIso }
                if (index >= 0) {
                    dateRowState.animateScrollToItem((index - 2).coerceAtLeast(0))
                }
            }

            LazyRow(
                state = dateRowState,
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
                            text = if (selectedDate == LocalDate.now()) "Today's Classes" else "Classes on ${DateUtils.formatDateToHuman(selectedDateIso)}",
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
                                    text = "No Classes Scheduled",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "No classes found for this date. Tap '+' to add entries to your timetable or select another date above.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = todaySchedules,
                        key = { it.timetableEntry.id }
                    ) { item ->
                        ClassCard(
                            timetableEntry = item.timetableEntry,
                            subject = item.subject,
                            session = item.session,
                            units = item.units,
                            onMarkPresent = {
                                viewModel.markPresent(item)
                            },
                            onMarkAbsent = {
                                viewModel.markAbsent(item)
                            },
                            onMarkBunked = {
                                viewModel.markBunked(item)
                            },
                            onMarkCancelled = {
                                viewModel.markCancelled(item)
                            },
                            onResetSession = {
                                viewModel.resetSession(item)
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

    // Material 3 Date Picker Dialog
    if (showDatePickerDialog) {
        val initialMillis = remember(selectedDate) {
            selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val pickedLocalDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            viewModel.selectDate(pickedLocalDate.format(DateUtils.isoDateFormatter))
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
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
