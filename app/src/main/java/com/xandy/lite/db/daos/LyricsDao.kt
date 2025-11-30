package com.xandy.lite.db.daos

import android.util.Log
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.xandy.lite.db.LyricsId
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.db.tables.LyricsWithAudio
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.ui.InsertResult
import kotlinx.coroutines.flow.Flow

@Dao
interface LyricsDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLyrics(lyrics: Lyrics)

    @Update
    suspend fun updateLyrics(lyrics: Lyrics)

    @Upsert
    suspend fun upsertLyrics(lyrics: Lyrics)

    @Transaction
    @Query("""SELECT * FROM lyrics""")
    fun getLyricsWithAudio(): Flow<List<LyricsWithAudio>>

    @Query("""SELECT * FROM lyrics WHERE id = :id""")
    suspend fun getLyrics(id: String): Lyrics?

    @Delete
    suspend fun deleteLyrics(lyrics: Lyrics)

    @Query("""SELECT id FROM lyrics WHERE id = :id""")
    suspend fun getLyricsIdOrNull(id: String): LyricsId?

    @Query("""UPDATE local_audio SET lyrics_id = :lyricsId WHERE uri = :songUri""")
    suspend fun updateLyricsOfSong(lyricsId: String, songUri: String)

    /** This REMOVES the relationship, it does not delete the lyrics */
    @Query("""UPDATE local_audio SET lyrics_id = NULL WHERE song_id = :songId""")
    suspend fun removeLyricsOfSong(songId: String)

    @Query("""SELECT * FROM local_audio WHERE lyrics_id = :lyricsId LIMIT 1""")
    suspend fun getSongOrNullByLyricsId(lyricsId: String): AudioFile?

    @Transaction
    suspend fun importLyrics(lyrics: Lyrics): InsertResult = try {
        val l = getLyricsIdOrNull(lyrics.id)
        if (l != null) InsertResult.Exists
        else {
            insertLyrics(lyrics)
            InsertResult.Success
        }
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "${e.printStackTrace()}")
        InsertResult.Failure
    }

    @Transaction
    suspend fun upsertLyricsWithSong(songUri: String, lyrics: Lyrics) {
        upsertLyrics(lyrics)
        updateLyricsOfSong(lyricsId = lyrics.id, songUri = songUri)
    }
}