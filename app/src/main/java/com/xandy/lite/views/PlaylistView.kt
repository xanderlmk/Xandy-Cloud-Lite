package com.xandy.lite.views

import android.widget.Toast
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xandy.lite.R
import com.xandy.lite.ui.functions.item.details.PlaylistOrderRow
import com.xandy.lite.controllers.view.models.LocalPLVM
import com.xandy.lite.models.AudioDialog
import com.xandy.lite.models.XCToast
import com.xandy.lite.models.ui.InsertResult
import com.xandy.lite.models.ui.PlaylistWithCount
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.LyricsListDialog
import com.xandy.lite.ui.functions.SongLazyColumn
import com.xandy.lite.ui.functions.item.details.Artwork
import com.xandy.lite.ui.functions.item.details.SongRow
import com.xandy.lite.ui.GetUIStyle
import com.xandy.lite.ui.functions.AudioDetailsDialog
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings


@Composable
fun LocalPlaylistView(
    vm: LocalPLVM, currentId: String, getUIStyle: GetUIStyle, songsInPL: PlaylistWithCount,
    onAdd: (String) -> Unit, onEditSong: (String) -> Unit
) {
    val sd by vm.songDetails.collectAsStateWithLifecycle()
    val isSelecting by vm.isSelecting.collectAsStateWithLifecycle()
    val selectedSongIds by vm.selectedSongIds.collectAsStateWithLifecycle()
    val isAdding by vm.isAdding.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val isSearching by vm.isSearching.collectAsStateWithLifecycle()
    val alIsLoading by vm.localAudiosLoading.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val isPlaying by vm.isPlaying.collectAsStateWithLifecycle()
    val controller by vm.mediaController.collectAsStateWithLifecycle()
    val ci = ContentIcons(getUIStyle)
    val pickedQueueName by vm.pickedQueueName.collectAsStateWithLifecycle()
    var afDetails by rememberSaveable { mutableStateOf(AudioDialog()) }

    val name = "local_playlist_${songsInPL.playlist.name}"
    val onPlay: () -> Unit = {
        songsInPL.songs.takeIf { it.isNotEmpty() }
            ?.let { songs ->
                val index = songs.indices.random()
                vm.selectSong(songs[index].data, songs.map { it.data }, name)
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
    var showDialog by rememberSaveable { mutableStateOf(Pair(false, "")) }
    val lyricsList by vm.lyricsList.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var enabled by rememberSaveable { mutableStateOf(true) }
    val appStrings by vm.appStrings.collectAsStateWithLifecycle()
    val toast = XCToast(context)
    val trackText = if (songsInPL.songCount == 1) stringResource(R.string.one_track) else
        stringResource(R.string.num_tracks, songsInPL.songCount)
    LazyColumnScrollbar(
        state = state,
        settings = ScrollbarSettings(
            thumbSelectedColor = getUIStyle.selectedThumbColor(),
            thumbUnselectedColor = getUIStyle.unSelectedThumbColor()
        ), modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        val selectedSongSet = selectedSongIds.toSet()
        if (!isAdding) {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(), state = state,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                item {
                    val picture = songsInPL.playlist.picture
                    if (picture != null) Artwork(picture, LocalContext.current, pictureModifier)
                    else Artwork(pictureModifier)
                    Text(
                        text = trackText,
                        style = MaterialTheme.typography.titleMedium, fontSize = 16.sp
                    )
                }
                item {
                    PlaylistOrderRow(
                        songsInPL.order, ci, onUpdate = { order ->
                            coroutineScope.launch {
                                vm.updateOrder(
                                    order, songsInPL.playlist
                                )
                            }
                        }, onShuffle = {
                            songsInPL.songs.takeIf { it.isNotEmpty() }?.let { songs ->
                                val index = songs.indices.random()
                                vm.setShuffleOn(
                                    songs[index].data, songs.map { it.data }, name
                                )
                            }
                        }, onPlay = {
                            if (pickedQueueName != name) onPlay()
                            else if (isPlaying) controller?.pause() else controller?.play()
                        }, isPlaying = (isPlaying && pickedQueueName == name),
                        enabled = !isSearching
                    )
                }

                val filtered = songsInPL.songs.filter { audio ->
                    if (query.isBlank() || !isSearching) return@filter true
                    audio.data.title.contains(query, ignoreCase = true) ||
                            audio.data.artist?.contains(query, ignoreCase = true) ?: false
                }

                items(filtered, key = { it.data.uri.toString() }) { song ->
                    val id = song.data.id
                    val selected = song.data.id in selectedSongSet
                    SongRow(
                        song.data, vm.appStrings.collectAsStateWithLifecycle().value, getUIStyle,
                        isSelecting = isSelecting, isSelected = selected,
                        onClick = {
                            if (!isSelecting) vm.selectSong(
                                song.data, songsInPL.songs.map { it.data }, name
                            )
                            else {
                                val limitReached = vm.toggleSong(song.data.id)
                                if (limitReached) toast.makeMessage(toast.unableToGet2kPlusFiles)
                            }
                        }, enabled = !alIsLoading,
                        onLongPress = {
                            if (isSelecting) {
                                val limitReached = vm.toggleSong(song.data.id)
                                if (limitReached) toast.makeMessage(toast.unableToGet2kPlusFiles)
                            } else vm.startSelecting(song.data.id)
                        },
                        onDelete = {
                            coroutineScope.launch {
                                vm.removeLocalSongsFromPL(
                                    songIds = listOf(song.data.id),
                                    playlistId = songsInPL.playlist.name
                                )
                            }
                        }, context = LocalContext.current, isPickedSong = currentId == id,
                        onEdit = { onEditSong(song.data.uri.toString()) },
                        onAdd = { onAdd(song.data.id) },
                        onAddFavorite = {
                            coroutineScope.launch {
                                val result = vm.onFavoriteSong(song.data.uri)
                                when (result) {
                                    InsertResult.Exists ->
                                        toast.makeMessage(toast.trackAlreadyInFavorites)

                                    InsertResult.Failure ->
                                        toast.makeMessage(toast.failedToAddToFavorites)

                                    InsertResult.Success -> {}
                                }
                            }
                        },
                        onEnqueue = {
                            val result = vm.addToQueue(listOf(song.data))
                            if (result) toast.makeMessage(toast.trackAlreadyInQueue)
                            else toast.makeMessage(toast.trackAddedToQueue)
                        },
                        onShowDetails = { afDetails = AudioDialog(song.data, true) },
                        onAddNext = {
                            val result = vm.playNext(song.data)
                            when (result) {
                                InsertResult.Exists ->
                                    toast.makeMessage(toast.trackAlreadyInPlayNext)

                                InsertResult.Failure ->
                                    toast.makeMessage(toast.failedToAddTrackNullMC)

                                InsertResult.Success ->
                                    toast.makeMessage(toast.trackAddedToPlayNext)
                            }
                        },
                        onUpsertLyrics = { showDialog = Pair(true, song.data.uri.toString()) }
                    )
                }
            }
        } else {
            val filtered by vm.filteredAudioFiles.collectAsStateWithLifecycle()
            val selectedSongSet = selectedSongIds.toSet()
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(), state = state,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                items(filtered, key = { it.song.id }) { af ->
                    val id = af.song.id
                    val selected = af.song.id in selectedSongSet
                    SongRow(
                        af.song, getUIStyle = getUIStyle, isSelected = selected, context = context,
                        isPickedSong = currentId == id, appStrings = appStrings
                    ) {
                        val limitReached = vm.toggleSong(af.song.id)
                        if (limitReached) toast.makeMessage(toast.unableToGet2kPlusFiles)
                    }
                }
            }
        }
    }
    AudioDetailsDialog(
        audio = afDetails.af, showDialog = afDetails.show, getUIStyle = getUIStyle,
        onDismiss = { afDetails = AudioDialog() }
    )
    LyricsListDialog(
        showDialog = showDialog.first, onDismiss = { showDialog = Pair(false, "") },
        getUIStyle = getUIStyle, list = lyricsList, enabled = enabled,
        onSubmit = { lyricsId ->
            coroutineScope.launch {
                val songUri = showDialog.second
                if (songUri.isBlank()) {
                    toast.makeMessage(toast.nullTrack)
                    showDialog = Pair(false, "")
                    return@launch
                }
                enabled = false
                val result =
                    vm.updateSongLyrics(lyricsId = lyricsId, songUri = songUri)
                if (!result)
                    toast.makeMessage(toast.failedToAddLyricsTo(songUri))
                enabled = true
                showDialog = Pair(false, "")
            }
        }
    )
}