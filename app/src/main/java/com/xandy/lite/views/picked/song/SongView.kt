package com.xandy.lite.views.picked.song

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import com.xandy.lite.controllers.view.models.PickedSongVM
import com.xandy.lite.models.ui.SongDetails
import com.xandy.lite.models.ui.SongToggle
import com.xandy.lite.models.itemKey
import com.xandy.lite.models.ui.order.by.reverseSort
import com.xandy.lite.models.ui.order.by.toOrderedByClass
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.collectPickedSongVMStatesWithLifecycle
import com.xandy.lite.ui.theme.GetUIStyle
import kotlinx.coroutines.launch


@Composable
fun SongView(
    songVM: PickedSongVM, getUIStyle: GetUIStyle,
    onToggle: (SongToggle) -> Unit, songToggle: SongToggle
) {
    val song by songVM.song.collectAsStateWithLifecycle()
    val mediaController by songVM.mediaController.collectAsStateWithLifecycle()
    val states = collectPickedSongVMStatesWithLifecycle(songVM)
    val songIdx = states.sortedQueue.find { it.mediaItem.itemKey() == song?.id }?.let {
        states.sortedQueue.indexOf(it).takeIf { idx -> idx >= 0 }?.plus(1) ?: return
    } ?: 1
    val ci = ContentIcons(getUIStyle)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    mediaController?.let { controller ->
        if (isLandscape)
            HorizontalSongView(
                controller = controller, states = states, songVM = songVM, songToggle = songToggle,
                onToggle = onToggle, onUpdateOrder = {
                    songVM.updateQueueOrder(it.toOrderedByClass(states.queueAsc))
                },
                onReverseOrder = {
                    songVM.updateQueueOrder(states.queueOrder.reverseSort())
                }, ci = ci, getUIStyle = getUIStyle, songIdx = songIdx
            )
        else
            VerticalSongView(
                controller = controller, states = states, songVM = songVM, songToggle = songToggle,
                onToggle = onToggle, onUpdateOrder = {
                    songVM.updateQueueOrder(it.toOrderedByClass(states.queueAsc))
                },
                onReverseOrder = {
                    songVM.updateQueueOrder(states.queueOrder.reverseSort())
                }, ci = ci, getUIStyle = getUIStyle, songIdx = songIdx
            )
    }
    if (mediaController == null) Text("Null Controller")
}

@Composable
fun PlaybackProgress(
    mediaController: MediaController, songVM: PickedSongVM, modifier: Modifier = Modifier
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
                mediaController.seekTo((dragFraction * duration).toLong())
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
fun SongLyrics(song: SongDetails?, position: Long, getUIStyle: GetUIStyle, modifier: Modifier) {
    val list = song?.lyrics?.scroll?.toList() ?: emptyList()
    val plainLyrics = song?.lyrics?.plain
    val state = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var viewPortHeight by rememberSaveable { mutableIntStateOf(0) }
    val itemHeights = rememberSaveable { mutableListOf<Int>() }
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
        val index =
            list.indexOfFirst { position in it.range }.takeIf { it != -1 } ?: return@LaunchedEffect
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
        if (list.isNotEmpty())
            itemsIndexed(list) { index, lyricLine ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { cords ->
                            if (index !in itemHeights.indices) return@onGloballyPositioned
                            itemHeights[index] = cords.size.height
                        }
                ) {
                    Text(
                        text = lyricLine.text, style = MaterialTheme.typography.displaySmall,
                        fontWeight = if (position in lyricLine.range) FontWeight.SemiBold else null,
                        color = if (position in lyricLine.range) getUIStyle.themedColor()
                        else getUIStyle.tabTextColor(false)
                    )
                }
            }
        else
            item {
                Text(
                    text = plainLyrics ?: "No Lyrics Available",
                    style = MaterialTheme.typography.displaySmall
                )
            }
    }
}

private fun Long.formatTime(): String {
    val totalSec = (this / 1000).toInt().coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}