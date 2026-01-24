package com.xandy.lite.views

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.xandy.lite.R
import com.xandy.lite.ui.functions.item.details.PlaylistRow
import com.xandy.lite.controllers.view.models.LocalMediaVM
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.Playlist
import com.xandy.lite.db.tables.firstId
import com.xandy.lite.models.AudioDialog
import com.xandy.lite.models.XCToast
import com.xandy.lite.models.ui.InsertResult
import com.xandy.lite.models.ui.LocalMusicTabs
import com.xandy.lite.models.ui.MediaState
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.DeleteModal
import com.xandy.lite.ui.functions.LyricsListDialog
import com.xandy.lite.ui.functions.SongLazyColumn
import com.xandy.lite.ui.functions.item.details.AlbumBox
import com.xandy.lite.ui.functions.item.details.ArtistRow
import com.xandy.lite.ui.functions.item.details.FolderRow
import com.xandy.lite.ui.functions.item.details.GenreRow
import com.xandy.lite.ui.GetUIStyle
import com.xandy.lite.ui.functions.AudioDetailsDialog
import com.xandy.lite.ui.functions.ChangePlNameDialog
import com.xandy.lite.ui.functions.item.details.SongRow
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalMusicView(
    modifier: Modifier,
    currentId: String,
    getUIStyle: GetUIStyle,
    onAdd: (String) -> Unit,
    onNavToArtist: (MediaState) -> Unit,
    onNavToAlbum: (MediaState) -> Unit,
    onNavToPl: (String) -> Unit,
    onNavToGenre: (MediaState) -> Unit,
    onNavToFolder: (String, Long) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (AudioFile) -> Unit,
    vm: LocalMediaVM
) {
    val selectedTab by vm.tab.collectAsStateWithLifecycle()
    val isSelecting by vm.isSelecting.collectAsStateWithLifecycle()
    val isSearching by vm.isSearching.collectAsStateWithLifecycle()
    val selectedSongSet = vm.selectedSongIds.collectAsStateWithLifecycle().value.toSet()
    val alIsLoading by vm.localAudiosLoading.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val library by vm.musicLibrary.collectAsStateWithLifecycle()
    val lyricsList by vm.lyricsList.collectAsStateWithLifecycle()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val selectedFolders by vm.selectedFolders.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showDialog by rememberSaveable { mutableStateOf(Pair(false, "")) }
    var enabled by rememberSaveable { mutableStateOf(true) }
    var afDetails by rememberSaveable { mutableStateOf(AudioDialog()) }
    val tabs = listOf(
        stringResource(R.string.Favorites) to LocalMusicTabs.FAVORITES,
        stringResource(R.string.Library) to LocalMusicTabs.LIBRARY,
        stringResource(R.string.Playlists) to LocalMusicTabs.PLAYLIST,
        stringResource(R.string.Albums) to LocalMusicTabs.ALBUMS,
        stringResource(R.string.Artists) to LocalMusicTabs.ARTISTS,
        stringResource(R.string.Genres) to LocalMusicTabs.GENRES,
        stringResource(R.string.Folders) to LocalMusicTabs.FOLDERS,
        stringResource(R.string.Hidden) to LocalMusicTabs.HIDDEN
    )
    val toast = XCToast(context)
    val ci = ContentIcons(getUIStyle)
    val favoriteState = rememberLazyListState()
    val libraryState = rememberLazyListState()
    val hiddenState = rememberLazyListState()
    val albumState = rememberLazyListState()
    val artistState = rememberLazyListState()
    val genreState = rememberLazyListState()
    val onLongPress: (String) -> Unit = {
        if (isSelecting) {
            val limitReached = vm.toggleSong(it)
            if (limitReached) toast.makeMessage(toast.unableToGet2kPlusFiles)
        } else vm.startSelecting(it)
    }
    val onClick: (AudioFile, List<AudioFile>) -> Unit = { audio, list ->
        if (!isSelecting) {
            vm.selectSong(audio, list)
        } else {
            val limitReached = vm.toggleSong(audio.id)
            if (limitReached) toast.makeMessage(toast.unableToGet2kPlusFiles)
        }
    }
    var clicked by remember { mutableStateOf(false) }
    val appStrings by vm.appStrings.collectAsStateWithLifecycle()

    MediaPermissionView(modifier.padding(top = 4.dp)) {
        if (!isSelecting && !isSearching) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                tabs.forEach { (label, tab) ->
                    val isSelected = tab == selectedTab
                    val bgColor = getUIStyle.tabColor(isSelected)
                    val textColor = getUIStyle.tabTextColor(isSelected)
                    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    Text(
                        text = label, color = textColor, fontWeight = fontWeight,
                        modifier = Modifier
                            .clickable(enabled = !clicked) { vm.updateTab(tab) }
                            .padding(horizontal = 4.dp)
                            .background(bgColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
        when (selectedTab) {
            LocalMusicTabs.FAVORITES -> {
                LazyColumnScrollbar(
                    state = favoriteState,
                    settings = ScrollbarSettings(
                        thumbSelectedColor = getUIStyle.selectedThumbColor(),
                        thumbUnselectedColor = getUIStyle.unSelectedThumbColor()
                    )
                ) {
                    val filtered = library.favorites.filter {
                        if (query.isBlank() || !isSearching) return@filter true
                        it.title.contains(query, ignoreCase = true) ||
                                it.artist?.contains(query, ignoreCase = true) ?: false
                    }

                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(filtered, key = { it.id }) { audio ->
                            val isPicked = audio.id == currentId
                            val selected = audio.id in selectedSongSet

                            SongRow(
                                song = audio, appStrings,
                                getUIStyle = getUIStyle, context = context,
                                isSelecting = isSelecting, enabled = !alIsLoading,
                                onClick = {
                                    onClick(audio, library.audioUIState.list.map { it.song })
                                }, isSelected = selected, isPickedSong = isPicked,
                                onLongPress = { onLongPress(audio.id) },
                                onAdd = { onAdd(audio.id) },
                                onEnqueue = {
                                    val result = vm.addToQueue(listOf(audio))
                                    if (result) toast.makeMessage(toast.trackAlreadyInQueue)
                                    else toast.makeMessage(toast.trackAddedToQueue)
                                },
                                onAddNext = {
                                    val result = vm.playNext(audio)
                                    when (result) {
                                        InsertResult.Exists ->
                                            toast.makeMessage(toast.trackAlreadyInPlayNext)

                                        InsertResult.Failure ->
                                            toast.makeMessage(toast.failedToAddTrackNullMC)

                                        InsertResult.Success ->
                                            toast.makeMessage(toast.trackAddedToPlayNext)
                                    }
                                },
                                onEdit = { onEdit(audio.uri.toString()) },
                                onRemoveFavorite = {
                                    coroutineScope.launch {
                                        val result = vm.onUnfavoriteSong(audio.uri)
                                        if (!result)
                                            toast.makeMessage(toast.failedToUnfavoriteTrack)
                                    }
                                },
                                onDelete = { onDelete(audio) },
                                onUpsertLyrics = { showDialog = Pair(true, audio.uri.toString()) }
                            )

                        }
                    }
                }
            }

            LocalMusicTabs.LIBRARY -> {
                LazyColumnScrollbar(
                    state = libraryState,
                    settings = ScrollbarSettings(
                        thumbSelectedColor = getUIStyle.selectedThumbColor(),
                        thumbUnselectedColor = getUIStyle.unSelectedThumbColor()
                    )
                ) {
                    val filtered by vm.filteredAudioFiles.collectAsStateWithLifecycle()
                    SongLazyColumn(
                        list = filtered, getUIStyle = getUIStyle,
                        hideAllowed = Pair(true, stringResource(R.string.Hide)),
                        isSelecting = isSelecting, enabled = !alIsLoading,
                        appStrings = appStrings, selectedSongSet = selectedSongSet,
                        state = libraryState,
                        onClick = { audio ->
                            onClick(audio, library.audioUIState.list.map { it.song })
                        }, modifier = Modifier.fillMaxWidth(),
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

                                InsertResult.Success ->
                                    toast.makeMessage(toast.trackAddedToPlayNext)
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
                        onEdit = onEdit, onDelete = onDelete, onAdd = onAdd,
                        onLongPress = onLongPress,
                        onToggleHide = { uri ->
                            coroutineScope.launch {
                                val result = vm.onHideSong(uri)
                                if (!result) toast.makeMessage(toast.failedToHideTrack)
                            }
                        }, currentId = currentId,
                        onUpsertLyrics = { showDialog = Pair(true, it) }
                    )
                }

            }

            LocalMusicTabs.PLAYLIST -> {
                var showDelete by rememberSaveable {
                    mutableStateOf<Pair<Boolean, Playlist?>>(Pair(false, null))
                }
                var showDialog by rememberSaveable {
                    mutableStateOf<Pair<Boolean, Playlist?>>(Pair(false, null))
                }
                var dismissEnabled by rememberSaveable { mutableStateOf(true) }
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(
                        library.plsWithAudios.list,
                        key = { it.playlist.name }
                    ) { pl ->
                        PlaylistRow(
                            pl = pl, getUIStyle, ci = ci,
                            onDelete = { showDelete = Pair(true, pl.playlist) },
                            onClick = {
                                if (clicked) return@PlaylistRow
                                clicked = true
                                onNavToPl(pl.playlist.id)
                                clicked = false
                            },
                            onChangeName = { showDialog = Pair(true, pl.playlist) }
                        )

                    }
                }
                ChangePlNameDialog(
                    showDialog = showDialog.first, originalName = showDialog.second?.name,
                    onDismiss = { if (dismissEnabled) showDialog = Pair(false, null) },
                    onSubmit = { newName ->
                        coroutineScope.launch {
                            val pl = showDialog.second
                            if (pl == null) {
                                toast.makeMessage(toast.undefinedPlaylist)
                                return@launch
                            }
                            dismissEnabled = false
                            val result = vm.changePlName(newName, pl.name, pl.id)
                            when (result) {
                                InsertResult.Exists -> toast.makeMessage(toast.nameAlreadyExists)
                                InsertResult.Failure -> toast.makeMessage(toast.failedToAddPl)
                                InsertResult.Success -> showDialog = Pair(false, null)
                            }
                            dismissEnabled = true
                        }
                    },
                    enabled = dismissEnabled, getUIStyle = getUIStyle,
                )
                if (showDelete.first) {
                    DeleteModal(
                        onDismissRequest = { showDelete = Pair(false, null) },
                        onDelete = {
                            val pl = showDelete.second
                            if (pl != null)
                                coroutineScope.launch {
                                    dismissEnabled = false
                                    val result = vm.deletePlaylist(pl)
                                    if (!result) toast.makeMessage(toast.failedToDeleteTrack)
                                    else showDelete = Pair(false, null)
                                    dismissEnabled = true
                                }
                        }, string = showDelete.second?.name
                    )
                }
            }

            LocalMusicTabs.ALBUMS -> {
                LazyColumnScrollbar(
                    state = albumState,
                    settings = ScrollbarSettings(
                        thumbSelectedColor = getUIStyle.selectedThumbColor(),
                        thumbUnselectedColor = getUIStyle.unSelectedThumbColor()
                    )
                ) {
                    val filtered = library.albums.filter { album ->
                        if (query.isBlank() || !isSearching) return@filter true
                        album.name.contains(query, ignoreCase = true) ||
                                album.artist.contains(query, ignoreCase = true)
                    }
                    val rows = filtered.chunked(if (isLandscape) 5 else 2)

                    LazyColumn(modifier = Modifier.fillMaxWidth(), state = albumState) {
                        items(rows) { rowItems ->
                            Row(modifier = Modifier.fillMaxSize()) {
                                rowItems.forEach { album ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.85f)
                                            .padding(4.dp)
                                    ) {
                                        AlbumBox(album, getUIStyle) {
                                            if (clicked) return@AlbumBox
                                            clicked = true
                                            onNavToAlbum(
                                                MediaState(album.name, album.songs.firstId())
                                            )

                                            clicked = false
                                        }
                                    }
                                }

                                if (rowItems.size == 1 && !isLandscape) {
                                    Spacer(modifier = Modifier.weight(1f))
                                } else if (isLandscape) when (rowItems.size) {
                                    1 -> Spacer(modifier = Modifier.weight(4f))
                                    2 -> Spacer(modifier = Modifier.weight(3f))
                                    3 -> Spacer(modifier = Modifier.weight(2f))
                                    4 -> Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            LocalMusicTabs.ARTISTS -> {
                LazyColumnScrollbar(
                    state = artistState,
                    settings = ScrollbarSettings(
                        thumbSelectedColor = getUIStyle.selectedThumbColor(),
                        thumbUnselectedColor = getUIStyle.unSelectedThumbColor()
                    )
                ) {
                    val filtered = library.artists.filter { artist ->
                        if (query.isBlank() || !isSearching) return@filter true
                        artist.name.contains(query, ignoreCase = true)
                    }
                    LazyColumn(modifier = Modifier.fillMaxWidth(), state = artistState) {
                        items(filtered) { artist ->
                            ArtistRow(artist, getUIStyle, onClick = {
                                if (clicked) return@ArtistRow
                                clicked = true
                                onNavToArtist(
                                    MediaState(artist.name, artist.songs.firstId())
                                )
                                clicked = false
                            })
                        }
                    }
                }
            }

            LocalMusicTabs.GENRES -> {
                LazyColumnScrollbar(
                    state = genreState,
                    settings = ScrollbarSettings(
                        thumbSelectedColor = getUIStyle.selectedThumbColor(),
                        thumbUnselectedColor = getUIStyle.unSelectedThumbColor()
                    )
                ) {
                    val filtered = library.genres.filter { genre ->
                        if (query.isBlank() || !isSearching) return@filter true
                        genre.name.contains(query, ignoreCase = true)
                    }
                    LazyColumn(modifier = Modifier.fillMaxWidth(), state = genreState) {
                        items(filtered) { genre ->
                            GenreRow(genre, getUIStyle, onClick = {
                                if (clicked) return@GenreRow
                                clicked = true
                                onNavToGenre(
                                    MediaState(genre.name, genre.songs.firstId())
                                )
                                clicked = false
                            })
                        }
                    }
                }
            }

            LocalMusicTabs.FOLDERS -> {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(library.folders, key = { (it.bucket.volumeName to it.bucket.id) }) { f ->
                        val pairedId = Pair(f.bucket.volumeName, f.bucket.id)
                        val isSelected = pairedId in selectedFolders
                        FolderRow(
                            folder = f.bucket, getUIStyle = getUIStyle, ci = ci,
                            onClick = {
                                if (isSelecting) vm.toggleFolder(f.bucket)
                                else onNavToFolder(f.bucket.volumeName, f.bucket.id)
                            },
                            onLongPress = {
                                if (isSelecting) vm.toggleFolder(f.bucket)
                                else vm.startSelectingFolders(
                                    Pair(f.bucket.volumeName, f.bucket.id)
                                )
                            },
                            isSelecting = isSelecting,
                            isSelected = isSelected
                        )
                    }
                }
            }

            LocalMusicTabs.HIDDEN -> {
                LazyColumnScrollbar(
                    state = hiddenState,
                    settings = ScrollbarSettings(
                        thumbSelectedColor = getUIStyle.selectedThumbColor(),
                        thumbUnselectedColor = getUIStyle.unSelectedThumbColor()
                    )
                ) {
                    val filtered = library.hiddenAudios.filter { audio ->
                        if (query.isBlank() || !isSearching) return@filter true
                        audio.title.contains(query, ignoreCase = true) ||
                                audio.artist?.contains(query, ignoreCase = true) ?: false
                    }
                    SongLazyColumn(
                        list = filtered, enabled = !alIsLoading, getUIStyle = getUIStyle,
                        onClick = { audio ->
                            onClick(audio, library.hiddenAudios)
                        },
                        appStrings = appStrings,
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

                                InsertResult.Success ->
                                    toast.makeMessage(toast.trackAddedToPlayNext)
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
                        modifier = Modifier.fillMaxWidth(),
                        onDelete = { audio -> onDelete(audio) },
                        onEdit = { onEdit(it) }, isSelecting = isSelecting,
                        onLongPress = onLongPress,
                        onToggleHide = { uri ->
                            coroutineScope.launch {
                                val result = vm.onShowSong(uri)
                                if (!result) toast.makeMessage(toast.failedToShowTrack)
                            }
                        },
                        currentId = currentId, state = hiddenState,
                        onUpsertLyrics = { showDialog = Pair(true, it) },
                        hideAllowed = Pair(true, stringResource(R.string.Show)),
                        onAdd = { onAdd(it) }, selectedSongSet = selectedSongSet,
                    )
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
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MediaPermissionView(modifier: Modifier, content: @Composable () -> Unit) {
    val readAudioState = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
    )

    LaunchedEffect(readAudioState.status) {
        if (!readAudioState.status.isGranted) readAudioState.launchPermissionRequest()
    }

    if (readAudioState.status.isGranted) {
        Column(
            modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            content()
        }
    } else {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.PERMISSION_TO_READ_FILES))
            Spacer(Modifier.height(8.dp))
            Button(onClick = { readAudioState.launchPermissionRequest() }) {
                Text(stringResource(R.string.Grant))
            }
        }
    }
}

