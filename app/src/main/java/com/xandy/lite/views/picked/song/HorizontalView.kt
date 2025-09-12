package com.xandy.lite.views.picked.song

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.session.MediaController
import com.xandy.lite.R
import com.xandy.lite.controllers.view.models.PickedSongVM
import com.xandy.lite.db.tables.toAudioFile
import com.xandy.lite.models.ui.PickedSongVMStates
import com.xandy.lite.models.ui.drawableResUri
import com.xandy.lite.models.ui.itemKey
import com.xandy.lite.models.ui.order.by.QueueOrder
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.item.details.Artwork
import com.xandy.lite.ui.functions.item.details.QueueRow
import com.xandy.lite.ui.functions.item.details.SongRow
import com.xandy.lite.ui.theme.GetUIStyle
import com.xandy.lite.views.player.controller.PlayerButtons


@Composable
fun HorizontalSongView(
    controller: MediaController, onUpdateOrder: (QueueOrder) -> Unit, showQueue: Boolean,
    onReverseOrder: () -> Unit, ci: ContentIcons, states: PickedSongVMStates,
    songIdx: Int, getUIStyle: GetUIStyle, onToggle: () -> Unit,
    songVM: PickedSongVM
) {
    val unknownTrackUri = LocalContext.current.drawableResUri(R.drawable.unknown_track)
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
        if (showQueue) {
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
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .align(Alignment.TopEnd),
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
                        .fillMaxHeight()
                        .zIndex(-1f),
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
        } else {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.55f)
                    .align(Alignment.CenterEnd),
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 60.dp)
                .fillMaxWidth(0.45f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.align(Alignment.Start)
            ) {
                ci.ContentIcon(painterResource(R.drawable.music_notation))
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