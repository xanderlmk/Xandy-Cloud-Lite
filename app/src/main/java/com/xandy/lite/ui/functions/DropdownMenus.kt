package com.xandy.lite.ui.functions

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.taglib.TagProperty
import com.xandy.lite.R
import com.xandy.lite.models.ui.LocalAudioStates
import com.xandy.lite.models.ui.LocalMusicTabs
import com.xandy.lite.models.ui.order.by.PlaylistOrder
import com.xandy.lite.models.ui.order.by.SongOrder
import com.xandy.lite.navigation.NavViewModel
import com.xandy.lite.ui.GetUIStyle
import kotlinx.coroutines.launch


@Composable
fun LocalAudioOptions(
    audioStates: LocalAudioStates, onRefresh: () -> Unit, onReverseSongOrder: () -> Unit,
    onUpdateSongOrder: (SongOrder) -> Unit, onReversePlsOrder: () -> Unit,
    onUpdatePlsOrder: (PlaylistOrder) -> Unit, onSearch: () -> Unit, onHideAudios: () -> Unit,
    onHideFolders: () -> Unit, onAddSongs: () -> Unit, onShareSongs: () -> Unit,
    onUpdateMetadata: (String) -> Unit, onShowAudios: () -> Unit,
    onSelectAll: () -> Unit, onDeleteSongs: () -> Unit, getUIStyle: GetUIStyle
) {
    val ci = ContentIcons(getUIStyle)

    if (!audioStates.isSearching) {
        SearchIconButton(ci) { onSearch() }
        if (!audioStates.isSelecting) {
            ci.RefreshButton(
                audioStates.isLoading, isGettingPics = audioStates.gettingPics,
                onClick = onRefresh, getUIStyle.themedOnContainerColor()
            )
        }
    }

    when (audioStates.tab) {
        LocalMusicTabs.LIBRARY -> {
            if (!audioStates.isSelecting) AllSongOptions(
                onReverseSongOrder = onReverseSongOrder,
                onUpdateSongOrder = onUpdateSongOrder,
                getUIStyle = getUIStyle, asc = audioStates.alDirection
            )
            else EditSongOptions(
                onHideOrShow = onHideAudios, onAddSongs = onAddSongs, onShareSongs = onShareSongs,
                getUIStyle = getUIStyle, hide = true, onUpdateMetadata = onUpdateMetadata,
                onSelectAll = onSelectAll,onDeleteSongs = onDeleteSongs,
                enabled = !audioStates.isLoading
            )

        }

        LocalMusicTabs.PLAYLIST -> AllPLsOptions(
            onReversePlsOrder = onReversePlsOrder,
            onUpdatePlsOrder = onUpdatePlsOrder,
            getUIStyle = getUIStyle, direction = audioStates.plsDirection
        )

        LocalMusicTabs.ALBUMS -> {

        }

        LocalMusicTabs.ARTISTS -> {

        }

        LocalMusicTabs.GENRES -> {

        }

        LocalMusicTabs.FOLDERS -> {
            if (audioStates.isSelecting) Text(
                text = "Hide",
                modifier = Modifier
                    .padding(start = 2.dp, end = 4.dp)
                    .clickable(enabled = !audioStates.isLoading) { onHideFolders() },
                color = getUIStyle.tabTextColor(!audioStates.isLoading)
            )
        }

        LocalMusicTabs.HIDDEN -> {
            if (audioStates.isSelecting) EditSongOptions(
                onHideOrShow = onShowAudios, onAddSongs = onAddSongs,
                onShareSongs = onShareSongs, getUIStyle = getUIStyle,
                hide = false, onUpdateMetadata = onUpdateMetadata, onSelectAll = onSelectAll,
                onDeleteSongs = onDeleteSongs, enabled = !audioStates.isLoading
            )
        }
    }
}

