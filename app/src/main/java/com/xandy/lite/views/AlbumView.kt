package com.xandy.lite.views

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
import com.xandy.lite.controllers.view.models.LocalAlbumVM
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.models.XCToast
import com.xandy.lite.models.ui.Album
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.LyricsListDialog
import com.xandy.lite.ui.functions.SongLazyColumn
import com.xandy.lite.ui.functions.item.details.Artwork
import com.xandy.lite.ui.functions.item.details.PlayOptions
import com.xandy.lite.ui.theme.GetUIStyle
import kotlinx.coroutines.launch


@Composable
fun LocalAlbumView(
    album: Album,currentId: String, modifier: Modifier, enabled: Boolean, getUIStyle: GetUIStyle,
    onDelete: (AudioFile) -> Unit, onAdd: (String) -> Unit, onEdit: (String) -> Unit,
    onEnabled: (Boolean) -> Unit,
    vm: LocalAlbumVM
) {
    val selectedSongSet = vm.selectedSongIds.collectAsStateWithLifecycle().value.toSet()
    val isSelecting by vm.isSelecting.collectAsStateWithLifecycle()
    val isPlaying by vm.isPlaying.collectAsStateWithLifecycle()
    val controller by vm.mediaController.collectAsStateWithLifecycle()
    val pickedQueueName by vm.pickedQueueName.collectAsStateWithLifecycle()
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val isSearching by vm.isSearching.collectAsStateWithLifecycle()
    var showDialog by rememberSaveable { mutableStateOf(Pair(false, "")) }
    val lyricsList by vm.lyricsList.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val toast = XCToast(LocalContext.current)

    val name = "local_album_${album.name}"
    val ci = ContentIcons(getUIStyle)
    val pictureModifier = Modifier
        .size(200.dp)
        .padding(2.dp)
    val onPlay: () -> Unit = {
        album.songs.takeIf { it.isNotEmpty() }
            ?.let { songs ->
                val index = songs.indices.random()
                vm.selectSong(songs[index], songs, name)
            }
    }

    val filtered = album.songs.filter { audio ->
        if (query.isBlank() || !isSearching) return@filter true
        audio.title.contains(query, ignoreCase = true) ||
                audio.artist.contains(query, ignoreCase = true)
    }
    SongLazyColumn(
        list = filtered, enabled = enabled, getUIStyle = getUIStyle, isSelecting = isSelecting,
        onClick = { audio ->
            if (!isSelecting) {
                vm.selectSong(audio, album.songs, name)
            } else vm.toggleSong(audio.uri.toString())
        }, onEdit = onEdit, onAdd = onAdd, currentId = currentId,
        onLongPress = {
            if (isSelecting) vm.toggleSong(it)
            else vm.startSelecting(it)
        }, onDelete = onDelete,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp), selectedSongSet = selectedSongSet,
        onUpsertLyrics = { showDialog = Pair(true, it) },
        topContent = {
            item {
                Artwork(album.picture, LocalContext.current, pictureModifier)
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.displayLarge, fontSize = 20.sp,
                    lineHeight = 25.sp
                )
                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.displayMedium, fontSize = 18.sp
                )
                Text(
                    text = "${album.songCount} tracks",
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
                        album.songs.takeIf { it.isNotEmpty() }?.let { songs ->
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
                    toast.makeMessage("Null song")
                    showDialog = Pair(false, "")
                    return@launch
                }
                onEnabled(false)
                val result =
                    vm.updateSongLyrics(lyricsId = lyricsId, songUri = songId)
                if (!result)
                    toast.makeMessage("Failed to add lyrics to $songId")
                onEnabled(true)
                showDialog = Pair(false, "")
            }
        }
    )
}