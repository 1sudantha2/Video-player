package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AspectRatioMode
import com.example.ui.ActiveDialog
import com.example.ui.AeroPlayerViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.AspectRatioDialog
import com.example.ui.components.BookmarksDialog
import com.example.ui.components.GestureDetectorBox
import com.example.ui.components.GestureHUDOverlay
import com.example.ui.components.NerdStatsHUD
import com.example.ui.components.PlayerControlsOverlay
import com.example.ui.components.PlayerSurface
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.SpeedDialog
import com.example.ui.components.StreamUrlDialog

@Composable
fun PlayerScreen(
    viewModel: AeroPlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val currentMedia by viewModel.currentMedia.collectAsStateWithLifecycle()
    val isControlsVisible by viewModel.isControlsVisible.collectAsStateWithLifecycle()
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val showNerdStats by viewModel.showNerdStats.collectAsStateWithLifecycle()
    val activeDialog by viewModel.activeDialog.collectAsStateWithLifecycle()
    val screenBrightness by viewModel.screenBrightness.collectAsStateWithLifecycle()
    val gestureFeedback by viewModel.gestureFeedback.collectAsStateWithLifecycle()
    val rewindAnim by viewModel.doubleTapRewindAnim.collectAsStateWithLifecycle()
    val forwardAnim by viewModel.doubleTapForwardAnim.collectAsStateWithLifecycle()
    val sleepTimerSec by viewModel.sleepTimerSeconds.collectAsStateWithLifecycle()
    val isBgAudio by viewModel.isBackgroundAudioActive.collectAsStateWithLifecycle()

    val engine = viewModel.engine
    val isPlaying by engine.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by engine.isBuffering.collectAsStateWithLifecycle()
    val currentPosMs by engine.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by engine.durationMs.collectAsStateWithLifecycle()
    val bufferedPosMs by engine.bufferedPositionMs.collectAsStateWithLifecycle()
    val playbackSpeed by engine.playbackSpeed.collectAsStateWithLifecycle()
    val volumeLevel by engine.volumeLevel.collectAsStateWithLifecycle()
    val aspectRatio by engine.aspectRatioMode.collectAsStateWithLifecycle()
    val abRepeat by engine.abRepeat.collectAsStateWithLifecycle()
    val isEcoMode by engine.isEcoMode.collectAsStateWithLifecycle()
    val metrics by engine.performanceMetrics.collectAsStateWithLifecycle()
    val bookmarks by viewModel.repository.bookmarks.collectAsStateWithLifecycle()

    // Keep screen on during playback
    DisposableEffect(isPlaying) {
        val window = activity?.window
        if (isPlaying) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Apply brightness changes to window
    LaunchedEffect(screenBrightness) {
        activity?.window?.let { window ->
            val lp = window.attributes
            lp.screenBrightness = screenBrightness
            window.attributes = lp
        }
    }

    // Restore system brightness on leave
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.let { window ->
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
        }
    }

    BackHandler {
        if (isLocked) {
            viewModel.toggleLock()
        } else {
            onBack()
        }
    }

    fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            try {
                val rational = when (aspectRatio) {
                    AspectRatioMode.SIXTEEN_NINE -> Rational(16, 9)
                    AspectRatioMode.FOUR_THREE -> Rational(4, 3)
                    else -> Rational(16, 9)
                }
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(rational)
                    .build()
                activity.enterPictureInPictureMode(params)
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen_root")
    ) {
        // Video Surface Rendering
        PlayerSurface(
            player = engine.getPlayer(),
            aspectRatioMode = aspectRatio
        )

        // Gesture detector overlay
        GestureDetectorBox(
            isEnabled = !isLocked,
            onSingleTap = { viewModel.toggleControlsVisibility() },
            onDoubleTapLeft = { viewModel.onDoubleTapRewind() },
            onDoubleTapRight = { viewModel.onDoubleTapForward() },
            onDoubleTapCenter = { engine.togglePlayPause() },
            onBrightnessDelta = { delta -> viewModel.setBrightness(delta) },
            onVolumeDelta = { delta -> viewModel.setVolumeDelta(delta) },
            onSeekDelta = { delta -> viewModel.seekRelativeWithFeedback(delta) }
        ) {
            // Transient Gesture Feedback HUD (Brightness, Volume, Scrub, Double Tap)
            GestureHUDOverlay(
                feedback = gestureFeedback,
                rewindAnim = rewindAnim,
                forwardAnim = forwardAnim
            )

            // Comprehensive On-Screen HUD Overlay
            PlayerControlsOverlay(
                isVisible = isControlsVisible,
                isLocked = isLocked,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                currentPosMs = currentPosMs,
                durationMs = durationMs,
                bufferedPosMs = bufferedPosMs,
                playbackSpeed = playbackSpeed,
                volumeLevel = volumeLevel,
                aspectRatio = aspectRatio,
                abRepeat = abRepeat,
                isEcoMode = isEcoMode,
                sleepTimerSeconds = sleepTimerSec,
                isBackgroundAudio = isBgAudio,
                currentMedia = currentMedia,
                onBack = { onBack() },
                onTogglePlayPause = { engine.togglePlayPause() },
                onSeekTo = { pos -> engine.seekTo(pos) },
                onSeekRelative = { delta -> engine.seekRelative(delta) },
                onToggleLock = { viewModel.toggleLock() },
                onToggleNerdStats = { viewModel.toggleNerdStats() },
                onToggleEcoMode = { engine.toggleEcoMode() },
                onOpenDialog = { dialog -> viewModel.openDialog(dialog) },
                onAbRepeatStep = {
                    when {
                        abRepeat.startMs == null -> engine.setAbRepeatPointA()
                        abRepeat.endMs == null -> engine.setAbRepeatPointB()
                        else -> engine.clearAbRepeat()
                    }
                },
                onClearAbRepeat = { engine.clearAbRepeat() },
                onToggleBackgroundAudio = { viewModel.toggleBackgroundAudio() },
                onEnterPip = { enterPipMode() },
                onUserInteraction = { viewModel.triggerControlsTimeout() }
            )

            // Floating Nerd Stats overlay
            if (showNerdStats) {
                NerdStatsHUD(
                    metrics = metrics,
                    onClose = { viewModel.toggleNerdStats() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 70.dp)
                )
            }
        }

        // Dialogs
        when (activeDialog) {
            ActiveDialog.SPEED -> {
                SpeedDialog(
                    currentSpeed = playbackSpeed,
                    onSpeedSelected = { spd -> engine.setSpeed(spd) },
                    onDismiss = { viewModel.closeDialog() }
                )
            }
            ActiveDialog.ASPECT_RATIO -> {
                AspectRatioDialog(
                    currentMode = aspectRatio,
                    onModeSelected = { mode -> engine.setAspectRatio(mode) },
                    onDismiss = { viewModel.closeDialog() }
                )
            }
            ActiveDialog.SLEEP_TIMER -> {
                SleepTimerDialog(
                    remainingSeconds = sleepTimerSec,
                    onSetTimer = { mins -> viewModel.setSleepTimer(mins) },
                    onDismiss = { viewModel.closeDialog() }
                )
            }
            ActiveDialog.BOOKMARKS -> {
                BookmarksDialog(
                    bookmarks = bookmarks.filter { it.videoUri == currentMedia?.uri.toString() },
                    currentPosMs = currentPosMs,
                    onAddBookmark = { note -> viewModel.addBookmarkCurrentPosition(note) },
                    onJumpTo = { pos -> engine.seekTo(pos) },
                    onDelete = { id -> viewModel.repository.removeBookmark(id) },
                    onDismiss = { viewModel.closeDialog() }
                )
            }
            ActiveDialog.STREAM_URL -> {
                StreamUrlDialog(
                    onPlayUrl = { uri, title -> viewModel.playCustomUri(uri, title, isStream = true) },
                    onDismiss = { viewModel.closeDialog() }
                )
            }
            else -> {}
        }
    }
}
