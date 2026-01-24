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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.taglib.TagProperty
import com.xandy.lite.R
import com.xandy.lite.controllers.view.models.LibraryBasedRouteVM
import com.xandy.lite.models.application.AppVMProvider
import com.xandy.lite.models.ui.IsDefaultMediaOrder
import com.xandy.lite.models.ui.LocalMusicTabs
import com.xandy.lite.models.ui.order.by.AlbumOrder
import com.xandy.lite.models.ui.order.by.ArtistOrder
import com.xandy.lite.models.ui.order.by.GenreOrder
import com.xandy.lite.models.ui.order.by.PlaylistOrder
import com.xandy.lite.models.ui.order.by.SongOrder
import com.xandy.lite.navigation.NavViewModel
import com.xandy.lite.ui.GetUIStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch


/**
 *  @param onReverseSongOrder Reverse song order for: false to library tracks, true to hidden tracks
 *  @param onUpdateSongOrder Update song order for: false to library tracks, true to hidden tracks
 */
@Composable
fun LocalAudioOptions(
    navVM: NavViewModel, defaultMediaDir: IsDefaultMediaOrder, onRefresh: () -> Unit,
    onReverseSongOrder: (Boolean) -> Unit, onUpdateSongOrder: (SongOrder, Boolean) -> Unit,
    onReversePlsOrder: () -> Unit, onUpdatePlsOrder: (PlaylistOrder) -> Unit,
    onReverseAlbumOrder: () -> Unit, onUpdateAlbumOrder: (AlbumOrder) -> Unit,
    onReverseArtistOrder: () -> Unit, onUpdateArtistOrder: (ArtistOrder) -> Unit,
    onReverseGenreOrder: () -> Unit, onUpdateGenreOrder: (GenreOrder) -> Unit,
    onSearch: () -> Unit, onHideAudios: () -> Unit,
    onHideFolders: () -> Unit, onAddSongs: () -> Unit, onShareSongs: () -> Unit,
    onUpdateMetadata: (String) -> Unit, onShowAudios: () -> Unit,
    onSelectAll: () -> Unit, onDeleteSongs: () -> Unit, getUIStyle: GetUIStyle
) {
    val ci = ContentIcons(getUIStyle)

    val isNotEmpty by navVM.listNotEmpty.collectAsStateWithLifecycle()
    val audioStates by navVM.audioStates.collectAsStateWithLifecycle()

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
        LocalMusicTabs.FAVORITES -> {
            if (!audioStates.isSelecting) AllSongOptions(
                onReverseSongOrder = { navVM.reverseFavoriteOrder() },
                onUpdateSongOrder = { navVM.updateFavoriteOrder(it) },
                getUIStyle = getUIStyle, asc = audioStates.favDirections
            )
            else EditSongOptions(
                onHideOrShow = onHideAudios, onAddSongs = onAddSongs, onShareSongs = onShareSongs,
                getUIStyle = getUIStyle, hide = true, onUpdateMetadata = onUpdateMetadata,
                onSelectAll = onSelectAll, onDeleteSongs = onDeleteSongs,
                enabled = !audioStates.isLoading && isNotEmpty, showMiscOptions = true
            )
        }

        LocalMusicTabs.LIBRARY -> {
            if (!audioStates.isSelecting) AllSongOptions(
                onReverseSongOrder = { onReverseSongOrder(false) },
                onUpdateSongOrder = { onUpdateSongOrder(it, false) },
                getUIStyle = getUIStyle, asc = audioStates.alDirection
            )
            else EditSongOptions(
                onHideOrShow = onHideAudios, onAddSongs = onAddSongs, onShareSongs = onShareSongs,
                getUIStyle = getUIStyle, hide = true, onUpdateMetadata = onUpdateMetadata,
                onSelectAll = onSelectAll, onDeleteSongs = onDeleteSongs,
                enabled = !audioStates.isLoading && isNotEmpty, showMiscOptions = true
            )

        }

        LocalMusicTabs.PLAYLIST -> AllPLsOptions(
            onReversePlsOrder = onReversePlsOrder,
            onUpdatePlsOrder = onUpdatePlsOrder,
            getUIStyle = getUIStyle, direction = audioStates.plsDirection
        )

        LocalMusicTabs.ALBUMS -> AlbumOptions(
            onReverseAlbumOrder = onReverseAlbumOrder, onUpdateAlbumOrder = onUpdateAlbumOrder,
            isDefault = defaultMediaDir.album,
            getUIStyle = getUIStyle, direction = audioStates.albumDirection
        )

        LocalMusicTabs.ARTISTS -> ArtistOptions(
            onReverseArtistOrder = onReverseArtistOrder, onUpdateArtistOrder = onUpdateArtistOrder,
            isDefault = defaultMediaDir.artist,
            getUIStyle = getUIStyle, direction = audioStates.artistDirection
        )

        LocalMusicTabs.GENRES -> GenreOptions(
            onReverseGenreOrder = onReverseGenreOrder, onUpdateGenreOrder = onUpdateGenreOrder,
            isDefault = defaultMediaDir.genre,
            getUIStyle = getUIStyle, direction = audioStates.genreDirection
        )

        LocalMusicTabs.FOLDERS -> {
            if (audioStates.isSelecting) Text(
                text = stringResource(R.string.Hide),
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
                onDeleteSongs = onDeleteSongs, enabled = !audioStates.isLoading && isNotEmpty,
                showMiscOptions = true
            ) else AllSongOptions(
                onReverseSongOrder = { onReverseSongOrder(true) },
                onUpdateSongOrder = { onUpdateSongOrder(it, true) },
                getUIStyle = getUIStyle, asc = audioStates.hiddenDirection
            )
        }
    }
}

