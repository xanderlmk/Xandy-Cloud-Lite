package com.xandy.lite.views.picked.song

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import com.xandy.lite.controllers.view.models.PickedSongVM
import com.xandy.lite.models.ui.itemKey
import com.xandy.lite.models.ui.order.by.reverseSort
import com.xandy.lite.models.ui.order.by.toOrderedByClass
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.collectPickedSongVMStatesWithLifecycle
import com.xandy.lite.ui.theme.GetUIStyle


@Composable
fun SongView(
    songVM: PickedSongVM, getUIStyle: GetUIStyle,
    onToggle: () -> Unit, showQueue: Boolean
) {
    val song by songVM.song.collectAsStateWithLifecycle()
    val mediaController by songVM.mediaController.collectAsStateWithLifecycle()
    val sortedQueue by songVM.sortedQueue.collectAsStateWithLifecycle()
    val queueOrder by songVM.queueOrder.collectAsStateWithLifecycle()
    val queueAsc by songVM.queueAsc.collectAsStateWithLifecycle()
    val songIdx = sortedQueue.find { it.mediaItem.itemKey() == song?.id }?.let {
        sortedQueue.indexOf(it).takeIf { idx -> idx >= 0 }?.plus(1) ?: return
    } ?: 1
    val ci = ContentIcons(getUIStyle)
    val states = collectPickedSongVMStatesWithLifecycle(songVM)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    mediaController?.let { controller ->
        if (isLandscape)
            HorizontalSongView(
                controller = controller, states = states, songVM = songVM, showQueue = showQueue,
                onToggle = onToggle, onUpdateOrder = {
                    songVM.updateQueueOrder(it.toOrderedByClass(queueAsc))
                },
                onReverseOrder = {
                    songVM.updateQueueOrder(queueOrder.reverseSort())
                }, ci = ci, getUIStyle = getUIStyle, songIdx = songIdx
            )
        else
            VerticalSongView(
                controller = controller, states = states, songVM = songVM, showQueue = showQueue,
                onToggle = onToggle, onUpdateOrder = {
                    songVM.updateQueueOrder(it.toOrderedByClass(queueAsc))
                },
                onReverseOrder = {
                    songVM.updateQueueOrder(queueOrder.reverseSort())
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

private fun Long.formatTime(): String {
    val totalSec = (this / 1000).toInt().coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}