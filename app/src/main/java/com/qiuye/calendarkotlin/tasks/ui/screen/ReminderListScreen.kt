package com.qiuye.calendarkotlin.tasks.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiuye.calendarkotlin.R
import com.qiuye.calendarkotlin.tasks.data.ReminderEntity
import com.qiuye.calendarkotlin.tasks.data.formatDate
import com.qiuye.calendarkotlin.tasks.data.formatTime
import com.qiuye.calendarkotlin.tasks.ui.theme.AutumnGradient
import com.qiuye.calendarkotlin.ui.darkSeasonBackground
import com.qiuye.calendarkotlin.tasks.ui.theme.LargeCardShape
import com.qiuye.calendarkotlin.tasks.ui.theme.PrimaryAccent
import kotlinx.coroutines.launch

@Composable
fun ReminderListScreen(
    reminders: List<ReminderEntity>,
    hasNotificationPermission: Boolean,
    hasExactAlarmPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onAddReminder: () -> Unit,
    onOpenReminder: (Long) -> Unit,
    onToggleReminderCompleted: suspend (Long, Boolean) -> Unit,
    onDeleteReminder: suspend (Long) -> Unit,
    onNavigateBack: () -> Unit = {},
    isDark: Boolean = false
) {
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(if (isDark) darkSeasonBackground else AutumnGradient))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.task_reminders_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF333333),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onNavigateBack) {
                    Text(stringResource(R.string.back_to_calendar), color = PrimaryAccent, fontWeight = FontWeight.Bold)
                }
            }
            Surface(
                color = PrimaryAccent.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.lightweight_local_reminders),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!hasNotificationPermission || !hasExactAlarmPermission) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!hasNotificationPermission) {
                        PermissionBanner(onRequestNotificationPermission, isDark)
                    }
                    if (!hasExactAlarmPermission) {
                        ExactAlarmBanner(onRequestExactAlarmPermission, isDark)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (reminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(onAddReminder = onAddReminder, isDark = isDark)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(reminders, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            isDark = isDark,
                            onClick = { onOpenReminder(reminder.id) },
                            onToggleCompleted = {
                                scope.launch { onToggleReminderCompleted(reminder.id, !reminder.isCompleted) }
                            },
                            onDelete = {
                                scope.launch { onDeleteReminder(reminder.id) }
                            }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddReminder,
            containerColor = PrimaryAccent,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 26.dp, end = 22.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_reminder), modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun PermissionBanner(onRequestNotificationPermission: () -> Unit, isDark: Boolean = false) {
    Surface(
        color = if (isDark) Color(0xFF1E1E1E) else Color.White.copy(alpha = 0.9f),
        shape = LargeCardShape,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, (if (isDark) Color(0xFF333333) else Color.White).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.NotificationsOff, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.notification_not_granted), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = if (isDark) Color.White else Color.Black)
                Text(
                    text = stringResource(R.string.notification_permission_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onRequestNotificationPermission
            ) {
                Text(stringResource(R.string.go_enable), fontWeight = FontWeight.Bold, color = PrimaryAccent)
            }
        }
    }
}

@Composable
private fun ExactAlarmBanner(onRequestExactAlarmPermission: () -> Unit, isDark: Boolean = false) {
    Surface(
        color = if (isDark) Color(0xFF1E1E1E) else Color.White.copy(alpha = 0.9f),
        shape = LargeCardShape,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, (if (isDark) Color(0xFF333333) else Color.White).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.EventBusy, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.exact_reminder_not_enabled), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = if (isDark) Color.White else Color.Black)
                Text(
                    text = stringResource(R.string.exact_alarm_permission_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onRequestExactAlarmPermission
            ) {
                Text(stringResource(R.string.go_to_settings_text), fontWeight = FontWeight.Bold, color = PrimaryAccent)
            }
        }
    }
}

