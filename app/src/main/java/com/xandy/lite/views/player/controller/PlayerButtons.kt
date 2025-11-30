package com.xandy.lite.views.player.controller

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.R as AndroidR
import com.xandy.lite.R
import com.xandy.lite.models.ui.PickedSongVMStates
import com.xandy.lite.ui.functions.ContentIcons

private const val COMMAND_REPEAT = "Cycle_Repeat"
private const val COMMAND_SHUFFLE = "Shuffle_Songs"

@Composable
fun PlayerButtons(
    states: PickedSongVMStates, controller: MediaController, ci: ContentIcons, modifier: Modifier
) {
    val iconSize = Modifier.size(35.dp)
    val repeatOne = painterResource(R.drawable.sharp_repeat_one)
    val repeatAll = painterResource(R.drawable.sharp_repeat)
    val repeatOff = painterResource(R.drawable.repeat_off)
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        IconButton(onClick = {
            controller.sendCustomCommand(
                SessionCommand(COMMAND_REPEAT, Bundle()), Bundle()
            )
        }) {
            ci.ContentIcon(
                when (states.repeatMode) {
                    Player.REPEAT_MODE_ONE -> repeatOne
                    Player.REPEAT_MODE_ALL -> repeatAll
                    else -> repeatOff
                },
                contentDescription = "Repeat Mode", modifier = iconSize
            )
        }
        IconButton(onClick = {
           handleSkipPrevious(states.repeatMode, controller)
        }) {
            ci.ContentIcon(
                painterResource(R.drawable.baseline_skip_previous),
                contentDescription = "Prev", modifier = iconSize
            )
        }
        if (states.isLoading) {
            CircularProgressIndicator()
        } else {
            if (states.isPlaying) {
                IconButton(onClick = { controller.pause() }) {
                    ci.ContentIcon(
                        painterResource(R.drawable.baseline_pause),
                        contentDescription = "Pause", modifier = iconSize
                    )
                }
            } else {
                IconButton(onClick = { controller.play() }) {
                    ci.ContentIcon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play", modifier = iconSize
                    )
                }
            }
        }

        IconButton(onClick = {
           handleSkipNext(states.shuffleMode, states.repeatMode, controller)
        }
        ) {
            ci.ContentIcon(
                painterResource(R.drawable.baseline_skip_next),
                contentDescription = "Next", modifier = iconSize
            )
        }
        IconButton(onClick = {
            controller.sendCustomCommand(
                SessionCommand(COMMAND_SHUFFLE, Bundle()), Bundle()
            )
        }) {
            ci.ContentIcon(
                if (states.shuffleMode) painterResource(AndroidR.drawable.media3_icon_shuffle_on)
                else painterResource(AndroidR.drawable.media3_icon_shuffle_off),
                contentDescription = "Shuffle", modifier = iconSize
            )
        }
    }
}

private fun handleSkipPrevious(repeatMode: Int, mc: MediaController) {
    val pos = mc.currentPosition
    if (repeatMode == Player.REPEAT_MODE_OFF || repeatMode == Player.REPEAT_MODE_ONE) {
        if (pos > 5_000) {
            mc.seekTo(0)
        } else {
            if (!mc.hasPreviousMediaItem())
                mc.seekToDefaultPosition(mc.mediaItemCount - 1)
            else mc.seekToPrevious()
        }
    } else {
        // REPEAT_MODE_ALL
        if (pos > 5_000) mc.seekTo(0) else mc.seekToPrevious()
    }
}

private fun handleSkipNext(
    shuffleEnabled: Boolean, repeatMode: Int, mc: MediaController
) {
    if (repeatMode == Player.REPEAT_MODE_OFF || repeatMode == Player.REPEAT_MODE_ONE) {
        if (!mc.hasNextMediaItem()) {
            if (shuffleEnabled) {
                val songCount = mc.mediaItemCount.takeIf { it > 0 } ?: return
                val index = (0 until songCount).random().takeIf {
                    it != mc.currentMediaItemIndex
                } ?: 0
                mc.seekToDefaultPosition(index)
            } else mc.seekToDefaultPosition(0)
        } else {
            mc.seekToNext()
        }
    } else {
        // REPEAT_MODE_ALL
        mc.seekToNext()
    }
}
