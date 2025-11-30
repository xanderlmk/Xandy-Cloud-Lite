package com.xandy.lite.controllers.view.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xandy.lite.db.lyrics.repo.LyricsRepository
import com.xandy.lite.db.song.repo.SongRepository
import com.xandy.lite.db.tables.LyricLine
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.db.tables.TranslatedLyrics
import com.xandy.lite.db.tables.TranslatedText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsEditorVM(
    private val lyricsRepository: LyricsRepository, private val songRepository: SongRepository
) : ViewModel() {
    private val _scrollSet = MutableStateFlow(emptyList<LyricLine>())
    val scrollSet = _scrollSet.asStateFlow()
    private val _lyrics = MutableStateFlow(Lyrics(plain = ""))
    val lyrics = _lyrics.asStateFlow()
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()
    val songs = _query.flatMapLatest { lyricsRepository.searchForSong(it) }.stateIn(
        scope = viewModelScope, started = SharingStarted.Lazily, initialValue = emptyList()
    )
    val isPlaying = songRepository.isPlaying
    val isLoading = songRepository.isLoading
    val songDetails = songRepository.songDetails.stateIn(
        scope = viewModelScope, started = SharingStarted.Lazily,
        initialValue = null
    )
    val queue = songRepository.unsortedQueue.stateIn(
        scope = viewModelScope, started = SharingStarted.Lazily,
        initialValue = emptyList()
    )
    val position = songRepository.positionMs
    val duration = songRepository.durationMs
    val controller = songRepository.mediaController

    private val _pronunciationSet = MutableStateFlow(emptyList<LyricLine>())
    val pronunciationSet = _pronunciationSet.asStateFlow()
    private val _translationSet = MutableStateFlow(emptyList<LyricLine>())
    val translationSet = _translationSet.asStateFlow()
    val index = lyricsRepository.indexListener


    fun updateQuery(q: String) = _query.update { q }

    fun updatePlain(n: String) = _lyrics.update { it.copy(plain = n) }

    fun updateDescription(n: String) = _lyrics.update { it.copy(description = n) }

    /** Add to the stack */
    fun addToScrollSet(start: Long, end: Long) = _scrollSet.update {
        onAddToOtherSets(start, end)
        addToList(it, start, end)
    }

    private fun onAddToOtherSets(start: Long, end: Long) {
        if (_lyrics.value.pronunciation?.lyrics is TranslatedText.Scroll)
            _pronunciationSet.update { addToList(it, start, end) }
        if (_lyrics.value.translation?.lyrics is TranslatedText.Scroll)
            _translationSet.update { addToList(it, start, end) }
    }

    private fun addToList(it: List<LyricLine>, start: Long, end: Long): List<LyricLine> {
        val new = it.toMutableList()
        new.add(LyricLine(LongRange(start, end), ""))
        return new.toList()
    }

    /** FIFO */
    fun removeFromScrollSet() = _scrollSet.update {
        val new = removeFromSet(it)
        if (new.isEmpty()) onEmptyScroll()
        else removeFromOtherSets()
        new
    }

    private fun onEmptyScroll() = _lyrics.update { l ->
        if (l.pronunciation?.lyrics is TranslatedText.Scroll &&
            l.translation?.lyrics is TranslatedText.Scroll
        ) l.copy(pronunciation = null, translation = null)
        else if (l.pronunciation?.lyrics is TranslatedText.Scroll) l.copy(pronunciation = null)
        else if (l.translation?.lyrics is TranslatedText.Scroll) l.copy(translation = null)
        else l
    }

    private fun removeFromOtherSets() {
        if (_lyrics.value.pronunciation?.lyrics is TranslatedText.Scroll)
            _pronunciationSet.update { removeFromSet(it) }
        if (_lyrics.value.translation?.lyrics is TranslatedText.Scroll)
            _translationSet.update { removeFromSet(it) }
    }

    private fun removeFromSet(it: List<LyricLine>): List<LyricLine> {
        val new = it.toMutableList()
        new.removeAt(new.lastIndex)
        return new.toList()
    }

    fun updateScrollSetTextAt(index: Int, text: String) =
        _scrollSet.update { updateSetText(it, index, text) }

    private fun updateSetText(it: List<LyricLine>, index: Int, text: String): List<LyricLine> {
        val new = it.toMutableList()
        new[index] = new[index].copy(text = text)
        return new.toList()
    }

    fun updateScrollLineRangeAt(index: Int, start: Long, end: Long) {
        val new = _scrollSet.value.toMutableList()
        _scrollSet.update {
            new[index] = new[index].copy(range = LongRange(start, end))
            new
        }
        onUpdateOtherLyricLineSets(new)
    }

    private fun onUpdateOtherLyricLineSets(new: List<LyricLine>) {
        if (_lyrics.value.pronunciation?.lyrics is TranslatedText.Scroll)
            _pronunciationSet.update { pro ->
                new.mapIndexed { idx, it -> it.copy(text = pro[idx].text) }
            }
        if (_lyrics.value.translation?.lyrics is TranslatedText.Scroll)
            _translationSet.update { pro ->
                new.mapIndexed { idx, it -> it.copy(text = pro[idx].text) }
            }
    }

    fun togglePronunciationType(new: TranslatedText, language: String) = _lyrics.update {
        it.copy(pronunciation = TranslatedLyrics(new, language))
    }.run {
        if (new is TranslatedText.Scroll)
            _pronunciationSet.update { new.set.map { it.copy(text = "") } }
        else _pronunciationSet.update { emptyList() }
    }

    fun pronunciationTypeToNull() {
        _lyrics.update { it.copy(pronunciation = null) }
        _pronunciationSet.update { emptyList() }
    }

    fun updatePronunciationPlainText(new: TranslatedLyrics) = _lyrics.update {
        it.copy(pronunciation = new)
    }

    fun updatePronunciationSetTextAt(index: Int, text: String) =
        _pronunciationSet.update { updateSetText(it, index, text) }

    fun toggleTranslationType(new: TranslatedText, language: String) = _lyrics.update {
        it.copy(translation = TranslatedLyrics(new, language))
    }.run {
        if (new is TranslatedText.Scroll)
            _translationSet.update { new.set.map { it.copy(text = "") } }
        else _translationSet.update { emptyList() }
    }

    fun translationTypeToNull() {
        _lyrics.update { it.copy(translation = null) }
        _translationSet.update { emptyList() }
    }

    fun updateTranslationPlainText(new: TranslatedLyrics) = _lyrics.update {
        it.copy(translation = new)
    }

    fun updateTranslationSetTextAt(index: Int, text: String) =
        _translationSet.update { updateSetText(it, index, text) }

    suspend fun getSongOrNullByLyricsId(lyricsId: String) =
        lyricsRepository.getSongOrNullByLyricsId(lyricsId)

    suspend fun upsertLyrics(songUri: String) =
        lyricsRepository.upsertLyrics(songUri, _lyrics.value)

    fun updateIndexListener(idx: Int) = lyricsRepository.updateIndex(idx)
    /** Start Checking the position of the song */
    fun checkPosition() = songRepository.checkPlaybackPosition()

    init {
        checkPosition()
        viewModelScope.launch {
            val lyricsOrNull = lyricsRepository.getLyrics()
            lyricsOrNull?.let { l ->
                _lyrics.update { l }
                _scrollSet.update { (l.scroll ?: emptySet()).toList() }
                val p =
                    if (l.pronunciation?.lyrics is TranslatedText.Scroll)
                        l.pronunciation.lyrics.set.toList()
                    else emptyList()
                _pronunciationSet.update { p }
            }
        }
    }
}