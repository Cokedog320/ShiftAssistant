package com.qiuye.calendarkotlin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiuye.calendarkotlin.model.DayCell
import com.qiuye.calendarkotlin.model.ShiftDefinition
import java.time.LocalDate

@Composable
fun CalendarGrid(
    dayCells: List<DayCell>,
    seasonAccent: Color,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.White.copy(alpha = 0.86f),
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 6.dp,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                weekdayLabels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            dayCells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    week.forEach { dayCell ->
                        DayCellCard(
                            dayCell = dayCell,
                            accentColor = seasonAccent,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { onSelectDate(dayCell.date) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCellCard(
    dayCell: DayCell,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shiftPalette = dayCell.shift?.color?.palette()
    val isDoubleDigitDay = dayCell.date.dayOfMonth >= 10
    Surface(
        modifier = modifier
            .testTag("day_cell_${dayCell.date}")
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .alpha(if (dayCell.inCurrentMonth) 1f else 0.35f),
        color = Color.White.copy(alpha = 0.95f),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = if (dayCell.isSelected || dayCell.isToday) 4.dp else 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = when {
                        dayCell.isSelected -> accentColor
                        dayCell.isToday -> accentColor.copy(alpha = 0.5f)
                        else -> Color.Transparent
                    },
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                if (dayCell.holiday != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp),
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(bottomStart = 8.dp, topEnd = 10.dp))
                                .background(
                                    if (dayCell.holiday.isWorkday) Color(0xFFE7EDF4) else Color(0xFFFFE2E2),
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = dayCell.holiday.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 8.sp),
                                color = if (dayCell.holiday.isWorkday) Color(0xFF526171) else Color(0xFFB42318),
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .heightIn(min = 24.dp)
                        .background(
                            color = if (dayCell.isToday || dayCell.isSelected) accentColor else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = if (isDoubleDigitDay) 4.dp else 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = dayCell.date.dayOfMonth.toString(),
                        fontSize = if (isDoubleDigitDay) 14.sp else 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        color = if (dayCell.isToday || dayCell.isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (dayCell.lunarLabel.isNotBlank()) {
                    Text(
                        text = dayCell.lunarLabel,
                        maxLines = 2,
                        overflow = TextOverflow.Visible,
                        softWrap = true,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            lineHeight = 9.sp,
                            letterSpacing = (-0.5).sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp),
                    )
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                if (dayCell.shift != null && shiftPalette != null) {
                    ShiftBadge(
                        shift = dayCell.shift,
                        palette = shiftPalette,
                    )
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                }

                if (dayCell.hasNote || dayCell.hasReminder || dayCell.hasDiary) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.height(6.dp),
                    ) {
                        if (dayCell.hasNote) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape),
                            )
                        }
                        if (dayCell.hasReminder) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(Color(0xFF1976D2), CircleShape),
                            )
                        }
                        if (dayCell.hasDiary) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(Color(0xFF4CAF50), CircleShape),
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun ShiftBadge(
    shift: ShiftDefinition,
    palette: ShiftPalette,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .wrapContentWidth(Alignment.CenterHorizontally)
            .height(18.dp)
            .widthIn(min = 32.dp),
        shape = RoundedCornerShape(999.dp),
        color = palette.container.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, palette.content.copy(alpha = 0.16f)),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 6.dp, top = 1.dp, bottom = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.content.copy(alpha = 0.9f)),
            )
            Text(
                text = shift.monthGridLabel(),
                maxLines = 1,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                ),
                fontWeight = FontWeight.Bold,
                color = palette.content,
            )
        }
    }
}

private fun ShiftDefinition.monthGridLabel(): String = when (name) {
    "白班" -> "白"
    "夜班" -> "夜"
    "休息" -> "休"
    "休假" -> "假"
    else -> name.take(2)
}


