package com.xandy.lite.controllers.view.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xandy.lite.db.song.repo.SongRepository
import com.xandy.lite.db.tables.LyricLine
import com.xandy.lite.db.tables.Lyrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LyricsEditorVM(private val songRepository: SongRepository) : ViewModel() {
    private val _scrollSet = MutableStateFlow(emptyList<LyricLine>())
    val scrollSet = _scrollSet.asStateFlow()
    private val _lyrics = MutableStateFlow(Lyrics(plain = ""))
    val lyrics = _lyrics.asStateFlow()

    private val _pronunciationSet = MutableStateFlow(emptyList<LyricLine>())
    val pronunciationSet = _pronunciationSet.asStateFlow()

    fun updatePlain(n: String) = _lyrics.update {
        it.copy(plain = n)
    }

    fun updateDescription(n: String) = _lyrics.update {
        it.copy(description = n)
    }

    fun addToScrollSet(start: Long, end: Long) = _scrollSet.update {
        val new = it.toMutableList()
        new.add(LyricLine(LongRange(start, end), ""))
        new.toList()
    }

    fun removeScrollLast() = _scrollSet.update {
        val new = it.toMutableList()
        new.removeAt(new.lastIndex)
        new.toList()
    }

    fun updateScrollLineTextAt(index: Int, text: String) = _scrollSet.update {
        val new = it.toMutableList()
        new[index] = new[index].copy(text = text)
        new
    }

    fun updateScrollLineRangeAt(index: Int, start: Long, end: Long) = _scrollSet.update {
        if ((index - 1) in it.indices && start < it[index - 1].range.last ||
            (index + 1) in it.indices && end > it[index + 1].range.first
        ) return@update it
        val new = it.toMutableList()
        new[index] = new[index].copy(range = LongRange(start, end))
        new
    }

    init {
        viewModelScope.launch {
            val lyricsOrNull = songRepository.getLyrics()
            lyricsOrNull?.let { l ->
                _lyrics.update { l }
                _scrollSet.update { (l.scroll ?: emptySet()).toList() }
            }
        }
    }
}