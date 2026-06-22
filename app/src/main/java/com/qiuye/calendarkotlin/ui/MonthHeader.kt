package com.qiuye.calendarkotlin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qiuye.calendarkotlin.R
import java.time.YearMonth

@Composable
fun MonthHeader(
    month: YearMonth,
    accentColor: Color,
    isDark: Boolean = false,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onOpenSelectedDayDetail: () -> Unit,
    isDayDetailEnabled: Boolean,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 1f else 0.8f),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 4.dp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            ) {
                Text(
                    text = month.format(monthFormatter()),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.testTag("month_title"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.view_schedule_by_month),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.wrapContentWidth(align = Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onOpenSelectedDayDetail,
                    enabled = isDayDetailEnabled,
                    modifier = Modifier.testTag("btn_day_detail"),
                ) {
                    Icon(Icons.Rounded.EditNote, contentDescription = stringResource(R.string.edit_selected_date))
                }
                TextButton(
                    onClick = onToday,
                    modifier = Modifier.testTag("btn_today"),
                ) {
                    Text(stringResource(R.string.today), color = accentColor)
                }
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = stringResource(R.string.prev_month))
                }
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = stringResource(R.string.next_month))
                }
            }
        }
    }
}
