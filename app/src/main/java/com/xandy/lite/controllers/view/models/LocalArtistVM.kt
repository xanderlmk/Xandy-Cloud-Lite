package com.xandy.lite.controllers.view.models

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.SessionCommand
import com.xandy.lite.controllers.Controller
import com.xandy.lite.controllers.setQueue
import com.xandy.lite.db.lyrics.repo.LyricsRepository
import com.xandy.lite.db.song.repo.SongRepository
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.models.ui.MediaItemWithCreatedOn
import com.xandy.lite.navigation.UIRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class LocalArtistVM(
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
    val localAudiosLoading = songRepository.filesLoading
    val tracks = songRepository.tracks
    val isPlaying = songRepository.isPlaying
    val pickedQueueName = songRepository.pickedQueueName.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(4_000L), initialValue = ""
    )
    val artist = songRepository.pickedLocalArtist.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(4_000L),
        initialValue = null
    )
    val unsortedQueue = songRepository.unsortedQueue.stateIn(
        scope = viewModelScope, started = SharingStarted.Eagerly, emptyList()
    )
    fun addToQueue(list: List<AudioFile>): Boolean =
        Controller.addToQueue(mediaController.value, list, unsortedQueue.value) {
            viewModelScope.launch { songRepository.addToQueue(it) }
        }

    fun startSelecting(songId: String) = uiRepository.startSelectingSongs(songId)

    fun toggleSong(songId: String) = uiRepository.toggleSong(songId)

    fun selectSong(song: AudioFile, list: List<AudioFile>, albumName: String) {
        mediaController.value?.let { ctrl ->
            setQueue(ctrl, list, song) {
                viewModelScope.launch { songRepository.setNewQueue(it, albumName) }
            }
            songRepository.updatePickedSong(song.id)
        }
    }

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