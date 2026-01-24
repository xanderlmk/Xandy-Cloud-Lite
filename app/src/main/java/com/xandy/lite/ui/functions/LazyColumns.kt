package com.xandy.lite.ui.functions

import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.AudioWithPls
import com.xandy.lite.models.application.AppStrings
import com.xandy.lite.ui.functions.item.details.SongRow
import com.xandy.lite.ui.GetUIStyle


@Composable
fun SongLazyColumn(
    list: List<AudioFile>, appStrings: AppStrings, enabled: Boolean, onClick: (AudioFile) -> Unit,
    getUIStyle: GetUIStyle, onEdit: (String) -> Unit, onDelete: (AudioFile) -> Unit,
    onLongPress: (String) -> Unit, onAdd: (String) -> Unit, onEnqueue: (AudioFile) -> Unit,
    onAddNext: (AudioFile) -> Unit, onAddFavorite: (Uri) -> Unit, onToggleHide: (Uri) -> Unit = {},
    selectedSongSet: Set<String>, hideAllowed: Pair<Boolean, String> = Pair(false, ""),
    onUpsertLyrics: (String) -> Unit, topContent: LazyListScope.() -> Unit = {},
    state: LazyListState = rememberLazyListState(), onShowDetails: (AudioFile) -> Unit,
    currentId: String, isSelecting: Boolean, modifier: Modifier
) {
    LazyColumn(
        modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, state = state
    ) {
        topContent()
        itemsIndexed(list, key = { idx, it -> "${it.uri}-$idx" }) { idx, audio ->
            val selected = audio.id in selectedSongSet
            val isPicked = currentId == audio.id
            SongRow(
                audio, appStrings, getUIStyle, isSelected = selected, enabled = enabled,
                context = LocalContext.current, isPickedSong = isPicked,
                onClick = { onClick(audio) },
                onShowDetails = { onShowDetails(audio) },
                onEdit = { onEdit(audio.uri.toString()) },
                onEnqueue = { onEnqueue(audio) },
                onAddNext = { onAddNext(audio) },
                onAddFavorite = { onAddFavorite(audio.uri) },
                onDelete = { onDelete(audio) },
                onLongPress = { onLongPress(audio.id) },
                onToggleHide = { onToggleHide(audio.uri) },
                isSelecting = isSelecting, hideAllowed = hideAllowed,
                onAdd = { onAdd(audio.id) },
                onUpsertLyrics = { onUpsertLyrics(audio.uri.toString()) }
            )
        }
    }
}

@Composable
fun SongLazyColumn(
    list: List<AudioWithPls>, appStrings: AppStrings, enabled: Boolean,
    onClick: (AudioFile) -> Unit, getUIStyle: GetUIStyle, onAdd: (String) -> Unit,
    onToggleHide: (Uri) -> Unit = {}, onEdit: (String) -> Unit, onDelete: (AudioFile) -> Unit,
    onUpsertLyrics: (String) -> Unit, onLongPress: (String) -> Unit,
    hideAllowed: Pair<Boolean, String> = Pair(false, ""), selectedSongSet: Set<String>,
    contentPadding: PaddingValues = PaddingValues(0.dp), onEnqueue: (AudioFile) -> Unit,
    onAddNext: (AudioFile) -> Unit, currentId: String, onAddFavorite: (Uri) -> Unit,
    onShowDetails: (AudioFile) -> Unit, state: LazyListState = rememberLazyListState(),
    isSelecting: Boolean, modifier: Modifier
) {
    LazyColumn(
        modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, state = state,
        contentPadding = contentPadding
    ) {
        itemsIndexed(list, key = { idx, it -> "${it.song.uri}-$idx" }) { idx, af ->
            val selected = af.song.id in selectedSongSet
            val isPicked = currentId == af.song.id

            SongRow(
                af.song, appStrings, getUIStyle = getUIStyle, isSelected = selected,
                isSelecting = isSelecting, enabled = enabled, isPickedSong = isPicked,
                context = LocalContext.current, hideAllowed = hideAllowed,
                onClick = { onClick(af.song) },
                onShowDetails = { onShowDetails(af.song) },
                onDelete = { onDelete(af.song) },
                onEdit = { onEdit(af.song.uri.toString()) },
                onEnqueue = { onEnqueue(af.song) },
                onAddNext = { onAddNext(af.song) },
                onAddFavorite = { onAddFavorite(af.song.uri) },
                onLongPress = { onLongPress(af.song.id) },
                onToggleHide = { onToggleHide(af.song.uri) },
                onAdd = { onAdd(af.song.id) },
                onUpsertLyrics = { onUpsertLyrics(af.song.uri.toString()) }
            )
        }
    }
}