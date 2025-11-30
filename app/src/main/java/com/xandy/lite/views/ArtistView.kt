package com.xandy.lite.views

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xandy.lite.controllers.view.models.LocalArtistVM
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.models.XCToast
import com.xandy.lite.models.ui.Artist
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.LyricsListDialog
import com.xandy.lite.ui.functions.SongLazyColumn
import com.xandy.lite.ui.functions.item.details.Artwork
import com.xandy.lite.ui.functions.item.details.PlayOptions
import com.xandy.lite.ui.GetUIStyle
import kotlinx.coroutines.launch


@Composable
fun LocalArtistView(
    artist: Artist, currentId: String, modifier: Modifier, enabled: Boolean, getUIStyle: GetUIStyle,
    onDelete: (AudioFile) -> Unit, onAdd: (String) -> Unit, onEdit: (String) -> Unit,
    onEnabled: (Boolean) -> Unit, vm: LocalArtistVM
) {
    val selectedSongSet = vm.selectedSongIds.collectAsStateWithLifecycle().value.toSet()
    val isSelecting by vm.isSelecting.collectAsStateWithLifecycle()
    val isPlaying by vm.isPlaying.collectAsStateWithLifecycle()
    val controller by vm.mediaController.collectAsStateWithLifecycle()
    val pickedQueueName by vm.pickedQueueName.collectAsStateWithLifecycle()
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val isSearching by vm.isSearching.collectAsStateWithLifecycle()
    val name = "local_artist_${artist.name}"
    val ci = ContentIcons(getUIStyle)
    val pictureModifier = Modifier
        .size(200.dp)
        .padding(2.dp)
    val onPlay: () -> Unit = {
        artist.songs.takeIf { it.isNotEmpty() }
            ?.let { songs ->
                val index = songs.indices.random()
                vm.selectSong(songs[index], songs, name)
            }
    }
    val filtered = artist.songs.filter { audio ->
        if (query.isBlank() || !isSearching) return@filter true
        audio.title.contains(query, ignoreCase = true) ||
                audio.artist.contains(query, ignoreCase = true)
    }
    var showDialog by rememberSaveable { mutableStateOf(Pair(false, "")) }
    val lyricsList by vm.lyricsList.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val toast = XCToast(context)
    SongLazyColumn(
        list = filtered, enabled = enabled, getUIStyle = getUIStyle, isSelecting = isSelecting,
        onClick = { audio ->
            if (!isSelecting) {
                vm.selectSong(audio, artist.songs, name)
            } else vm.toggleSong(audio.uri.toString())
        }, onEdit = onEdit, onAdd = onAdd,
        onLongPress = {
            if (isSelecting) vm.toggleSong(it)
            else vm.startSelecting(it)
        }, onDelete = onDelete, currentId = currentId,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp), selectedSongSet = selectedSongSet,
        onUpsertLyrics = { showDialog = Pair(true, it) },
        onEnqueue = {
            val result = vm.addToQueue(listOf(it))
            if (result) toast.makeMessage("Song already in queue")
        },
        topContent = {
            item {
                Artwork(artist.picture, LocalContext.current, pictureModifier)
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.displayLarge, fontSize = 20.sp,
                    lineHeight = 25.sp
                )
                Text(
                    text = "${artist.songCount} tracks",
                    style = MaterialTheme.typography.titleMedium, fontSize = 16.sp
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
                        artist.songs.takeIf { it.isNotEmpty() }?.let { songs ->
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