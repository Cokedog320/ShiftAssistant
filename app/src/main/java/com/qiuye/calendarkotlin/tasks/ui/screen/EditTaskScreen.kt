package com.qiuye.calendarkotlin.tasks.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiuye.calendarkotlin.tasks.data.ReminderEntity
import com.qiuye.calendarkotlin.tasks.data.combineDateAndMinutes
import com.qiuye.calendarkotlin.tasks.data.formatDate
import com.qiuye.calendarkotlin.tasks.data.formatTime
import com.qiuye.calendarkotlin.tasks.data.minutesOfDay as extractMinutesOfDay
import com.qiuye.calendarkotlin.tasks.data.nowDateStartMillis
import com.qiuye.calendarkotlin.tasks.data.roundedUpFiveMinuteSlot
import com.qiuye.calendarkotlin.tasks.data.startOfDayMillis
import com.qiuye.calendarkotlin.tasks.service.SaveReminderResult
import com.qiuye.calendarkotlin.tasks.ui.theme.AutumnGradient
import com.qiuye.calendarkotlin.tasks.ui.theme.LargeCardShape
import com.qiuye.calendarkotlin.tasks.ui.theme.PrimaryAccent
import com.qiuye.calendarkotlin.tasks.ui.theme.TasksTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val datePickerZoneId: ZoneId = ZoneId.systemDefault()
private val chineseDatePickerLocale: Locale = Locale.SIMPLIFIED_CHINESE
private val chineseMonthYearFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月", chineseDatePickerLocale)
private val chineseDayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd", chineseDatePickerLocale)
private val chineseDayContentFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月d日", chineseDatePickerLocale)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    reminderId: Long?,
    initialDateMillis: Long? = null,
    hasNotificationPermission: Boolean,
    hasExactAlarmPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onNavigateBack: () -> Unit,
    onSave: suspend (Long?, String, String, Long, Int, Boolean) -> SaveReminderResult,
    onDelete: (suspend (Long) -> Unit)?,
    onLoadReminder: suspend (Long) -> ReminderEntity?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val datePickerFormatter = remember {
        object : DatePickerFormatter {
            override fun formatMonthYear(monthMillis: Long?, locale: androidx.compose.material3.CalendarLocale): String? {
                if (monthMillis == null) return null
                return Instant.ofEpochMilli(monthMillis)
                    .atZone(datePickerZoneId)
                    .toLocalDate()
                    .format(chineseMonthYearFormatter)
            }

            override fun formatDate(
                dateMillis: Long?,
                locale: androidx.compose.material3.CalendarLocale,
                forContentDescription: Boolean
            ): String? {
                if (dateMillis == null) return null
                val localDate = Instant.ofEpochMilli(dateMillis)
                    .atZone(datePickerZoneId)
                    .toLocalDate()
                return if (forContentDescription) {
                    localDate.format(chineseDayContentFormatter)
                } else {
                    localDate.format(chineseDayFormatter)
                }
            }
        }
    }

    var title by rememberSaveable(reminderId, initialDateMillis) { mutableStateOf("") }
    val parsedTitle = remember(title) { title.lines().firstOrNull()?.trim().orEmpty() }
    var dateStartMillis by rememberSaveable(reminderId, initialDateMillis) { mutableStateOf(nowDateStartMillis()) }
    var minutesOfDay by rememberSaveable(reminderId, initialDateMillis) { mutableStateOf(roundedUpFiveMinuteSlot().second) }
    var loadedReminder by rememberSaveable(reminderId) { mutableStateOf(false) }
    var missingReminder by rememberSaveable(reminderId) { mutableStateOf(false) }
    var statusMessage by rememberSaveable(reminderId) { mutableStateOf<String?>(null) }
    var showDateDialog by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }
    var showPastConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf(false) }
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(reminderId, initialDateMillis) {
        if (reminderId == null) {
            val todayStart = startOfDayMillis(System.currentTimeMillis())
            val targetStart = startOfDayMillis(initialDateMillis ?: System.currentTimeMillis())
            dateStartMillis = targetStart

            if (targetStart != todayStart) {
                minutesOfDay = 9 * 60
            } else {
                val (roundedDateMillis, roundedMinutes) = roundedUpFiveMinuteSlot()
                minutesOfDay = roundedMinutes
            }
            loadedReminder = true
            return@LaunchedEffect
        }

        val reminder = onLoadReminder(reminderId)
        if (reminder == null) {
            missingReminder = true
            loadedReminder = true
            return@LaunchedEffect
        }

        title = reminder.title.trimEnd() + if (reminder.note.trim().isEmpty()) "" else "\n" + reminder.note.trim()
        dateStartMillis = startOfDayMillis(reminder.scheduledAtMillis)
        minutesOfDay = extractMinutesOfDay(reminder.scheduledAtMillis)
        loadedReminder = true
    }

    suspend fun performSave(allowPast: Boolean) {
        pendingSave = true
        val lines = title.lines()
        val titleToSave = lines.firstOrNull()?.trim().orEmpty()
        val noteToSave = lines.drop(1).joinToString("\n").trim()

        when (val result = onSave(reminderId, titleToSave, noteToSave, dateStartMillis, minutesOfDay, allowPast)) {
            is SaveReminderResult.Success -> {
                val baseMessage = if (result.scheduledAlarm) "保存成功" else "已保存（时间已过去，不会闹铃）"
                val warning = if (result.needsNotificationWarning || result.needsExactAlarmWarning) {
                    if (result.needsNotificationWarning && result.needsExactAlarmWarning) {
                        "，但通知和闹钟权限未开启"
                    } else if (result.needsNotificationWarning) {
                        "，但通知权限未开启"
                    } else {
                        "，但精确闹钟权限未开启"
                    }
                } else ""
                
                val toastDuration = if (warning.isNotEmpty()) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                Toast.makeText(context, baseMessage + warning, toastDuration).show()
                onNavigateBack()
            }
            SaveReminderResult.NeedsPastConfirmation -> {
                showPastConfirm = true
            }
            is SaveReminderResult.ValidationError -> {
                snackbarHostState.showSnackbar(result.message)
            }
        }
        pendingSave = false
    }

    if (showDateDialog) {
        val datePickerState = remember(dateStartMillis) {
            DatePickerState(
                locale = chineseDatePickerLocale,
                initialSelectedDateMillis = dateStartMillis,
                initialDisplayedMonthMillis = dateStartMillis
            )
        }
        val datePickerHeadline = remember(datePickerState.selectedDateMillis, dateStartMillis) {
            formatDate(datePickerState.selectedDateMillis ?: dateStartMillis)
        }

        DatePickerDialog(
            onDismissRequest = { showDateDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            dateStartMillis = startOfDayMillis(it)
                        }
                        showDateDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateDialog = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                dateFormatter = datePickerFormatter,
                title = {
                    Text(
                        text = "选择日期",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                },
                headline = {
                    Text(
                        text = datePickerHeadline,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    }

    if (showTimeDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = minutesOfDay / 60,
            initialMinute = minutesOfDay % 60,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimeDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        minutesOfDay = timePickerState.hour * 60 + timePickerState.minute
                        showTimeDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimeDialog = false }) {
                    Text("取消")
                }
            },
            title = { Text("选择时间") },
            text = { TimePicker(state = timePickerState) }
        )
    }



    if (showPastConfirm) {
        AlertDialog(
            onDismissRequest = { showPastConfirm = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPastConfirm = false
                        scope.launch {
                            performSave(allowPast = true)
                        }
                    }
                ) {
                    Text("继续保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPastConfirm = false }) {
                    Text("取消")
                }
            },
            title = { Text("时间已过去") },
            text = { Text("保存后不会注册闹钟，是否继续？") }
        )
    }

    if (showDeleteConfirm && reminderId != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        scope.launch {
                            onDelete(reminderId)
                            onNavigateBack()
                        }
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            },
            title = { Text("删除提醒") },
            text = { Text("删除后会同时取消系统闹钟。") }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (reminderId == null) "新建提醒" else "编辑提醒",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF333333)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.Black)
                    }
                },
                actions = {
                    if (reminderId != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFFD32F2F))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!pendingSave && parsedTitle.isNotBlank()) Color(0xFF5EBC83) else Color(0xFFE5E5E5))
                            .clickable(enabled = !pendingSave && parsedTitle.isNotBlank()) {
                                scope.launch { performSave(allowPast = false) }
                            }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "保存", 
                            fontWeight = FontWeight.Medium, 
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFF9F5EA))
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9F5EA))
                .padding(innerPadding)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 日期时间组合卡片
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 1.dp,
                    border = BorderStroke(1.dp, Color(0xFFE5E5E5).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingRow(
                            label = "提醒日期",
                            value = formatDate(dateStartMillis),
                            onClick = { showDateDialog = true },
                            iconContent = {
                                Icon(Icons.Rounded.DateRange, contentDescription = null, tint = Color(0xFF68B48B), modifier = Modifier.size(20.dp))
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color(0xFFE5E5E5).copy(alpha = 0.4f)
                        )
                        SettingRow(
                            label = "提醒时间",
                            value = formatTime(combineDateAndMinutes(dateStartMillis, minutesOfDay)),
                            onClick = { showTimeDialog = true },
                            iconContent = {
                                Icon(Icons.Rounded.Notifications, contentDescription = null, tint = Color(0xFF68B48B), modifier = Modifier.size(20.dp))
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color(0xFFE5E5E5).copy(alpha = 0.4f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "快捷",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val now = System.currentTimeMillis()
                            listOf(
                                Triple("早上", 9, 0),
                                Triple("下午", 13, 0),
                                Triple("晚上", 20, 0)
                            ).forEach { (label, h, m) ->
                                val minutes = h * 60 + m
                                val presetMillis = combineDateAndMinutes(dateStartMillis, minutes)
                                val isPast = presetMillis <= now
                                val isSelected = minutesOfDay == minutes
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { minutesOfDay = minutes },
                                    enabled = !isPast,
                                    label = { Text("$label ${String.format("%02d:%02d", h, m)}", style = MaterialTheme.typography.bodySmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                )
                            }
                        }
                    }
                }

                // 精确闹钟权限提示
                if (!hasExactAlarmPermission) {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFCC02).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Notifications,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "精确闹钟权限未开启",
                                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE65100))
                                )
                                Text(
                                    text = "提醒可能会延迟约1分钟。点击右侧按钮前往设置开启。",
                                    style = TextStyle(fontSize = 12.sp, color = Color(0xFF795548))
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE65100))
                                    .clickable { onRequestExactAlarmPermission() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("去开启", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // 任务内容卡片
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 1.dp,
                    border = BorderStroke(1.dp, Color(0xFFE5E5E5).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        TextField(
                            value = title,
                            onValueChange = { title = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                            placeholder = { Text("准备做什么？", color = Color(0xFF333333).copy(alpha = 0.4f), fontSize = 16.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Default)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        iconContent()
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, fontSize = 16.sp, color = Color(0xFF333333))
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 16.sp, color = Color(0xFF333333))
    }
}

