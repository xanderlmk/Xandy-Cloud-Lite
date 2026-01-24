package com.xandy.lite.views.picked.song

import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import com.xandy.lite.R
import com.xandy.lite.controllers.view.models.PickedSongVM
import com.xandy.lite.controllers.view.models.PickedSongVM.PickedSongVMStates
import com.xandy.lite.db.tables.itemKey
import com.xandy.lite.db.tables.toAudioFile
import com.xandy.lite.models.XCToast
import com.xandy.lite.models.ui.InsertResult
import com.xandy.lite.models.ui.SongDetails
import com.xandy.lite.models.ui.SongToggle
import com.xandy.lite.models.ui.order.by.QueueOrder
import com.xandy.lite.models.ui.order.by.reverseSort
import com.xandy.lite.models.ui.order.by.toOrderedByClass
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.GetUIStyle
import com.xandy.lite.ui.functions.item.details.QueueRow
import com.xandy.lite.ui.functions.item.details.SongRow
import kotlinx.coroutines.launch


@Composable
fun SongView(
    songVM: PickedSongVM, getUIStyle: GetUIStyle,
    onToggle: (SongToggle) -> Unit, songToggle: SongToggle
) {
    val controller by songVM.mediaController.collectAsStateWithLifecycle()
    val states by songVM.vmStates.collectAsStateWithLifecycle()
    val songIdx = states.sortedQueue.find { it.itemKey() == states.song?.id }?.let {
        states.sortedQueue.indexOf(it).takeIf { idx -> idx >= 0 }?.plus(1) ?: return
    } ?: 1
    val ci = ContentIcons(getUIStyle)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val onClick: (MediaItem) -> Unit = onClick@{ item ->
        val currentItem = controller?.currentMediaItem
        val (currentItemKey, currentIdx) =
            songVM.getCurrentPriorityItem()
        val prospectiveIdx = states.unsortedQueue.indexOfFirst {
            item.itemKey() == it.itemKey()
        }.takeIf { idx -> idx >= 0 } ?: return@onClick
        val index =
            if (currentIdx > C.INDEX_UNSET &&
                currentItemKey.isNotBlank() &&
                currentItemKey == currentItem?.itemKey() &&
                prospectiveIdx >= currentIdx
            ) prospectiveIdx + 1
            else prospectiveIdx
        songVM.removePriorityItemStates()
        controller?.seekTo(index, 0)

        if (currentIdx > C.INDEX_UNSET &&
            currentItemKey == currentItem?.itemKey()
        ) controller?.removeMediaItem(currentIdx)

        if (!states.isPlaying) controller?.play()
    }
    if (isLandscape) HorizontalSongView(
        controller = controller, states = states, songVM = songVM, songToggle = songToggle,
        onToggle = onToggle, onUpdateOrder = {
            songVM.updateQueueOrder(it.toOrderedByClass(states.queueAsc))
        },
        onReverseOrder = {
            songVM.updateQueueOrder(states.queueOrder.reverseSort())
        }, ci = ci, getUIStyle = getUIStyle, songIdx = songIdx, onClick = onClick
    )
    else VerticalSongView(
        controller = controller, states = states, songVM = songVM, songToggle = songToggle,
        onToggle = onToggle, onUpdateOrder = {
            songVM.updateQueueOrder(it.toOrderedByClass(states.queueAsc))
        },
        onReverseOrder = {
            songVM.updateQueueOrder(states.queueOrder.reverseSort())
        }, ci = ci, getUIStyle = getUIStyle, songIdx = songIdx, onClick = onClick
    )
    // if (controller == null) Text(stringResource(R.string.null_controller))

}

private const val COMMAND_CHECK_TIME = "Check_Time"

@Composable
internal fun PlaybackProgress(
    mediaController: MediaController?, songVM: PickedSongVM, modifier: Modifier = Modifier
) {
    val duration by songVM.duration.collectAsStateWithLifecycle()
    val position by songVM.position.collectAsStateWithLifecycle()

    val fraction = if (duration > 0) (position / duration.toFloat()).coerceIn(0f, 1f) else 0f
    // thumb drag state
    var dragFraction by remember { mutableFloatStateOf(fraction) }

    Column(modifier = modifier.fillMaxWidth()) {
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
                mediaController?.seekTo((dragFraction * duration).toLong())
                mediaController?.sendCustomCommand(
                    SessionCommand(COMMAND_CHECK_TIME, Bundle()), Bundle()
                )
            },
            modifier = Modifier
                .height(20.dp)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
        )

        LaunchedEffect(fraction) {
            if (dragFraction != fraction) {
                dragFraction = fraction
            }
        }
    }
}

