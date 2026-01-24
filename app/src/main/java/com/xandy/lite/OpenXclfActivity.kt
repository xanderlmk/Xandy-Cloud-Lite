package com.xandy.lite

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.compose.rememberNavController
import com.google.common.util.concurrent.ListenableFuture
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.db.tables.itemKey
import com.xandy.lite.models.AppPref
import com.xandy.lite.models.Theme
import com.xandy.lite.models.application.AppVMProvider
import com.xandy.lite.models.application.PrefRepository
import com.xandy.lite.models.application.PrefRepositoryImpl
import com.xandy.lite.models.application.PreferencesManager
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.application.mediaControllerBuilder
import com.xandy.lite.models.lyrics.adapter.LyricsXclfAdapter
import com.xandy.lite.models.media.player.PlaybackService
import com.xandy.lite.navigation.LyricsListDestination
import com.xandy.lite.navigation.NavHosts
import com.xandy.lite.navigation.NavViewModel
import com.xandy.lite.ui.GetUIStyle
import com.xandy.lite.ui.theme.XandyCloudTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.Locale
import kotlin.getValue

@OptIn(UnstableApi::class)
class OpenXclfActivity : ComponentActivity() {
    companion object {
        private const val PREFERENCES = "preferences"
        private const val CHANGED_PLAYBACK_SETTINGS = "playback_settings_changed"

    }

    private val applicationScope = CoroutineScope(SupervisorJob())
    private val navVM: NavViewModel by viewModels { AppVMProvider.Factory }
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val lyrics = MutableStateFlow<Lyrics?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        val appPref = applicationContext.getSharedPreferences(PREFERENCES, MODE_PRIVATE)
        val preferences: PrefRepository = PrefRepositoryImpl(application, appPref)
        val pm = PreferencesManager(preferences, applicationScope)
        super.onCreate(savedInstanceState)
        navVM.updateRoute(LyricsListDestination.route)
        lyrics.update { intent?.data?.let { LyricsXclfAdapter.importLyricsFromXclf(this, it) } }
        enableEdgeToEdge()
        setContent {
            val controller by navVM.mediaController.collectAsStateWithLifecycle()
            LaunchedEffect(controller) {
                if (isTaskRoot)
                    while (controller == null) {
                        delay(2_000)
                        getController()
                    }
            }
            val navHosts = NavHosts(
                onRestartPlayer = {
                    try {
                        lifecycleScope.launch(Dispatchers.Main.immediate) {
                            navVM.updateLastestPlayerInfo()
                            delay(250L)
                            val mc = controllerFuture?.get() ?: return@launch
                            appPref.edit { putBoolean(CHANGED_PLAYBACK_SETTINGS, true) }
                            val wasPlaying = mc.isPlaying
                            mc.pause()
                            safeReleaseController()
                            delay(250L)
                            connectToPlaybackService().apply {
                                delay(250L)
                                if (wasPlaying) {
                                    val newMc = controllerFuture?.get() ?: return@apply
                                    newMc.play()
                                }
                            }
                        }
                    } catch (_: Exception) {
                        Log.e(XANDY_CLOUD, "Failed to restart player")
                    }
                },
                onRecreate = {
                    lifecycleScope.launch(Dispatchers.Main.immediate) {
                        navVM.updateLanguage()
                        delay(200L); pm.updateAcknowledgement(true); delay(50L)
                        this@OpenXclfActivity.recreate()
                    }
                }
            )
            val navController = rememberNavController()

            val theme by pm.theme.collectAsStateWithLifecycle()
            val cs = MaterialTheme.colorScheme
            val isSystemDark =
                if (theme is Theme.Default) isSystemInDarkTheme() else theme is Theme.Dark

            val getUIStyle = GetUIStyle(
                cs, isSystemDark, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            )
            XandyCloudTheme(isSystemDark) {
                navHosts.XclfNavHost(
                    navController, getUIStyle, pm, navVM,
                    lyrics = lyrics.collectAsStateWithLifecycle().value
                ) {
                    navigateUpToMain()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        lyrics.update { intent.data?.let { LyricsXclfAdapter.importLyricsFromXclf(this, it) } }
        connectToPlaybackService()
    }


    override fun onStart() {
        super.onStart()
        try {
            connectToPlaybackService()
        } catch (_: Exception) {

        }
    }

    override fun onStop() {
        super.onStop()
        navVM.updateLastestPlayerInfo()
        navVM.stopCheckingPosition()
        safeReleaseController()
    }

    override fun onPause() {
        super.onPause()
        navVM.updateLastestPlayerInfo()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        navVM.updateLastestPlayerInfo()
    }

    override fun onDestroy() {
        super.onDestroy()
        navVM.stopCheckingPosition()
        safeReleaseController()
    }

    private fun navigateUpToMain() {
        lyrics.update { null }
        this.finishAffinity()
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        })
    }

    override fun attachBaseContext(newBase: Context?) {
        val new = newBase?.let { context ->
            val appPref = context.getSharedPreferences(PREFERENCES, MODE_PRIVATE)
            val locale = AppPref.getLanguage(appPref)
                ?.let { Locale.forLanguageTag(it) } ?: Locale.getDefault()
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            context.createConfigurationContext(config)
        }
        super.attachBaseContext(new ?: newBase)
    }

    private fun getController() {
        try {
            val controller = controllerFuture?.get() ?: return
            navVM.updateMediaController(controller)
            navVM.updateIsPlaying(controller.isPlaying)
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed to get controller: ${e.printStackTrace()}")
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
            controllerFuture = null; navVM.resetMediaController()
        }
    }

    private fun connectToPlaybackService() = try {
        if (controllerFuture != null)
            safeReleaseController()
        val sessionToken =
            SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = try {
            mediaControllerBuilder(
                sessionToken = sessionToken, activity = this,
                updatePickedSong = { lifecycleLaunch(navVM.updatePickedSong(it?.itemKey())) },
                updateDuration = { lifecycleLaunch(navVM.updateDuration(it)) },
                updatePosition = { lifecycleLaunch(navVM.updatePosition(it)) },
                updateIsLoading = { lifecycleLaunch(navVM.updateIsLoading(it)) },
                updateIsPlaying = { lifecycleLaunch(navVM.updateIsPlaying(it)) },
                updateMediaController = { navVM.updateMediaController(it) }
            )
        } catch (e: Exception) {
            Log.e(XANDY_CLOUD, "Failed to build controller: $e")
            null
        }
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to build controller: $e")
    }

    private fun lifecycleLaunch(func: Unit) = try {
        lifecycleScope.launch { withTimeout(3_000L) { func } }
    } catch (_: Exception) {
    }
}
