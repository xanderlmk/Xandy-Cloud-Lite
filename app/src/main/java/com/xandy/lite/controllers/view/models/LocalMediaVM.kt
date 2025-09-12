package com.xandy.lite.controllers.view.models

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xandy.lite.controllers.setQueue
import com.xandy.lite.db.song.repo.SongRepository
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.Bucket
import com.xandy.lite.db.tables.Playlist
import com.xandy.lite.models.ui.LocalMediaStates
import com.xandy.lite.models.ui.LocalMusicTabs
import com.xandy.lite.navigation.UIRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocalMediaVM(
    private val songRepository: SongRepository, private val uiRepository: UIRepository
) : ViewModel() {
    companion object {
        private const val TIMEOUT_MILLIS = 4_000L
    }

    val folders = songRepository.bucketsWithAudio.stateIn(
        scope = viewModelScope, started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
    private val mediaTypes = combine(
        songRepository.localAlbums, songRepository.localArtists, songRepository.localGenres
    ) { albums, artists, genres ->
        Triple(albums, artists, genres)
    }

    val musicLibrary = combine(
        songRepository.audioFiles, songRepository.bucketsWithAudio, songRepository.localPlaylists,
        songRepository.hiddenAudio, mediaTypes
    ) { al, bwa, pl, ha, mt ->
        LocalMediaStates(
            audioUIState = al, plsWithAudios = pl, hiddenAudios = ha, albums = mt.first,
            artists = mt.second, genres = mt.third, folders = bwa
        )
    }.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = LocalMediaStates()
    )
    val mediaController = songRepository.mediaController
    val tab = songRepository.localTab.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = LocalMusicTabs.LIBRARY
    )
    val selectedFolders = uiRepository.selectedBuckets
    val localAudiosLoading = songRepository.filesLoading
    fun startSelectingFolders(pairedId: Pair<String, Long>) =
        uiRepository.startSelectingFolders(folders.value.map { it.bucket }, pairedId)

    fun toggleFolder(bucket: Bucket) = uiRepository.toggleBucket(Pair(bucket.volumeName, bucket.id))

    val isSearching = uiRepository.isSearching
    val isSelecting = uiRepository.isSelecting
    val query = uiRepository.query

    val selectedSongIds = uiRepository.selectedSongIds
    fun startSelecting(songId: String) = uiRepository.startSelectingSongs(songId)

    fun toggleSong(songId: String) = uiRepository.toggleSong(songId)

    fun selectSong(song: AudioFile, list: List<AudioFile>) {
        mediaController.value?.let { ctrl ->
            setQueue(ctrl, list, song) {
                viewModelScope.launch { songRepository.setNewQueue(it, "") }
            }
            songRepository.updatePickedSong(song)
        }
    }

    fun updateTab(tab: LocalMusicTabs) = songRepository.updateLocalTab(tab)
    suspend fun onHideSong(uri: Uri) = songRepository.hideAudioFile(uri.toString())
    suspend fun onShowSong(uri: Uri) = songRepository.showAudioFile(uri.toString())
    suspend fun deletePlaylist(playlist: Playlist) =
        songRepository.deleteLocalPlaylist(playlist)
}