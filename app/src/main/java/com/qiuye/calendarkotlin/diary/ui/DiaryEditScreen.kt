package com.qiuye.calendarkotlin.diary.ui

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiuye.calendarkotlin.ui.fullDateFormatter
import java.time.LocalDate

@Composable
fun DiaryEditScreen(
    dateKey: String,
    viewModel: DiaryViewModel,
    onNavigateBack: () -> Unit
) {
    val date = remember(dateKey) { LocalDate.parse(dateKey) }
    val existingEntry by viewModel.observeByDate(dateKey).collectAsState(initial = null)

    var draftContent by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf("") }
    var isInitialized by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(existingEntry) {
        val entry = existingEntry
        if (entry != null && !isInitialized) {
            draftContent = entry.content
            selectedMood = entry.mood
            isInitialized = true
        }
    }

    val moodEmojis = listOf("😃", "😊", "🙂", "😔", "😠")
    val moodLabels = listOf("开心", "平静", "一般", "低落", "烦躁")

    // Theme Colors matching prototype
    val bgGentle = Color(0xFFF4F7F6)
    val accentGentle = Color(0xFF8EB6A8)
    val accentGentleLight = Color(0xFFEBF2F0)
    val textPrimary = Color(0xFF333333)
    val textSecondary = Color(0xFF777777)
    val dangerColor = Color(0xFFFF6B6B)

    Scaffold(
        containerColor = bgGentle,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("screen_diary_edit")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Back Button Circle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(2.dp, CircleShape)
                            .background(Color.White, CircleShape)
                            .clickable { onNavigateBack() }
                            .testTag("btn_diary_back"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = date.format(fullDateFormatter),
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Delete Button
                if (existingEntry != null) {
                    Box(
                        modifier = Modifier
                            .shadow(2.dp, RoundedCornerShape(20.dp))
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .clickable {
                                showDeleteDialog = true
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("btn_diary_delete"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "删除",
                            color = dangerColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Card Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 20.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "当前心情",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF999999),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Mood Select Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        moodEmojis.forEachIndexed { index, emoji ->
                            val isSelected = selectedMood == emoji
                            val label = moodLabels[index]

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .padding(horizontal = 4.dp)
                                    .graphicsLayer {
                                        translationY = if (isSelected) -4.dp.toPx() else 0f
                                    }
                                    .scale(if (isSelected) 1.05f else 1.0f)
                                    .then(
                                        if (isSelected) {
                                            Modifier.coloredShadow(
                                                color = Color(0xFF8EB6A8),
                                                alpha = 0.25f,
                                                borderRadius = 16.dp,
                                                shadowRadius = 12.dp,
                                                offsetY = 6.dp
                                            )
                                        } else Modifier
                                    )
                                    .background(
                                        if (isSelected) accentGentleLight else Color(0xFFF9FAFB),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        selectedMood = if (isSelected) "" else emoji
                                    }
                                    .alpha(if (isSelected) 1.0f else 0.5f)
                                    .testTag("chip_mood_$emoji")
                            ) {
                                Text(text = emoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    color = textSecondary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Visible
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Text Editor
                    TextField(
                        value = draftContent,
                        onValueChange = { draftContent = it },
                        placeholder = {
                            Text(
                                text = "在这里记录下今天的点滴...",
                                color = Color(0xFFD0D0D0)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .testTag("input_diary_content"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button Actions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveDiary(dateKey, draftContent, selectedMood)
                        onNavigateBack()
                    },
                    enabled = draftContent.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("btn_diary_save"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentGentle,
                        contentColor = Color.White,
                        disabledContainerColor = accentGentle.copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "保存日记",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(text = "删除确认") },
                text = { Text(text = "日记删除后将无法恢复，确定要删除吗？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteDiary(dateKey)
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("btn_dialog_confirm")
                    ) {
                        Text(text = "确定", color = dangerColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        modifier = Modifier.testTag("btn_dialog_cancel")
                    ) {
                        Text(text = "取消", color = textSecondary)
                    }
                }
            )
        }
    }
}

fun Modifier.coloredShadow(
    color: Color,
    alpha: Float = 0.2f,
    borderRadius: Dp = 0.dp,
    shadowRadius: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    offsetX: Dp = 0.dp
) = this.drawBehind {
    val shadowColor = color.copy(alpha = alpha).toArgb()
    val transparentColor = color.copy(alpha = 0.01f).toArgb()
    
    this.drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = transparentColor
        frameworkPaint.setShadowLayer(
            shadowRadius.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            shadowColor
        )
        canvas.drawRoundRect(
            left = 0f,
            top = 0f,
            right = this.size.width,
            bottom = this.size.height,
            radiusX = borderRadius.toPx(),
            radiusY = borderRadius.toPx(),
            paint = paint
        )
    }
}