@Composable
fun PlayListOptions(
    navVM: NavViewModel, getUIStyle: GetUIStyle,
    onChangeArt: () -> Unit, onSearch: () -> Unit, onChangeName: () -> Unit
) {
    val ci = ContentIcons(getUIStyle)
    val coroutineScope = rememberCoroutineScope()
    var expanded by rememberSaveable { mutableStateOf(false) }
    val plWithAudio by navVM.plWithAudio.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isSelecting by navVM.isSelecting.collectAsStateWithLifecycle()
    val isAdding by navVM.isAdding.collectAsStateWithLifecycle()
    PLContent(
        ci, expanded, { expanded = it }, onSearch = onSearch,
        onSubmit = {
            coroutineScope.launch {
                result(
                    id = plWithAudio?.playlist?.id,
                    onResult = { navVM.addLocalSongsToPL(navVM.getSelectedSongIds(), it) },
                    onEndSelect = { navVM.endSelect() },
                    context = context, failureText = "Failed to add songs"
                )
            }
        },
        onRemove = {
            coroutineScope.launch {
                result(
                    id = plWithAudio?.playlist?.name,
                    onResult = { navVM.removeLocalSongsFromPL(navVM.getSelectedSongIds(), it) },
                    onEndSelect = { navVM.endSelect(); expanded = false },
                    context = context, failureText = "Failed to remove songs"
                )

            }
        }, onAdd = { plWithAudio?.let { navVM.startAdding() } },
        isSelecting = isSelecting, isAdding = isAdding,
        onChangeArt = onChangeArt, onChangeName = onChangeName
    )
}


private suspend fun result(
    id: String?, onResult: suspend (String) -> Boolean,
    onEndSelect: () -> Unit, context: Context, failureText: String,
) {
    if (id != null) {
        val success = onResult(id)
        if (!success) Toast.makeText(
            context, failureText, Toast.LENGTH_SHORT
        ).show()
        else onEndSelect()
    } else Toast.makeText(
        context, "No Playlist Selected", Toast.LENGTH_SHORT
    ).show()
}


@Composable
private fun PLContent(
    ci: ContentIcons, expanded: Boolean, onExpanded: (Boolean) -> Unit, onSearch: () -> Unit,
    onSubmit: () -> Unit, onRemove: () -> Unit, onChangeArt: () -> Unit, onChangeName: () -> Unit,
    onAdd: () -> Unit, isAdding: Boolean, isSelecting: Boolean,
) {
    if (isSelecting && isAdding) {
        SearchIconButton(ci) { onSearch() }
        Text(
            text = "Done",
            modifier = Modifier
                .padding(start = 2.dp, end = 4.dp)
                .clickable { onSubmit() }
        )
    } else if (isSelecting) {
        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
            IconButton(
                onClick = { onExpanded(!expanded) }
            ) {
                ci.ContentIcon(Icons.Default.MoreVert)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { onExpanded(false) }) {
                DropdownMenuItem(
                    text = { Text("Remove") },
                    trailingIcon = { ci.ContentIcon(painterResource(R.drawable.baseline_remove_circle)) },
                    onClick = onRemove
                )
            }
        }
    } else {
        SearchIconButton(ci) { onSearch() }
        AddIconButton(ci) { onAdd() }
        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
            IconButton(
                onClick = { onExpanded(!expanded) }
            ) {
                ci.ContentIcon(Icons.Default.MoreVert)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { onExpanded(false) }) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = onChangeName
                )
                DropdownMenuItem(
                    text = { Text("Change cover image") },
                    trailingIcon = {
                        ci.ContentIcon(painterResource(R.drawable.outline_replace_image))
                    },
                    onClick = onChangeArt
                )
            }
        }
    }
}

@Composable
private fun AllSongOptions(
    onReverseSongOrder: () -> Unit, onUpdateSongOrder: (SongOrder) -> Unit, getUIStyle: GetUIStyle,
    asc: Boolean,
) {
    val ci = ContentIcons(getUIStyle)
    var expanded by rememberSaveable { mutableStateOf(false) }
    SortingOptionsForML(
        expanded = expanded, ci = ci, asc = asc,
        onReverseOrder = onReverseSongOrder,
        onToggle = { expanded = !expanded }, onDismiss = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text("Title") },
            onClick = { onUpdateSongOrder(SongOrder.Title) }
        )
        DropdownMenuItem(
            text = { Text("Artist") },
            onClick = { onUpdateSongOrder(SongOrder.Artist) }
        )
        DropdownMenuItem(
            text = { Text("Created Date") },
            onClick = { onUpdateSongOrder(SongOrder.CreatedOn) }
        )
    }
}

