package com.qiuye.calendarkotlin.diary.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qiuye.calendarkotlin.R
import com.qiuye.calendarkotlin.diary.data.DiaryEntity
import com.qiuye.calendarkotlin.ui.fullDateFormatter
import com.qiuye.calendarkotlin.ui.noteMonthFormatter
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiaryListBottomSheet(
    entries: List<DiaryEntity>,
    searchQuery: String,
    searchResults: List<DiaryEntity>,
    onSearchQueryChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onWriteDiaryClick: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val displayedEntries = remember(entries, searchQuery, searchResults) {
        if (searchQuery.isBlank()) entries else searchResults
    }

    var expandedYears by remember { mutableStateOf(setOf<Int>()) }
    var expandedMonths by remember { mutableStateOf(setOf<YearMonth>()) }
    val currentMonth = remember { YearMonth.now() }
    val monthGroups = remember(displayedEntries, currentMonth) {
        buildDiaryMonthGroups(displayedEntries, currentMonth)
    }
    val yearGroups = remember(displayedEntries, currentMonth) {
        buildDiaryYearGroups(displayedEntries, currentMonth)
    }
    LaunchedEffect(searchQuery, yearGroups, monthGroups, currentMonth) {
        if (searchQuery.isBlank()) {
            expandedYears = emptySet()
            expandedMonths = emptySet()
        } else {
            expandedYears = yearGroups.map { it.year }.toSet()
            expandedMonths = monthGroups.map { it.month }.toSet()
        }
    }

    val totalCount = entries.size
    val currentMonthCount = remember(entries) {
        val currentMonthPrefix = YearMonth.now().toString() // "2026-05"
        entries.count { it.dateKey.startsWith(currentMonthPrefix) }
    }
    val withMoodCount = remember(entries) {
        entries.count { it.mood.isNotBlank() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyColumn(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("sheet_diary_list"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            // Title Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.diary_center),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.diary_center_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
                    }
                }
            }

            // Write Diary Button
            item {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onWriteDiaryClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_write_diary"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.write_diary),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Stats
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DiaryStatCard(title = stringResource(R.string.total_diary_count), value = totalCount.toString())
                    DiaryStatCard(title = stringResource(R.string.this_month_diary), value = currentMonthCount.toString())
                    DiaryStatCard(title = stringResource(R.string.mood_records), value = withMoodCount.toString())
                    DiaryStatCard(title = stringResource(R.string.current_filter), value = displayedEntries.size.toString())
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_diary_search"),
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.clear))
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.search_diary_label)) },
                    placeholder = { Text(stringResource(R.string.search_diary_placeholder)) },
                    singleLine = true,
                )
            }

            // All Years label
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.46f),
                        modifier = Modifier.testTag("diary_all_years"),
                    ) {
                        Text(
                            text = stringResource(R.string.all_years),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            // Entries List
            if (displayedEntries.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = if (entries.isEmpty()) stringResource(R.string.no_diary_yet) else stringResource(R.string.no_diary_matching),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (entries.isEmpty()) stringResource(R.string.no_diary_hint) else stringResource(R.string.no_diary_filter_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                yearGroups.forEach { yearGroup ->
                    item(key = "diary_year_header_${yearGroup.year}") {
                        YearGroupHeader(
                            year = yearGroup.year,
                            expanded = expandedYears.contains(yearGroup.year),
                            onToggle = {
                                expandedYears =
                                    if (expandedYears.contains(yearGroup.year)) {
                                        expandedYears - yearGroup.year
                                    } else {
                                        expandedYears + yearGroup.year
                                    }
                            },
                        )
                    }
                    if (expandedYears.contains(yearGroup.year)) {
                        yearGroup.months.forEach { group ->
                            item(key = "diary_month_header_${group.month}") {
                                MonthGroupHeader(
                                    month = group.month,
                                    count = group.entries.size,
                                    expanded = expandedMonths.contains(group.month),
                                    onToggle = {
                                        expandedMonths =
                                            if (expandedMonths.contains(group.month)) {
                                                expandedMonths - group.month
                                            } else {
                                                expandedMonths + group.month
                                            }
                                    },
                                )
                            }
                            if (expandedMonths.contains(group.month)) {
                                items(group.entries, key = { it.id }) { entry ->
                                    DiaryItemCard(
                                        entry = entry,
                                        onClick = {
                                            coroutineScope.launch {
                                                sheetState.hide()
                                                val parsedDate = LocalDate.parse(entry.dateKey)
                                                onSelectDate(parsedDate)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearGroupHeader(
    year: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag("diary_year_group_$year"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$year",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun MonthGroupHeader(
    month: YearMonth,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag("diary_month_group_${month.format(noteMonthFormatter())}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${month.format(noteMonthFormatter())} · $count",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun DiaryStatCard(
    title: String,
    value: String,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
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

@Composable
private fun DiaryItemCard(
    entry: DiaryEntity,
    onClick: () -> Unit,
) {
    val date = remember(entry.dateKey) { LocalDate.parse(entry.dateKey) }
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diary_item_${entry.dateKey}")
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = date.format(fullDateFormatter()),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (entry.mood.isNotBlank()) {
                        Text(
                            text = entry.mood,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
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

private data class DiaryMonthGroup(
    val month: YearMonth,
    val entries: List<DiaryEntity>,
)

private data class DiaryYearGroup(
    val year: Int,
    val months: List<DiaryMonthGroup>,
)

private fun DiaryEntity.toYearMonth(): YearMonth {
    return YearMonth.parse(dateKey.substring(0, 7))
}

private fun buildDiaryMonthGroups(
    entries: List<DiaryEntity>,
    currentMonth: YearMonth,
): List<DiaryMonthGroup> {
    val grouped = entries.groupBy { it.toYearMonth() }
    val months = grouped.keys.sortedDescending()
    val orderedMonths =
        buildList {
            if (grouped.containsKey(currentMonth)) {
                add(currentMonth)
            }
            addAll(months.filterNot { it == currentMonth })
        }
    return orderedMonths.map { month ->
        DiaryMonthGroup(
            month = month,
            entries = grouped[month].orEmpty().sortedByDescending { it.dateKey },
        )
    }
}

private fun buildDiaryYearGroups(
    entries: List<DiaryEntity>,
    currentMonth: YearMonth,
): List<DiaryYearGroup> {
    val entriesByYear = entries.groupBy { it.toYearMonth().year }
    return entriesByYear.keys.sortedDescending().map { year ->
        val yearEntries = entriesByYear[year].orEmpty()
        val groupedByMonth = yearEntries.groupBy { it.toYearMonth() }
        val months = groupedByMonth.keys.sortedDescending()
        val orderedMonths =
            if (year == currentMonth.year && groupedByMonth.containsKey(currentMonth)) {
                listOf(currentMonth) + months.filterNot { it == currentMonth }
            } else {
                months
            }
        DiaryYearGroup(
            year = year,
            months =
                orderedMonths.map { month ->
                    DiaryMonthGroup(
                        month = month,
                        entries = groupedByMonth[month].orEmpty().sortedByDescending { it.dateKey },
                    )
                },
        )
    }
}