@Composable
private fun EmptyStateCard(onAddReminder: () -> Unit, isDark: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .graphicsLayer { translationY = offsetY }
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val calColor = Color(0xFF81B98F)
                val calendarWidth = size.width * 0.65f
                val calendarHeight = size.height * 0.5f
                val calendarLeft = size.width * 0.15f
                val calendarTop = size.height * 0.25f

                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.04f), Color.Transparent),
                        center = Offset(size.width / 2f, calendarTop + calendarHeight + 35f),
                        radius = calendarWidth * 0.7f
                    ),
                    topLeft = Offset(calendarLeft, calendarTop + calendarHeight + 20f),
                    size = Size(calendarWidth, 30f)
                )

                drawRoundRect(
                    color = if (isDark) Color.Gray else Color.White,
                    topLeft = Offset(calendarLeft, calendarTop),
                    size = Size(calendarWidth, calendarHeight),
                    cornerRadius = CornerRadius(24f, 24f),
                    style = Stroke(width = 6f)
                )
                
                drawRoundRect(
                    color = calColor,
                    topLeft = Offset(calendarLeft, calendarTop),
                    size = Size(calendarWidth, calendarHeight * 0.25f),
                    cornerRadius = CornerRadius(24f, 24f)
                )
                
                val ring1X = calendarLeft + calendarWidth * 0.25f
                val ring2X = calendarLeft + calendarWidth * 0.75f
                drawRoundRect(
                    color = if (isDark) Color.Gray else Color.White,
                    topLeft = Offset(ring1X - 8f, calendarTop - 15f),
                    size = Size(16f, 30f),
                    cornerRadius = CornerRadius(10f, 10f),
                    style = Stroke(width = 6f)
                )
                drawRoundRect(
                    color = if (isDark) Color.Gray else Color.White,
                    topLeft = Offset(ring2X - 8f, calendarTop - 15f),
                    size = Size(16f, 30f),
                    cornerRadius = CornerRadius(10f, 10f),
                    style = Stroke(width = 6f)
                )

                val lineLeft = calendarLeft + 30f
                val lineStartTop = calendarTop + calendarHeight * 0.4f
                repeat(2) { i ->
                    val y = lineStartTop + i * 40f
                    drawRoundRect(
                        color = calColor.copy(alpha = 0.3f),
                        topLeft = Offset(lineLeft, y),
                        size = Size(24f, 24f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = if (isDark) Color(0xFF333333) else Color(0xFFE8F0E8),
                        topLeft = Offset(lineLeft + 35f, y + 8f),
                        size = Size(calendarWidth * 0.5f, 10f),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                    if (i == 0) {
                        drawLine(
                            color = calColor,
                            start = Offset(lineLeft + 5f, y + 12f),
                            end = Offset(lineLeft + 10f, y + 20f),
                            strokeWidth = 4f
                        )
                        drawLine(
                            color = calColor,
                            start = Offset(lineLeft + 10f, y + 20f),
                            end = Offset(lineLeft + 20f, y + 5f),
                            strokeWidth = 4f
                        )
                    }
                }

                val characterRadius = 55f
                val charCenter = Offset(calendarLeft + calendarWidth * 0.85f, calendarTop + calendarHeight * 0.85f)
                val charBodyColor = if (isDark) Color(0xFF333333) else Color(0xFFF2EFE8)
                
                drawCircle(color = charBodyColor, radius = characterRadius, center = charCenter)
                drawCircle(color = calColor, radius = characterRadius, center = charCenter, style = Stroke(width = 5f))
                
                drawCircle(color = calColor, radius = 4f, center = Offset(charCenter.x - 18f, charCenter.y - 5f))
                drawCircle(color = calColor, radius = 4f, center = Offset(charCenter.x + 18f, charCenter.y - 5f))
                drawArc(color = calColor, startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = Offset(charCenter.x - 10f, charCenter.y + 5f), size = Size(20f, 12f), style = Stroke(width = 4f))
                drawCircle(color = Color(0xFFC8E6C9).copy(alpha = 0.5f), radius = 8f, center = Offset(charCenter.x - 30f, charCenter.y + 5f))
                drawCircle(color = Color(0xFFC8E6C9).copy(alpha = 0.5f), radius = 8f, center = Offset(charCenter.x + 30f, charCenter.y + 5f))

                drawRoundRect(color = calColor, topLeft = Offset(charCenter.x - characterRadius - 5f, charCenter.y + 5f), size = Size(15f, 8f), cornerRadius = CornerRadius(5f, 5f))
                drawRoundRect(color = calColor, topLeft = Offset(charCenter.x - 25f, charCenter.y + characterRadius - 5f), size = Size(8f, 15f), cornerRadius = CornerRadius(5f, 5f))
                drawRoundRect(color = calColor, topLeft = Offset(charCenter.x + 15f, charCenter.y + characterRadius - 5f), size = Size(8f, 15f), cornerRadius = CornerRadius(5f, 5f))

                val pencilLeft = charCenter.x - 55f
                val pencilTop = charCenter.y - 15f
                rotate(degrees = -15f, pivot = Offset(pencilLeft, pencilTop)) {
                    drawRoundRect(color = calColor, topLeft = Offset(pencilLeft, pencilTop), size = Size(12f, 50f), cornerRadius = CornerRadius(4f, 4f))
                    drawPath(path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(pencilLeft, pencilTop + 50f)
                        lineTo(pencilLeft + 12f, pencilTop + 50f)
                        lineTo(pencilLeft + 6f, pencilTop + 65f)
                        close()
                    }, color = Color(0xFFF2DEB4))
                }
                
                drawCircle(color = Color(0xFFD1E1CE), radius = 5f, center = Offset(size.width * 0.85f, size.height * 0.35f))
                drawCircle(color = Color(0xFFD1E1CE), radius = 3f, center = Offset(size.width * 0.15f, size.height * 0.75f))
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = stringResource(R.string.no_reminders_yet),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDark) Color.White else Color(0xFF333333)
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = stringResource(R.string.no_reminders_desc),
            fontSize = 16.sp,
            color = if (isDark) Color.Gray else Color.Gray.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderEntity,
    isDark: Boolean = false,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit
) {
    val isExpired = !reminder.isCompleted && reminder.scheduledAtMillis <= System.currentTimeMillis()
    val targetVal = if (isDark) {
        if (reminder.isCompleted) Color(0xFF1E1E1E).copy(alpha = 0.6f) else Color(0xFF1E1E1E)
    } else {
        if (reminder.isCompleted) Color.White.copy(alpha = 0.6f) else Color.White
    }
    val backgroundColor = animateColorAsState(
        targetValue = targetVal,
        label = "reminderCardColor"
    ).value

    Surface(
        color = backgroundColor,
        shape = LargeCardShape,
        shadowElevation = if (reminder.isCompleted) 0.dp else 10.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, (if (isDark) Color(0xFF333333) else Color.White).copy(alpha = 0.8f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (reminder.isCompleted) PrimaryAccent else PrimaryAccent.copy(alpha = 0.1f))
                        .clickable { onToggleCompleted() },
                    contentAlignment = Alignment.Center
                ) {
                    if (reminder.isCompleted) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.completed), tint = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Canvas(modifier = Modifier.size(28.dp)) {
                            drawCircle(
                                color = PrimaryAccent.copy(alpha = 0.5f),
                                radius = size.minDimension / 2.2f,
                                style = Stroke(width = 4f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(18.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        fontSize = 18.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        fontWeight = if (reminder.isCompleted) FontWeight.Medium else FontWeight.Bold,
                        textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (reminder.isCompleted) Color.Gray else (if (isDark) Color.White else Color(0xFF333333))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = if (isExpired && !reminder.isCompleted) Color(0xFFFFEBEE) else PrimaryAccent.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${formatDate(reminder.scheduledAtMillis)}  ${formatTime(reminder.scheduledAtMillis)}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isExpired && !reminder.isCompleted) Color(0xFFD32F2F) else PrimaryAccent
                        )
                    }
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = Color.LightGray.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun ReminderListScreenPreview() {
    com.qiuye.calendarkotlin.ui.theme.CalendarKotlinTheme {
        ReminderListScreen(
            reminders = emptyList(),
            hasNotificationPermission = false,
            hasExactAlarmPermission = false,
            onRequestNotificationPermission = {},
            onRequestExactAlarmPermission = {},
            onAddReminder = {},
            onOpenReminder = {},
            onToggleReminderCompleted = { _, _ -> },
            onDeleteReminder = {}
        )
    }
}