@Composable
fun AllPLsOptions(
    onReversePlsOrder: () -> Unit,
    onUpdatePlsOrder: (PlaylistOrder) -> Unit,
    getUIStyle: GetUIStyle,
    direction: Boolean
) {
    val ci = ContentIcons(getUIStyle)

    var expanded by rememberSaveable { mutableStateOf(false) }
    SortingOptionsForML(
        expanded = expanded, ci = ci, asc = direction,
        onReverseOrder = onReversePlsOrder,
        onToggle = { expanded = !expanded }, onDismiss = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text("Name") },
            onClick = { onUpdatePlsOrder(PlaylistOrder.Name) }
        )
        DropdownMenuItem(
            text = { Text("Created Date") },
            onClick = { onUpdatePlsOrder(PlaylistOrder.CreatedOn) }
        )
    }
}


@Composable
private fun EditSongOptions(
    hide: Boolean, onHideOrShow: () -> Unit, onAddSongs: () -> Unit, onShareSongs: () -> Unit,
    onUpdateMetadata: (String) -> Unit, onSelectAll: () -> Unit, onDeleteSongs: () -> Unit,
    getUIStyle: GetUIStyle, enabled: Boolean
) {
    val ci = ContentIcons(getUIStyle)
    var expanded by rememberSaveable { mutableStateOf(false) }
    val icon =
        if (hide) ImageVector.vectorResource(R.drawable.sharp_hide_source)
        else Icons.Default.CheckCircle
    val text = if (hide) "Hide" else "Show"
    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        IconButton(onClick = { expanded = !expanded }) {
            ci.ContentIcon(Icons.Default.MoreVert)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Add") },
                trailingIcon = { ci.ContentIcon(Icons.Default.Add) },
                onClick = onAddSongs
            )
            DropdownMenuItem(
                text = { Text(text) },
                trailingIcon = { ci.ContentIcon(icon) },
                onClick = onHideOrShow

            )
            DropdownMenuItem(
                text = { Text("Select All") },
                onClick = onSelectAll
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Share") },
                trailingIcon = { ci.ContentIcon(Icons.Default.Share) },
                onClick = onShareSongs
            )

            DropdownMenuItem(
                text = { Text("Delete") },
                trailingIcon = { ci.ContentIcon(Icons.Default.Delete) },
                onClick = onDeleteSongs
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Edit artist") },
                trailingIcon = { ci.ContentIcon(painterResource(R.drawable.rounded_person_edit)) },
                onClick = { onUpdateMetadata(TagProperty.ARTIST) },
                enabled = enabled
            )
            DropdownMenuItem(
                text = { Text("Edit album") },
                trailingIcon = { ci.ContentIcon(painterResource(R.drawable.baseline_album)) },
                onClick = { onUpdateMetadata(TagProperty.ALBUM) },
                enabled = enabled
            )
            DropdownMenuItem(
                text = { Text("Edit genre") },
                trailingIcon = { ci.ContentIcon(painterResource(R.drawable.rounded_genres)) },
                onClick = { onUpdateMetadata(TagProperty.GENRE) },
                enabled = enabled
            )
        }
    }
}

@Composable
private fun SortingOptionsForML(
    expanded: Boolean, ci: ContentIcons, asc: Boolean,
    onReverseOrder: () -> Unit, onToggle: () -> Unit, onDismiss: () -> Unit,
    content: @Composable (ColumnScope.() -> Unit)
) {
    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        IconButton(onClick = onToggle) {
            ci.ContentIcon(Icons.Default.MoreVert)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            Text(
                text = "Order by", modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                textAlign = TextAlign.Center
            )
            HorizontalDivider()
            DropdownMenuItem(
                onClick = onReverseOrder,
                text = { Text(if (asc) "Ascending" else "Descending") }
            )
            HorizontalDivider()
            content()
        }
    }
}

@Composable
fun RecentQueries(
    expanded: Boolean, querySet: Set<String>, updateQuery: (String) -> Unit,
    onToggle: () -> Unit, onDismiss: () -> Unit, ci: ContentIcons
) {
    val icon = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.TopEnd)
    ) {
        IconButton(onClick = onToggle) { ci.ContentIcon(icon) }
        DropdownMenu(
            expanded = expanded, onDismissRequest = onDismiss,
            modifier = Modifier
                .fillMaxWidth(0.85f)
        ) {
            if (querySet.isEmpty()) Text(
                text = "No Recent Queries",
                modifier = Modifier.padding(4.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            else querySet.forEachIndexed { index, it ->
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = { updateQuery(it) }
                )
                if (index != querySet.indices.last)
                    HorizontalDivider()
            }
        }
    }
}