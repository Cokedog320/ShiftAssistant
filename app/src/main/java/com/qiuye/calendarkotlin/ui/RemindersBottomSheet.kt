package com.qiuye.calendarkotlin.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qiuye.calendarkotlin.tasks.data.ReminderEntity
import com.qiuye.calendarkotlin.tasks.data.formatDate
import com.qiuye.calendarkotlin.tasks.data.formatTime
import java.time.LocalDate
import java.time.YearMonth

private enum class ReminderScope {
    ALL,
    PENDING,
    COMPLETED,
    OVERDUE,
}

private fun ReminderScope.label(): String =
    when (this) {
        ReminderScope.ALL -> "全部"
        ReminderScope.PENDING -> "待完成"
        ReminderScope.COMPLETED -> "已完成"
        ReminderScope.OVERDUE -> "已过期"
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RemindersBottomSheet(
    reminders: List<ReminderEntity>,
    onDismiss: () -> Unit,
    onToggleReminder: (ReminderEntity) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit,
    onOpenReminder: (ReminderEntity) -> Unit,
    onJumpToDate: (LocalDate) -> Unit,
    onAddReminder: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var keyword by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableStateOf<YearMonth?>(null) }
    var selectedScope by remember { mutableStateOf(ReminderScope.ALL) }
    
    val now = remember(reminders) { System.currentTimeMillis() }
    
    val months = remember(reminders) {
        reminders.map { reminder ->
            val localDate = java.time.Instant.ofEpochMilli(reminder.scheduledAtMillis)
                .atZone(com.qiuye.calendarkotlin.tasks.data.zoneIdProvider())
                .toLocalDate()
            YearMonth.from(localDate)
        }.distinct().sortedDescending()
    }
    
    val filteredReminders = remember(reminders, keyword, selectedMonth, selectedScope, now) {
        reminders.filter { reminder ->
            val reminderLocalDate = java.time.Instant.ofEpochMilli(reminder.scheduledAtMillis)
                .atZone(com.qiuye.calendarkotlin.tasks.data.zoneIdProvider())
                .toLocalDate()
            
            val monthMatches = selectedMonth == null || YearMonth.from(reminderLocalDate) == selectedMonth
            
            val scopeMatches = when (selectedScope) {
                ReminderScope.ALL -> true
                ReminderScope.PENDING -> !reminder.isCompleted && reminder.scheduledAtMillis > now
                ReminderScope.COMPLETED -> reminder.isCompleted
                ReminderScope.OVERDUE -> !reminder.isCompleted && reminder.scheduledAtMillis <= now
            }
            
            val keywordMatches = keyword.isBlank() ||
                reminder.title.contains(keyword, ignoreCase = true) ||
                reminder.note.contains(keyword, ignoreCase = true) ||
                dateMatchesKeyword(reminderLocalDate, keyword)
                
            monthMatches && scopeMatches && keywordMatches
        }
    }
    
    val totalCount = reminders.size
    val pendingCount = remember(reminders, now) {
        reminders.count { !it.isCompleted && it.scheduledAtMillis > now }
    }
    val completedCount = remember(reminders) {
        reminders.count { it.isCompleted }
    }
    val upcomingReminder = remember(reminders, now) {
        reminders
            .filter { !it.isCompleted && it.scheduledAtMillis > now }
            .minByOrNull { it.scheduledAtMillis }
    }
    val upcomingText = remember(upcomingReminder) {
        upcomingReminder?.let {
            val localDate = java.time.Instant.ofEpochMilli(it.scheduledAtMillis)
                .atZone(com.qiuye.calendarkotlin.tasks.data.zoneIdProvider())
                .toLocalDate()
            val monthAndDay = localDate.format(java.time.format.DateTimeFormatter.ofPattern("M月d日", java.util.Locale.CHINA))
            "$monthAndDay ${formatTime(it.scheduledAtMillis)}"
        } ?: "无"
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("sheet_reminders"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "提醒中心",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "汇总并管理所有日程、待办和定时提醒事项",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledIconButton(
                            onClick = {
                                coroutineScope.launch {
                                    sheetState.hide()
                                    onAddReminder()
                                }
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "添加提醒", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "关闭")
                        }
                    }
                }
            }
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ReminderStatCard(
                        title = "总提醒数",
                        value = totalCount.toString(),
                    )
                    ReminderStatCard(
                        title = "待完成",
                        value = pendingCount.toString(),
                    )
                    ReminderStatCard(
                        title = "已完成",
                        value = completedCount.toString(),
                    )
                    ReminderStatCard(
                        title = "最近一条",
                        value = upcomingText,
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ReminderScope.entries.forEach { scope ->
                        FilterChip(
                            selected = selectedScope == scope,
                            onClick = { selectedScope = scope },
                            label = { Text(scope.label()) },
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
                        .testTag("input_reminders_search"),
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    label = { Text("搜索提醒") },
                    placeholder = { Text("输入提醒标题、备注或日期关键词") },
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
                        label = { Text("全部月份") },
                    )
                    months.forEach { month ->
                        FilterChip(
                            selected = selectedMonth == month,
                            onClick = { selectedMonth = month },
                            label = { Text(month.format(noteMonthFormatter)) },
                        )
                    }
                }
            }
            item {
                Text(
                    text = "共 ${filteredReminders.size} 条提醒",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (filteredReminders.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = if (reminders.isEmpty()) "还没有任何提醒" else "没有符合条件的提醒",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (reminders.isEmpty()) "在主界面的具体日期详情中点击下方按钮创建提醒。" else "换个关键词、月份或筛选状态试试。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(filteredReminders, key = { it.id }) { reminder ->
                    ReminderItemCard(
                        reminder = reminder,
                        onClick = {
                            coroutineScope.launch {
                                sheetState.hide()
                                onOpenReminder(reminder)
                            }
                        },
                        onToggleCompleted = { onToggleReminder(reminder) },
                        onDelete = { onDeleteReminder(reminder) },
                        onJumpToDate = {
                            coroutineScope.launch {
                                sheetState.hide()
                                val localDate = java.time.Instant.ofEpochMilli(reminder.scheduledAtMillis)
                                    .atZone(com.qiuye.calendarkotlin.tasks.data.zoneIdProvider())
                                    .toLocalDate()
                                onJumpToDate(localDate)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderStatCard(
    title: String,
    value: String,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.82f),
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReminderItemCard(
    reminder: ReminderEntity,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit,
    onJumpToDate: () -> Unit,
) {
    val now = remember { System.currentTimeMillis() }
    val isExpired = !reminder.isCompleted && reminder.scheduledAtMillis <= now
    val backgroundColor = androidx.compose.animation.animateColorAsState(
        targetValue = if (reminder.isCompleted) Color.White.copy(alpha = 0.6f) else Color.White,
        label = "reminderCardColor"
    ).value

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = if (reminder.isCompleted) 0.dp else 4.dp,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (reminder.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable { onToggleCompleted() },
                    contentAlignment = Alignment.Center
                ) {
                    if (reminder.isCompleted) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "已完成",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Canvas(modifier = Modifier.size(26.dp)) {
                            drawCircle(
                                color = Color(0xFF81B98F),
                                radius = size.minDimension / 2.2f,
                                style = Stroke(width = 3f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (reminder.isCompleted) FontWeight.Medium else FontWeight.Bold,
                        textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (reminder.isCompleted) Color.Gray else Color(0xFF333333),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = if (isExpired && !reminder.isCompleted) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${formatDate(reminder.scheduledAtMillis)} ${formatTime(reminder.scheduledAtMillis)}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isExpired && !reminder.isCompleted) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        AssistChip(
                            onClick = onJumpToDate,
                            label = { Text("查看日期", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "删除",
                        tint = Color.LightGray.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (reminder.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reminder.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 38.dp)
                )
            }
        }
    }
}

private fun dateMatchesKeyword(date: LocalDate, keyword: String): Boolean {
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
