package com.qiuye.calendarkotlin.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qiuye.calendarkotlin.R
import com.qiuye.calendarkotlin.viewmodel.NoteEntry
import com.qiuye.calendarkotlin.tasks.data.ReminderEntity
import com.qiuye.calendarkotlin.tasks.data.formatTime
import androidx.compose.ui.text.style.TextDecoration
import java.time.LocalDate
import java.time.YearMonth
import com.qiuye.calendarkotlin.domain.displayName

private enum class NoteScope {
    ALL,
    THIS_MONTH,
    RECENT_30_DAYS,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotesBottomSheet(
    noteEntries: List<NoteEntry>,
    showLunar: Boolean,
    reminders: List<ReminderEntity>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSelectNoteDate: (LocalDate) -> Unit,
    onDeleteNote: (LocalDate) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var keyword by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableStateOf<YearMonth?>(null) }
    var selectedScope by remember { mutableStateOf(NoteScope.ALL) }
    var selectedNoteEntry by remember { mutableStateOf<NoteEntry?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<NoteEntry?>(null) }
    val months = remember(noteEntries) {
        noteEntries.map { it.month }.distinct().sortedDescending()
    }
    val recentCutoff = remember { LocalDate.now().minusDays(30) }
    val filteredEntries = remember(noteEntries, keyword, selectedMonth, selectedScope, recentCutoff) {
        noteEntries.filter { entry ->
            val monthMatches = selectedMonth == null || entry.month == selectedMonth
            val scopeMatches =
                when (selectedScope) {
                    NoteScope.ALL -> true
                    NoteScope.THIS_MONTH -> entry.month == YearMonth.now()
                    NoteScope.RECENT_30_DAYS -> !entry.date.isBefore(recentCutoff)
                }
            val keywordMatches =
                keyword.isBlank() ||
                    entry.text.contains(keyword, ignoreCase = true) ||
                    dateMatchesKeyword(entry.date, keyword) ||
                    entry.month.format(noteMonthFormatter()).contains(keyword, ignoreCase = true) ||
                    entry.shift?.displayName()?.contains(keyword, ignoreCase = true) == true ||
                    entry.lunarLabel.contains(keyword, ignoreCase = true)
            monthMatches && scopeMatches && keywordMatches
        }
    }
    val latestEntry = noteEntries.firstOrNull()
    val currentMonthCount = remember(noteEntries) {
        val currentMonth = YearMonth.now()
        noteEntries.count { it.month == currentMonth }
    }
    val noneText = stringResource(R.string.none)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.notes_center),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.notes_center_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("sheet_notes"),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MemoStatCard(
                            title = stringResource(R.string.total_notes_stat),
                            value = noteEntries.size.toString(),
                        )
                        MemoStatCard(
                            title = stringResource(R.string.current_results),
                            value = filteredEntries.size.toString(),
                        )
                        MemoStatCard(
                            title = stringResource(R.string.this_month_notes),
                            value = currentMonthCount.toString(),
                        )
                        MemoStatCard(
                            title = stringResource(R.string.latest_entry),
                            value = latestEntry?.date?.format(noteDateFormatter()) ?: noneText,
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NoteScope.entries.forEach { scope ->
                            FilterChip(
                                selected = selectedScope == scope,
                                onClick = { selectedScope = scope },
                                label = {
                                    Text(when (scope) {
                                        NoteScope.ALL -> stringResource(R.string.all)
                                        NoteScope.THIS_MONTH -> stringResource(R.string.note_scope_this_month)
                                        NoteScope.RECENT_30_DAYS -> stringResource(R.string.note_scope_recent_30_days)
                                    })
                                },
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_notes_search"),
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        label = { Text(stringResource(R.string.search_notes_label)) },
                        placeholder = { Text(stringResource(R.string.search_notes_placeholder)) },
                        singleLine = true,
                    )
                }
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = selectedMonth == null,
                            onClick = { selectedMonth = null },
                            label = { Text(stringResource(R.string.all_months)) },
                        )
                        months.forEach { month ->
                            FilterChip(
                                selected = selectedMonth == month,
                                onClick = { selectedMonth = month },
                                label = { Text(month.format(noteMonthFormatter())) },
                            )
                        }
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.total_notes_count, filteredEntries.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (filteredEntries.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = if (noteEntries.isEmpty()) stringResource(R.string.no_notes_yet) else stringResource(R.string.no_notes_matching),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = if (noteEntries.isEmpty()) stringResource(R.string.no_notes_hint) else stringResource(R.string.no_notes_filter_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    items(filteredEntries, key = { it.date.toString() }) { entry ->
                        val isSelected = selectedNoteEntry?.date == entry.date
                        NoteItemCard(
                            entry = entry,
                            showLunar = showLunar,
                            reminders = reminders,
                            accentColor = accentColor,
                            isSelected = isSelected,
                            onClick = {
                                onSelectNoteDate(entry.date)
                            },
                            onSelectToggle = {
                                selectedNoteEntry = if (isSelected) null else entry
                            },
                            onDeleteClick = {
                                showDeleteConfirmDialog = entry
                            }
                        )
                    }
                }
            }
        }

        if (showDeleteConfirmDialog != null) {
            val entry = showDeleteConfirmDialog!!
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = null },
                title = { Text(stringResource(R.string.delete_confirm_title)) },
                text = { Text(stringResource(R.string.delete_note_confirm_message, entry.date)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteNote(entry.date)
                            if (selectedNoteEntry?.date == entry.date) {
                                selectedNoteEntry = null
                            }
                            showDeleteConfirmDialog = null
                        },
                        modifier = Modifier.testTag("btn_dialog_confirm")
                    ) {
                        Text(stringResource(R.string.confirm), color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteConfirmDialog = null },
                        modifier = Modifier.testTag("btn_dialog_cancel")
                    ) {
                        Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}

