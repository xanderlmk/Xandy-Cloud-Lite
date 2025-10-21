package com.xandy.lite.views.player.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xandy.lite.R
import com.xandy.lite.models.itemKey
import com.xandy.lite.navigation.NavViewModel
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.theme.GetUIStyle
import kotlinx.coroutines.delay

@Composable
fun PlayerController(
    controller: MediaController, navVM: NavViewModel, onClickSong: () -> Unit,
    getUIStyle: GetUIStyle, modifier: Modifier
) {
    val tracks by navVM.tracks.collectAsStateWithLifecycle()
    val isPlaying by navVM.isPlaying.collectAsStateWithLifecycle()
    val isLoading by navVM.isLoading.collectAsStateWithLifecycle()
    val repeatMode by navVM.repeatMode.collectAsStateWithLifecycle()
    val sd by navVM.songDetails.collectAsStateWithLifecycle()
    val ci = ContentIcons(getUIStyle)
    if (!tracks.isEmpty && sd != null) {
        Box(
            modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
                .background(getUIStyle.floatingPlayerBackground(), RoundedCornerShape(24.dp))
                .padding(2.dp)
                .zIndex(4f)
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = onClickSong)
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(.625f)
            ) {
                sd?.let { details ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(details.picture)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album art",
                        placeholder = painterResource(R.drawable.unknown_track),
                        error = painterResource(R.drawable.unknown_track),
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Column {
                        Text(
                            details.title, maxLines = 1, fontSize = 16.sp,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .basicMarquee()
                                .padding(start = 4.dp),
                        )
                        Text(
                            details.artist, maxLines = 1, fontSize = 14.sp,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                } ?: Box {
                    var show by rememberSaveable { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(2_000L); show = true
                    }
                    if (show) Text(
                        text = "Null songs, click here to reset.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .height(60.dp)
                            .clickable {
                                navVM.updateTracks(controller.currentTracks)
                                navVM.updatePickedSong(controller.currentMediaItem?.itemKey())
                            }
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(.375f)
            ) {
                IconButton(onClick = {
                    val position = controller.currentPosition
                    val queueSize = controller.mediaItemCount
                    val currentIndex = controller.currentMediaItemIndex
                    if (repeatMode == Player.REPEAT_MODE_OFF ||
                        repeatMode == Player.REPEAT_MODE_ONE
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
                        cd = "Prev"
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                } else {
                    if (isPlaying) {
                        IconButton(onClick = { controller.pause() }) {
                            ci.ContentIcon(
                                painterResource(R.drawable.baseline_pause),
                                cd = "Pause"
                            )
                        }
                    } else {
                        IconButton(onClick = { controller.play() }) {
                            ci.ContentIcon(Icons.Default.PlayArrow, cd = "Play")
                        }
                    }
                }
                IconButton(onClick = {
                    if (repeatMode == Player.REPEAT_MODE_OFF ||
                        repeatMode == Player.REPEAT_MODE_ONE
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
                }) {
                    ci.ContentIcon(
                        painterResource(R.drawable.baseline_skip_next),
                        cd = "Next"
                    )
                }
            }
        }
    } else LaunchedEffect(Unit) {
        delay(3_000L)
        navVM.updateTracks(controller.currentTracks)
        navVM.updatePickedSong(controller.currentMediaItem?.itemKey())
    }
}

