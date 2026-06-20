package com.qiuye.calendarkotlin.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qiuye.calendarkotlin.R
import com.qiuye.calendarkotlin.domain.displayName
import com.qiuye.calendarkotlin.viewmodel.CalendarUiState

@Composable
fun CalendarSummary(uiState: CalendarUiState, accentColor: Color, isDark: Boolean = false) {
    val startLabel = stringResource(R.string.start_date_label)
    val notSetText = stringResource(R.string.not_set)
    val endSeparator = stringResource(R.string.end_date_separator)
    val noLimitText = stringResource(R.string.no_limit)

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 1f else 0.75f),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("calendar_summary"),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.current_rule),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = buildString {
                    append(startLabel)
                    append(uiState.calendarData.cycleStartDate ?: notSetText)
                    append(endSeparator)
                    append(uiState.calendarData.cycleEndDate ?: noLimitText)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.calendarData.pattern.forEach { shift ->
                    val palette = shift.color.palette(isDark = isDark)
                    AssistChip(
                        onClick = {},
                        label = { Text(shift.displayName()) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = palette.container,
                            labelColor = palette.content,
                        ),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.holiday_chip_label)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isDark) Color(0xFF3A1818) else Color(0xFFFFE2E2),
                        labelColor = if (isDark) Color(0xFFFFB4B4) else Color(0xFFB42318),
                    ),
                )
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.work_chip_label)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isDark) Color(0xFF223042) else Color(0xFFE7EDF4),
                        labelColor = if (isDark) Color(0xFFB8D4F0) else Color(0xFF526171),
                    ),
                )
            }
            Text(
                text = stringResource(R.string.calendar_summary_stats, uiState.calendarData.pattern.size, uiState.calendarData.notes.size, uiState.calendarData.overrides.size),
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
            )
        }
    }
}
