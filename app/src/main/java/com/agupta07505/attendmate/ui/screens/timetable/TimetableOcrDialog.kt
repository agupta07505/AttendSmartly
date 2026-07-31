package com.agupta07505.attendmate.ui.screens.timetable

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.agupta07505.attendmate.domain.model.ParsedTimetableItem
import com.agupta07505.attendmate.util.DateUtils
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableOcrDialog(
    state: AiTimetableOcrState,
    onPickImage: (Uri) -> Unit,
    onProcessSampleImage: (Bitmap) -> Unit,
    onConfirmImport: (List<ParsedTimetableItem>, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (state) {
                    is AiTimetableOcrState.Idle -> {
                        PickImageStep(
                            onPickImage = onPickImage,
                            onProcessSampleImage = onProcessSampleImage,
                            onDismiss = onDismiss
                        )
                    }
                    is AiTimetableOcrState.Processing -> {
                        ProcessingStep(
                            message = state.stepMessage,
                            imageUri = state.imageUri
                        )
                    }
                    is AiTimetableOcrState.Preview -> {
                        PreviewStep(
                            initialItems = state.parsedItems,
                            imageUri = state.imageUri,
                            onConfirm = { items, replace ->
                                onConfirmImport(items, replace)
                            },
                            onDismiss = onDismiss
                        )
                    }
                    is AiTimetableOcrState.Error -> {
                        ErrorStep(
                            message = state.message,
                            onRetry = onDismiss
                        )
                    }
                    is AiTimetableOcrState.Success -> {
                        SuccessStep(
                            message = state.message,
                            onDone = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickImageStep(
    onPickImage: (Uri) -> Unit,
    onProcessSampleImage: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onPickImage(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Timetable Scanner",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                        )
                    )
                )
                .border(
                    2.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Scan or Upload Your Timetable",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Gemini AI instantly extracts subjects, times, and days while filtering out unnecessary headers and noise.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Timetable Image", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = {
                    val sampleBitmap = createSampleTimetableBitmap()
                    onProcessSampleImage(sampleBitmap)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Demo Timetable Image", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ProcessingStep(
    message: String,
    imageUri: Uri?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "rotate"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Timetable Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val sampleBitmap = remember { createSampleTimetableBitmap() }
                Image(
                    bitmap = sampleBitmap.asImageBitmap(),
                    contentDescription = "Sample Timetable",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Scanning line animation overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.08f)
                    .align(Alignment.TopCenter)
                    .offset(y = (200 * scanLineProgress).dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                    )
            )

            // Central rotating AI icon badge
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                shadowElevation = 8.dp,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .size(32.dp)
                            .rotate(rotateAngle)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "AI Timetable Detection",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            modifier = Modifier
                .width(200.dp)
                .height(6.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PreviewStep(
    initialItems: List<ParsedTimetableItem>,
    imageUri: Uri?,
    onConfirm: (List<ParsedTimetableItem>, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var items by remember { mutableStateOf(initialItems.toMutableList()) }
    var selectedDay by remember { mutableIntStateOf(items.firstOrNull()?.dayOfWeek ?: 1) }
    var replaceExisting by remember { mutableStateOf(false) }

    var editingItem by remember { mutableStateOf<ParsedTimetableItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Review Extracted Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${items.size} classes detected • Check or edit before import",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Days Selector Tabs
        val days = listOf(
            1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu",
            5 to "Fri", 6 to "Sat", 7 to "Sun"
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(days) { (dayNum, dayName) ->
                val dayCount = items.count { it.dayOfWeek == dayNum }
                val isSelected = selectedDay == dayNum

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedDay = dayNum },
                    label = {
                        Text(
                            text = if (dayCount > 0) "$dayName ($dayCount)" else dayName,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Classes List for Selected Day
        val dayItems = items.filter { it.dayOfWeek == selectedDay }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (dayItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No classes extracted for ${DateUtils.getDayOfWeekName(selectedDay)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(dayItems, key = { it.id }) { item ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.subjectName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (item.subjectCode.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = item.subjectCode,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.AccessTime,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${item.startTime} - ${item.endTime}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        if (item.roomLocation.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Place,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.secondary
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = item.roomLocation,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                Row {
                                    IconButton(onClick = { editingItem = item }) {
                                        Icon(
                                            Icons.Outlined.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(onClick = {
                                        items = items.filter { it.id != item.id }.toMutableList()
                                    }) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Add class button
        OutlinedButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Class Manually to ${DateUtils.getDayOfWeekName(selectedDay)}")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Replace vs Merge Option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Replace existing timetable entries",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Switch(
                checked = replaceExisting,
                onCheckedChange = { replaceExisting = it }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Confirm Action Button
        Button(
            onClick = { onConfirm(items, replaceExisting) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = items.isNotEmpty(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Confirm & Import Schedule (${items.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // Dialogs for editing or adding
    if (editingItem != null) {
        EditParsedItemDialog(
            item = editingItem!!,
            onSave = { updated ->
                items = items.map { if (it.id == updated.id) updated else it }.toMutableList()
                editingItem = null
            },
            onDismiss = { editingItem = null }
        )
    }

    if (showAddDialog) {
        EditParsedItemDialog(
            item = ParsedTimetableItem(
                subjectName = "",
                startTime = "09:00",
                endTime = "10:00",
                dayOfWeek = selectedDay
            ),
            isNew = true,
            onSave = { newItem ->
                items = (items + newItem).toMutableList()
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun EditParsedItemDialog(
    item: ParsedTimetableItem,
    isNew: Boolean = false,
    onSave: (ParsedTimetableItem) -> Unit,
    onDismiss: () -> Unit
) {
    var subjectName by remember { mutableStateOf(item.subjectName) }
    var subjectCode by remember { mutableStateOf(item.subjectCode) }
    var startTime by remember { mutableStateOf(item.startTime) }
    var endTime by remember { mutableStateOf(item.endTime) }
    var roomLocation by remember { mutableStateOf(item.roomLocation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add Class" else "Edit Class Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = subjectCode,
                    onValueChange = { subjectCode = it },
                    label = { Text("Subject Code (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = roomLocation,
                    onValueChange = { roomLocation = it },
                    label = { Text("Room / Location (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subjectName.isNotBlank()) {
                        onSave(
                            item.copy(
                                subjectName = subjectName.trim(),
                                subjectCode = subjectCode.trim(),
                                startTime = startTime.trim(),
                                endTime = endTime.trim(),
                                roomLocation = roomLocation.trim()
                            )
                        )
                    }
                },
                enabled = subjectName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ErrorStep(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Detection Failed",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Try Again")
        }
    }
}

@Composable
private fun SuccessStep(
    message: String,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Timetable Imported!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDone,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Done! View Schedule", fontWeight = FontWeight.Bold)
        }
    }
}

// Helper to construct a clear demo timetable image bitmap
fun createSampleTimetableBitmap(): Bitmap {
    val width = 1000
    val height = 700
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    canvas.drawColor(Color.WHITE)

    val paint = Paint().apply {
        isAntiAlias = true
    }

    // Header Title
    paint.color = Color.rgb(33, 150, 243)
    paint.textSize = 32f
    paint.typeface = Typeface.DEFAULT_BOLD
    canvas.drawText("WEEKLY CLASS TIMETABLE - FALL SEMESTER", 50f, 60f, paint)

    paint.color = Color.GRAY
    paint.textSize = 18f
    paint.typeface = Typeface.DEFAULT
    canvas.drawText("Department of Computer Science & Engineering | Room Block B", 50f, 90f, paint)

    // Table headers
    paint.color = Color.rgb(240, 240, 240)
    canvas.drawRect(40f, 120f, 960f, 160f, paint)

    paint.color = Color.BLACK
    paint.textSize = 20f
    paint.typeface = Typeface.DEFAULT_BOLD
    canvas.drawText("DAY", 60f, 148f, paint)
    canvas.drawText("TIME", 180f, 148f, paint)
    canvas.drawText("SUBJECT / CODE", 380f, 148f, paint)
    canvas.drawText("ROOM", 820f, 148f, paint)

    val rows = listOf(
        Tuple("MON", "09:00 - 10:00", "CS201 Data Structures", "Lab 302"),
        Tuple("MON", "11:00 - 12:30", "MATH101 Linear Algebra", "Hall B"),
        Tuple("TUE", "10:00 - 11:30", "PHY201 Physics II", "Lab 105"),
        Tuple("TUE", "14:00 - 15:30", "CS202 Object Oriented Prog", "Room 401"),
        Tuple("WED", "09:00 - 10:30", "CS201 Data Structures", "Lab 302"),
        Tuple("THU", "11:00 - 12:30", "ENG102 Technical Comm", "Room 204"),
        Tuple("FRI", "10:00 - 12:00", "CS203 Database Systems", "Lab 305")
    )

    var yPos = 210f
    paint.textSize = 19f

    for ((day, time, subject, room) in rows) {
        paint.color = Color.rgb(248, 249, 250)
        canvas.drawRect(40f, yPos - 30f, 960f, yPos + 15f, paint)

        paint.color = Color.rgb(25, 118, 210)
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(day, 60f, yPos, paint)

        paint.color = Color.DKGRAY
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(time, 180f, yPos, paint)

        paint.color = Color.BLACK
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(subject, 380f, yPos, paint)

        paint.color = Color.rgb(76, 175, 80)
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(room, 820f, yPos, paint)

        yPos += 60f
    }

    return bitmap
}

private data class Tuple(val day: String, val time: String, val subject: String, val room: String)
