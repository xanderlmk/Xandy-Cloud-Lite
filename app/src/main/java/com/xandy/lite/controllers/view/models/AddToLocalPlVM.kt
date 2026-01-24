package com.xandy.lite.controllers.view.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xandy.lite.controllers.Controller
import com.xandy.lite.db.song.repo.SongRepository
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.models.application.toStrings
import com.xandy.lite.models.ui.toPlaylists
import com.xandy.lite.navigation.UIRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class AddToLocalPlVM(
    private val songRepository: SongRepository, uiRepository: UIRepository
) : ViewModel() {
    val pls = songRepository.localPlaylists.toPlaylists().stateIn(
        scope = viewModelScope, started = SharingStarted.Companion.Lazily,
        initialValue = emptyList()
    )

    suspend fun insertSongsToPl(songIds: List<String>, playListId: String) =
        songRepository.addLocalSongsToPl(songIds, playListId)

    suspend fun addPlWithSongs(songIds: List<String>, name: String) =
        songRepository.addPlWithSongs(songIds, name)

    private val mediaController = songRepository.mediaController
    private val appStrings = songRepository.appValues.toStrings(viewModelScope)
    private val unsortedQueue = songRepository.unsortedQueue.stateIn(
        scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptyList(),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val filtered = uiRepository.selectedSongIds.flatMapLatest {
        songRepository.getAfsByIds(it)
    }.stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptyList())

    fun addToQueue(): Boolean =
        Controller.addToQueue(
            mediaController.value, filtered.value, appStrings.value, unsortedQueue.value
        ) { viewModelScope.launch { songRepository.updateQueue(it) } }

    fun playNext() = songRepository.addItemsToPriorityQueue(filtered.value)
}