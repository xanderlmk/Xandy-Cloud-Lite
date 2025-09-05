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
                cd = "Repeat Mode", modifier = iconSize
            )
        }
        IconButton(onClick = {
            val position = controller.currentPosition
            val queueSize = controller.mediaItemCount
            val currentIndex = controller.currentMediaItemIndex
            if (states.repeatMode == Player.REPEAT_MODE_OFF ||
                states.repeatMode == Player.REPEAT_MODE_ONE
            ) {
                if (position > 5_000) {
                    controller.seekTo(0)
                } else {
                    if (currentIndex == 0) {
                        controller.seekToDefaultPosition(queueSize - 1)
                    } else {
                        controller.seekToPrevious()
                    }
                }
            } else {
                if (position > 5_000) {
                    controller.seekTo(0)
                } else {
                    controller.seekToPrevious()
                }
            }
        }) {
            ci.ContentIcon(
                painterResource(R.drawable.baseline_skip_previous),
                cd = "Prev", modifier = iconSize
            )
        }
        if (states.isLoading) {
            CircularProgressIndicator()
        } else {
            if (states.isPlaying) {
                IconButton(onClick = { controller.pause() }) {
                    ci.ContentIcon(
                        painterResource(R.drawable.baseline_pause),
                        cd = "Pause", modifier = iconSize
                    )
                }
            } else {
                IconButton(onClick = { controller.play() }) {
                    ci.ContentIcon(
                        Icons.Default.PlayArrow,
                        cd = "Play", modifier = iconSize
                    )
                }
            }
        }

        IconButton(onClick = {
            if (states.repeatMode == Player.REPEAT_MODE_OFF ||
                states.repeatMode == Player.REPEAT_MODE_ONE
            ) {
                val queueSize = controller.mediaItemCount
                val currentIndex = controller.currentMediaItemIndex
                if (currentIndex == (queueSize - 1) || queueSize == 0) {
                    controller.seekToDefaultPosition(0)
                } else {
                    controller.seekToNext()
                }
            } else {
                controller.seekToNext()
            }
        }
        ) {
            ci.ContentIcon(
                painterResource(R.drawable.baseline_skip_next),
                cd = "Next", modifier = iconSize
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
                cd = "Shuffle", modifier = iconSize
            )
        }
    }
}