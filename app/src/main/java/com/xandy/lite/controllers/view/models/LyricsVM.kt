package com.xandy.lite.controllers.view.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xandy.lite.db.song.repo.SongRepository
import com.xandy.lite.db.tables.Lyrics
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class LyricsVM(private val songRepository: SongRepository) : ViewModel() {
    val lyricsList = songRepository.lyricsFlow().stateIn(
        scope = viewModelScope, started = SharingStarted.Lazily,
        initialValue = emptyList()
    )
    suspend fun updateLyrics(lyrics: Lyrics) = songRepository.updateLyrics(lyrics)

    suspend fun deleteLyrics(lyrics: Lyrics) = songRepository.deleteLyrics(lyrics)
}