package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GestureFeedback
import com.example.ui.GestureType
import com.example.ui.theme.AeroAmber
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.DarkBackground

@Composable
fun GestureHUDOverlay(
    feedback: GestureFeedback,
    rewindAnim: Boolean,
    forwardAnim: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Double Tap Rewind ripple indicator (Left side)
        AnimatedVisibility(
            visible = rewindAnim,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 48.dp)
        ) {
            DoubleTapIndicator(
                icon = Icons.Default.FastRewind,
                text = "-10s"
            )
        }

        // Double Tap Forward ripple indicator (Right side)
        AnimatedVisibility(
            visible = forwardAnim,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 48.dp)
        ) {
            DoubleTapIndicator(
                icon = Icons.Default.FastForward,
                text = "+10s"
            )
        }

        // Center gesture feedback (Volume, Brightness, Scrub Seek)
        AnimatedVisibility(
            visible = feedback.type != GestureType.NONE,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            when (feedback.type) {
                GestureType.BRIGHTNESS -> {
                    HudPill(
                        icon = Icons.Default.BrightnessMedium,
                        label = "Brightness",
                        valueText = feedback.label,
                        progress = feedback.value.coerceIn(0f, 1f),
                        accentColor = AeroCyan
                    )
                }
                GestureType.VOLUME -> {
                    val isBoost = feedback.value > 100f
                    HudPill(
                        icon = Icons.Default.VolumeUp,
                        label = if (isBoost) "Audio Boost" else "Volume",
                        valueText = feedback.label,
                        progress = (feedback.value / 200f).coerceIn(0f, 1f),
                        accentColor = if (isBoost) AeroAmber else AeroCyan
                    )
                }
                GestureType.SEEK -> {
                    HudSeekPill(text = feedback.label)
                }
                GestureType.NONE -> {}
            }
        }
    }
}

@Composable
private fun DoubleTapIndicator(
    icon: ImageVector,
    text: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .background(DarkBackground.copy(alpha = 0.8f))
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AeroCyan,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun HudPill(
    icon: ImageVector,
    label: String,
    valueText: String,
    progress: Float,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(DarkBackground.copy(alpha = 0.88f), RoundedCornerShape(16.dp))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .width(160.dp)
            .testTag("gesture_hud_pill")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = valueText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = accentColor,
            trackColor = Color.White.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun HudSeekPill(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(DarkBackground.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
            .border(1.dp, AeroCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("gesture_seek_pill")
    ) {
        Icon(
            imageVector = Icons.Default.FastForward,
            contentDescription = null,
            tint = AeroCyan,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
