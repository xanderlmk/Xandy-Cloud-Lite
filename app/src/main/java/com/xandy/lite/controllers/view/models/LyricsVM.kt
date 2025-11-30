package com.xandy.lite.controllers.view.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xandy.lite.db.lyrics.repo.LyricsRepository
import com.xandy.lite.db.tables.Lyrics
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class LyricsVM(private val lyricsRepository: LyricsRepository) : ViewModel() {
    val lyricsList = lyricsRepository.lyricsFlow().stateIn(
        scope = viewModelScope, started = SharingStarted.Lazily,
        initialValue = emptyList()
    )
    suspend fun importLyrics(lyrics: Lyrics) = lyricsRepository.importLyrics(lyrics)
    suspend fun updateLyrics(lyrics: Lyrics) = lyricsRepository.updateLyrics(lyrics)

    suspend fun deleteLyrics(lyrics: Lyrics) = lyricsRepository.deleteLyrics(lyrics)
}