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
import com.xandy.lite.db.tables.Playlist
import com.xandy.lite.db.tables.PlaylistSongOrder
import com.xandy.lite.db.tables.toMediaItems
import com.xandy.lite.models.itemKey
import com.xandy.lite.models.ui.MediaItemWithCreatedOn
import com.xandy.lite.models.ui.order.by.OrderSongsBy
import com.xandy.lite.navigation.UIRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocalPLVM(
    private val songRepository: SongRepository,
    private val lyricsRepository: LyricsRepository,
    private val uiRepository: UIRepository) : ViewModel() {
    companion object {
        private const val COMMAND_SHUFFLE = "Shuffle_Songs"
        private const val TIMEOUT_MILLIS = 4_000L
    }

    val plWithAudio = songRepository.pickedPlaylist.stateIn(
        scope = viewModelScope, started = SharingStarted.Lazily,
        initialValue = null
    )
    val isSelecting = uiRepository.isSelecting
    val selectedSongIds = uiRepository.selectedSongIds
    val isAdding = uiRepository.isAdding
    val query = uiRepository.query
    val mediaController = songRepository.mediaController
    val isSearching = uiRepository.isSearching
    val songDetails = songRepository.songDetails.stateIn(
        scope = viewModelScope, started = SharingStarted.Companion.Eagerly,
        initialValue = null
    )
    val localAudiosLoading = songRepository.filesLoading
    val isPlaying = songRepository.isPlaying
    val tracks = songRepository.tracks
    val pickedQueueName = songRepository.pickedQueueName.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = ""
    )

    val filteredAudioFiles = combine(songRepository.audioFiles, query, isSearching) { al, q, s ->
        al.list.filter { audio ->
            if (q.isBlank() || !s) return@filter true
            audio.song.title.contains(q, ignoreCase = true) ||
                    audio.song.artist.contains(q, ignoreCase = true)
        }
    }.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = emptyList()
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

    fun selectSong(song: AudioFile, list: List<AudioFile>, plName: String) {
        mediaController.value?.let { ctrl ->
            setQueue(ctrl, list, song) {
                viewModelScope.launch { songRepository.setNewQueue(it, plName) }
            }
            songRepository.updatePickedSong(song.id)        }
    }

    fun setShuffleOn(song: AudioFile, list: List<AudioFile>, plName: String) =
        viewModelScope.launch {
            if (mediaController.value?.shuffleModeEnabled == false)
                mediaController.value?.sendCustomCommand(
                    SessionCommand(COMMAND_SHUFFLE, Bundle()), Bundle()
                )
            selectSong(song, list, plName)
        }

    /** Remove Local Songs from a Local Playlist */
    suspend fun removeLocalSongsFromPL(songIds: List<String>, playlistId: String) =
        songRepository.removeLocalSongsFromPl(songIds, playlistId)

    fun updateOrder(orderBy: OrderSongsBy, pl: Playlist) = viewModelScope.launch {
        val new = PlaylistSongOrder(pl.name, orderBy)
        songRepository.updatePLSongOrder(new)
    }
    val lyricsList = lyricsRepository.lyricsFlow().stateIn(
        scope = viewModelScope, started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    suspend fun updateSongLyrics(lyricsId:String, songUri: String) =
        lyricsRepository.updateSongLyrics(lyricsId = lyricsId, songUri)
}