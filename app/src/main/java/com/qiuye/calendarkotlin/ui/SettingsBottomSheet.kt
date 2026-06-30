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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qiuye.calendarkotlin.R
import com.qiuye.calendarkotlin.model.CalendarData
import com.qiuye.calendarkotlin.ui.theme.LanguageMode
import com.qiuye.calendarkotlin.model.ShiftColorOption
import com.qiuye.calendarkotlin.model.ShiftDefinition
import com.qiuye.calendarkotlin.domain.normalizeProfileName
import com.qiuye.calendarkotlin.domain.normalizeProfileNameInput
import com.qiuye.calendarkotlin.model.defaultPattern
import com.qiuye.calendarkotlin.ui.theme.ThemeMode
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    calendarData: CalendarData,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    languageMode: LanguageMode,
    onLanguageModeChange: (LanguageMode) -> Unit,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onClearOverrides: () -> Unit,
    onSave: (String, String?, String?, List<ShiftDefinition>, Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onSwitchProfile: (String) -> Unit,
    onAddProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var profileName by remember(calendarData.activeProfileId) { mutableStateOf(calendarData.activeProfile.name) }
    var startDate by remember(calendarData.cycleStartDate) { mutableStateOf(calendarData.cycleStartDate.orEmpty()) }
    var endDate by remember(calendarData.cycleEndDate) { mutableStateOf(calendarData.cycleEndDate.orEmpty()) }
    var showLunar by remember(calendarData.showLunar) { mutableStateOf(calendarData.showLunar) }
    val pattern = remember(calendarData.pattern) {
        mutableStateListOf<ShiftDefinition>().apply { addAll(calendarData.pattern) }
    }
    val followSystemLabel = stringResource(R.string.theme_follow_system)
    val lightLabel = stringResource(R.string.theme_light)
    val darkLabel = stringResource(R.string.theme_dark)
    val zhLabel = stringResource(R.string.language_chinese)
    val newShiftName = stringResource(R.string.new_shift_default_name)

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
                    text = stringResource(R.string.schedule_settings),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 1f else 0.74f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.shift_profile_section),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var showProfileListBottomSheet by remember { mutableStateOf(false) }
                                Box {
                                    val defaultName = stringResource(R.string.new_shift_profile)
                                    OutlinedButton(
                                        onClick = { showProfileListBottomSheet = true },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.widthIn(max = 180.dp).testTag("btn_select_profile")
                                    ) {
                                        Text(
                                            text = calendarData.activeProfile.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (showProfileListBottomSheet) {
                                        ModalBottomSheet(
                                            onDismissRequest = { showProfileListBottomSheet = false }
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .navigationBarsPadding()
                                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.select_shift_profile),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                )
                                                LazyColumn(
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    items(calendarData.profiles) { profile ->
                                                        val isActive = profile.id == calendarData.activeProfileId
                                                        Surface(
                                                            onClick = {
                                                                showProfileListBottomSheet = false
                                                                onSwitchProfile(profile.id)
                                                                onDismiss() // Close settings sheet instantly on switch
                                                            },
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = profile.name,
                                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else if (isDark) Color(0xFFE3E3E3) else Color.Black,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                                if (isActive) {
                                                                    Icon(
                                                                        imageVector = Icons.Rounded.Check,
                                                                        contentDescription = stringResource(R.string.currently_active_cd),
                                                                        tint = MaterialTheme.colorScheme.primary
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

                                val defaultName = stringResource(R.string.new_shift_profile)
                                OutlinedButton(
                                    onClick = { onAddProfile(defaultName) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("btn_add_profile")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = stringResource(R.string.new_profile_cd),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.new_text))
                                }

                                if (calendarData.profiles.size > 1) {
                                    IconButton(
                                        onClick = { onDeleteProfile(calendarData.activeProfileId) },
                                        modifier = Modifier.testTag("btn_delete_profile")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = stringResource(R.string.delete_current_profile_cd),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = profileName,
                            onValueChange = { value ->
                                normalizeProfileNameInput(value, maxLength = 16)?.let { normalized ->
                                    profileName = normalized
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("field_profile_name"),
                            label = { Text(stringResource(R.string.profile_name_label)) },
                            singleLine = true,
                            maxLines = 1
                        )
                    }
                }
            }
            item {
                DatePickerField(
                    label = stringResource(R.string.cycle_start_date),
                    value = startDate,
                    onPick = { startDate = it },
                    onClear = { startDate = "" },
                    testTagPrefix = "field_cycle_start_date",
                )
            }
            item {
                DatePickerField(
                    label = stringResource(R.string.cycle_end_date),
                    value = endDate,
                    onPick = { endDate = it },
                    onClear = { endDate = "" },
                    testTagPrefix = "field_cycle_end_date",
                )
            }
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 1f else 0.74f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.theme_mode),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(
                                ThemeMode.SYSTEM to followSystemLabel,
                                ThemeMode.LIGHT to lightLabel,
                                ThemeMode.DARK to darkLabel,
                            ).forEach { (mode, label) ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { onThemeModeChange(mode) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (isDark) Color(0xFF3A4458) else MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = if (isDark) Color(0xFFE8F0FF) else MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 1f else 0.74f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.language_label),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(
                                LanguageMode.SYSTEM to followSystemLabel,
                                LanguageMode.ZH to zhLabel,
                                LanguageMode.EN to "English",
                            ).forEach { (mode, label) ->
                                FilterChip(
                                    selected = languageMode == mode,
                                    onClick = { onLanguageModeChange(mode) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (isDark) Color(0xFF3A4458) else MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = if (isDark) Color(0xFFE8F0FF) else MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 1f else 0.74f)) {
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
                            text = stringResource(R.string.show_lunar),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                            text = stringResource(R.string.show_lunar_desc),
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
                    onClick = {
                        onClearOverrides()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_clear_overrides"),
                ) {
                    Icon(Icons.Rounded.ClearAll, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.clear_all_overrides))
                }
            }
            item { HorizontalDivider() }
            item {
                Text(
                    text = stringResource(R.string.data_management),
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
                        Text(stringResource(R.string.export_backup))
                    }
                    OutlinedButton(
                        onClick = onImport,
                        modifier = Modifier.weight(1f)
                            .testTag("btn_settings_import"),
                    ) {
                        Text(stringResource(R.string.import_data))
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                Text(
                    text = stringResource(R.string.shift_pattern_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            itemsIndexed(pattern, key = { index, item -> "${index}_${item.id}" }) { index, shift ->
                PatternEditorCard(
                    index = index,
                    shift = shift,
                    isDark = isDark,
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
                            name = newShiftName,
                            color = ShiftColorOption.GRAY,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_day))
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    TextButton(
                        onClick = {
                            onSave(
                                profileName,
                                startDate.ifBlank { null },
                                endDate.ifBlank { null },
                                pattern.toList().ifEmpty { defaultPattern },
                                showLunar,
                            )
                        },
                        modifier = Modifier.testTag("btn_settings_save"),
                    ) {
                        Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
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
    isDark: Boolean = false,
    onUpdate: (ShiftDefinition) -> Unit,
    onRemove: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 1f else 0.74f)) {
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
                    text = stringResource(R.string.day_number_format, index + 1),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.delete))
                }
            }
            OutlinedTextField(
                value = shift.name,
                onValueChange = { value ->
                    normalizeProfileNameInput(value, maxLength = 20)?.let { normalizedName ->
                        val updatedShift =
                            if (shift.isBuiltInShift() && normalizedName != shift.name) {
                                shift.copy(
                                    id = UUID.randomUUID().toString(),
                                    name = normalizedName,
                                )
                            } else {
                                shift.copy(name = normalizedName)
                            }
                        onUpdate(updatedShift)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.shift_name_label)) },
                singleLine = true,
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.color_label),
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

private fun ShiftDefinition.isBuiltInShift(): Boolean =
    id == "1" || id == "2" || id == "3" || id == "vacation" || id == "business_trip"