@Composable
fun LibraryRouteOptions(
    routeFlow: Flow<String>,
    onSearch: () -> Unit, onShareSongs: () -> Unit,
    onAddSongs: () -> Unit, onDeleteSongs: () -> Unit,
    getUIStyle: GetUIStyle
) {
    val lbrVM: LibraryBasedRouteVM = viewModel(factory = AppVMProvider.Factory)
    val ci = ContentIcons(getUIStyle)
    val isNotEmpty by lbrVM.listNotEmpty.collectAsStateWithLifecycle()
    val uiStates by lbrVM.uiStates.collectAsStateWithLifecycle()
    val list by lbrVM.getAudioFiles(routeFlow).collectAsStateWithLifecycle()
    val onSelectAll: () -> Unit = { lbrVM.selectAll(list) }
    if (!uiStates.isSearching)
        SearchIconButton(ci) { onSearch() }
    if (uiStates.isSelecting) EditSongOptions(
        onHideOrShow = { }, onAddSongs = onAddSongs, onShareSongs = onShareSongs,
        getUIStyle = getUIStyle, hide = true, onUpdateMetadata = {},
        onSelectAll = onSelectAll, onDeleteSongs = onDeleteSongs,
        enabled = !uiStates.isLoading && isNotEmpty, showMiscOptions = false
    )
}

@Composable
fun PlayListOptions(
    navVM: NavViewModel, getUIStyle: GetUIStyle, onShareSongs: () -> Unit,
    onChangeArt: () -> Unit, onSearch: () -> Unit, onChangeName: () -> Unit
) {
    val ci = ContentIcons(getUIStyle)
    val coroutineScope = rememberCoroutineScope()
    var expanded by rememberSaveable { mutableStateOf(false) }
    val plWithAudio by navVM.plWithAudio.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isSelecting by navVM.isSelecting.collectAsStateWithLifecycle()
    val isAdding by navVM.isAdding.collectAsStateWithLifecycle()
    val isNotEmpty by navVM.listNotEmpty.collectAsStateWithLifecycle()
    val failedToAddTracks = stringResource(R.string.failed_to_add_tracks)
    val failedToRemoveTracks = stringResource(R.string.failed_to_delete_tracks)
    PLContent(
        ci, expanded, { expanded = it }, onSearch = onSearch,
        onSubmit = {
            coroutineScope.launch {
                result(
                    id = plWithAudio?.playlist?.id,
                    onResult = { navVM.addLocalSongsToPL(navVM.getSelectedSongIds(), it) },
                    onEndSelect = { navVM.endSelect() },
                    context = context, failureText = failedToAddTracks
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
        onChangeArt = onChangeArt, onChangeName = onChangeName, onShareSongs = onShareSongs,
        enabled = isNotEmpty
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
    onAdd: () -> Unit, onShareSongs: () -> Unit, isAdding: Boolean, isSelecting: Boolean,
    enabled: Boolean
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
            ) { ci.ContentIcon(Icons.Default.MoreVert) }
            DropdownMenu(expanded = expanded, onDismissRequest = { onExpanded(false) }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.Remove)) },
                    trailingIcon = {
                        ci.ContentIcon(R.drawable.baseline_remove_circle, enabled = enabled)
                    },
                    onClick = onRemove,
                    enabled = enabled
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.Share)) },
                    trailingIcon = {
                        ci.ContentIcon(Icons.Default.Share, enabled = enabled)
                    },
                    onClick = onShareSongs,
                    enabled = enabled
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
                    text = { Text(stringResource(R.string.Rename)) },
                    onClick = onChangeName
                )
                DropdownMenuItem(
                    text = { Text("Change cover image") },
                    trailingIcon = { ci.ContentIcon(R.drawable.outline_replace_image) },
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
            text = { Text(stringResource(R.string.Title)) },
            onClick = { onUpdateSongOrder(SongOrder.Title) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Artist)) },
            onClick = { onUpdateSongOrder(SongOrder.Artist) }
        )
        DropdownMenuItem(
            text = { Text("Created Date") },
            onClick = { onUpdateSongOrder(SongOrder.CreatedOn) }
        )
    }
}

