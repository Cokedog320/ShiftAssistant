package com.qiuye.calendarkotlin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberModalBottomSheetState

import com.qiuye.calendarkotlin.tasks.data.ReminderEntity
import com.qiuye.calendarkotlin.tasks.data.formatTime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qiuye.calendarkotlin.model.ShiftDefinition
import com.qiuye.calendarkotlin.model.vacationShift
import java.time.LocalDate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DayDetailBottomSheet(
    date: LocalDate,
    currentShift: ShiftDefinition?,
    note: String,
    pattern: List<ShiftDefinition>,
    overrideShift: ShiftDefinition?,
    lunarFullText: String,
    holidayName: String?,
    holidayLabel: String?,
    tasks: List<ReminderEntity>,
    onDismiss: () -> Unit,
    onSave: (String, ShiftDefinition?, Int) -> Unit,
    onToggleTask: (ReminderEntity) -> Unit,
    onDeleteTask: (ReminderEntity) -> Unit,
    onOpenReminder: (ReminderEntity) -> Unit,
    onAddFullReminder: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var draftNote by remember(date, note) { mutableStateOf(note) }
    var selectedOverride by remember(date, overrideShift) { mutableStateOf(overrideShift) }
    var durationDays by remember(date) { mutableStateOf(1) }

    val uniqueShifts = remember(pattern) { pattern.distinctBy { it.name } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("sheet_day_detail"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = date.format(fullDateFormatter),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "当前显示：${currentShift?.name ?: "无排班"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (lunarFullText.isNotBlank() || holidayName != null) {
                        Text(
                            text = buildString {
                                if (lunarFullText.isNotBlank()) {
                                    append("农历 ")
                                    append(lunarFullText)
                                }
                                if (holidayName != null) {
                                    if (isNotEmpty()) append(" · ")
                                    append(holidayLabel ?: "")
                                    append(" ")
                                    append(holidayName)
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭")
                }
            }

            Text(
                text = "手动改班",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OverrideChip(
                    label = "跟随规律",
                    selected = selectedOverride == null,
                    palette = null,
                    onClick = { selectedOverride = null },
                )
                uniqueShifts.forEach { shift ->
                    OverrideChip(
                        label = shift.name,
                        selected = selectedOverride?.name == shift.name,
                        palette = shift.color.palette(),
                        onClick = { selectedOverride = shift },
                    )
                }
                OverrideChip(
                    label = vacationShift.name,
                    selected = selectedOverride?.id == vacationShift.id,
                    palette = vacationShift.color.palette(),
                    onClick = { selectedOverride = vacationShift },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "持续天数",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledIconButton(
                        onClick = { if (durationDays > 1) durationDays-- },
                        modifier = Modifier.size(32.dp),
                        enabled = durationDays > 1
                    ) {
                        Text("-", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "$durationDays 天",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    FilledIconButton(
                        onClick = { durationDays++ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("+", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "任务提醒",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                FilledIconButton(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onAddFullReminder()
                        }
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "添加提醒", modifier = Modifier.size(18.dp))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tasks.forEach { task ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { onToggleTask(task) }
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    coroutineScope.launch {
                                        sheetState.hide()
                                        onOpenReminder(task)
                                    }
                                },
                        ) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            )
                            Text(
                                text = "⏰ ${formatTime(task.scheduledAtMillis)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        IconButton(onClick = {
                            coroutineScope.launch {
                                sheetState.hide()
                                onOpenReminder(task)
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "查看详情", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { onDeleteTask(task) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "删除任务", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            OutlinedTextField(
                value = draftNote,
                onValueChange = { draftNote = it },
                modifier = Modifier
                    .height(160.dp)
                    .fillMaxWidth()
                    .testTag("input_day_note"),
                label = { Text("备注") },
                placeholder = { Text("添加备忘、待办事项或交接说明") },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("取消")
                }
                Spacer(modifier = Modifier.width(12.dp))
                FilledIconButton(
                    onClick = { onSave(draftNote, selectedOverride, durationDays) },
                    modifier = Modifier.testTag("btn_day_save"),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "保存")
                }
            }
        }
    }
}

@Composable
private fun OverrideChip(
    label: String,
    selected: Boolean,
    palette: ShiftPalette?,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = palette?.container ?: MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = palette?.content ?: MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}


