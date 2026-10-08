package com.example.alldemo.features.lockscreentestapp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Monitor
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alldemo.ui.theme.AllDemoTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun LockscreenOverlayContent(
    screenEvent: String,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
    eventTimestamp: Long = System.currentTimeMillis(),
) {
    var currentTime by remember { mutableStateOf(Date()) }

    // Digital Clock ticker
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance().time
            delay(1000)
        }
    }

    val timeFormat = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }
    val timeBigFormat = remember { SimpleDateFormat("hh:mm", Locale.getDefault()) }
    val amPmFormat = remember { SimpleDateFormat("a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }

    val formattedBigTime = remember(currentTime) { timeBigFormat.format(currentTime) }
    val formattedAmPm = remember(currentTime) { amPmFormat.format(currentTime) }
    val formattedDate = remember(currentTime) { dateFormat.format(currentTime) }
    val formattedEventTime = remember(eventTimestamp) { timeFormat.format(Date(eventTimestamp)) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A).copy(alpha = 0.95f), // Dark navy slate
                        Color(0xFF1E1B4B).copy(alpha = 0.96f), // Deep indigo
                        Color(0xFF020617).copy(alpha = 0.98f)  // Near black
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 48.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Screen Event Status Indicator & Lock Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 24.dp)
            ) {
                // Lock Icon Header
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .shadow(8.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Lock Status",
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Screen Event Status Card
                ScreenEventStatusBadge(
                    screenEvent = screenEvent,
                    formattedEventTime = formattedEventTime
                )
            }

            // Center Section: Digital Clock & Date
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formattedBigTime,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 72.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-2).sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedAmPm,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF93C5FD),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            // Bottom Section: Interactive Swipe Gesture & Unlock Button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Interactive Swipe To Unlock Slider
                SwipeToUnlockBar(onUnlock = onUnlock)

                // Direct Unlock Button
                Button(
                    onClick = onUnlock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LockOpen,
                            contentDescription = "Unlock",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "UNLOCK OVERLAY",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenEventStatusBadge(
    screenEvent: String,
    formattedEventTime: String,
    modifier: Modifier = Modifier
) {
    val statusColor = when (screenEvent) {
        "SCREEN_ON", "ACTION_SCREEN_ON" -> Color(0xFF4ADE80)
        "SCREEN_OFF", "ACTION_SCREEN_OFF" -> Color(0xFFF87171)
        "USER_PRESENT", "ACTION_USER_PRESENT" -> Color(0xFF60A5FA)
        else -> Color(0xFFFBBF24)
    }

    val displayEventText = when (screenEvent) {
        "SCREEN_ON", "ACTION_SCREEN_ON" -> "Intent.ACTION_SCREEN_ON"
        "SCREEN_OFF", "ACTION_SCREEN_OFF" -> "Intent.ACTION_SCREEN_OFF"
        "USER_PRESENT", "ACTION_USER_PRESENT" -> "Intent.ACTION_USER_PRESENT"
        else -> screenEvent
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.10f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Monitor,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = displayEventText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = "Received at $formattedEventTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SwipeToUnlockBar(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val barWidthDp = 300.dp
    val thumbSizeDp = 52.dp

    val maxOffsetPx = with(density) { (barWidthDp - thumbSizeDp).toPx() }
    var offsetXPx by remember { mutableFloatStateOf(0f) }

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetXPx,
        animationSpec = spring(),
        label = "SwipeOffsetAnimation"
    )

    val draggableState = rememberDraggableState { delta ->
        val newOffset = (offsetXPx + delta).coerceIn(0f, maxOffsetPx)
        offsetXPx = newOffset
        if (newOffset >= maxOffsetPx * 0.85f) {
            onUnlock()
        }
    }

    Surface(
        modifier = modifier
            .width(barWidthDp)
            .height(60.dp),
        shape = RoundedCornerShape(30.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Hint Text
            Text(
                text = "Swipe right to unlock >>>",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.Center)
            )

            // Draggable Thumb Button
            Box(
                modifier = Modifier
                    .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                    .size(thumbSizeDp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .shadow(6.dp, CircleShape)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = draggableState,
                        onDragStopped = {
                            if (offsetXPx < maxOffsetPx * 0.85f) {
                                offsetXPx = 0f
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Swipe handle",
                    tint = Color(0xFF1E1B4B),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF020617)
@Composable
fun LockscreenOverlayContentPreview() {
    AllDemoTheme {
        LockscreenOverlayContent(
            screenEvent = "ACTION_SCREEN_ON",
            onUnlock = {}
        )
    }
}
