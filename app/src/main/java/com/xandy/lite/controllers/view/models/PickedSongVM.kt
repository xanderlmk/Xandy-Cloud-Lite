package com.xandy.lite.controllers.view.models

import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.xandy.lite.controllers.getQueueOrderedBy
import com.xandy.lite.db.song.repo.SongRepository
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.models.application.toStrings
import com.xandy.lite.models.ui.SongDetails
import com.xandy.lite.models.ui.order.by.OrderQueueBy
import com.xandy.lite.models.ui.order.by.isAscending
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


@OptIn(UnstableApi::class)
class PickedSongVM(
    private val songRepository: SongRepository, private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    companion object {
        private const val TIMEOUT_MILLIS = 4_000L
        private const val IS_PLAYING = "is_playing"
        private const val REPEAT_MODE = "repeat_mode"
        private const val SHUFFLE_ENABLED = "shuffle_enabled"
    }

    private var _isPlaying: Boolean?
        get() = savedStateHandle[IS_PLAYING]
        set(value) = savedStateHandle.set(IS_PLAYING, value)
    private var _repeatMode: Int
        get() = savedStateHandle[REPEAT_MODE] ?: Player.REPEAT_MODE_OFF
        set(value) = savedStateHandle.set(REPEAT_MODE, value)
    private var _shuffleEnabled: Boolean
        get() = savedStateHandle[SHUFFLE_ENABLED] ?: false
        set(value) = savedStateHandle.set(SHUFFLE_ENABLED, value)
    val av = songRepository.appValues
    val appStrings = songRepository.appValues.toStrings(viewModelScope)
    val mediaController = songRepository.mediaController
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
    private val queueOrder = songRepository.queueOrder
    private val queueAsc = getQueueOrderedBy(viewModelScope, queueOrder, TIMEOUT_MILLIS)
    private val queueSize = songRepository.sortedQueue.map { it.size }.stateIn(
        scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = 0
    )
    private val queueStates = combine(
        sortedQueue, unsortedQueue, queueSize, queueAsc, queueOrder
    ) { sorted, unsorted, size, asc, order ->
        QueueStates(sorted, unsorted, size, asc, order)
    }
    private val mediaStates =
        combine(
            songRepository.songDetails, songRepository.isPlaying, songRepository.isLoading,
            songRepository.repeatMode, songRepository.shuffleEnabled
        ) { song, isPlaying, isLoading, repeatMode, shuffleEnabled ->
            MediaStates(song, isPlaying, isLoading, repeatMode, shuffleEnabled)
        }
    val vmStates = combine(queueStates, mediaStates) { qs, ms ->
        PickedSongVMStates(
            song = ms.song, isPlaying = ms.isPlaying, isLoading = ms.isLoading,
            repeatMode = ms.repeatMode, shuffleEnabled = ms.shuffleMode,
            sortedQueue = qs.sortedQueue, unsortedQueue = qs.unsortedQueue,
            queueSize = qs.size, queueAsc = qs.isAscending, queueOrder = qs.order,
        )
    }.stateIn(
        scope = songRepository.scope, started = SharingStarted.Eagerly,
        initialValue = PickedSongVMStates(
            song = null, isPlaying = _isPlaying ?: songRepository.isPlaying.value,
            isLoading = songRepository.isLoading.value,
            repeatMode = _repeatMode, shuffleEnabled = _shuffleEnabled,
            sortedQueue = emptyList(), unsortedQueue = emptyList(),
            queueSize = 0, queueAsc = songRepository.queueOrder.value.isAscending(),
            queueOrder = songRepository.queueOrder.value,
        )
    )
    val priorityQueue = songRepository.priorityList

    fun updateMediaController(mc: MediaController) = songRepository.updateMediaController(mc)

    fun resetMediaController() = songRepository.resetMediaController()

    fun updateIsPlaying(isPlaying: Boolean) = songRepository.updateIsPlaying(isPlaying).also {
        _isPlaying = isPlaying
    }

    fun updateIsLoading(isLoading: Boolean) = songRepository.updateIsLoading(isLoading)

    fun updatePickedSong(id: String?) {
        viewModelScope.launch {
            id ?: return@launch
            songRepository.updatePickedSong(id)
        }
    }

    fun handleSkipNext(shuffleEnabled: Boolean, repeatMode: Int, mc: MediaController) =
        songRepository.handleSkipNext(shuffleEnabled, repeatMode, mc)

    fun updateDuration(duration: Long) = songRepository.updateDuration(duration)
    fun updatePosition(position: Long) = songRepository.updatePosition(position)

    fun updateLastestPlayerInfo() = songRepository.updateLastestPlayerInfo()

    fun playNext(song: AudioFile) = songRepository.addItemToPriorityQueue(song)

    /** Start Checking the position of the song */
    fun checkPosition() = songRepository.checkPlaybackPosition()
    fun stopCheckingPosition() = songRepository.stopCheckingPlaybackPosition()
    fun updateQueueOrder(orderQueueBy: OrderQueueBy) = songRepository.updateQueueOrder(orderQueueBy)


    fun getCurrentPriorityItem() = songRepository.getCurrentPriorityItem()
    fun removePriorityItemStates () = songRepository.clearPriorityItemStates()

    override fun onCleared() {
        _repeatMode = vmStates.value.repeatMode; _shuffleEnabled = vmStates.value.shuffleEnabled
        _isPlaying = vmStates.value.isPlaying
        super.onCleared()
    }

    init {
        _isPlaying?.let { value ->
            viewModelScope.launch { delay(100); songRepository.updateIsPlaying(value) }
        }
        checkPosition()
    }
    data class PickedSongVMStates(
        val song: SongDetails?, val isPlaying: Boolean,
        val isLoading: Boolean, val repeatMode: Int, val shuffleEnabled: Boolean,
        val sortedQueue: List<MediaItem>, val unsortedQueue: List<MediaItem>,
        val queueSize: Int, val queueAsc: Boolean, val queueOrder: OrderQueueBy

    )
}


private data class MediaStates(
    val song: SongDetails? = null, val isPlaying: Boolean, val isLoading: Boolean,
    val repeatMode: Int, val shuffleMode: Boolean
)

private data class QueueStates(
    val unsortedQueue: List<MediaItem> = emptyList(),
    val sortedQueue: List<MediaItem> = emptyList(),
    val size: Int = 0, val isAscending: Boolean, val order: OrderQueueBy
)