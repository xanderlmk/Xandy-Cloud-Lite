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
    updateMediaController: (MediaController) -> Unit,
    activity: ComponentActivity, sessionToken: SessionToken
) = try {
    val future = MediaController
        .Builder(activity, sessionToken)
        .buildAsync()
    future.addListener({
        try {
            val mc = future.get()
            mc.addListener(object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    try {
                        updateTracks(tracks)
                    } catch (_: Exception) {
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    try {
                        updatePosition(mc.currentPosition)
                    } catch (_: Exception) {
                    }
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
                    try {
                        updateMediaController(mc)
                    } catch (_: Exception) {
                    }
                }
            })
            try {
                updateMediaController(mc)
                updateIsPlaying(mc.isPlaying)
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
