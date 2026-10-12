package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AeroPlayerRepository
import com.example.data.model.AbRepeatState
import com.example.data.model.AspectRatioMode
import com.example.data.model.MediaItemData
import com.example.ui.ActiveDialog
import com.example.ui.theme.AeroAmber
import com.example.ui.theme.AeroCyan
import com.example.ui.theme.AeroGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassScrim

@Composable
fun PlayerControlsOverlay(
    isVisible: Boolean,
    isLocked: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPosMs: Long,
    durationMs: Long,
    bufferedPosMs: Long,
    playbackSpeed: Float,
    volumeLevel: Int,
    aspectRatio: AspectRatioMode,
    abRepeat: AbRepeatState,
    isEcoMode: Boolean,
    sleepTimerSeconds: Int,
    isBackgroundAudio: Boolean,
    currentMedia: MediaItemData?,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onToggleLock: () -> Unit,
    onToggleNerdStats: () -> Unit,
    onToggleEcoMode: () -> Unit,
    onOpenDialog: (ActiveDialog) -> Unit,
    onAbRepeatStep: () -> Unit,
    onClearAbRepeat: () -> Unit,
    onToggleBackgroundAudio: () -> Unit,
    onEnterPip: () -> Unit,
    onUserInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMoreMenuOpen by remember { mutableStateOf(false) }
    var isSeekingByUser by remember { mutableStateOf(false) }
    var userSeekFraction by remember { mutableFloatStateOf(0f) }

    // If screen is locked, only display a floating unlock button
    if (isLocked) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Surface(
                onClick = { onToggleLock() },
                shape = CircleShape,
                color = DarkBackground.copy(alpha = 0.75f),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(54.dp)
                    .testTag("unlock_screen_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Screen Locked - Tap to Unlock",
                        tint = AeroAmber,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        return
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkBackground.copy(alpha = 0.85f),
                            Color.Transparent,
                            Color.Transparent,
                            DarkBackground.copy(alpha = 0.90f)
                        )
                    )
                )
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            onBack()
                        },
                        modifier = Modifier.testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column {
                        Text(
                            text = currentMedia?.title ?: "Video Playback",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentMedia?.resolution ?: "1080p",
                                color = AeroCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (isEcoMode) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AeroGreen.copy(alpha = 0.25f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "ECO POWER",
                                        color = AeroGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Top Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Eco Mode toggle
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            onToggleEcoMode()
                        },
                        modifier = Modifier.testTag("toggle_eco_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Eco Mode",
                            tint = if (isEcoMode) AeroGreen else Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // Nerd Stats toggle
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            onToggleNerdStats()
                        },
                        modifier = Modifier.testTag("toggle_nerd_stats_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Performance Stats",
                            tint = AeroCyan
                        )
                    }

                    // Aspect Ratio
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            onOpenDialog(ActiveDialog.ASPECT_RATIO)
                        },
                        modifier = Modifier.testTag("aspect_ratio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Aspect Ratio",
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    // PiP Mode
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            onEnterPip()
                        },
                        modifier = Modifier.testTag("pip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Picture in Picture",
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    // Lock Screen
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            onToggleLock()
                        },
                        modifier = Modifier.testTag("lock_screen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Lock Controls",
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    // More Menu
                    Box {
                        IconButton(
                            onClick = {
                                onUserInteraction()
                                isMoreMenuOpen = true
                            },
                            modifier = Modifier.testTag("more_options_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = isMoreMenuOpen,
                            onDismissRequest = { isMoreMenuOpen = false },
                            modifier = Modifier.background(DarkBackground)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Playback Speed (${playbackSpeed}x)", color = Color.White) },
                                onClick = {
                                    isMoreMenuOpen = false
                                    onOpenDialog(ActiveDialog.SPEED)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (sleepTimerSeconds > 0) "Sleep Timer (${sleepTimerSeconds / 60}m)" else "Sleep Timer",
                                        color = if (sleepTimerSeconds > 0) AeroCyan else Color.White
                                    )
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    onOpenDialog(ActiveDialog.SLEEP_TIMER)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Bookmarks & Notes", color = Color.White) },
                                onClick = {
                                    isMoreMenuOpen = false
                                    onOpenDialog(ActiveDialog.BOOKMARKS)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isBackgroundAudio) "Background Audio: ON" else "Background Audio: OFF", color = if (isBackgroundAudio) AeroGreen else Color.White) },
                                onClick = {
                                    isMoreMenuOpen = false
                                    onToggleBackgroundAudio()
                                }
                            )
                        }
                    }
                }
            }

            // CENTER PLAYBACK CONTROLS
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rewind 10s
                IconButton(
                    onClick = {
                        onUserInteraction()
                        onSeekRelative(-10_000L)
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("seek_rewind_10s")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 10 seconds",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Play / Pause / Buffering Primary Button
                Surface(
                    onClick = {
                        onUserInteraction()
                        onTogglePlayPause()
                    },
                    shape = CircleShape,
                    color = AeroCyan,
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("center_play_pause_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                color = DarkBackground,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(34.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = DarkBackground,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Forward 10s
                IconButton(
                    onClick = {
                        onUserInteraction()
                        onSeekRelative(10_000L)
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("seek_forward_10s")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Forward 10 seconds",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            // BOTTOM CONTROLS & TIMELINE
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Time & Buffering info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayCurrentMs = if (isSeekingByUser) {
                        (userSeekFraction * durationMs).toLong()
                    } else {
                        currentPosMs
                    }

                    Text(
                        text = AeroPlayerRepository.formatTime(displayCurrentMs),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Middle status badge (A-B repeat or Sleep timer)
                    if (abRepeat.isEnabled) {
                        Text(
                            text = "A-B Loop [${AeroPlayerRepository.formatTime(abRepeat.startMs ?: 0)} - ${AeroPlayerRepository.formatTime(abRepeat.endMs ?: 0)}]",
                            color = AeroAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (sleepTimerSeconds > 0) {
                        Text(
                            text = "Sleep in ${sleepTimerSeconds / 60}m ${sleepTimerSeconds % 60}s",
                            color = AeroCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = AeroPlayerRepository.formatTime(durationMs),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Seekbar Slider with custom colors
                val currentFraction = if (durationMs > 0) {
                    (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Slider(
                    value = if (isSeekingByUser) userSeekFraction else currentFraction,
                    onValueChange = { frac ->
                        onUserInteraction()
                        isSeekingByUser = true
                        userSeekFraction = frac
                    },
                    onValueChangeFinished = {
                        val targetMs = (userSeekFraction * durationMs).toLong()
                        onSeekTo(targetMs)
                        isSeekingByUser = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = AeroCyan,
                        activeTrackColor = AeroCyan,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_seek_slider")
                )

                // Quick Function Bar below Seekbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed selector pill
                    Surface(
                        onClick = {
                            onUserInteraction()
                            onOpenDialog(ActiveDialog.SPEED)
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.testTag("speed_pill_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = AeroCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${playbackSpeed}x",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // A-B Repeat Controller
                    Surface(
                        onClick = {
                            onUserInteraction()
                            onAbRepeatStep()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (abRepeat.isEnabled) AeroAmber.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.testTag("ab_repeat_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = if (abRepeat.isEnabled) AeroAmber else Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val label = when {
                                abRepeat.isEnabled -> "Loop ON"
                                abRepeat.startMs != null -> "Set B"
                                else -> "A-B Loop"
                            }
                            Text(
                                text = label,
                                color = if (abRepeat.isEnabled) AeroAmber else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Audio Boost level indicator
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (volumeLevel > 100) AeroAmber.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.testTag("audio_volume_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = if (volumeLevel > 100) AeroAmber else AeroCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${volumeLevel}%",
                                color = if (volumeLevel > 100) AeroAmber else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Bookmark timestamp button
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            onOpenDialog(ActiveDialog.BOOKMARKS)
                        },
                        modifier = Modifier.testTag("quick_bookmark_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Bookmarks",
                            tint = Color.White
                        )
                    }

                    // Background Audio Mode
                    IconButton(
                        onClick = {
                            onUserInteraction()
                            onToggleBackgroundAudio()
                        },
                        modifier = Modifier.testTag("background_audio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "Background Audio",
                            tint = if (isBackgroundAudio) AeroGreen else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
