package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.example.data.model.Bookmark
import com.example.data.model.MediaItemData
import com.example.data.model.PlaybackHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AeroPlayerRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aero_player_prefs", Context.MODE_PRIVATE)

    private val _history = MutableStateFlow<List<PlaybackHistoryItem>>(emptyList())
    val history: StateFlow<List<PlaybackHistoryItem>> = _history.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    // Pre-curated high-performance test videos for instant zero-setup playback
    val sampleVideos: List<MediaItemData> = listOf(
        MediaItemData(
            id = "sample_bbb_1080p",
            title = "Big Buck Bunny (1080p 60fps)",
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
            durationMs = 596000L,
            resolution = "1920x1080",
            codec = "H.264 High @ 60fps",
            isStream = false,
            description = "High framerate animated benchmark test, crisp edges and vibrant color grading.",
            tag = "60 FPS Benchmark"
        ),
        MediaItemData(
            id = "sample_tears_of_steel",
            title = "Tears of Steel (Sci-Fi VFX)",
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"),
            durationMs = 734000L,
            resolution = "1920x804 (Cinemascope)",
            codec = "H.264 / 5.1 Audio",
            isStream = false,
            description = "High dynamic range visual effects clip with complex sound mixing.",
            tag = "VFX Master"
        ),
        MediaItemData(
            id = "sample_sintel",
            title = "Sintel (Fantasy CGI)",
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"),
            durationMs = 888000L,
            resolution = "1920x818",
            codec = "H.264 / AAC Stereo",
            isStream = false,
            description = "Rich textures, snowfall and high-motion action sequences.",
            tag = "HDR Colors"
        ),
        MediaItemData(
            id = "sample_elephants_dream",
            title = "Elephant\'s Dream (Open Movie)",
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
            durationMs = 653000L,
            resolution = "1920x1080",
            codec = "H.264 / AAC",
            isStream = false,
            description = "Pioneering open source cinema with intricate mechanical rendering.",
            tag = "Classic 3D"
        ),
        MediaItemData(
            id = "sample_for_bigger_blazes",
            title = "For Bigger Blazes (Chroma Test)",
            uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
            durationMs = 15000L,
            resolution = "1920x1080",
            codec = "H.264 / 60fps",
            isStream = false,
            description = "Rapid fire high contrast colors for testing display response.",
            tag = "Short Clip"
        ),
        MediaItemData(
            id = "sample_hls_stream",
            title = "Adaptive HLS Live Stream",
            uri = Uri.parse("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
            durationMs = 634000L,
            resolution = "Multi-bitrate (Adaptive)",
            codec = "H.264 / AAC (HLS)",
            isStream = true,
            description = "Multi-bitrate HTTP Live Streaming with dynamic bandwidth adaptation.",
            tag = "Live HLS"
        )
    )

    init {
        loadHistory()
        loadBookmarks()
    }

    private fun loadHistory() {
        val raw = prefs.getString("playback_history_json", null) ?: return
        try {
            val arr = JSONArray(raw)
            val list = mutableListOf<PlaybackHistoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PlaybackHistoryItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        uriString = obj.getString("uri"),
                        lastPositionMs = obj.getLong("lastPos"),
                        durationMs = obj.getLong("duration"),
                        lastPlayedTimestamp = obj.getLong("timestamp")
                    )
                )
            }
            _history.value = list
        } catch (_: Exception) {
            _history.value = emptyList()
        }
    }

    fun saveHistoryProgress(item: MediaItemData, positionMs: Long, durationMs: Long) {
        if (positionMs <= 1000L && durationMs <= 0L) return
        val current = _history.value.toMutableList()
        current.removeAll { it.uriString == item.uri.toString() }
        current.add(
            0,
            PlaybackHistoryItem(
                id = UUID.randomUUID().toString(),
                title = item.title,
                uriString = item.uri.toString(),
                lastPositionMs = positionMs,
                durationMs = durationMs,
                lastPlayedTimestamp = System.currentTimeMillis()
            )
        )
        // Keep max 25 items for low memory
        val trimmed = current.take(25)
        _history.value = trimmed

        try {
            val arr = JSONArray()
            for (it in trimmed) {
                val obj = JSONObject().apply {
                    put("id", it.id)
                    put("title", it.title)
                    put("uri", it.uriString)
                    put("lastPos", it.lastPositionMs)
                    put("duration", it.durationMs)
                    put("timestamp", it.lastPlayedTimestamp)
                }
                arr.put(obj)
            }
            prefs.edit().putString("playback_history_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs.edit().remove("playback_history_json").apply()
    }

    private fun loadBookmarks() {
        val raw = prefs.getString("bookmarks_json", null) ?: return
        try {
            val arr = JSONArray(raw)
            val list = mutableListOf<Bookmark>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Bookmark(
                        id = obj.getString("id"),
                        videoUri = obj.getString("videoUri"),
                        positionMs = obj.getLong("positionMs"),
                        label = obj.getString("label"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
            _bookmarks.value = list
        } catch (_: Exception) {
            _bookmarks.value = emptyList()
        }
    }

    fun addBookmark(videoUri: String, positionMs: Long, label: String) {
        val current = _bookmarks.value.toMutableList()
        current.add(
            Bookmark(
                id = UUID.randomUUID().toString(),
                videoUri = videoUri,
                positionMs = positionMs,
                label = label.ifBlank { "Marker at ${formatTime(positionMs)}" },
                timestamp = System.currentTimeMillis()
            )
        )
        _bookmarks.value = current
        saveBookmarksToDisk(current)
    }

    fun removeBookmark(id: String) {
        val current = _bookmarks.value.filterNot { it.id == id }
        _bookmarks.value = current
        saveBookmarksToDisk(current)
    }

    private fun saveBookmarksToDisk(list: List<Bookmark>) {
        try {
            val arr = JSONArray()
            for (b in list) {
                val obj = JSONObject().apply {
                    put("id", b.id)
                    put("videoUri", b.videoUri)
                    put("positionMs", b.positionMs)
                    put("label", b.label)
                    put("timestamp", b.timestamp)
                }
                arr.put(obj)
            }
            prefs.edit().putString("bookmarks_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getLastSavedPosition(uri: Uri): Long {
        return _history.value.firstOrNull { it.uriString == uri.toString() }?.lastPositionMs ?: 0L
    }

    companion object {
        fun formatTime(ms: Long): String {
            val totalSeconds = (ms / 1000).coerceAtLeast(0)
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }
}
