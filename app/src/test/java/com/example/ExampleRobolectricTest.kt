package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.AeroPlayerRepository
import com.example.data.model.MediaItemData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AeroPlayer", appName)
    }

    @Test
    fun `formatTime formats milliseconds correctly`() {
        assertEquals("00:05", AeroPlayerRepository.formatTime(5000L))
        assertEquals("01:30", AeroPlayerRepository.formatTime(90000L))
        assertEquals("01:05:20", AeroPlayerRepository.formatTime(3920000L))
    }

    @Test
    fun `repository manages history and bookmarks`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = AeroPlayerRepository(context)

        val testMedia = MediaItemData(
            id = "test_1",
            title = "Test Video",
            uri = Uri.parse("https://example.com/video.mp4"),
            durationMs = 60000L
        )

        repo.saveHistoryProgress(testMedia, 15000L, 60000L)
        val lastPos = repo.getLastSavedPosition(testMedia.uri)
        assertEquals(15000L, lastPos)

        repo.addBookmark(testMedia.uri.toString(), 15000L, "Epic scene")
        val bookmarks = repo.bookmarks.value
        assertTrue(bookmarks.any { it.label == "Epic scene" })
    }
}
