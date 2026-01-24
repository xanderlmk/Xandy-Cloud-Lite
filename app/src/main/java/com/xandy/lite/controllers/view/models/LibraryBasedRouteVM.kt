package com.xandy.lite.controllers.view.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xandy.lite.db.song.repo.SongRepository
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.navigation.LocalAlbumDestination
import com.xandy.lite.navigation.LocalArtistDestination
import com.xandy.lite.navigation.LocalBucketDestination
import com.xandy.lite.navigation.LocalGenreDestination
import com.xandy.lite.navigation.UIRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LibraryBasedRouteVM(
    private val songRepository: SongRepository,
    private val uiRepository: UIRepository,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAudioFiles(routeFlow: Flow<String>) = routeFlow.flatMapLatest { route ->
        when (route) {
            LocalAlbumDestination.route -> {
                songRepository.pickedLocalAlbum.map { it?.songs ?: emptyList() }
            }

            LocalBucketDestination.route -> {
                songRepository.pickedLocalBucket.map { it?.audioList ?: emptyList() }
            }

            LocalArtistDestination.route -> {
                songRepository.pickedLocalArtist.map { it?.songs ?: emptyList() }
            }

            LocalGenreDestination.route -> {
                songRepository.pickedLocalGenre.map { it?.songs ?: emptyList() }
            }

            else -> {
                songRepository.audioFiles.map { it.list.map { af -> af.song } }
            }
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = emptyList())

    val uiStates =
        combine(
            uiRepository.isSearching, uiRepository.isSelecting, songRepository.filesLoading,
        ) { searching, selecting, loading ->
            UiStates(searching, selecting, loading)
        }.stateIn(
            scope = viewModelScope, started = SharingStarted.Lazily,
            initialValue = UiStates(
                uiRepository.isSearching.value, uiRepository.isSelecting.value,
                songRepository.filesLoading.value
            )
        )

    val listNotEmpty = uiRepository.selectedSongIds.map { it.isNotEmpty() }.stateIn(
        scope = viewModelScope, started = SharingStarted.Lazily,
        initialValue = true
    )

    fun selectAll(list: List<AudioFile>) = uiRepository.selectAll(list.map { it.id })

    data class UiStates(
        val isSearching: Boolean, val isSelecting: Boolean, val isLoading: Boolean,
    )
}


