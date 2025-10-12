package com.xandy.lite

import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import androidx.navigation.compose.rememberNavController
import com.google.common.util.concurrent.ListenableFuture
import com.xandy.lite.models.PlaybackService
import com.xandy.lite.models.Theme
import com.xandy.lite.models.application.AppVMProvider
import com.xandy.lite.models.application.PrefRepository
import com.xandy.lite.models.application.PrefRepositoryImpl
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.application.mediaControllerBuilder
import com.xandy.lite.models.itemKey
import com.xandy.lite.navigation.MainNavHost
import com.xandy.lite.navigation.NavViewModel
import com.xandy.lite.ui.theme.GetUIStyle
import com.xandy.lite.ui.theme.XandyCloudTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.getValue

@UnstableApi
class MainActivity : ComponentActivity() {
    private val navVM: NavViewModel by viewModels { AppVMProvider.Factory }
    private lateinit var playerView: PlayerView
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        val preferences: PrefRepository = PrefRepositoryImpl(application, applicationScope)
        super.onCreate(savedInstanceState)
        playerView = PlayerView(this)
        enableEdgeToEdge()
        setContent {
            val theme by preferences.theme.collectAsStateWithLifecycle()
            val mainNavController = rememberNavController()
            val cs = MaterialTheme.colorScheme
            val isSystemDark =
                if (theme is Theme.Default) isSystemInDarkTheme() else theme is Theme.Dark

            val getUIStyle = GetUIStyle(
                cs, isSystemDark, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S, preferences

            )
            XandyCloudTheme(isSystemDark) {
                MainNavHost(
                    mainNavController, getUIStyle, navVM = navVM,
                    getController = { getController() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            val sessionToken =
                SessionToken(this, ComponentName(this, PlaybackService::class.java))
            controllerFuture = mediaControllerBuilder(
                sessionToken = sessionToken, activity = this,
                onDisconnect = { lifecycleLaunch(navVM.resetMediaController()) },
                updatePickedSong = { lifecycleLaunch(navVM.updatePickedSong(it?.itemKey())) },
                updateDuration = { lifecycleLaunch(navVM.updateDuration(it)) },
                updatePosition = { lifecycleLaunch(navVM.updatePosition(it)) },
                updateTracks = { lifecycleLaunch(navVM.updateTracks(it)) },
                updateIsLoading = { lifecycleLaunch(navVM.updateIsLoading(it)) },
                updateIsPlaying = { lifecycleLaunch(navVM.updateIsPlaying(it)) },
                updateMediaController = { lifecycleLaunch(navVM.updateMediaController(it)) }
            )
        } catch (e: Exception) {
            Log.e(XANDY_CLOUD, "Failed to build controller: $e")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        safeReleaseController()
    }

    private fun getController() {
        lifecycleScope.launch {
            try {
                val controller = controllerFuture?.get() ?: return@launch
                navVM.updateMediaController(controller)
                navVM.updateIsPlaying(controller.isPlaying)
            } catch (e: Exception) {
                Log.w(XANDY_CLOUD, "Failed to get controller: ${e.printStackTrace()}")
            }
        }
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
            controllerFuture = null
            navVM.resetMediaController()
        }
    }

    private fun lifecycleLaunch(func: Unit) = lifecycleScope.launch { withTimeout(4_000L) { func } }
}
