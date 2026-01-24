package com.xandy.lite.models.application


import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

@OptIn(UnstableApi::class)
fun mediaControllerBuilder(
    updatePickedSong: (MediaItem?) -> Unit,
    updatePosition: (Long) -> Unit, updateIsLoading: (Boolean) -> Unit,
    updateIsPlaying: (Boolean) -> Unit, updateDuration: (Long) -> Unit,
    updateMediaController: (MediaController) -> Unit,
    activity: ComponentActivity, sessionToken: SessionToken, onFinish: () -> Unit = {}
) = try {
    val future = MediaController
        .Builder(activity, sessionToken)
        .buildAsync()
    future.addListener({
        try {
            val mc = future.get()
            mc.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    super.onEvents(player, events)
                    try {
                        updatePosition(player.currentPosition)
                        updateDuration(player.duration.coerceAtLeast(0L))
                    } catch (_: Exception) {
                    }
                    updateIsPlaying(player.isPlaying)
                    updateIsLoading(player.isLoading)
                    updatePickedSong(player.currentMediaItem)
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    super.onPlayWhenReadyChanged(playWhenReady, reason)
                    try {
                        updateMediaController(mc)
                    } catch (_: Exception) {
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    updateIsPlaying(isPlaying)
                }
            })
            try {
                updateMediaController(mc)
                updatePosition(mc.currentPosition)
                updateDuration(mc.duration.coerceAtLeast(0L))
                updateIsPlaying(mc.isPlaying)
                onFinish()
            } catch (_: Exception) {
            }
        } catch (_: Exception) {

        }
    }, ContextCompat.getMainExecutor(activity))
    future
} catch (e: Exception) {
    Log.e(XANDY_CLOUD, "Failed to build controller: ${e.printStackTrace()}")
    null
}

@OptIn(UnstableApi::class)
fun mediaControllerBuilder(
    onGetController: (MediaController) -> Unit,
    context: Context, sessionToken: SessionToken,
) = try {
    val future = MediaController
        .Builder(context, sessionToken)
        .buildAsync()
    future.addListener({
        try {
            val mc = future.get()
            try {
                onGetController(mc)
            } catch (_: Exception) {
            }
        } catch (_: Exception) {

        }
    }, ContextCompat.getMainExecutor(context))
} catch (e: Exception) {
    Log.e(XANDY_CLOUD, "Failed to build controller: ${e.printStackTrace()}")
    Unit
}
