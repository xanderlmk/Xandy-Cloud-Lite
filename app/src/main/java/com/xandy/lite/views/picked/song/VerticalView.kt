package com.xandy.lite.views.picked.song

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.xandy.lite.models.ui.SongToggle
import com.xandy.lite.models.ui.drawableResUri
import com.xandy.lite.models.itemKey
import com.xandy.lite.models.ui.order.by.QueueOrder
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.item.details.Artwork
import com.xandy.lite.ui.functions.item.details.QueueRow
import com.xandy.lite.ui.functions.item.details.SongRow
import com.xandy.lite.ui.theme.GetUIStyle
import com.xandy.lite.views.player.controller.PlayerButtons


@Composable
fun VerticalSongView(
    controller: MediaController, onUpdateOrder: (QueueOrder) -> Unit, songToggle: SongToggle,
    onReverseOrder: () -> Unit, ci: ContentIcons, states: PickedSongVMStates,
    songIdx: Int, getUIStyle: GetUIStyle, onToggle: (SongToggle) -> Unit,
    songVM: PickedSongVM
) {
    val position by songVM.position.collectAsStateWithLifecycle()
    val unknownTrackUri = LocalContext.current.drawableResUri(R.drawable.unknown_track)
    val pictureModifier = Modifier
        .size(250.dp)
        .padding(2.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp)
    ) {
        val thisModifier = Modifier
            .fillMaxWidth()
            .height(450.dp)
            .zIndex(-1f)
        when (songToggle) {
            is SongToggle.Queue -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
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
                        modifier = thisModifier,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(states.sortedQueue) { item ->
                            val isPicked = states.song?.id == item.mediaItem.itemKey()
                            SongRow(
                                item.mediaItem.toAudioFile(unknownTrackUri), getUIStyle,
                                isPicked, LocalContext.current
                            ) {
                                val index =
                                    states.unsortedQueue.indexOf(item).takeIf { it >= 0 }
                                        ?: return@SongRow
                                controller.seekTo(index, 0)
                                if (!states.isPlaying) controller.play()
                            }
                        }
                    }
                }
            }

            is SongToggle.Details -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(bottom = 60.dp),
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
                SongLyrics(states.song, position, getUIStyle,thisModifier)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (songToggle is SongToggle.Lyrics) Arrangement.End
                else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (songToggle !is SongToggle.Lyrics)
                    IconButton(onClick = {
                        onToggle(
                            if (songToggle !is SongToggle.Queue) SongToggle.Queue
                            else SongToggle.Details
                        )
                    }) {
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
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )
    }
}