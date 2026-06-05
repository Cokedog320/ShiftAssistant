package com.qiuye.calendarkotlin.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qiuye.calendarkotlin.model.CalendarData
import com.qiuye.calendarkotlin.model.ShiftColorOption
import com.qiuye.calendarkotlin.model.ShiftDefinition
import com.qiuye.calendarkotlin.model.defaultPattern
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    calendarData: CalendarData,
    onDismiss: () -> Unit,
    onClearOverrides: () -> Unit,
    onSave: (String?, String?, List<ShiftDefinition>, Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var startDate by remember(calendarData.cycleStartDate) { mutableStateOf(calendarData.cycleStartDate.orEmpty()) }
    var endDate by remember(calendarData.cycleEndDate) { mutableStateOf(calendarData.cycleEndDate.orEmpty()) }
    var showLunar by remember(calendarData.showLunar) { mutableStateOf(calendarData.showLunar) }
    val pattern = remember(calendarData.pattern) {
        mutableStateListOf<ShiftDefinition>().apply { addAll(calendarData.pattern) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .navigationBarsPadding()
                .testTag("sheet_settings"),
        ) {
            item {
                Text(
                    text = "排班设置",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            item {
                DatePickerField(
                    label = "周期开始日期",
                    value = startDate,
                    onPick = { startDate = it },
                    onClear = { startDate = "" },
                    testTagPrefix = "field_cycle_start_date",
                )
            }
            item {
                DatePickerField(
                    label = "周期结束日期",
                    value = endDate,
                    onPick = { endDate = it },
                    onClear = { endDate = "" },
                    testTagPrefix = "field_cycle_end_date",
                )
            }
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = 0.74f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "显示农历",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "在月历格子和日期详情中显示农历、节气和传统节日",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = showLunar,
                            onCheckedChange = { showLunar = it },
                            modifier = Modifier.testTag("switch_show_lunar"),
                        )
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = onClearOverrides,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_clear_overrides"),
                ) {
                    Icon(Icons.Rounded.ClearAll, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("清除所有手动改班")
                }
            }
            item { HorizontalDivider() }
            item {
                Text(
                    text = "数据管理",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onExport,
                        modifier = Modifier.weight(1f)
                            .testTag("btn_settings_export"),
                    ) {
                        Text("导出备份")
                    }
                    OutlinedButton(
                        onClick = onImport,
                        modifier = Modifier.weight(1f)
                            .testTag("btn_settings_import"),
                    ) {
                        Text("导入数据")
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                Text(
                    text = "排班规律",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            itemsIndexed(pattern, key = { _, item -> item.id }) { index, shift ->
                PatternEditorCard(
                    index = index,
                    shift = shift,
                    onUpdate = { updated -> pattern[index] = updated },
                    onRemove = {
                        if (pattern.size > 1) {
                            pattern.removeAt(index)
                        }
                    },
                )
            }
            item {
                OutlinedButton(
                    onClick = {
                        pattern += ShiftDefinition(
                            id = UUID.randomUUID().toString(),
                            name = "新班次",
                            color = ShiftColorOption.GRAY,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加一天")
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    TextButton(
                        onClick = {
                            onSave(
                                startDate.ifBlank { null },
                                endDate.ifBlank { null },
                                pattern.toList().ifEmpty { defaultPattern },
                                showLunar,
                            )
                        },
                        modifier = Modifier.testTag("btn_settings_save"),
                    ) {
                        Text("保存", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PatternEditorCard(
    index: Int,
    shift: ShiftDefinition,
    onUpdate: (ShiftDefinition) -> Unit,
    onRemove: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = 0.74f)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "第 ${index + 1} 天",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Rounded.Close, contentDescription = "删除")
                }
            }
            OutlinedTextField(
                value = shift.name,
                onValueChange = { onUpdate(shift.copy(name = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("班次名称") },
            )
            Text(
                text = "颜色",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShiftColorOption.entries.forEach { option ->
                    val palette = option.palette()
                    FilterChip(
                        selected = option == shift.color,
                        onClick = { onUpdate(shift.copy(color = option)) },
                        label = { Text(option.label()) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.container,
                            selectedLabelColor = palette.content,
                        ),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}