@Composable
private fun AllPLsOptions(
    onReversePlsOrder: () -> Unit, onUpdatePlsOrder: (PlaylistOrder) -> Unit,
    getUIStyle: GetUIStyle, direction: Boolean
) {
    val ci = ContentIcons(getUIStyle)

    var expanded by rememberSaveable { mutableStateOf(false) }
    SortingOptionsForML(
        expanded = expanded, ci = ci, asc = direction,
        onReverseOrder = onReversePlsOrder,
        onToggle = { expanded = !expanded }, onDismiss = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Name)) },
            onClick = { onUpdatePlsOrder(PlaylistOrder.Name) }
        )
        DropdownMenuItem(
            text = { Text("Created Date") },
            onClick = { onUpdatePlsOrder(PlaylistOrder.CreatedOn) }
        )
    }
}

@Composable
private fun AlbumOptions(
    onReverseAlbumOrder: () -> Unit, onUpdateAlbumOrder: (AlbumOrder) -> Unit,
    getUIStyle: GetUIStyle, direction: Boolean, isDefault: Boolean
) {
    val ci = ContentIcons(getUIStyle)
    var expanded by rememberSaveable { mutableStateOf(false) }

    SortingOptionsForML(
        expanded = expanded, ci = ci, asc = direction,
        onReverseOrder = onReverseAlbumOrder, isDefault = isDefault,
        onToggle = { expanded = !expanded }, onDismiss = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.default_language)) },
            onClick = { onUpdateAlbumOrder(AlbumOrder.Default) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Name)) },
            onClick = { onUpdateAlbumOrder(AlbumOrder.Name) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Artist)) },
            onClick = { onUpdateAlbumOrder(AlbumOrder.Artist) }
        )
        DropdownMenuItem(
            text = { Text("Track Count") },
            onClick = { onUpdateAlbumOrder(AlbumOrder.TrackCount) }
        )

    }
}

@Composable
private fun ArtistOptions(
    onReverseArtistOrder: () -> Unit, onUpdateArtistOrder: (ArtistOrder) -> Unit,
    getUIStyle: GetUIStyle, direction: Boolean, isDefault: Boolean
) {
    val ci = ContentIcons(getUIStyle)
    var expanded by rememberSaveable { mutableStateOf(false) }

    SortingOptionsForML(
        expanded = expanded, ci = ci, asc = direction,
        onReverseOrder = onReverseArtistOrder, isDefault = isDefault,
        onToggle = { expanded = !expanded }, onDismiss = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.default_language)) },
            onClick = { onUpdateArtistOrder(ArtistOrder.Default) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Name)) },
            onClick = { onUpdateArtistOrder(ArtistOrder.Name) }
        )
        DropdownMenuItem(
            text = { Text("Track Count") },
            onClick = { onUpdateArtistOrder(ArtistOrder.TrackCount) }
        )
        DropdownMenuItem(
            text = { Text("Album Count") },
            onClick = { onUpdateArtistOrder(ArtistOrder.AlbumCount) }
        )

    }
}

