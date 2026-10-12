package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AeroPlayerRepository
import com.example.data.model.AspectRatioMode
import com.example.data.model.Bookmark
import com.example.data.model.MediaItemData
import com.example.data.model.PlaybackHistoryItem
import com.example.player.AeroPlayerEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScreenDestination {
    object Gallery : ScreenDestination()
    object Player : ScreenDestination()
}

enum class ActiveDialog {
    NONE,
    SPEED,
    ASPECT_RATIO,
    SLEEP_TIMER,
    STREAM_URL,
    BOOKMARKS,
    AUDIO_TRACKS,
    SUBTITLES
}

data class GestureFeedback(
    val type: GestureType = GestureType.NONE,
    val value: Float = 0f, // 0..1 for brightness, 0..200 for volume, seconds for seek
    val label: String = ""
)

enum class GestureType {
    NONE,
    BRIGHTNESS,
    VOLUME,
    SEEK
}

class AeroPlayerViewModel(application: Application) : AndroidViewModel(application) {

    val repository = AeroPlayerRepository(application)
    val engine = AeroPlayerEngine(application)

    private val _currentScreen = MutableStateFlow<ScreenDestination>(ScreenDestination.Gallery)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _currentMedia = MutableStateFlow<MediaItemData?>(null)
    val currentMedia: StateFlow<MediaItemData?> = _currentMedia.asStateFlow()

    private val _isControlsVisible = MutableStateFlow(true)
    val isControlsVisible: StateFlow<Boolean> = _isControlsVisible.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _showNerdStats = MutableStateFlow(false)
    val showNerdStats: StateFlow<Boolean> = _showNerdStats.asStateFlow()

    private val _activeDialog = MutableStateFlow(ActiveDialog.NONE)
    val activeDialog: StateFlow<ActiveDialog> = _activeDialog.asStateFlow()

    // Screen brightness (0.01f to 1.0f)
    private val _screenBrightness = MutableStateFlow(0.65f)
    val screenBrightness: StateFlow<Float> = _screenBrightness.asStateFlow()

    // Transient HUD gesture feedback
    private val _gestureFeedback = MutableStateFlow(GestureFeedback())
    val gestureFeedback: StateFlow<GestureFeedback> = _gestureFeedback.asStateFlow()

    // Sleep Timer in seconds (0 = disabled)
    private val _sleepTimerSeconds = MutableStateFlow(0)
    val sleepTimerSeconds: StateFlow<Int> = _sleepTimerSeconds.asStateFlow()

    // Double tap ripple visual cues
    private val _doubleTapRewindAnim = MutableStateFlow(false)
    val doubleTapRewindAnim: StateFlow<Boolean> = _doubleTapRewindAnim.asStateFlow()

    private val _doubleTapForwardAnim = MutableStateFlow(false)
    val doubleTapForwardAnim: StateFlow<Boolean> = _doubleTapForwardAnim.asStateFlow()

    // Background audio mode
    private val _isBackgroundAudioActive = MutableStateFlow(false)
    val isBackgroundAudioActive: StateFlow<Boolean> = _isBackgroundAudioActive.asStateFlow()

    private var hideControlsJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var gestureResetJob: Job? = null

    init {
        // Collect engine state to persist history periodically
        viewModelScope.launch {
            engine.currentPositionMs.collect { pos ->
                val media = _currentMedia.value
                val duration = engine.durationMs.value
                if (media != null && pos > 2000L && duration > 0L) {
                    repository.saveHistoryProgress(media, pos, duration)
                }
            }
        }
    }

    fun navigateTo(screen: ScreenDestination) {
        _currentScreen.value = screen
        if (screen == ScreenDestination.Player) {
            triggerControlsTimeout()
        }
    }

    fun playMediaItem(media: MediaItemData, startPositionMs: Long? = null) {
        _currentMedia.value = media
        _currentScreen.value = ScreenDestination.Player
        _isLocked.value = false

        val resumePos = startPositionMs ?: repository.getLastSavedPosition(media.uri)
        engine.playMedia(media.uri, startPositionMs = resumePos, subtitleUri = media.subtitleUri)
        triggerControlsTimeout()
    }

