package com.xandy.lite.views

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xandy.lite.controllers.view.models.LocalGenreVM
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.models.ui.Genre
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.SongLazyColumn
import com.xandy.lite.ui.functions.item.details.PlayOptions
import com.xandy.lite.ui.theme.GetUIStyle
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings


@Composable
fun LocalGenreView(
    genre: Genre, modifier: Modifier, enabled: Boolean, getUIStyle: GetUIStyle,
    onDelete: (AudioFile) -> Unit, onAdd: (String) -> Unit, onEdit: (String) -> Unit,
    vm: LocalGenreVM
) {
    val selectedSongSet = vm.selectedSongIds.collectAsStateWithLifecycle().value.toSet()
    val isSelecting by vm.isSelecting.collectAsStateWithLifecycle()
    val isPlaying by vm.isPlaying.collectAsStateWithLifecycle()
    val controller by vm.mediaController.collectAsStateWithLifecycle()
    val pickedQueueName by vm.pickedQueueName.collectAsStateWithLifecycle()
    val tracks by vm.tracks.collectAsStateWithLifecycle()
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
                audio.artist.contains(query, ignoreCase = true)
    }

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
            isSelecting = isSelecting,
            onClick = { audio ->
                if (!isSelecting) {
                    vm.selectSong(audio, genre.songs, name)
                } else vm.toggleSong(audio.uri.toString())
            },
            onEdit = onEdit, onAdd = onAdd, state = state,
            onLongPress = {
                if (isSelecting) vm.toggleSong(it)
                else vm.startSelecting(it)
            }, onDelete = onDelete, modifier = Modifier.fillMaxWidth(),
            selectedSongSet = selectedSongSet,
            topContent = {
                item {
                    Text(
                        text = genre.name,
                        style = MaterialTheme.typography.displayLarge, fontSize = 20.sp,
                        lineHeight = 25.sp
                    )
                    Text(
                        text = "${genre.songCount} tracks",
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
    }
}