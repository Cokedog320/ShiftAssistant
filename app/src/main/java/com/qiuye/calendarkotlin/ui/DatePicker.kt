package com.qiuye.calendarkotlin.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.qiuye.calendarkotlin.R
import com.qiuye.calendarkotlin.domain.parseStorageDateOrNull
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Composable
fun DateJumpButton(
    onDatePicked: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPickerVisible by remember { mutableStateOf(false) }
    IconButton(
        onClick = { isPickerVisible = true },
        modifier = modifier.testTag("btn_date_jump"),
    ) {
        Icon(Icons.Rounded.EditCalendar, contentDescription = stringResource(R.string.jump_to_date))
    }

    if (isPickerVisible) {
        ChineseDatePickerBottomSheet(
            initialDate = LocalDate.now(),
            title = stringResource(R.string.jump_to_date),
            onDismiss = { isPickerVisible = false },
            onConfirm = { pickedDate ->
                onDatePicked(pickedDate)
                isPickerVisible = false
            },
        )
    }
}

@Composable
internal fun DatePickerField(
    label: String,
    value: String,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
    testTagPrefix: String? = null,
) {
    val initialDate = parseStorageDateOrNull(value) ?: LocalDate.now()
    var isPickerVisible by remember(value) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { isPickerVisible = true },
                modifier = Modifier
                    .weight(1f)
                    .thenTestTag(testTagPrefix?.let { "${it}_open" }),
            ) {
                Text(if (value.isBlank()) stringResource(R.string.select_date) else value)
            }
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.thenTestTag(testTagPrefix?.let { "${it}_clear" }),
            ) {
                Text(stringResource(R.string.clear))
            }
        }
    }

    if (isPickerVisible) {
        ChineseDatePickerBottomSheet(
            initialDate = initialDate,
            title = label,
            onDismiss = { isPickerVisible = false },
            onConfirm = { pickedDate ->
                onPick(pickedDate.format(shortDateFormatter()))
                isPickerVisible = false
            },
        )
    }
}

private fun Modifier.thenTestTag(tag: String?): Modifier = if (tag == null) this else testTag(tag)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChineseDatePickerBottomSheet(
    initialDate: LocalDate,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentYear = remember { LocalDate.now().year }
    val years = remember(currentYear) { (currentYear - 50..currentYear + 50).toList() }
    val months = remember { (1..12).toList() }
    var selectedYear by remember(initialDate) { mutableStateOf(initialDate.year) }
    var selectedMonth by remember(initialDate) { mutableStateOf(initialDate.monthValue) }
    var selectedDay by remember(initialDate) { mutableStateOf(initialDate.dayOfMonth) }

    val dayCount = remember(selectedYear, selectedMonth) {
        YearMonth.of(selectedYear, selectedMonth).lengthOfMonth()
    }
    val days = remember(dayCount) { (1..dayCount).toList() }
    if (selectedDay > dayCount) {
        selectedDay = dayCount
    }
    val selectedDate = remember(selectedYear, selectedMonth, selectedDay) {
        LocalDate.of(selectedYear, selectedMonth, selectedDay)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = selectedDate.format(fullDateFormatter()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WheelPicker(
                        values = years,
                        selectedValue = selectedYear,
                        label = { "$it" },
                        onValueSelected = { selectedYear = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wheel_year"),
                    )
                    WheelPicker(
                        values = months,
                        selectedValue = selectedMonth,
                        label = ::chineseMonthName,
                        onValueSelected = { selectedMonth = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wheel_month"),
                    )
                    WheelPicker(
                        values = days,
                        selectedValue = selectedDay.coerceAtMost(dayCount),
                        label = ::chineseDayName,
                        onValueSelected = { selectedDay = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wheel_day"),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(12.dp))
                TextButton(
                    onClick = { onConfirm(selectedDate) },
                    modifier = Modifier.testTag("btn_date_confirm"),
                ) {
                    Text(stringResource(R.string.confirm), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WheelPicker(
    values: List<Int>,
    selectedValue: Int,
    label: (Int) -> String,
    onValueSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val itemHeight = 56.dp
    val visibleItemCount = 3

    LaunchedEffect(values, selectedValue) {
        val targetIndex = values.indexOf(selectedValue)
        if (targetIndex >= 0 && targetIndex != listState.firstVisibleItemIndex) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    LaunchedEffect(values) {
        snapshotFlow {
            WheelScrollPosition(
                index = listState.firstVisibleItemIndex,
                offset = listState.firstVisibleItemScrollOffset,
                isScrolling = listState.isScrollInProgress,
            )
        }
            .filter { !it.isScrolling }
            .map { (index, offset) ->
                val adjustedIndex = if (offset > 0) index + 1 else index
                values.getOrNull(adjustedIndex.coerceIn(values.indices))
            }
            .distinctUntilChanged()
            .collect { value ->
                if (value != null && value != selectedValue) {
                    onValueSelected(value)
                }
            }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemCount),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ) {}
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { Spacer(modifier = Modifier.height(itemHeight)) }
            items(values.size) { index ->
                val value = values[index]
                val isSelected = value == selectedValue
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable { onValueSelected(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(value),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(itemHeight)) }
        }
    }
}

private data class WheelScrollPosition(
    val index: Int,
    val offset: Int,
    val isScrolling: Boolean,
)

private fun chineseMonthName(month: Int): String = "${chineseNumber(month)}月"

private fun chineseDayName(day: Int): String = when (day) {
    in 1..10 -> "${chineseNumber(day)}日"
    in 11..19 -> "十${chineseNumber(day % 10)}日"
    20 -> "二十日"
    in 21..29 -> "二十${chineseNumber(day % 10)}日"
    30 -> "三十日"
    31 -> "三十一日"
    else -> "${day}日"
}

private fun chineseNumber(value: Int): String = when (value) {
    1 -> "一"
    2 -> "二"
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    7 -> "七"
    8 -> "八"
    9 -> "九"
    10 -> "十"
    11 -> "十一"
    12 -> "十二"
    else -> value.toString()
}
