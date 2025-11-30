package com.xandy.lite.views.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import com.xandy.lite.R
import com.xandy.lite.controllers.view.models.LyricsEditorVM
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.toMediaItem
import com.xandy.lite.models.itemKey
import com.xandy.lite.models.ui.Transitions
import com.xandy.lite.ui.GetUIStyle
import com.xandy.lite.ui.functions.ContentIcons
import kotlinx.coroutines.delay


@Composable
fun PlayerController(
    controller: MediaController, vm: LyricsEditorVM, audio: AudioFile,
    toggled: Boolean, onToggle: (Boolean) -> Unit, getUIStyle: GetUIStyle, modifier: Modifier
) {
    val isPlaying by vm.isPlaying.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val sd by vm.songDetails.collectAsStateWithLifecycle()
    val queue by vm.queue.collectAsStateWithLifecycle()
    val ci = ContentIcons(getUIStyle)
    val onClick: () -> Unit = {
        val af = audio
        val index = queue.indexOfFirst {
            it.mediaItem.itemKey() == af.id
        }.takeIf { it >= 0 } ?: queue.size

        if (index == queue.size)
            controller.addMediaItem(af.toMediaItem())

        controller.seekToDefaultPosition(index)
        if (!isPlaying)
            controller.play()
    }


    if (sd != null) {
        val transitionState = remember {
            MutableTransitionState(false).apply { targetState = true }
        }
        AnimatedVisibility(
            visibleState = transitionState,
            enter = Transitions.composableEnter,
            exit = Transitions.composableExit,
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
                .background(getUIStyle.floatingPlayerBackground(), RoundedCornerShape(24.dp))
                .padding(0.5.dp)
                .zIndex(4f)
        ) {
            if (toggled) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PlaybackProgress(
                        controller, vm, Modifier
                            .fillMaxHeight(.3f)
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .offset(y = (-5).dp, x= 0.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(.7f)
                            .fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .fillMaxWidth(.55f)
                        ) {
                            sd?.let { details ->
                                IconButton(onClick = onClick) {
                                    ci.ContentIcon(
                                        Icons.Default.Refresh, contentDescription = "Refresh"
                                    )
                                }
                                Column {
                                    val title =
                                        if (details.id != audio.id) "Click reset button to enqueue picked song." else audio.title
                                    val artist =
                                        if (details.id != audio.id) details.title else audio.artist
                                    Text(
                                        title, maxLines = 1, fontSize = 16.sp,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier
                                            .basicMarquee()
                                            .padding(start = 4.dp),
                                    )
                                    Text(
                                        artist, maxLines = 1, fontSize = 14.sp,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 4.dp),
                                    )
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .fillMaxWidth(.45f)
                        ) {
                            IconButton(onClick = { controller.seekBack() }) {
                                ci.ContentIcon(
                                    painterResource(R.drawable.fast_rewind),
                                    contentDescription = "Rewind"
                                )
                            }
                            if (isLoading) {
                                CircularProgressIndicator(Modifier.size(20.dp))
                            } else {
                                if (isPlaying) {
                                    IconButton(onClick = { controller.pause() }) {
                                        ci.ContentIcon(
                                            painterResource(R.drawable.baseline_pause),
                                            contentDescription = "Pause"
                                        )
                                    }
                                } else {
                                    IconButton(onClick = {
                                        if (sd?.id != audio.id) onClick()
                                        else controller.play()
                                    }) {
                                        ci.ContentIcon(
                                            Icons.Default.PlayArrow, contentDescription = "Play"
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { controller.seekForward() }) {
                                ci.ContentIcon(
                                    painterResource(R.drawable.fast_forward),
                                    contentDescription = "Forward"
                                )
                            }
                            IconButton(onClick = {
                                transitionState.targetState = false; onToggle(false)
                            }) {
                                ci.ContentIcon(
                                    Icons.Default.KeyboardArrowDown, contentDescription = "Dismiss"
                                )
                            }
                        }
                    }
                }
            } else {
                LaunchedEffect(Unit) {
                    delay(40); transitionState.targetState = true
                }
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .clickable { onToggle(true) },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ci.ContentIcon(Icons.Default.KeyboardArrowUp)
                    ci.ContentIcon(Icons.Default.KeyboardArrowUp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackProgress(
    mediaController: MediaController, vm: LyricsEditorVM, modifier: Modifier = Modifier
) {
    val duration by vm.duration.collectAsStateWithLifecycle()
    val position by vm.position.collectAsStateWithLifecycle()

    val fraction = if (duration > 0) (position / duration.toFloat()).coerceIn(0f, 1f) else 0f
    // thumb drag state
    var dragFraction by remember { mutableFloatStateOf(fraction) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = (position).formatTime(), style = MaterialTheme.typography.bodySmall)
            Text(text = (duration).formatTime(), style = MaterialTheme.typography.bodySmall)
        }

        Slider(
            value = dragFraction,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                mediaController.seekTo((dragFraction * duration).toLong())
            },
            modifier = Modifier
                .height(10.dp)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember { MutableInteractionSource() },
                    thumbSize = DpSize(3.dp, 10.dp)
                )
            }
        )

        LaunchedEffect(fraction) {
            if (dragFraction != fraction) {
                dragFraction = fraction
            }
        }
    }
}


/** Returns "M:SS.mmm" */
private fun Long.formatTime(): String {
    val total = this.coerceAtLeast(0L)
    val minutes = total / 60_000
    val seconds = (total % 60_000) / 1000
    val millis = total % 1000
    return "%d:%02d.%03d".format(minutes, seconds, millis)
}
