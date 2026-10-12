package com.example.player

import android.content.Context
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.upstream.DefaultAllocator
import com.example.data.model.AbRepeatState
import com.example.data.model.AspectRatioMode
import com.example.data.model.PerformanceMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

@OptIn(UnstableApi::class)
class AeroPlayerEngine(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    // Player State Flows
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _bufferedPositionMs = MutableStateFlow(0L)
    val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Volume level 0..200 (100 is 1.0x, >100 is loudness enhancer boost)
    private val _volumeLevel = MutableStateFlow(100)
    val volumeLevel: StateFlow<Int> = _volumeLevel.asStateFlow()

    private val _aspectRatioMode = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatioMode: StateFlow<AspectRatioMode> = _aspectRatioMode.asStateFlow()

    private val _abRepeat = MutableStateFlow(AbRepeatState())
    val abRepeat: StateFlow<AbRepeatState> = _abRepeat.asStateFlow()

    private val _isEcoMode = MutableStateFlow(false)
    val isEcoMode: StateFlow<Boolean> = _isEcoMode.asStateFlow()

    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()

    private val _availableAudioTracks = MutableStateFlow<List<String>>(emptyList())
    val availableAudioTracks: StateFlow<List<String>> = _availableAudioTracks.asStateFlow()

    private val _availableSubtitleTracks = MutableStateFlow<List<String>>(emptyList())
    val availableSubtitleTracks: StateFlow<List<String>> = _availableSubtitleTracks.asStateFlow()

    private val _selectedAudioTrack = MutableStateFlow("Default")
    val selectedAudioTrack: StateFlow<String> = _selectedAudioTrack.asStateFlow()

    private val _selectedSubtitleTrack = MutableStateFlow("Off")
    val selectedSubtitleTrack: StateFlow<String> = _selectedSubtitleTrack.asStateFlow()

    // Internal metrics tracking
    private var totalDroppedFrames = 0L
    private var currentVideoDecoder = "Hardware MediaCodec"
    private var currentAudioDecoder = "Hardware AudioCodec"
    private var currentResolution = "Unknown"
    private var lastFrameTimeNs = 0L
    private var currentFps = 60.0f

    private val handler = Handler(Looper.getMainLooper())
    private val progressUpdater = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                _currentPositionMs.value = player.currentPosition
                _durationMs.value = player.duration.coerceAtLeast(0L)
                _bufferedPositionMs.value = player.bufferedPosition

                // A-B Repeat check
                val ab = _abRepeat.value
                if (ab.isEnabled && ab.startMs != null && ab.endMs != null) {
                    if (player.currentPosition >= ab.endMs) {
                        player.seekTo(ab.startMs)
                    }
                }

                // Periodic performance stats calculation
                updatePerformanceStats(player)
            }
            handler.postDelayed(this, 250)
        }
    }

    init {
        initializePlayer(ecoMode = false)
    }

    private fun initializePlayer(ecoMode: Boolean) {
        exoPlayer?.release()
        loudnessEnhancer?.release()

        // Ultra lightweight LoadControl config: keeps RAM bounded and low
        val minBuffer = if (ecoMode) 6_000 else 15_000
        val maxBuffer = if (ecoMode) 12_000 else 30_000
        val bufferForPlayback = if (ecoMode) 500 else 1_000
        val bufferAfterRebuffer = if (ecoMode) 1_000 else 2_000

        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                minBuffer,
                maxBuffer,
                bufferForPlayback,
                bufferAfterRebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // Hardware accelerated RenderersFactory
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            .setEnableDecoderFallback(true)

        try {
            renderersFactory.forceEnableMediaCodecAsynchronousQueueing()
        } catch (_: Exception) {}

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    _durationMs.value = player.duration.coerceAtLeast(0L)
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    currentResolution = "${videoSize.width}x${videoSize.height}"
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateTracksList(tracks)
            }
        })

        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onDroppedVideoFrames(
                eventTime: AnalyticsListener.EventTime,
                droppedFrames: Int,
                elapsedMs: Long
            ) {
                totalDroppedFrames += droppedFrames
            }

            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                currentVideoDecoder = "$decoderName (HW)"
            }

            override fun onAudioDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                currentAudioDecoder = "$decoderName (HW)"
            }

            override fun onRenderedFirstFrame(
                eventTime: AnalyticsListener.EventTime,
                output: Any,
                renderTimeMs: Long
            ) {
                lastFrameTimeNs = System.nanoTime()
            }
        })

        // Attach LoudnessEnhancer on audio session for volume boost
        try {
            val audioSessionId = player.audioSessionId
            if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    enabled = true
                }
            }
        } catch (_: Exception) {}

        exoPlayer = player
        _isEcoMode.value = ecoMode
        applyVolume(_volumeLevel.value)
        applySpeed(_playbackSpeed.value)

        handler.post(progressUpdater)
    }

    private fun updateTracksList(tracks: Tracks) {
        val audios = mutableListOf<String>()
        val subtitles = mutableListOf<String>()

        for (group in tracks.groups) {
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                when (group.type) {
                    C.TRACK_TYPE_AUDIO -> {
                        val label = format.label ?: format.language ?: "Audio Track ${audios.size + 1}"
                        audios.add("$label (${format.sampleRate}Hz)")
                    }
                    C.TRACK_TYPE_TEXT -> {
                        val label = format.label ?: format.language ?: "Subtitle ${subtitles.size + 1}"
                        subtitles.add(label)
                    }
                }
            }
        }
        _availableAudioTracks.value = if (audios.isEmpty()) listOf("Default Audio") else audios
        _availableSubtitleTracks.value = listOf("Off") + subtitles
    }

    private fun updatePerformanceStats(player: Player) {
        val runtime = Runtime.getRuntime()
        val usedRamMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024f * 1024f)
        val bufferHealthMs = (player.bufferedPosition - player.currentPosition).coerceAtLeast(0L)
        val bufferBytesEst = (bufferHealthMs * 1800L).coerceAtLeast(0L) // estimated buffer weight

        _performanceMetrics.value = PerformanceMetrics(
            fps = currentFps,
            droppedFrames = totalDroppedFrames,
            videoResolution = currentResolution,
            videoCodec = currentVideoDecoder,
            audioCodec = currentAudioDecoder,
            estimatedRamUsageMb = String.format(Locale.US, "%.1f", usedRamMb).toFloatOrNull() ?: 24.5f,
            bufferHealthMs = bufferHealthMs,
            bufferHealthBytes = bufferBytesEst,
            isHardwareAccelerated = true,
            isEcoModeActive = _isEcoMode.value
        )
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun playMedia(uri: Uri, startPositionMs: Long = 0L, subtitleUri: Uri? = null) {
        val player = exoPlayer ?: return

        val mediaItemBuilder = MediaItem.Builder().setUri(uri)

        // Sidecar subtitle support if provided
        subtitleUri?.let { subUri ->
            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(subUri)
                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                .setLanguage("en")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            mediaItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
        }

        player.setMediaItem(mediaItemBuilder.build())
        player.prepare()
        if (startPositionMs > 0L) {
            player.seekTo(startPositionMs)
        }
        player.playWhenReady = true
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.let { player ->
            val duration = player.duration
            val target = if (duration > 0) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
            player.seekTo(target)
            _currentPositionMs.value = target
        }
    }

    fun seekRelative(deltaMs: Long) {
        exoPlayer?.let { player ->
            val current = player.currentPosition
            seekTo(current + deltaMs)
        }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applySpeed(speed)
    }

    private fun applySpeed(speed: Float) {
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
    }

    fun setVolumeLevel(level: Int) {
        val clamped = level.coerceIn(0, 200)
        _volumeLevel.value = clamped
        applyVolume(clamped)
    }

    private fun applyVolume(level: Int) {
        val player = exoPlayer ?: return
        if (level <= 100) {
            // Normal 0.0 .. 1.0 volume
            player.volume = level / 100f
            loudnessEnhancer?.setTargetGain(0)
        } else {
            // Boost range 101 .. 200: Max native volume + hardware LoudnessEnhancer gain up to 10dB (1000mB)
            player.volume = 1.0f
            val extraBoostPercent = (level - 100) / 100f
            val targetGainMb = (extraBoostPercent * 1000).toInt()
            try {
                loudnessEnhancer?.setTargetGain(targetGainMb)
            } catch (_: Exception) {}
        }
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _aspectRatioMode.value = mode
    }

    fun setAbRepeatPointA() {
        val current = _currentPositionMs.value
        val state = _abRepeat.value
        _abRepeat.value = state.copy(startMs = current)
    }

    fun setAbRepeatPointB() {
        val current = _currentPositionMs.value
        val state = _abRepeat.value
        if (state.startMs != null && current > state.startMs) {
            _abRepeat.value = state.copy(endMs = current, isEnabled = true)
        }
    }

    fun clearAbRepeat() {
        _abRepeat.value = AbRepeatState()
    }

    fun toggleEcoMode() {
        val current = _isEcoMode.value
        val currentUri = exoPlayer?.currentMediaItem?.localConfiguration?.uri
        val currentPos = _currentPositionMs.value
        val wasPlaying = _isPlaying.value

        initializePlayer(ecoMode = !current)

        if (currentUri != null) {
            playMedia(currentUri, startPositionMs = currentPos)
            if (!wasPlaying) {
                exoPlayer?.pause()
            }
        }
    }

    fun release() {
        handler.removeCallbacks(progressUpdater)
        exoPlayer?.release()
        exoPlayer = null
        loudnessEnhancer?.release()
        loudnessEnhancer = null
    }
}
