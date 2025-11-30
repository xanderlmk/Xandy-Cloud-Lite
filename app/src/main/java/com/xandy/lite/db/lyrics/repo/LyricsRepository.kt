package com.xandy.lite.db.lyrics.repo

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xandy.lite.db.daos.AudioDao
import com.xandy.lite.db.song.repo.LocalLibraryStates
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.db.tables.LyricsWithAudio
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.application.dataStore
import com.xandy.lite.models.ui.InsertResult
import com.xandy.lite.views.lyrics.LyricIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

interface LyricsRepository {
    /** Get all stored lyrics */
    fun lyricsFlow(): Flow<List<LyricsWithAudio>>
    suspend fun getLyrics(): Lyrics?
    suspend fun updatePickedLyrics(n: String)
    suspend fun importLyrics(lyrics: Lyrics): InsertResult
    suspend fun upsertLyrics(songUri: String, lyrics: Lyrics): Boolean
    suspend fun updateSongLyrics(lyricsId: String, songUri: String): Boolean
    suspend fun updateLyrics(lyrics: Lyrics): Boolean
    suspend fun deleteLyrics(lyrics: Lyrics): Boolean
    fun searchForSong(query: String): Flow<List<AudioFile>>
    suspend fun getSongOrNullByLyricsId(lyricsId: String): AudioFile?
    val indexListener: StateFlow<Int>
    fun updateIndex(idx: Int)
}

class OfflineLyricsRepo(
    private val audioDao: AudioDao, private val context: Context
) : LyricsRepository {
    companion object {
        private val LYRICS_ID = stringPreferencesKey("picked_lyrics_id")
    }

    override suspend fun getLyrics() = withContext(Dispatchers.IO) {
        audioDao.getLyrics(
            context.dataStore.data.map { preferences -> preferences[LYRICS_ID] ?: "" }.first()
        )
    }

    override suspend fun updatePickedLyrics(n: String) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { settings ->
                settings[LYRICS_ID] = n
            }
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed updating lyrics id: $e")
            return@withContext
        }
    }

    override suspend fun importLyrics(lyrics: Lyrics) = audioDao.importLyrics(lyrics)
    override suspend fun upsertLyrics(songUri: String, lyrics: Lyrics) = withContext(Dispatchers.IO) {
        try {
            audioDao.upsertLyricsWithSong(songUri, lyrics)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun lyricsFlow() = audioDao.getLyricsWithAudio()

    override suspend fun updateSongLyrics(lyricsId: String, songUri: String) =
        withContext(Dispatchers.IO) {
            try {
                audioDao.updateLyricsOfSong(lyricsId, songUri = songUri)
                true
            } catch (_: Exception) {
                false
            }
        }

    override suspend fun updateLyrics(lyrics: Lyrics) = withContext(Dispatchers.IO) {
        try {
            audioDao.updateLyrics(lyrics)
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun deleteLyrics(lyrics: Lyrics) = withContext(Dispatchers.IO) {
        try {
            audioDao.deleteLyrics(lyrics)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun searchForSong(query: String) =
        if (query.isBlank()) flowOf(emptyList()) else audioDao.searchForSong(query)

    override suspend fun getSongOrNullByLyricsId(lyricsId: String) = withContext(Dispatchers.IO) {
        audioDao.getSongOrNullByLyricsId(lyricsId)
    }

    private val _indexListener = MutableStateFlow(LyricIndex.UNAVAILABLE)
    override val indexListener = _indexListener.asStateFlow()
    override fun updateIndex(idx: Int) = _indexListener.update { idx }
}
