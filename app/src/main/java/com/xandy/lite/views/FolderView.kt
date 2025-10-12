package com.xandy.lite.views

import android.widget.Toast
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xandy.lite.controllers.view.models.LocalFolderVM
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.BucketWithAudio
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.LyricsListDialog
import com.xandy.lite.ui.functions.SongLazyColumn
import com.xandy.lite.ui.functions.item.details.PlayOptions
import com.xandy.lite.ui.theme.GetUIStyle
import kotlinx.coroutines.launch


@Composable
fun LocalFolderView(
    b: BucketWithAudio, currentId: String, modifier: Modifier, enabled: Boolean,
    getUIStyle: GetUIStyle, onDelete: (AudioFile) -> Unit, onAdd: (String) -> Unit,
    onEdit: (String) -> Unit, onEnabled: (Boolean) -> Unit, vm: LocalFolderVM
) {
    val selectedSongSet = vm.selectedSongIds.collectAsStateWithLifecycle().value.toSet()
    val isSelecting by vm.isSelecting.collectAsStateWithLifecycle()
    val isPlaying by vm.isPlaying.collectAsStateWithLifecycle()
    val controller by vm.mediaController.collectAsStateWithLifecycle()
    val pickedQueueName by vm.pickedQueueName.collectAsStateWithLifecycle()
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    val name = "local_bucket_${b.bucket.volumeName}_${b.bucket.id}"
    val ci = ContentIcons(getUIStyle)

    val onPlay: () -> Unit = {
        b.audioList.takeIf { it.isNotEmpty() }
            ?.let { songs ->
                val index = songs.indices.random()
                vm.selectSong(songs[index], songs, name)
            }
    }
    var showDialog by rememberSaveable { mutableStateOf(Pair(false, "")) }
    val lyricsList by vm.lyricsList.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    SongLazyColumn(
        list = b.audioList, enabled = enabled, getUIStyle = getUIStyle, isSelecting = isSelecting,
        onClick = { audio ->
            if (!isSelecting) {
                vm.selectSong(audio, b.audioList, name)
            } else vm.toggleSong(audio.uri.toString())
        }, onEdit = onEdit, onAdd = onAdd, currentId = currentId,
        onLongPress = {
            if (isSelecting) vm.toggleSong(it)
            else vm.startSelecting(it)
        }, onDelete = onDelete,
        onUpsertLyrics = { showDialog = Pair(true, it) },
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp), selectedSongSet = selectedSongSet,
        topContent = {
            item {
                Text(
                    text = b.bucket.name,
                    style = MaterialTheme.typography.displayLarge, fontSize = 22.sp,
                    lineHeight = 28.sp
                )
                Text(
                    text = "Is hidden: ${b.bucket.hidden}",
                    style = MaterialTheme.typography.titleMedium, fontSize = 16.sp
                )
                Text(
                    text = b.bucket.relativePath, maxLines = 1, softWrap = false,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall, fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .basicMarquee()
                )
                HorizontalDivider(
                    thickness = 3.dp, modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                )
            }
            item {
                PlayOptions(
                    ci = ci, isPlaying = (isPlaying && pickedQueueName == name),
                    onShuffle = {
                        b.audioList.takeIf { it.isNotEmpty() }?.let { songs ->
                            val index = songs.indices.random()
                            vm.setShuffleOn(songs[index], songs, name)
                        }
                    },
                    onPlay = {
                        if (tracks.isEmpty) onPlay()
                        else {
                            if (pickedQueueName != name) onPlay()
                            else
                                if (isPlaying) controller?.pause() else controller?.play()
                        }
                    }
                )
            }
        }
    )
    LyricsListDialog(
        showDialog = showDialog.first, onDismiss = { showDialog = Pair(false, "") },
        getUIStyle = getUIStyle, list = lyricsList, enabled = enabled,
        onSubmit = { lyricsId ->
            coroutineScope.launch {
                val songId = showDialog.second
                if (songId.isBlank()) {
                    Toast.makeText(context, "Null song", Toast.LENGTH_SHORT).show()
                    showDialog = Pair(false, "")
                    return@launch
                }
                onEnabled(false)
                val result =
                    vm.updateSongLyrics(lyricsId = lyricsId, songUri = songId)
                if (!result)
                    Toast.makeText(
                        context, "Failed to add lyrics to $songId", Toast.LENGTH_SHORT
                    ).show()
                onEnabled(true)
                showDialog = Pair(false, "")
            }
        }
    )
}