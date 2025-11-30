package com.xandy.lite.views

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.xandy.lite.ui.functions.item.details.PlaylistRow
import com.xandy.lite.controllers.view.models.LocalMediaVM
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.Playlist
import com.xandy.lite.models.XCToast
import com.xandy.lite.models.ui.LocalMusicTabs
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.DeleteModal
import com.xandy.lite.ui.functions.LyricsListDialog
import com.xandy.lite.ui.functions.SongLazyColumn
import com.xandy.lite.ui.functions.item.details.AlbumBox
import com.xandy.lite.ui.functions.item.details.ArtistRow
import com.xandy.lite.ui.functions.item.details.FolderRow
import com.xandy.lite.ui.functions.item.details.GenreRow
import com.xandy.lite.ui.GetUIStyle
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalMusicView(
    modifier: Modifier, currentId: String, getUIStyle: GetUIStyle, onAdd: (String) -> Unit,
    onNavToArtist: (String) -> Unit, onNavToAlbum: (String) -> Unit, onNavToPl: (String) -> Unit,
    onNavToGenre: (String) -> Unit, onNavToFolder: (String, Long) -> Unit,
    onEdit: (String) -> Unit, onDelete: (AudioFile) -> Unit, localMediaVM: LocalMediaVM
) {
    val selectedTab by localMediaVM.tab.collectAsStateWithLifecycle()
    val isSelecting by localMediaVM.isSelecting.collectAsStateWithLifecycle()
    val isSearching by localMediaVM.isSearching.collectAsStateWithLifecycle()
    val selectedSongSet = localMediaVM.selectedSongIds.collectAsStateWithLifecycle().value.toSet()
    val alIsLoading by localMediaVM.localAudiosLoading.collectAsStateWithLifecycle()
    val query by localMediaVM.query.collectAsStateWithLifecycle()
    val library by localMediaVM.musicLibrary.collectAsStateWithLifecycle()
    val lyricsList by localMediaVM.lyricsList.collectAsStateWithLifecycle()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val selectedFolders by localMediaVM.selectedFolders.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showDialog by rememberSaveable { mutableStateOf(Pair(false, "")) }
    var enabled by rememberSaveable { mutableStateOf(true) }
    val tabs = listOf(
        "Library" to LocalMusicTabs.LIBRARY,
        "Playlists" to LocalMusicTabs.PLAYLIST,
        "Albums" to LocalMusicTabs.ALBUMS,
        "Artists" to LocalMusicTabs.ARTISTS,
        "Genres" to LocalMusicTabs.GENRES,
        "Folders" to LocalMusicTabs.FOLDERS,
        "Hidden" to LocalMusicTabs.HIDDEN
    )
    val toast = XCToast(context)
    val ci = ContentIcons(getUIStyle)
    val libraryState = rememberLazyListState()
    val hiddenState = rememberLazyListState()
    val albumState = rememberLazyListState()
    val artistState = rememberLazyListState()
    val genreState = rememberLazyListState()
    val onLongPress: (String) -> Unit = {
        if (isSelecting) {
            val limitReached = localMediaVM.toggleSong(it)
            if (limitReached) toast.makeMessage("Cannot select more than 2000 files.")
        } else localMediaVM.startSelecting(it)
    }
    val onClick: (AudioFile, List<AudioFile>) -> Unit = { audio, list ->
        if (!isSelecting) {
            localMediaVM.selectSong(audio, list)
        } else {
            val limitReached = localMediaVM.toggleSong(audio.id)
            if (limitReached) toast.makeMessage("Cannot select more than 2000 files.")
        }
    }
    var clicked by remember { mutableStateOf(false) }

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
                            .clickable(enabled = !clicked) { localMediaVM.updateTab(tab) }
                            .padding(horizontal = 4.dp)
                            .background(bgColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
        when (selectedTab) {
            LocalMusicTabs.LIBRARY -> {
                LazyColumnScrollbar(
                    state = libraryState,
                    settings = ScrollbarSettings(
                        thumbSelectedColor = getUIStyle.selectedThumbColor(),
                        thumbUnselectedColor = getUIStyle.unSelectedThumbColor()
                    )
                ) {
                    val filtered by localMediaVM.filteredAudioFiles.collectAsStateWithLifecycle()
                    SongLazyColumn(
                        list = filtered, getUIStyle = getUIStyle, hideAllowed = Pair(true, "Hide"),
                        isSelecting = isSelecting, enabled = !alIsLoading,
                        selectedSongSet = selectedSongSet, state = libraryState,
                        onClick = { audio ->
                            onClick(audio, library.audioUIState.list.map { it.song })
                        }, modifier = Modifier.fillMaxWidth(),
                        onEnqueue = {
                            val result = localMediaVM.addToQueue(listOf(it))
                            if (result) toast.makeMessage("Song already in queue")
                        },
                        onEdit = onEdit, onDelete = onDelete, onAdd = onAdd,
                        onLongPress = onLongPress,
                        onToggleHide = { uri ->
                            coroutineScope.launch {
                                val result = localMediaVM.onHideSong(uri)
                                if (!result) Toast.makeText(
                                    context, "Failed to hide audio", Toast.LENGTH_SHORT
                                ).show()
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
                            })

                    }
                }
                if (showDelete.first) {
                    DeleteModal(
                        onDismissRequest = { showDelete = Pair(false, null) },
                        onDelete = {
                            val pl = showDelete.second
                            if (pl != null)
                                coroutineScope.launch {
                                    val result = localMediaVM.deletePlaylist(pl)
                                    if (!result) Toast.makeText(
                                        context, "Failed to delete song", Toast.LENGTH_SHORT
                                    ).show()
                                    else showDelete = Pair(false, null)
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
                                            onNavToAlbum(album.name)
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
                                onNavToArtist(artist.name)
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
                                onNavToGenre(genre.name)
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
                                if (isSelecting) localMediaVM.toggleFolder(f.bucket)
                                else onNavToFolder(f.bucket.volumeName, f.bucket.id)
                            },
                            onLongPress = {
                                if (isSelecting) localMediaVM.toggleFolder(f.bucket)
                                else localMediaVM.startSelectingFolders(
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
                                audio.artist.contains(query, ignoreCase = true)
                    }
                    SongLazyColumn(
                        list = filtered, enabled = !alIsLoading, getUIStyle = getUIStyle,
                        onClick = { audio ->
                            onClick(audio, library.hiddenAudios)
                        },
                        onEnqueue = {
                            val result = localMediaVM.addToQueue(listOf(it))
                            if (result) toast.makeMessage("Song already in queue")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        onDelete = { audio -> onDelete(audio) },
                        onEdit = { onEdit(it) }, isSelecting = isSelecting,
                        onLongPress = onLongPress,
                        onToggleHide = { uri ->
                            coroutineScope.launch {
                                val result = localMediaVM.onShowSong(uri)
                                if (!result) Toast.makeText(
                                    context, "Failed to show audio", Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        currentId = currentId,
                        onUpsertLyrics = { showDialog = Pair(true, it) },
                        hideAllowed = Pair(true, "Show"), state = hiddenState,
                        onAdd = { onAdd(it) }, selectedSongSet = selectedSongSet,
                    )
                }
            }
        }
        LyricsListDialog(
            showDialog = showDialog.first, onDismiss = { showDialog = Pair(false, "") },
            getUIStyle = getUIStyle, list = lyricsList, enabled = enabled,
            onSubmit = { lyricsId ->
                coroutineScope.launch {
                    val songId = showDialog.second
                    if (songId.isBlank()) {
                        Toast.makeText(context, "Null song", Toast.LENGTH_SHORT).show()
                        showDialog = Pair(false, "")
                        return@launch
                    }
                    enabled = false
                    val result =
                        localMediaVM.updateSongLyrics(lyricsId = lyricsId, songUri = songId)
                    if (!result)
                        Toast.makeText(
                            context, "Failed to add lyrics to $songId", Toast.LENGTH_SHORT
                        ).show()
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
            Text("This app needs permission to read your media files.")
            Spacer(Modifier.height(8.dp))
            Button(onClick = { readAudioState.launchPermissionRequest() }) {
                Text("Grant")
            }
        }
    }
}

