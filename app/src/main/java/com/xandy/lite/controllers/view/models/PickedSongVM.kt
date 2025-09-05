package com.xandy.lite.controllers.view.models

import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.xandy.lite.controllers.getQueueOrderedBy
import com.xandy.lite.db.song.repo.SongRepository
import com.xandy.lite.models.ui.AudioUIState
import com.xandy.lite.models.ui.order.by.OrderQueueBy
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


@OptIn(UnstableApi::class)
class PickedSongVM(private val songRepository: SongRepository) : ViewModel() {
    companion object {
        private const val TIMEOUT_MILLIS = 4_000L
    }

    val song = songRepository.songDetails.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = null
    )
    val mediaController = songRepository.mediaController
    val repeatMode = songRepository.repeatMode.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = Player.REPEAT_MODE_OFF
    )
    val shuffleMode = songRepository.shuffleEnabled.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = false
    )
    val isPlaying = songRepository.isPlaying
    val isLoading = songRepository.isLoading
    val position = songRepository.positionMs
    val duration = songRepository.durationMs
    val sortedQueue = songRepository.sortedQueue.stateIn(
        scope = viewModelScope, started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
    val unsortedQueue = songRepository.unsortedQueue.stateIn(
        scope = viewModelScope, started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
    val queueOrder = songRepository.queueOrder
    val queueAsc = getQueueOrderedBy(viewModelScope, queueOrder, TIMEOUT_MILLIS)
    val queueSize = songRepository.sortedQueue.map { it.size }.stateIn(
        scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = 0
    )
    val audioFiles = songRepository.audioFiles.stateIn(
        scope = viewModelScope, started = SharingStarted.Eagerly,
        initialValue = AudioUIState()
    )

    fun updateMediaController(mc: MediaController) {
        viewModelScope.launch {
            songRepository.updateMediaController(mc)
        }
    }

    fun resetMediaController() = songRepository.resetMediaController()

    fun updateTracks(tracks: Tracks) = songRepository.updateTracks(tracks)

    fun updateIsPlaying(isPlaying: Boolean) = songRepository.updateIsPlaying(isPlaying)
    fun updateIsLoading(isLoading: Boolean) = songRepository.updateIsLoading(isLoading)

    fun updatePickedSong(item: MediaItem?) {
        val targetUri = item?.requestMetadata?.mediaUri
            ?: item?.localConfiguration?.uri ?: item?.mediaId?.toUri()
        if (targetUri == null) {
            Log.e("Xandy-Cloud", "Failed to get uri"); return
        }
        val song =  audioFiles.value.list.find { it.song.uri == targetUri }?.song
        if (song == null) Log.e("Xandy-Cloud", "Failed to find song.")
        songRepository.updatePickedSong(song)
    }

    fun updateDuration(duration: Long) = songRepository.updateDuration(duration)
    fun updatePosition(position: Long) = songRepository.updatePosition(position)

    fun checkPosition() = songRepository.checkPlaybackPosition()
    fun updateQueueOrder(orderQueueBy: OrderQueueBy) = songRepository.updateQueueOrder(orderQueueBy)

    init {
        checkPosition()
    }
}