package com.xandy.lite.views.picked.song

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import com.xandy.lite.R
import com.xandy.lite.controllers.view.models.PickedSongVM
import com.xandy.lite.db.tables.toAudioFile
import com.xandy.lite.models.ui.PickedSongVMStates
import com.xandy.lite.models.ui.SongDetails
import com.xandy.lite.models.ui.SongToggle
import com.xandy.lite.models.ui.drawableResUri
import com.xandy.lite.models.itemKey
import com.xandy.lite.models.ui.order.by.QueueOrder
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.item.details.Artwork
import com.xandy.lite.ui.functions.item.details.QueueRow
import com.xandy.lite.ui.functions.item.details.SongRow
import com.xandy.lite.ui.GetUIStyle
import com.xandy.lite.views.player.controller.PlayerButtons


@Composable
fun HorizontalSongView(
    controller: MediaController, onUpdateOrder: (QueueOrder) -> Unit, songToggle: SongToggle,
    onReverseOrder: () -> Unit, ci: ContentIcons, states: PickedSongVMStates,
    songIdx: Int, getUIStyle: GetUIStyle, onToggle: (SongToggle) -> Unit,
    songVM: PickedSongVM
) {
    val unknownTrackUri = LocalContext.current.drawableResUri(R.drawable.unknown_track)
    val position by songVM.position.collectAsStateWithLifecycle()
    val pictureModifier = Modifier
        .size(200.dp)
        .padding(2.dp)
    val minifiedPicModifier = Modifier
        .size(100.dp)
        .padding(2.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val songDetailsModifier = Modifier
            .fillMaxWidth(0.45f)
            .align(Alignment.CenterStart)
            .padding(top = 4.dp, bottom = 70.dp)
        val otherHalfModifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.55f)
            .align(Alignment.CenterEnd)
        when (songToggle) {
            is SongToggle.Queue -> {
                SongDetails(states.song, songDetailsModifier, minifiedPicModifier)
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .align(Alignment.CenterStart)
                        .padding(top = 4.dp, bottom = 70.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    states.song?.let {
                        if (it.picture != null) Artwork(
                            it.picture, LocalContext.current, minifiedPicModifier
                        )
                        else Artwork(minifiedPicModifier)
                        Text(
                            text = it.title, style = MaterialTheme.typography.titleLarge,
                            fontSize = 20.sp, textAlign = TextAlign.Center,
                            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .fillMaxWidth()
                                .basicMarquee()
                        )
                        Text(
                            text = it.artist, style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp, textAlign = TextAlign.Center,
                            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .fillMaxWidth()
                        )
                    }
                }
                Column(
                    modifier = otherHalfModifier,
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    QueueRow(
                        ci,
                        onUpdateOrder = onUpdateOrder,
                        onReverseOrder = onReverseOrder, states.queueOrder, states.queueAsc,
                        songIdx, states.queueSize, Modifier.zIndex(2f),
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(-1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(states.sortedQueue, key = { it.mediaItem.itemKey() }) { item ->
                            val isPicked = states.song?.id == item.mediaItem.itemKey()
                            SongRow(
                                item.mediaItem.toAudioFile(unknownTrackUri), getUIStyle,
                                isPicked, LocalContext.current
                            ) {
                                val index =
                                    states.unsortedQueue.indexOfFirst {
                                        item.mediaItem.itemKey() == it.mediaItem.itemKey()
                                    }.takeIf { idx -> idx >= 0 } ?: return@SongRow
                                controller.seekTo(index, 0)
                                if (!states.isPlaying) controller.play()
                            }
                        }
                    }
                }
            }

            is SongToggle.Details -> {
                Column(
                    modifier = otherHalfModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    states.song?.let {
                        if (it.picture != null) Artwork(
                            it.picture, LocalContext.current, pictureModifier
                        )
                        else Artwork(pictureModifier)
                        Text(
                            text = it.title, style = MaterialTheme.typography.titleLarge,
                            fontSize = 22.sp, textAlign = TextAlign.Center,
                            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .fillMaxWidth()
                                .basicMarquee()
                        )
                        Text(
                            text = it.artist, style = MaterialTheme.typography.bodyMedium,
                            fontSize = 16.sp, textAlign = TextAlign.Center,
                            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }

            is SongToggle.Lyrics -> {
                SongDetails(states.song, songDetailsModifier, minifiedPicModifier)
                SongLyrics(states.song, position, getUIStyle, otherHalfModifier)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 60.dp)
                .fillMaxWidth(0.45f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (songToggle !is SongToggle.Lyrics)
                    IconButton(
                        onClick = {
                            onToggle(
                                if (songToggle !is SongToggle.Queue) SongToggle.Queue
                                else SongToggle.Details
                            )
                        }
                    ) {
                        ci.ContentIcon(painterResource(R.drawable.music_notation))
                    }

                IconButton(onClick = {
                    onToggle(
                        if (songToggle !is SongToggle.Lyrics) SongToggle.Lyrics
                        else SongToggle.Details
                    )
                }) {
                    ci.ContentIcon(painterResource(R.drawable.baseline_lyrics))
                }
            }
            PlaybackProgress(controller, songVM, Modifier.align(Alignment.CenterHorizontally))
        }
        PlayerButtons(
            states, controller, ci, Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.45f)
                .padding(bottom = 10.dp)
        )
    }
}

@Composable
private fun SongDetails(song: SongDetails?, modifier: Modifier, minifiedPicModifier: Modifier) {
    Column(
        modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        song?.let {
            if (it.picture != null) Artwork(
                it.picture, LocalContext.current, minifiedPicModifier
            )
            else Artwork(minifiedPicModifier)
            Text(
                text = it.title, style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp, textAlign = TextAlign.Center,
                maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .basicMarquee()
            )
            Text(
                text = it.artist, style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp, textAlign = TextAlign.Center,
                maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth()
            )
        }
    }
}