package com.xandy.lite.views

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xandy.cloud.ui.functions.item.details.PlaylistOrderRow
import com.xandy.lite.controllers.view.models.LocalPLVM
import com.xandy.lite.models.ui.PlaylistWithCount
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.SongLazyColumn
import com.xandy.lite.ui.functions.item.details.Artwork
import com.xandy.lite.ui.functions.item.details.SongRow
import com.xandy.lite.ui.theme.GetUIStyle
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings


@Composable
fun LocalPlaylistView(
    playlistVM: LocalPLVM, getUIStyle: GetUIStyle, songsInPL: PlaylistWithCount,
    onAdd: (String) -> Unit, onEditSong: (String) -> Unit
) {
    val audioWithPls = playlistVM.allSongs.collectAsStateWithLifecycle().value.list
    val sd by playlistVM.songDetails.collectAsStateWithLifecycle()
    val isSelecting by playlistVM.isSelecting.collectAsStateWithLifecycle()
    val selectedSongIds by playlistVM.selectedSongIds.collectAsStateWithLifecycle()
    val isAdding by playlistVM.isAdding.collectAsStateWithLifecycle()
    val query by playlistVM.query.collectAsStateWithLifecycle()
    val isSearching by playlistVM.isSearching.collectAsStateWithLifecycle()
    val alIsLoading by playlistVM.localAudiosLoading.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val isPlaying by playlistVM.isPlaying.collectAsStateWithLifecycle()
    val controller by playlistVM.mediaController.collectAsStateWithLifecycle()
    val tracks by playlistVM.tracks.collectAsStateWithLifecycle()
    val ci = ContentIcons(getUIStyle)
    val pickedQueueName by playlistVM.pickedQueueName.collectAsStateWithLifecycle()
    val name = "local_playlist_${songsInPL.playlist.name}"
    val onPlay: () -> Unit = {
        songsInPL.songs.takeIf { it.isNotEmpty() }
            ?.let { songs ->
                val index = songs.indices.random()
                playlistVM.selectSong(
                    songs[index].data, songs.map { it.data }, name
                )
            }
    }
    val modifier =
        if (sd != null) Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp)
        else Modifier.fillMaxSize()
    val pictureModifier = Modifier
        .size(200.dp)
        .padding(2.dp)
    val state = rememberLazyListState()
    LazyColumnScrollbar(
        state = state,
        settings = ScrollbarSettings(
            thumbSelectedColor = getUIStyle.selectedThumbColor(),
            thumbUnselectedColor = getUIStyle.unSelectedThumbColor()
        ), modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        if (!isAdding) {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(), state = state,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                item {
                    val picture = songsInPL.playlist.picture
                    if (picture != null) Artwork(picture, pictureModifier)
                    else Artwork(pictureModifier)
                    Text(
                        text = "${songsInPL.songCount} tracks",
                        style = MaterialTheme.typography.titleMedium, fontSize = 16.sp
                    )
                }
                item {
                    PlaylistOrderRow(
                        songsInPL.order, ci, onUpdate = { order ->
                            coroutineScope.launch {
                                playlistVM.updateOrder(
                                    order,
                                    songsInPL.playlist
                                )
                            }
                        }, onShuffle = {
                            songsInPL.songs.takeIf { it.isNotEmpty() }?.let { songs ->
                                val index = songs.indices.random()
                                playlistVM.setShuffleOn(
                                    songs[index].data,
                                    songs.map { it.data },
                                    name
                                )
                            }
                        }, onPlay = {
                            if (tracks.isEmpty) onPlay()
                            else {
                                if (pickedQueueName != name) onPlay()
                                else
                                    if (isPlaying) controller?.pause() else controller?.play()
                            }
                        }, isPlaying = (isPlaying && pickedQueueName == name)
                    )
                }

                val selectedSongSet = selectedSongIds.toSet()
                items(songsInPL.songs, key = { it.data.uri.toString() }) { song ->
                    val selected = song.data.uri.toString() in selectedSongSet
                    SongRow(
                        song.data, getUIStyle, isSelecting = isSelecting, isSelected = selected,
                        onClick = {
                            if (!isSelecting) playlistVM.selectSong(
                                song.data, songsInPL.songs.map { it.data }, name
                            )
                            else playlistVM.toggleSong(song.data.uri.toString())
                        }, enabled = !alIsLoading,
                        onLongPress = {
                            if (isSelecting) playlistVM.toggleSong(song.data.uri.toString())
                            else playlistVM.startSelecting(song.data.uri.toString())
                        },
                        onDelete = {
                            coroutineScope.launch {
                                playlistVM.removeLocalSongsFromPL(
                                    songIds = listOf(song.data.uri.toString()),
                                    playlistId = songsInPL.playlist.name
                                )
                            }
                        },
                        onEdit = { onEditSong(song.data.uri.toString()) },
                        onAdd = { onAdd(song.data.uri.toString()) }
                    )
                }
            }
        } else {
            val filtered = audioWithPls.filter { audio ->
                if (query.isBlank() || !isSearching) return@filter true
                audio.song.title.contains(query, ignoreCase = true) ||
                        audio.song.artist.contains(query, ignoreCase = true)
            }
            val selectedSongSet = selectedSongIds.toSet()
            SongLazyColumn(
                list = filtered, getUIStyle = getUIStyle, hideAllowed = Pair(true, "Hide"),
                isSelecting = isSelecting, enabled = !alIsLoading, state = state,
                selectedSongSet = selectedSongSet, modifier = Modifier.fillMaxWidth(),
                onClick = { audio ->
                    if (!isSelecting) playlistVM.selectSong(
                        audio, songsInPL.songs.map { it.data }, name
                    )
                    else playlistVM.toggleSong(audio.uri.toString())
                },
                onDelete = {}, onEdit = {}, onAdd = onAdd,
                onLongPress = {
                    if (isSelecting) playlistVM.toggleSong(it)
                    else playlistVM.startSelecting(it)
                }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}