@Composable
private fun MemoStatCard(
    title: String,
    value: String,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        modifier = Modifier.width(140.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteItemCard(
    entry: NoteEntry,
    showLunar: Boolean,
    reminders: List<ReminderEntity>,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSelectToggle: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val dateReminders = remember(entry.date, reminders) {
        reminders.filter { reminder ->
            java.time.Instant.ofEpochMilli(reminder.scheduledAtMillis)
                .atZone(com.qiuye.calendarkotlin.tasks.data.zoneIdProvider())
                .toLocalDate() == entry.date
        }.sortedBy { it.scheduledAtMillis }
    }
    val now = remember { System.currentTimeMillis() }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = if (isSelected) BorderStroke(1.5.dp, accentColor) else null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("note_item_${entry.date}")
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Checkbox selector on the left
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(22.dp)
                        .drawBehind {
                            val strokeWidth = 2.dp.toPx()
                            if (isSelected) {
                                drawCircle(
                                    color = accentColor,
                                    radius = size.minDimension / 2
                                )
                                // Draw checkmark
                                val checkColor = Color.White
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(size.width * 0.28f, size.height * 0.48f)
                                    lineTo(size.width * 0.45f, size.height * 0.65f)
                                    lineTo(size.width * 0.72f, size.height * 0.32f)
                                }
                                drawPath(
                                    path = path,
                                    color = checkColor,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 2.dp.toPx(),
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                                    )
                                )
                            } else {
                                drawCircle(
                                    color = Color.LightGray,
                                    radius = (size.minDimension - strokeWidth) / 2,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                )
                            }
                        }
                        .clickable(onClick = onSelectToggle)
                        .testTag("note_checkbox_${entry.date}")
                )

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.date.format(noteDateFormatter()),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = entry.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isSelected) {
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("btn_notes_delete_${entry.date}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.delete_note_cd),
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AssistChip(
                    onClick = onClick,
                    label = { Text(entry.month.format(monthFormatter())) },
                )
                entry.shift?.let { shift ->
                    val palette = shift.color.palette()
                    AssistChip(
                        onClick = onClick,
                        label = { Text(shift.displayName()) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = palette.container,
                            labelColor = palette.content,
                        ),
                    )
                }
                if (showLunar && entry.lunarLabel.isNotBlank()) {
                    AssistChip(
                        onClick = onClick,
                        label = { Text(stringResource(R.string.lunar_label_format, entry.lunarLabel)) },
                    )
                }

                if (dateReminders.isNotEmpty()) {
                    val allCompleted = dateReminders.all { it.isCompleted }
                    val anyExpired = dateReminders.any { !it.isCompleted && it.scheduledAtMillis <= now }
                    val chipColors = when {
                        allCompleted -> AssistChipDefaults.assistChipColors(
                            containerColor = Color.LightGray.copy(alpha = 0.2f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        anyExpired -> AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFFFFEBEE),
                            labelColor = Color(0xFFD32F2F)
                        )
                        else -> AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.primary
                        )
                    }

                    val chipText = if (dateReminders.size == 1) {
                        val reminder = dateReminders.first()
                        "⏰ ${formatTime(reminder.scheduledAtMillis)} ${reminder.title}"
                    } else {
                        stringResource(R.string.reminder_count_format, dateReminders.size)
                    }

                    AssistChip(
                        onClick = onClick,
                        label = { Text(chipText, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = chipColors
                    )
                }
            }

            if (dateReminders.size > 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    dateReminders.forEach { reminder ->
                        val isCompleted = reminder.isCompleted
                        val isExpired = !isCompleted && reminder.scheduledAtMillis <= now
                        
                        val (iconColor, textColor, textDecoration) = when {
                            isCompleted -> Triple(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), TextDecoration.LineThrough)
                            isExpired -> Triple(Color(0xFFD32F2F), Color(0xFFD32F2F), TextDecoration.None)
                            else -> Triple(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onSurface, TextDecoration.None)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "⏰ ${formatTime(reminder.scheduledAtMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = iconColor,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = reminder.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor,
                                textDecoration = textDecoration,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun dateMatchesKeyword(date: java.time.LocalDate, keyword: String): Boolean {
    val cleanKeyword = keyword.trim().replace("-", "").replace("/", "").replace(".", "").replace(" ", "")
    if (cleanKeyword.isEmpty()) return false

    val year = date.year.toString()
    val month = date.monthValue.toString()
    val monthZero = if (date.monthValue < 10) "0$month" else month
    val day = date.dayOfMonth.toString()
    val dayZero = if (date.dayOfMonth < 10) "0$day" else day

    val candidates = listOf(
        "$year$monthZero$dayZero",
        "$year$month$dayZero",
        "$year$monthZero$day",
        "$year$month$day",
        "$year$monthZero",
        "$year$month",
        "$monthZero$dayZero",
        "$month$day",
    )

    val originalClean = date.toString().replace("-", "")
    if (originalClean.contains(cleanKeyword)) return true

    return candidates.any { it.contains(cleanKeyword) }
}
