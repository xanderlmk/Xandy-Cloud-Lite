package com.xandy.lite.controllers.view.models

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xandy.lite.controllers.Controller
import com.xandy.lite.db.lyrics.repo.LyricsRepository
import com.xandy.lite.db.song.repo.SongRepository
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.Bucket
import com.xandy.lite.db.tables.Playlist
import com.xandy.lite.models.application.toStrings
import com.xandy.lite.models.ui.Album
import com.xandy.lite.models.ui.Artist
import com.xandy.lite.models.ui.Genre
import com.xandy.lite.models.ui.LocalMediaStates
import com.xandy.lite.models.ui.LocalMusicTabs
import com.xandy.lite.navigation.UIRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.filter
import kotlin.text.isBlank

class LocalMediaVM(
    private val songRepository: SongRepository,
    private val lyricsRepository: LyricsRepository,
    private val uiRepository: UIRepository
) : ViewModel() {
    companion object {
        private const val TIMEOUT_MILLIS = 4_000L
    }

    val folders = songRepository.bucketsWithAudio.stateIn(
        scope = viewModelScope, started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
    private val mediaTypes = combine(
        songRepository.localAlbums, songRepository.localArtists, songRepository.localGenres,
        songRepository.favorites
    ) { albums, artists, genres, favorites ->
        MediaTypes(albums, artists, genres, favorites)
    }

    val musicLibrary = combine(
        songRepository.audioFiles, songRepository.bucketsWithAudio, songRepository.localPlaylists,
        songRepository.hiddenAudios, mediaTypes
    ) { al, bwa, pl, ha, mt ->
        LocalMediaStates(
            audioUIState = al, plsWithAudios = pl, hiddenAudios = ha, albums = mt.albums,
            artists = mt.artists, genres = mt.genres, folders = bwa, favorites = mt.favorites
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

    val filteredAudioFiles =
        combine(songRepository.audioFiles, uiRepository.query, isSearching) { al, q, s ->
            al.list.filter { audio ->
                if (q.isBlank() || !s) return@filter true
                audio.song.title.contains(q, ignoreCase = true) ||
                        audio.song.artist?.contains(q, ignoreCase = true) ?: false
            }
        }.stateIn(
            scope = viewModelScope, started = SharingStarted.Lazily,
            initialValue = emptyList()
        )


    val unsortedQueue = songRepository.unsortedQueue.stateIn(
        scope = viewModelScope, started = SharingStarted.Eagerly, emptyList()
    )

    val appStrings = songRepository.appValues.toStrings(viewModelScope)

    fun addToQueue(list: List<AudioFile>): Boolean =
        Controller.addToQueue(mediaController.value, list, appStrings.value, unsortedQueue.value) {
            viewModelScope.launch { songRepository.updateQueue(it) }
        }

    fun selectSong(song: AudioFile, list: List<AudioFile>) {
        mediaController.value?.let { ctrl ->
            Controller.setQueue(ctrl, list, song, appStrings.value) {
                viewModelScope.launch { songRepository.setNewQueue(it, "") }
            }
            songRepository.updatePickedSong(song.id)
        }
    }

    val selectedSongIds = uiRepository.selectedSongIds
    fun startSelecting(songId: String) = uiRepository.startSelectingSongs(songId)

    fun toggleSong(songId: String) = uiRepository.toggleSong(songId)


    /*fun playNext(song: AudioFile) {
         mediaController.value?.let { ctrl ->
             Controller.playNext(ctrl, song, unsortedQueue.value) {
                 viewModelScope.launch { songRepository.updateQueue(it) }
             }
         }
     }*/
    fun playNext(song: AudioFile) = songRepository.addItemToPriorityQueue(song)

    fun updateTab(tab: LocalMusicTabs) = songRepository.updateLocalTab(tab)
    suspend fun onHideSong(uri: Uri) = songRepository.hideAudioFile(uri.toString())
    suspend fun onShowSong(uri: Uri) = songRepository.showAudioFile(uri.toString())
    suspend fun onFavoriteSong(uri: Uri) = songRepository.addToFavorites(uri)
    suspend fun onUnfavoriteSong(uri: Uri) = songRepository.removeFromFavorites(uri)

    suspend fun deletePlaylist(playlist: Playlist) =
        songRepository.deleteLocalPlaylist(playlist)

    val lyricsList = lyricsRepository.lyricsFlow().stateIn(
        scope = viewModelScope, started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    suspend fun updateSongLyrics(lyricsId: String, songUri: String) =
        lyricsRepository.updateSongLyrics(lyricsId = lyricsId, songUri)

    suspend fun changePlName(newName: String, name: String, id: String) =
        songRepository.changePlaylistName(newName, name, id)
}

private data class MediaTypes(
    val albums: List<Album>, val artists: List<Artist>, val genres: List<Genre>,
    val favorites: List<AudioFile>
)