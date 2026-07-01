package com.qiuye.calendarkotlin.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qiuye.calendarkotlin.R
import java.time.YearMonth

/**
 * Compact profile-name title meant to be placed inside the top app bar's
 * `title` slot, next to the settings/date-jump actions, so it stays centered
 * in that row instead of pushing the calendar grid down. The month stays in
 * its existing position inside [MonthHeader].
 */
@Composable
fun CalendarTitleBar(
    accentColor: Color,
    profileName: String,
    onProfileClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .then(if (onProfileClick != null) Modifier.clickable { onProfileClick() } else Modifier)
            .testTag("title_app_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = profileName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onProfileClick != null) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.switch_profile),
                tint = accentColor,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Season icon + month display alongside day-specific actions (jump to today /
 * edit the selected day's note). The profile name now lives in the top app
 * bar title; the month stays here, in its existing position.
 */
@Composable
fun MonthHeader(
    month: YearMonth,
    accentColor: Color,
    seasonIcon: ImageVector,
    onToday: () -> Unit,
    onOpenSelectedDayDetail: () -> Unit,
    isDayDetailEnabled: Boolean,
) {
    val detailIconColor =
        if (isDayDetailEnabled) accentColor else accentColor.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = seasonIcon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = month.format(shortMonthFormatter()),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("month_title"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.wrapContentWidth(align = Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    if (isDayDetailEnabled) {
                        onOpenSelectedDayDetail()
                    }
                },
                modifier = Modifier.testTag("btn_day_detail"),
            ) {
                Icon(
                    Icons.Rounded.EditNote,
                    contentDescription = stringResource(R.string.edit_selected_date),
                    tint = detailIconColor,
                )
            }
            TextButton(
                onClick = onToday,
                modifier = Modifier.testTag("btn_today"),
            ) {
                Text(stringResource(R.string.today), color = accentColor)
            }
        }
    }
}
