package com.xandy.lite.controllers.view.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xandy.lite.db.song.repo.SongRepository
import com.xandy.lite.models.ui.toPlaylists
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn


class AddToLocalPlVM(
    private val songRepository: SongRepository
) : ViewModel() {
    val pls = songRepository.localPlaylists.toPlaylists().stateIn(
        scope = viewModelScope, started = SharingStarted.Companion.Lazily,
        initialValue = emptyList()
    )
    suspend fun insertSongsToPl(songIds: List<String>, playListId: String) =
        songRepository.addLocalSongsToPl(songIds, playListId)
    suspend fun addPlWithSongs(songIds: List<String>, name: String) =
        songRepository.addPlWithSongs(songIds, name)
}