    fun playCustomUri(uri: Uri, title: String = "Custom Video", isStream: Boolean = false) {
        val media = MediaItemData(
            id = "custom_${System.currentTimeMillis()}",
            title = title,
            uri = uri,
            isStream = isStream,
            resolution = if (isStream) "Network Stream" else "Local Media",
            codec = "Hardware Auto-Detect"
        )
        playMediaItem(media, startPositionMs = 0L)
    }

    fun toggleControlsVisibility() {
        if (_isLocked.value) return
        val newVisibility = !_isControlsVisible.value
        _isControlsVisible.value = newVisibility
        if (newVisibility) {
            triggerControlsTimeout()
        } else {
            hideControlsJob?.cancel()
        }
    }

    fun triggerControlsTimeout() {
        hideControlsJob?.cancel()
        hideControlsJob = viewModelScope.launch {
            delay(4000)
            if (engine.isPlaying.value && !_isLocked.value) {
                _isControlsVisible.value = false
            }
        }
    }

    fun toggleLock() {
        _isLocked.value = !_isLocked.value
        _isControlsVisible.value = !_isLocked.value
    }

    fun toggleNerdStats() {
        _showNerdStats.value = !_showNerdStats.value
    }

    fun openDialog(dialog: ActiveDialog) {
        _activeDialog.value = dialog
        hideControlsJob?.cancel()
    }

    fun closeDialog() {
        _activeDialog.value = ActiveDialog.NONE
        triggerControlsTimeout()
    }

    fun setBrightness(delta: Float) {
        val updated = (_screenBrightness.value + delta).coerceIn(0.05f, 1.0f)
        _screenBrightness.value = updated
        showGestureFeedback(
            GestureType.BRIGHTNESS,
            updated,
            "${(updated * 100).toInt()}%"
        )
    }

    fun setVolumeDelta(deltaInt: Int) {
        val current = engine.volumeLevel.value
        val updated = (current + deltaInt).coerceIn(0, 200)
        engine.setVolumeLevel(updated)
        val label = if (updated > 100) "${updated}% (Boosted)" else "${updated}%"
        showGestureFeedback(
            GestureType.VOLUME,
            updated.toFloat(),
            label
        )
    }

    fun seekRelativeWithFeedback(deltaMs: Long) {
        engine.seekRelative(deltaMs)
        val targetPos = engine.currentPositionMs.value
        val sign = if (deltaMs >= 0) "+" else ""
        showGestureFeedback(
            GestureType.SEEK,
            (deltaMs / 1000).toFloat(),
            "$sign${deltaMs / 1000}s (${AeroPlayerRepository.formatTime(targetPos)})"
        )
    }

    fun onDoubleTapRewind() {
        seekRelativeWithFeedback(-10_000L)
        viewModelScope.launch {
            _doubleTapRewindAnim.value = true
            delay(500)
            _doubleTapRewindAnim.value = false
        }
    }

    fun onDoubleTapForward() {
        seekRelativeWithFeedback(10_000L)
        viewModelScope.launch {
            _doubleTapForwardAnim.value = true
            delay(500)
            _doubleTapForwardAnim.value = false
        }
    }

    private fun showGestureFeedback(type: GestureType, value: Float, label: String) {
        _gestureFeedback.value = GestureFeedback(type, value, label)
        gestureResetJob?.cancel()
        gestureResetJob = viewModelScope.launch {
            delay(1200)
            _gestureFeedback.value = GestureFeedback()
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerSeconds.value = 0
            return
        }
        _sleepTimerSeconds.value = minutes * 60
        sleepTimerJob = viewModelScope.launch {
            while (_sleepTimerSeconds.value > 0) {
                delay(1000)
                _sleepTimerSeconds.value -= 1
            }
            // Timer expired: pause video
            if (engine.isPlaying.value) {
                engine.togglePlayPause()
            }
        }
    }

    fun toggleBackgroundAudio() {
        _isBackgroundAudioActive.value = !_isBackgroundAudioActive.value
    }

    fun addBookmarkCurrentPosition(label: String) {
        val media = _currentMedia.value ?: return
        val pos = engine.currentPositionMs.value
        repository.addBookmark(media.uri.toString(), pos, label)
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}
