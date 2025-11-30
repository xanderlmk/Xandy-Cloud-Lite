package com.xandy.lite.controllers.view.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xandy.lite.db.song.repo.SongRepository
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.LyricLine
import com.xandy.lite.db.tables.Lyrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.collections.indices

class EditAudioVM(
    private val songRepository: SongRepository
) : ViewModel() {
    companion object {
        private const val TIMEOUT_MILLIS = 4_000L
    }

    val pickedAudio = songRepository.pickedAudio.stateIn(
        scope = viewModelScope, started = SharingStarted.Lazily,
        initialValue = null
    )
    val artworkList = songRepository.allMediaArtwork.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = emptyList()
    )
    suspend fun updateAudioTags(newAudio: AudioFile, lyrics: Lyrics?) =
        songRepository.updateAudioTags(
            newAudio, lyrics
        )
}