package com.xandy.lite.views

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xandy.lite.R
import com.xandy.lite.controllers.view.models.LocalGenreVM
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.models.AudioDialog
import com.xandy.lite.models.XCToast
import com.xandy.lite.models.ui.Genre
import com.xandy.lite.models.ui.InsertResult
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.LyricsListDialog
import com.xandy.lite.ui.functions.SongLazyColumn
import com.xandy.lite.ui.functions.item.details.PlayOptions
import com.xandy.lite.ui.GetUIStyle
import com.xandy.lite.ui.functions.AudioDetailsDialog
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings


@Composable
fun LocalGenreView(
    genre: Genre, currentId: String, modifier: Modifier, enabled: Boolean, getUIStyle: GetUIStyle,
    onDelete: (AudioFile) -> Unit, onAdd: (String) -> Unit, onEdit: (String) -> Unit,
    onEnabled: (Boolean) -> Unit, vm: LocalGenreVM
) {
    val selectedSongSet = vm.selectedSongIds.collectAsStateWithLifecycle().value.toSet()
    val isSelecting by vm.isSelecting.collectAsStateWithLifecycle()
    val isPlaying by vm.isPlaying.collectAsStateWithLifecycle()
    val controller by vm.mediaController.collectAsStateWithLifecycle()
    val pickedQueueName by vm.pickedQueueName.collectAsStateWithLifecycle()
    val name = "local_genre_${genre.name}"
    val ci = ContentIcons(getUIStyle)
    val state = rememberLazyListState()
    val query by vm.query.collectAsStateWithLifecycle()
    val isSearching by vm.isSearching.collectAsStateWithLifecycle()
    val onPlay: () -> Unit = {
        genre.songs.takeIf { it.isNotEmpty() }
            ?.let { songs ->
                val index = songs.indices.random()
                vm.selectSong(songs[index], songs, name)
            }
    }
    val filtered = genre.songs.filter { audio ->
        if (query.isBlank() || !isSearching) return@filter true
        audio.title.contains(query, ignoreCase = true) ||
                audio.artist?.contains(query, ignoreCase = true) ?: false
    }
    var showDialog by rememberSaveable { mutableStateOf(Pair(false, "")) }
    val lyricsList by vm.lyricsList.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val toast = XCToast(context)
    var afDetails by rememberSaveable { mutableStateOf(AudioDialog()) }
    val trackText = if (genre.songCount == 1) stringResource(R.string.one_track) else
        stringResource(R.string.num_tracks, genre.songCount)
    LazyColumnScrollbar(
        state = state,
        settings = ScrollbarSettings(
            thumbSelectedColor = getUIStyle.selectedThumbColor(),
            thumbUnselectedColor = getUIStyle.unSelectedThumbColor()
        ), modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        SongLazyColumn(
            list = filtered, enabled = enabled, getUIStyle = getUIStyle,
            appStrings = vm.appStrings.collectAsStateWithLifecycle().value,
            isSelecting = isSelecting, currentId = currentId,
            onClick = { audio ->
                if (!isSelecting) {
                    vm.selectSong(audio, genre.songs, name)
                } else {
                    val limitReached = vm.toggleSong(audio.id)
                    if (limitReached) toast.makeMessage(toast.unableToGet2kPlusFiles)
                }
            },
            onShowDetails = { afDetails = AudioDialog(it, true) },
            onEnqueue = {
                val result = vm.addToQueue(listOf(it))
                if (result) toast.makeMessage(toast.trackAlreadyInQueue)
                else toast.makeMessage(toast.trackAddedToQueue)
            },
            onAddNext = {
                val result = vm.playNext(it)
                when (result) {
                    InsertResult.Exists ->
                        toast.makeMessage(toast.trackAlreadyInPlayNext)

                    InsertResult.Failure ->
                        toast.makeMessage(toast.failedToAddTrackNullMC)

                    InsertResult.Success -> toast.makeMessage(toast.trackAddedToPlayNext)
                }
            },
            onAddFavorite = {
                coroutineScope.launch {
                    val result = vm.onFavoriteSong(it)
                    when (result) {
                        InsertResult.Exists ->
                            toast.makeMessage(toast.trackAlreadyInFavorites)

                        InsertResult.Failure ->
                            toast.makeMessage(toast.failedToAddToFavorites)

                        InsertResult.Success -> {}
                    }
                }
            },
            onEdit = onEdit, onAdd = onAdd, state = state,
            onLongPress = {
                if (isSelecting) {
                    val limitReached = vm.toggleSong(it)
                    if (limitReached) toast.makeMessage(toast.unableToGet2kPlusFiles)
                } else vm.startSelecting(it)
            }, onDelete = onDelete, modifier = Modifier.fillMaxWidth(),
            selectedSongSet = selectedSongSet,
            onUpsertLyrics = { showDialog = Pair(true, it) },
            topContent = {
                item {
                    Text(
                        text = genre.name,
                        style = MaterialTheme.typography.displayLarge, fontSize = 20.sp,
                        lineHeight = 25.sp
                    )
                    Text(
                        text = trackText,
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
                            genre.songs.takeIf { it.isNotEmpty() }?.let { songs ->
                                val index = songs.indices.random()
                                vm.setShuffleOn(songs[index], songs, name)
                            }
                        },
                        onPlay = {
                            if (pickedQueueName != name) onPlay()
                            else if (isPlaying) controller?.pause() else controller?.play()
                        }
                    )
                }
            }
        )
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
                val songId = showDialog.second
                if (songId.isBlank()) {
                    toast.makeMessage(toast.nullTrack)
                    showDialog = Pair(false, "")
                    return@launch
                }
                onEnabled(false)
                val result =
                    vm.updateSongLyrics(lyricsId = lyricsId, songUri = songId)
                if (!result)
                    toast.makeMessage(toast.failedToAddLyricsTo(songId))

                onEnabled(true)
                showDialog = Pair(false, "")
            }
        }
    )
}