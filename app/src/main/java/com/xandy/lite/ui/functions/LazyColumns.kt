package com.xandy.lite.ui.functions

import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.AudioWithPls
import com.xandy.lite.ui.functions.item.details.SongRow
import com.xandy.lite.ui.theme.GetUIStyle


@Composable
fun SongLazyColumn(
    list: List<AudioFile>, enabled: Boolean, onClick: (AudioFile) -> Unit, getUIStyle: GetUIStyle,
    onEdit: (String) -> Unit, onDelete: (AudioFile) -> Unit, onLongPress: (String) -> Unit,
    onAdd: (String) -> Unit, onToggleHide: (Uri) -> Unit = {}, selectedSongSet: Set<String>,
    hideAllowed: Pair<Boolean, String> = Pair(false, ""),onUpsertLyrics: (String) -> Unit,
    topContent: LazyListScope.() -> Unit = {}, state: LazyListState = rememberLazyListState(),
    currentId: String, isSelecting: Boolean, modifier: Modifier
) {
    LazyColumn(
        modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, state = state
    ) {
        topContent()
        items(list, key = { it.uri }) { audio ->
            val selected = audio.id in selectedSongSet
            val isPicked = currentId == audio.id
            SongRow(
                audio, getUIStyle, isSelected = selected, enabled = enabled,
                context = LocalContext.current, isPickedSong = isPicked,
                onClick = { onClick(audio) },
                onEdit = { onEdit(audio.uri.toString()) },
                onDelete = { onDelete(audio) },
                onLongPress = { onLongPress(audio.id) },
                onToggleHide = { onToggleHide(audio.uri) },
                isSelecting = isSelecting, hideAllowed = hideAllowed,
                onAdd = { onAdd(audio.uri.toString()) },
                onUpsertLyrics = { onUpsertLyrics(audio.uri.toString()) }
            )
        }
    }
}

@Composable
fun SongLazyColumn(
    list: List<AudioWithPls>, enabled: Boolean, onClick: (AudioFile) -> Unit,
    getUIStyle: GetUIStyle, onAdd: (String) -> Unit, onToggleHide: (Uri) -> Unit = {},
    onEdit: (String) -> Unit, onDelete: (AudioFile) -> Unit, onUpsertLyrics: (String) -> Unit,
    onLongPress: (String) -> Unit, hideAllowed: Pair<Boolean, String> = Pair(false, ""),
    selectedSongSet: Set<String>, contentPadding: PaddingValues = PaddingValues(0.dp),
    currentId: String,
    state: LazyListState = rememberLazyListState(), isSelecting: Boolean, modifier: Modifier
) {
    LazyColumn(
        modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, state = state,
        contentPadding = contentPadding
    ) {
        items(list, key = { it.song.uri.toString() }) { af ->
            val selected = af.song.id in selectedSongSet
            val isPicked = currentId == af.song.id

            SongRow(
                af.song, getUIStyle = getUIStyle, isSelected = selected,
                isSelecting = isSelecting, enabled = enabled, isPickedSong = isPicked,
                context = LocalContext.current, hideAllowed = hideAllowed,
                onClick = { onClick(af.song) },
                onDelete = { onDelete(af.song) },
                onEdit = { onEdit(af.song.uri.toString()) },
                onLongPress = { onLongPress(af.song.id) },
                onToggleHide = { onToggleHide(af.song.uri) },
                onAdd = { onAdd(af.song.id) },
                onUpsertLyrics = { onUpsertLyrics(af.song.uri.toString()) }
            )
        }
    }
}