package com.xandy.lite.views.player.controller

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.R as AndroidR
import com.xandy.lite.R
import com.xandy.lite.controllers.view.models.PickedSongVM.PickedSongVMStates
import com.xandy.lite.ui.functions.ContentIcons

private const val COMMAND_REPEAT = "Cycle_Repeat"
private const val COMMAND_SHUFFLE = "Shuffle_Songs"

@Composable
internal fun PlayerButtons(
    states: PickedSongVMStates, controller: MediaController?, ci: ContentIcons,
    onSkipNext: () -> Unit, modifier: Modifier
) {
    val iconSize = Modifier.size(35.dp)
    val repeatOne = R.drawable.sharp_repeat_one
    val repeatAll = R.drawable.sharp_repeat
    val repeatOff = R.drawable.repeat_off
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        IconButton(
            onClick = {
                controller?.sendCustomCommand(
                    SessionCommand(COMMAND_REPEAT, Bundle()), Bundle()
                )
            }, modifier = Modifier.padding(horizontal = 4.dp)
        ) {
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
            controller?.let { handleSkipPrevious(states.repeatMode, it) }
        }, modifier = Modifier.padding(horizontal = 4.dp)) {
            ci.ContentIcon(
                R.drawable.baseline_skip_previous, contentDescription = "Prev",
                modifier = iconSize
            )
        }
        if (states.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(horizontal = 4.dp))
        } else {
            if (states.isPlaying) {
                IconButton(
                    onClick = { controller?.pause() },
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    ci.ContentIcon(
                        R.drawable.baseline_pause,
                        contentDescription = "Pause",
                        modifier = iconSize
                    )
                }
            } else {
                IconButton(
                    onClick = { controller?.play() }, modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    ci.ContentIcon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play", modifier = iconSize
                    )
                }
            }
        }

        IconButton(onClick = onSkipNext, modifier = Modifier.padding(horizontal = 4.dp)) {
            ci.ContentIcon(
                R.drawable.baseline_skip_next, contentDescription = "Next", modifier = iconSize
            )
        }
        IconButton(
            onClick = {
                controller?.sendCustomCommand(
                    SessionCommand(COMMAND_SHUFFLE, Bundle()), Bundle()
                )
            }, modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            ci.ContentIcon(
                if (states.shuffleEnabled) AndroidR.drawable.media3_icon_shuffle_on
                else AndroidR.drawable.media3_icon_shuffle_off,
                contentDescription = "Shuffle", modifier = iconSize
            )
        }
    }
}
