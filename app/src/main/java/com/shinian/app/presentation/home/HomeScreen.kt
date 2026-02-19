package com.shinian.app.presentation.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinian.app.presentation.theme.Terracotta
import com.shinian.app.presentation.theme.WarmBeige

@Composable
fun HomeScreen(
    onStartRecording: (Boolean) -> Unit,
    onStopRecording: () -> Unit,
    recordingState: RecordingState
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val buttonScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 1.15f
            recordingState is RecordingState.Meeting || 
            recordingState is RecordingState.Flash -> 0.9f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "buttonScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBeige),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 状态提示文字
            when (recordingState) {
                is RecordingState.Meeting -> {
                    RecordingStatusText("正在记录会议... ${formatTime(recordingState.recordingTime)}")
                }
                is RecordingState.Flash -> {
                    RecordingStatusText("松手结束录音... ${formatTime(recordingState.recordingTime)}")
                }
                is RecordingState.Processing -> {
                    ProcessingIndicator()
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // 核心录音按钮
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(buttonScale)
                    .background(
                        color = when {
                            recordingState is RecordingState.Meeting -> Color(0xFF8B4513) // 更深的陶土色
                            isPressed -> Color(0xFFB87333)
                            else -> Terracotta
                        },
                        shape = when (recordingState) {
                            is RecordingState.Meeting -> RoundedCornerShape(16.dp)
                            else -> CircleShape
                        }
                    )
                    .pointerInput(recordingState) {
                        detectTapGestures(
                            onPress = {
                                if (recordingState is RecordingState.Idle) {
                                    isPressed = true
                                    tryAwaitRelease()
                                    isPressed = false
                                    
                                    // 判断是点击还是长按
                                    val pressTime = System.currentTimeMillis()
                                    // 这里简化处理：在 onTap 和 onLongPress 中分别处理
                                }
                            },
                            onTap = {
                                // 短按 - 会议模式
                                if (recordingState is RecordingState.Idle) {
                                    onStartRecording(false)
                                } else if (recordingState is RecordingState.Meeting) {
                                    onStopRecording()
                                }
                            },
                            onLongPress = {
                                // 长按 - 闪念模式
                                if (recordingState is RecordingState.Idle) {
                                    onStartRecording(true)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // 按钮图标
                when (recordingState) {
                    is RecordingState.Meeting -> {
                        // 方形停止图标
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                        )
                    }
                    is RecordingState.Flash -> {
                        // 圆形录制中
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                    else -> {
                        // 默认麦克风图标（简化为圆形）
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 底部提示文字
            Text(
                text = when (recordingState) {
                    is RecordingState.Idle -> "短按会议 · 长按闪念"
                    is RecordingState.Meeting -> "再次点击结束会议"
                    is RecordingState.Flash -> "松手结束录音"
                    is RecordingState.Processing -> "AI 整理中..."
                    else -> ""
                },
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun RecordingStatusText(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF8B4513)
    )
}

@Composable
private fun ProcessingIndicator() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            color = Color(0xFFC4A77D),
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "AI 处理中...",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}