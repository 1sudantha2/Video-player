package com.example

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AeroPlayerViewModel
import com.example.ui.ScreenDestination
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AeroPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

                    when (currentScreen) {
                        is ScreenDestination.Gallery -> {
                            GalleryScreen(
                                viewModel = viewModel,
                                onPlayMedia = { media, startPos ->
                                    viewModel.playMediaItem(media, startPos)
                                }
                            )
                        }
                        is ScreenDestination.Player -> {
                            PlayerScreen(
                                viewModel = viewModel,
                                onBack = {
                                    viewModel.navigateTo(ScreenDestination.Gallery)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            val title = uri.lastPathSegment?.substringAfterLast('/') ?: "External Video"
            viewModel.playCustomUri(uri, title, isStream = false)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            // In PiP mode, ensure HUD controls are hidden for clean playback
            if (viewModel.isControlsVisible.value) {
                viewModel.toggleControlsVisibility()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // If background audio is NOT active, pause player to conserve battery
        if (!viewModel.isBackgroundAudioActive.value && !isInPictureInPictureMode) {
            if (viewModel.engine.isPlaying.value) {
                viewModel.engine.togglePlayPause()
            }
        }
    }
}
