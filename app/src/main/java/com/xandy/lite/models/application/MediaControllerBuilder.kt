package com.xandy.lite.models.application


import android.app.PendingIntent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

@OptIn(UnstableApi::class)
fun mediaControllerBuilder(
    updatePickedSong: (MediaItem?) -> Unit, updateTracks: (Tracks) -> Unit,
    updatePosition: (Long) -> Unit, updateIsLoading: (Boolean) -> Unit,
    updateIsPlaying: (Boolean) -> Unit, updateDuration: (Long) -> Unit,
    updateMediaController: (MediaController) -> Unit, onDisconnect: () -> Unit,
    activity: ComponentActivity, sessionToken: SessionToken
) = try {
    MediaController
        .Builder(activity, sessionToken)
        .setListener(object : MediaController.Listener {
            override fun onSessionActivityChanged(
                controller: MediaController, sessionActivity: PendingIntent?
            ) {
                super.onSessionActivityChanged(controller, sessionActivity)
                try {
                    val item = controller.currentMediaItem
                    updateMediaController(controller)
                    item?.let { updatePickedSong(it) }
                } catch (e: Exception) {
                    Log.w(XANDY_CLOUD, "onSessionActivityChanged: $e")
                }
            }

            override fun onDisconnected(controller: MediaController) {
                super.onDisconnected(controller)
                try {
                    onDisconnect()
                } catch (e: Exception) {
                    Log.w(XANDY_CLOUD, "Failed to disconnect controller: $e")
                }
            }
        }).buildAsync().also { future ->
            future.addListener({
                val mc = future.get()
                mc.addListener(object : Player.Listener {
                    override fun onTracksChanged(tracks: Tracks) {
                        updateTracks(tracks)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        updatePosition(mc.currentPosition)
                    }

                    override fun onIsLoadingChanged(isLoading: Boolean) {
                        super.onIsLoadingChanged(isLoading)
                        updateIsLoading(isLoading)
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                        updatePosition(newPosition.positionMs)
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)
                        updatePosition(0L)
                        updatePickedSong(mediaItem)
                    }

                    override fun onEvents(player: Player, events: Player.Events) {
                        super.onEvents(player, events)
                        updatePosition(player.currentPosition)
                        updateDuration(player.duration.coerceAtLeast(0L))
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        super.onIsPlayingChanged(isPlaying)
                        updateIsPlaying(isPlaying)
                    }

                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        super.onPlayWhenReadyChanged(playWhenReady, reason)
                        updateMediaController(mc)
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)
                        updateMediaController(mc)
                    }
                })
                updateMediaController(mc)
                updateIsPlaying(mc.isPlaying)
            }, ContextCompat.getMainExecutor(activity))

        }
} catch (e: Exception) {
    Log.e(XANDY_CLOUD, "Failed to build controller: ${e.printStackTrace()}")
    null
}