@Composable
fun SongLyrics(
    song: SongDetails?, position: Long, songToggle: SongToggle.Lyrics,
    getUIStyle: GetUIStyle, modifier: Modifier
) {
    val list = song?.lyrics?.scroll?.toList() ?: emptyList()
    val plainLyrics = song?.lyrics?.plain
    val state = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var viewPortHeight by rememberSaveable { mutableIntStateOf(0) }
    val itemHeights = rememberSaveable { mutableListOf<Int>() }
    var activeIndex by rememberSaveable { mutableIntStateOf(0) }
    val musicPainter = R.drawable.outline_music_note
    val ci = ContentIcons(getUIStyle)
    LaunchedEffect(list.size) {
        when {
            itemHeights.size < list.size -> {
                repeat(list.size - itemHeights.size) { itemHeights.add(0) }
            }

            itemHeights.size > list.size -> {
                for (i in itemHeights.size - 1 downTo list.size) itemHeights.removeAt(i)
            }
        }
    }
    LaunchedEffect(position) {
        if (!songToggle.sync) return@LaunchedEffect
        val index =
            list.indexOfFirst { position in it.range }.takeIf { it != -1 }
                ?: list.indexOfLast { it.range.last < position }.takeIf { it != -1 }
                ?: return@LaunchedEffect
        if (activeIndex == index) return@LaunchedEffect
        activeIndex = index
        coroutineScope.launch {
            val itemH = itemHeights.getOrNull(index) ?: 0
            if (viewPortHeight > 0 && itemH > 0) {
                val desiredOffset = (itemH / 2 - viewPortHeight / 2)
                state.animateScrollToItem(index, desiredOffset)
            } else state.animateScrollToItem(index)
        }

    }
    LazyColumn(
        state = state,
        modifier = modifier
            .padding(horizontal = 4.dp)
            .onSizeChanged { viewPortHeight = it.height },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (list.isNotEmpty() && songToggle.sync)
            itemsIndexed(list) { index, lyricLine ->
                val isActive = activeIndex == index
                val tintColor = if (isActive) getUIStyle.themedOnContainerColor()
                else getUIStyle.tabTextColor(false)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { cords ->
                            if (index !in itemHeights.indices) return@onGloballyPositioned
                            itemHeights[index] = cords.size.height
                        }
                ) {
                    if (lyricLine.text == "$;12345$") Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(25.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ci.ContentIcon(musicPainter, tint = tintColor)
                        ci.ContentIcon(musicPainter, tint = tintColor)
                        ci.ContentIcon(musicPainter, tint = tintColor)
                    } else Text(
                        text = lyricLine.text, style = MaterialTheme.typography.displaySmall,
                        fontWeight = if (isActive) FontWeight.SemiBold else null,
                        color = tintColor
                    )
                }
            }
        else item {
            Text(
                text = plainLyrics ?: stringResource(R.string.no_lyrics_available),
                style = MaterialTheme.typography.displaySmall
            )
        }
    }
}

@Composable
internal fun QueueContent(
    songToggle: SongToggle.Queue, ci: ContentIcons, states: PickedSongVMStates,
    songIdx: Int,
    onReverseOrder: () -> Unit, onUpdateOrder: (QueueOrder) -> Unit,
    getUIStyle: GetUIStyle,
    onClick: (MediaItem) -> Unit, songVM: PickedSongVM, modifier: Modifier
) {
    val context = LocalContext.current
    val toast = XCToast(context)
    val av by songVM.av.collectAsStateWithLifecycle()
    val priorityQueue by songVM.priorityQueue.collectAsStateWithLifecycle()

    key(songToggle.priority, songToggle) {
        if (!songToggle.priority) {
            QueueRow(
                ci, onUpdateOrder = onUpdateOrder, onReverseOrder = onReverseOrder,
                states.queueOrder, states.queueAsc,
                songIdx, states.queueSize, Modifier.zIndex(2f),
            )
            LazyColumn(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(states.sortedQueue, key = { it.itemKey() }) { item ->
                    val isPicked = states.song?.id == item.itemKey()
                    SongRow(
                        item.toAudioFile(av),
                        songVM.appStrings.collectAsStateWithLifecycle().value,
                        getUIStyle,
                        isPicked,
                        context,
                        onClick = { onClick(item) },
                        onAddNext = {
                            val result =
                                songVM.playNext(item.toAudioFile(av))
                            when (result) {
                                InsertResult.Exists ->
                                    toast.makeMessage(toast.trackAlreadyInPlayNext)

                                InsertResult.Failure ->
                                    toast.makeMessage(toast.failedToAddTrackNullMC)

                                InsertResult.Success ->
                                    toast.makeMessage(toast.trackAddedToPlayNext)
                            }
                        }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .zIndex(2f)
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.priority_queue))
                }
            }
            LazyColumn(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(
                    priorityQueue, key = { idx, item -> "${item.id}-$idx" }
                ) { idx, item ->
                    val isPicked = states.song?.id == item.id
                    SongRow(
                        item, songVM.appStrings.collectAsStateWithLifecycle().value,
                        getUIStyle,
                        isPicked,
                        context,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MusicNotationIcon(
    songToggle: SongToggle, priorityNotEmpty: Boolean,
    ci: ContentIcons, onToggle: (SongToggle) -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .clickable {
                onToggle(
                    if (songToggle !is SongToggle.Queue) SongToggle.Queue()
                    else if (!songToggle.priority && priorityNotEmpty) {
                        SongToggle.Queue(true)
                    } else SongToggle.Details
                )
            }
    ) {
        ci.ContentIcon(
            R.drawable.music_notation, modifier = Modifier.size(28.dp)
        )
    }
}

private fun Long.formatTime(): String {
    val totalSec = (this / 1000).toInt().coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}