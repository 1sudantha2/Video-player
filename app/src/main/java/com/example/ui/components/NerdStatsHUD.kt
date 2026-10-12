package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PerformanceMetrics
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.AeroGreen
import com.example.ui.theme.AeroAmber
import com.example.ui.theme.DarkBackground

@Composable
fun NerdStatsHUD(
    metrics: PerformanceMetrics,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(DarkBackground.copy(alpha = 0.92f), RoundedCornerShape(12.dp))
            .border(1.dp, AeroCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .width(280.dp)
            .testTag("nerd_stats_hud")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Engine Stats",
                    tint = AeroCyan,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = "AERO ENGINE STATS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = AeroCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("close_nerd_stats_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Stats",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        StatRow("Resolution", metrics.videoResolution)
        StatRow("Framerate", "${metrics.fps.toInt()} FPS", valueColor = AeroGreen)
        StatRow("Dropped Frames", "${metrics.droppedFrames}", valueColor = if (metrics.droppedFrames == 0L) AeroGreen else AeroAmber)
        StatRow("Video Codec", metrics.videoCodec)
        StatRow("Audio Codec", metrics.audioCodec)
        StatRow("Buffer Health", "${metrics.bufferHealthMs} ms (~${metrics.bufferHealthBytes / 1024 / 1024} MB)")
        StatRow("Allocated RAM", "${metrics.estimatedRamUsageMb} MB", valueColor = AeroCyan)
        StatRow("HW Accelerated", if (metrics.isHardwareAccelerated) "Yes (Zero-Copy)" else "Software Fallback", valueColor = AeroGreen)
        StatRow("Eco Power Mode", if (metrics.isEcoModeActive) "Enabled (Low RAM)" else "Standard High-Fi", valueColor = if (metrics.isEcoModeActive) AeroGreen else Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: Color = Color.White.copy(alpha = 0.85f)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = valueColor,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}
