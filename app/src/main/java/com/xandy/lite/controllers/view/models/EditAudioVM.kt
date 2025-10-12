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
    private val _set = MutableStateFlow(emptyList<LyricLine>())
    val set = _set.asStateFlow()
    suspend fun updateAudioTags(newAudio: AudioFile, lyrics: Lyrics?) =
        songRepository.updateAudioTags(
            newAudio, lyrics?.copy(scroll = _set.value.toSet().takeIf { it.isNotEmpty() })
        )

    fun addToSet(start: Long, end: Long) = _set.update {
        val new = it.toMutableList()
        new.add(LyricLine(LongRange(start, end), ""))
        new.toList()
    }

    fun removeLast() = _set.update {
        val new = it.toMutableList()
        new.removeAt(new.lastIndex)
        new.toList()
    }

    fun updateLineTextAt(index: Int, text: String) = _set.update {
        val new = it.toMutableList()
        new[index] = new[index].copy(text = text)
        new
    }

    fun updateLineRangeAt(index: Int, start: Long, end: Long) = _set.update {
        if ((index - 1) in it.indices && start < it[index - 1].range.last ||
            (index + 1) in it.indices && end > it[index + 1].range.first
        ) return@update it
        val new = it.toMutableList()
        new[index] = new[index].copy(range = LongRange(start, end))
        new
    }
    fun updateScrollSet() =
        _set.update { pickedAudio.value?.lyrics?.scroll?.toList() ?: emptyList() }
}