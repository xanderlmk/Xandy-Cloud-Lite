package com.xandy.lite

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.ListenableFuture
import com.xandy.lite.controllers.view.models.PickedSongVM
import com.xandy.lite.ui.theme.GetUIStyle
import com.xandy.lite.models.PlaybackService
import com.xandy.lite.models.application.AppVMProvider
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.application.mediaControllerBuilder
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.theme.XandyCloudTheme
import com.xandy.lite.views.picked.song.SongView
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class SongViewActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val songVM: PickedSongVM by viewModels { AppVMProvider.Factory }
    private lateinit var playerView: PlayerView


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerView = PlayerView(this)
        enableEdgeToEdge()
        setContent {
            val cs = MaterialTheme.colorScheme
            val getUIStyle = GetUIStyle(
                cs, isSystemInDarkTheme(), Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            )
            val ci = ContentIcons(getUIStyle)
            var showQueue by rememberSaveable { mutableStateOf(false) }
            BackHandler {
                if (showQueue) {
                    showQueue = false
                } else {
                    navigateUpToMain()
                }
            }
            XandyCloudTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarColors(
                                containerColor = getUIStyle.topBarColor(),
                                navigationIconContentColor = getUIStyle.themedColor(),
                                titleContentColor = getUIStyle.themedColor(),
                                actionIconContentColor = getUIStyle.themedColor(),
                                scrolledContainerColor = getUIStyle.themedColor()
                            ),
                            navigationIcon = {
                                IconButton(onClick = { navigateUpToMain() }) {
                                    ci.ContentIcon(Icons.Default.KeyboardArrowDown)
                                }
                            },
                            title = {},
                        )
                    }
                ) { innerPadding ->
                    Box(Modifier.padding(innerPadding)) {
                        SongView(
                            songVM, getUIStyle,
                            onToggle = { showQueue = !showQueue }, showQueue
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        connectToPlaybackService()
    }

    override fun onStart() {
        super.onStart()
        connectToPlaybackService()

    }

    private fun connectToPlaybackService() {
        try {
            val sessionToken =
                SessionToken(this, ComponentName(this, PlaybackService::class.java))
            controllerFuture = mediaControllerBuilder(
                sessionToken = sessionToken, activity = this,
                onDisconnect = { },
                updatePickedSong = { lifecycleLaunch(songVM.updatePickedSong(it)) },
                updateDuration = { lifecycleLaunch(songVM.updateDuration(it)) },
                updatePosition = { lifecycleLaunch(songVM.updatePosition(it)) },
                updateTracks = { lifecycleLaunch(songVM.updateTracks(it)) },
                updateIsLoading = { lifecycleLaunch(songVM.updateIsLoading(it)) },
                updateIsPlaying = { lifecycleLaunch(songVM.updateIsPlaying(it)) },
                updateMediaController = { lifecycleLaunch(songVM.updateMediaController(it)) }
            )
        } catch (e: Exception) {
            Log.e(XANDY_CLOUD, "Failed to build controller: $e")
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            controllerFuture?.get()?.release()
            controllerFuture = null
        } catch (e: Exception) {
            Log.e(XANDY_CLOUD, "Failed to release and reset controller: $e")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isTaskRoot) safeReleaseController()
    }

    private fun safeReleaseController() {
        val future = controllerFuture ?: return
        try {
            if (future.isDone) {
                try {
                    future.get().release()
                } catch (e: Exception) {
                    Log.w(XANDY_CLOUD, "Failed to get/release controller", e)
                }
            } else {
                future.addListener({
                    try {
                        future.get().release()
                    } catch (e: Exception) {
                        Log.w(XANDY_CLOUD, "Failed to release controller after connect", e)
                    }
                }, ContextCompat.getMainExecutor(this))
            }
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed to release controller", e)
        } finally {
            controllerFuture = null; songVM.resetMediaController()
        }
    }

    private fun navigateUpToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        this.finish()
    }

    private fun lifecycleLaunch(func: Unit) = lifecycleScope.launch { withTimeout(3_000L) { func } }

}