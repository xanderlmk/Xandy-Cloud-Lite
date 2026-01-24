package com.xandy.lite.controllers.view.models

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.SessionCommand
import com.xandy.lite.controllers.Controller
import com.xandy.lite.db.lyrics.repo.LyricsRepository
import com.xandy.lite.db.song.repo.SongRepository
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.models.application.toStrings
import com.xandy.lite.navigation.UIRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class LocalAlbumVM(
    private val songRepository: SongRepository,
    private val lyricsRepository: LyricsRepository,
    private val uiRepository: UIRepository
) : ViewModel() {
    companion object {
        private const val COMMAND_SHUFFLE = "Shuffle_Songs"
    }

    val query = uiRepository.query
    val isSearching = uiRepository.isSearching
    val isSelecting = uiRepository.isSelecting
    val selectedSongIds = uiRepository.selectedSongIds
    val mediaController = songRepository.mediaController
    val album = songRepository.pickedLocalAlbum.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(4_000L),
        initialValue = null
    )
    val localAudiosLoading = songRepository.filesLoading
    val isPlaying = songRepository.isPlaying
    val pickedQueueName = songRepository.pickedQueueName.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(4_000L), initialValue = ""
    )

    val unsortedQueue = songRepository.unsortedQueue.stateIn(
        scope = viewModelScope, started = SharingStarted.Eagerly, emptyList()
    )

    val appStrings = songRepository.appValues.toStrings(viewModelScope)

    fun addToQueue(list: List<AudioFile>): Boolean =
        Controller.addToQueue(mediaController.value, list, appStrings.value, unsortedQueue.value) {
            viewModelScope.launch { songRepository.updateQueue(it) }
        }

    fun selectSong(song: AudioFile, list: List<AudioFile>, albumName: String) {
        mediaController.value?.let { ctrl ->
            Controller.setQueue(ctrl, list, song, appStrings.value) {
                viewModelScope.launch { songRepository.setNewQueue(it, albumName) }
            }
            songRepository.updatePickedSong(song.id)
        }
    }

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

    suspend fun onFavoriteSong(uri: Uri) = songRepository.addToFavorites(uri)


    fun setShuffleOn(song: AudioFile, list: List<AudioFile>, albumName: String) =
        viewModelScope.launch {
            if (mediaController.value?.shuffleModeEnabled == false)
                mediaController.value?.sendCustomCommand(
                    SessionCommand(COMMAND_SHUFFLE, Bundle()), Bundle()
                )
            selectSong(song, list, albumName)
        }

    val lyricsList = lyricsRepository.lyricsFlow().stateIn(
        scope = viewModelScope, started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    suspend fun updateSongLyrics(lyricsId: String, songUri: String) =
        lyricsRepository.updateSongLyrics(lyricsId = lyricsId, songUri)
}