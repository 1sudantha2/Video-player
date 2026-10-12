package com.example.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

@Composable
fun GestureDetectorBox(
    isEnabled: Boolean,
    onSingleTap: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onDoubleTapCenter: () -> Unit,
    onBrightnessDelta: (Float) -> Unit,
    onVolumeDelta: (Int) -> Unit,
    onSeekDelta: (Long) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var dragMode by remember { mutableStateOf<DragMode?>(null) }
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isEnabled) {
                if (!isEnabled) return@pointerInput
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { offset ->
                        val screenWidth = size.width
                        when {
                            offset.x < screenWidth * 0.35f -> onDoubleTapLeft()
                            offset.x > screenWidth * 0.65f -> onDoubleTapRight()
                            else -> onDoubleTapCenter()
                        }
                    }
                )
            }
            .pointerInput(isEnabled) {
                if (!isEnabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        dragMode = null
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y

                        val screenWidth = size.width
                        val screenHeight = size.height

                        // Determine gesture mode if not determined yet
                        if (dragMode == null) {
                            if (abs(totalDragX) > abs(totalDragY) && abs(totalDragX) > 20f) {
                                dragMode = DragMode.HORIZONTAL_SEEK
                            } else if (abs(totalDragY) > abs(totalDragX) && abs(totalDragY) > 20f) {
                                dragMode = if (change.position.x < screenWidth / 2f) {
                                    DragMode.VERTICAL_BRIGHTNESS
                                } else {
                                    DragMode.VERTICAL_VOLUME
                                }
                            }
                        }

                        // Apply continuous changes
                        when (dragMode) {
                            DragMode.HORIZONTAL_SEEK -> {
                                // 1 pixel = ~200ms
                                val deltaMs = (dragAmount.x * 200).toLong()
                                onSeekDelta(deltaMs)
                            }
                            DragMode.VERTICAL_BRIGHTNESS -> {
                                // Upward is negative y, downward is positive y
                                val deltaBrightness = -dragAmount.y / (screenHeight * 0.6f)
                                onBrightnessDelta(deltaBrightness)
                            }
                            DragMode.VERTICAL_VOLUME -> {
                                val deltaVolume = (-dragAmount.y / (screenHeight * 0.5f) * 100).toInt()
                                onVolumeDelta(deltaVolume)
                            }
                            null -> {}
                        }
                    },
                    onDragEnd = {
                        dragMode = null
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDragCancel = {
                        dragMode = null
                        totalDragX = 0f
                        totalDragY = 0f
                    }
                )
            }
    ) {
        content()
    }
}

private enum class DragMode {
    HORIZONTAL_SEEK,
    VERTICAL_BRIGHTNESS,
    VERTICAL_VOLUME
}
