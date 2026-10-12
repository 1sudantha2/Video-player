package com.example.data.model

import android.net.Uri

data class MediaItemData(
    val id: String,
    val title: String,
    val uri: Uri,
    val durationMs: Long = 0L,
    val resolution: String = "1080p",
    val codec: String = "H.264 / AAC",
    val isStream: Boolean = false,
    val description: String = "",
    val subtitleUri: Uri? = null,
    val thumbnailResId: Int? = null,
    val tag: String = "Local"
)

data class PerformanceMetrics(
    val fps: Float = 60.0f,
    val droppedFrames: Long = 0L,
    val videoResolution: String = "1920x1080",
    val videoCodec: String = "c2.android.avc.decoder (Hardware)",
    val audioCodec: String = "c2.android.aac.decoder (Hardware)",
    val estimatedRamUsageMb: Float = 28.4f,
    val bufferHealthMs: Long = 15000L,
    val bufferHealthBytes: Long = 8_500_000L,
    val isHardwareAccelerated: Boolean = true,
    val isEcoModeActive: Boolean = false
)

enum class AspectRatioMode(val label: String) {
    FIT("Fit to Screen"),
    FILL("Crop / Fill"),
    SIXTEEN_NINE("16:9"),
    FOUR_THREE("4:3"),
    STRETCH("Stretch")
}

data class AbRepeatState(
    val startMs: Long? = null,
    val endMs: Long? = null,
    val isEnabled: Boolean = false
)

data class Bookmark(
    val id: String,
    val videoUri: String,
    val positionMs: Long,
    val label: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class PlaybackHistoryItem(
    val id: String,
    val title: String,
    val uriString: String,
    val lastPositionMs: Long,
    val durationMs: Long,
    val lastPlayedTimestamp: Long = System.currentTimeMillis()
)