@Composable
private fun GenreOptions(
    onReverseGenreOrder: () -> Unit, onUpdateGenreOrder: (GenreOrder) -> Unit,
    getUIStyle: GetUIStyle, direction: Boolean, isDefault: Boolean
) {
    val ci = ContentIcons(getUIStyle)
    var expanded by rememberSaveable { mutableStateOf(false) }

    SortingOptionsForML(
        expanded = expanded, ci = ci, asc = direction,
        onReverseOrder = onReverseGenreOrder, isDefault = isDefault,
        onToggle = { expanded = !expanded }, onDismiss = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.default_language)) },
            onClick = { onUpdateGenreOrder(GenreOrder.Default) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.Name)) },
            onClick = { onUpdateGenreOrder(GenreOrder.Name) }
        )
        DropdownMenuItem(
            text = { Text("Track Count") },
            onClick = { onUpdateGenreOrder(GenreOrder.TrackCount) }
        )

    }
}

@Composable
private fun EditSongOptions(
    hide: Boolean, onHideOrShow: () -> Unit, onAddSongs: () -> Unit, onShareSongs: () -> Unit,
    onUpdateMetadata: (String) -> Unit, onSelectAll: () -> Unit, onDeleteSongs: () -> Unit,
    getUIStyle: GetUIStyle, enabled: Boolean, showMiscOptions: Boolean
) {
    val ci = ContentIcons(getUIStyle)
    var expanded by rememberSaveable { mutableStateOf(false) }
    val icon =
        if (hide) ImageVector.vectorResource(R.drawable.sharp_hide_source)
        else Icons.Default.CheckCircle
    val text = if (hide) stringResource(R.string.Hide) else stringResource(R.string.Show)
    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        IconButton(onClick = { expanded = !expanded }) {
            ci.ContentIcon(Icons.Default.MoreVert)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.Add)) },
                trailingIcon = { ci.ContentIcon(Icons.Default.Add, enabled = enabled) },
                onClick = onAddSongs, enabled = enabled
            )
            if (showMiscOptions) {
                DropdownMenuItem(
                    text = { Text(text) },
                    trailingIcon = { ci.ContentIcon(icon, enabled = enabled) },
                    onClick = onHideOrShow, enabled = enabled
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.select_all)) },
                onClick = onSelectAll
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.Share)) },
                trailingIcon = { ci.ContentIcon(Icons.Default.Share, enabled = enabled) },
                onClick = onShareSongs, enabled = enabled
            )

            DropdownMenuItem(
                text = { Text(stringResource(R.string.Delete)) },
                trailingIcon = { ci.ContentIcon(Icons.Default.Delete, enabled = enabled) },
                onClick = onDeleteSongs, enabled = enabled
            )
            if (showMiscOptions) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_artist)) },
                    trailingIcon = {
                        ci.ContentIcon(R.drawable.rounded_person_edit, enabled = enabled)
                    },
                    onClick = { onUpdateMetadata(TagProperty.ARTIST) },
                    enabled = enabled
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_album)) },
                    trailingIcon = { ci.ContentIcon(R.drawable.baseline_album, enabled = enabled) },
                    onClick = { onUpdateMetadata(TagProperty.ALBUM) },
                    enabled = enabled
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_genre)) },
                    trailingIcon = { ci.ContentIcon(R.drawable.rounded_genres, enabled = enabled) },
                    onClick = { onUpdateMetadata(TagProperty.GENRE) },
                    enabled = enabled
                )
            }
        }
    }
}

@Composable
private fun SortingOptionsForML(
    expanded: Boolean, ci: ContentIcons, asc: Boolean,
    onReverseOrder: () -> Unit, onToggle: () -> Unit, onDismiss: () -> Unit,
    isDefault: Boolean = false, content: @Composable (ColumnScope.() -> Unit)
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
            if (!isDefault) {
                DropdownMenuItem(
                    onClick = onReverseOrder,
                    text = { Text(if (asc) "Ascending" else "Descending") }
                )
                HorizontalDivider()
            }
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
            properties = PopupProperties(dismissOnClickOutside = false),
            modifier = Modifier
                .fillMaxWidth(0.85f)
        ) {
            if (querySet.isEmpty()) Text(
                text = stringResource(R.string.no_recent_queries),
                modifier = Modifier.padding(4.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            else querySet.forEachIndexed { index, it ->
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = { updateQuery(it); onDismiss() }
                )
                if (index != querySet.indices.last)
                    HorizontalDivider()
            }
        }
    }
}