package com.xandy.lite.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.db.tables.LyricsWithAudio
import kotlinx.coroutines.flow.Flow

@Dao
interface LyricsDao {
    @Upsert
    suspend fun upsertLyrics(lyrics: Lyrics)

    @Query("""SELECT * FROM lyrics""")
    fun getLyricsWithAudio(): Flow<List<LyricsWithAudio>>

    @Update
    suspend fun updateLyrics(lyrics: Lyrics)

    @Delete
    suspend fun deleteLyrics(lyrics: Lyrics)

    @Query("""UPDATE local_audio SET lyrics_id = :lyricsId WHERE uri = :songUri""")
    suspend fun updateLyricsOfSong(lyricsId: String, songUri: String)

    /** This REMOVES the relationship, it does not delete the lyrics */
    @Query("""UPDATE local_audio SET lyrics_id = NULL WHERE song_id = :songId""")
    suspend fun removeLyricsOfSong(songId: String)
}