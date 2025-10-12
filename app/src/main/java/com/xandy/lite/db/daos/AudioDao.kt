package com.xandy.lite.db.daos

import android.net.Uri
import android.util.Log
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.xandy.lite.db.AudioUri
import com.xandy.lite.db.AudioDetails
import com.xandy.lite.db.AudioSongId
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.AudioWithPls
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.db.tables.PLSongCrossRef
import com.xandy.lite.models.application.XANDY_CLOUD
import kotlinx.coroutines.flow.Flow


@Dao
interface AudioDao : LyricsDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAudio(audioFile: AudioFile)

    @Update
    suspend fun updateAudio(audioFile: AudioFile)

    @Delete(entity = AudioFile::class)
    suspend fun deleteAudioByUri(vararg uri: AudioUri)

    @Delete
    suspend fun deleteAudioFiles(list: List<AudioFile>)

    @Query("""SELECT * FROM local_audio WHERE song_id IN (:ids) OR uri IN (:ids)""")
    suspend fun getInitialQueue(ids: List<String>): List<AudioFile>

    /** Permanently hide selected audio files. */
    @Query("""UPDATE local_audio SET hidden = 1, permanentlyHidden = 1 WHERE uri IN (:ids)""")
    suspend fun hideAudioFiles(ids: List<String>)

    @Query("""UPDATE local_audio SET hidden = 0, permanentlyHidden = 0 WHERE uri in (:ids)""")
    suspend fun showAudioFiles(ids: List<String>)

    @Query("""UPDATE local_audio SET hidden = 1, permanentlyHidden = 1 WHERE uri = :uri""")
    suspend fun hideAudioFile(uri: String)

    @Query("""UPDATE local_audio SET hidden = 0, permanentlyHidden = 0 WHERE uri = :uri""")
    suspend fun showAudioFile(uri: String)

    @Query(
        """
        UPDATE local_audio SET hidden = CASE WHEN uri IN (:uris) THEN 0 ELSE 1 END
        """
    )
    suspend fun updateShownAudios(uris: List<String>)

    @Query("""SELECT * FROM local_audio WHERE song_id IN (:ids) OR uri IN (:uris)""")
    suspend fun getAudioFiles(ids: List<String>, uris: List<Uri>): List<AudioFile>

    @Query("""SELECT uri FROM local_audio WHERE song_id IN (:ids)""")
    suspend fun getAudioUris(ids: List<String>): List<AudioUri>

    @Query("""SELECT * FROM local_audio""")
    suspend fun getAudioFiles(): List<AudioFile>

    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE song_id = :key
        """
    )
    suspend fun getSongWithPls(key: String): AudioWithPls?

    @Query("""
        SELECT hidden, permanentlyHidden, lyrics_id 
        FROM local_audio WHERE song_id = :id OR uri = :uri
        """)
    suspend fun getAudioDetails(id: String, uri: Uri): AudioDetails

    @Query(
        """SELECT song_id FROM local_audio WHERE uri = :uri"""
    )
    suspend fun getAudioId(uri: Uri): AudioSongId?

    @Transaction
    @Query("""SELECT * FROM local_audio WHERE uri = :uri""")
    fun getFlowOfPickedAudio(uri: String): Flow<AudioWithPls?>


    @Transaction
    suspend fun upsertAudios(audios: List<AudioFile>) {
        val originalList = getAudioFiles(audios.map { it.id }, audios.map { it.uri })
        val originalIds = originalList.map { it.id }
        val originalUris = originalList.map { it.uri }
        Log.i(XANDY_CLOUD, "Upserting ${audios.size} audio files.")
        audios.forEach { audio ->
            if (audio.uri in originalUris || audio.id in originalIds) {
                val d = getAudioDetails(audio.id, audio.uri)
                updateAudio(
                    AudioFile(
                        id = audio.id, uri = audio.uri, displayName = audio.displayName,
                        picture = audio.picture, title = audio.title, artist = audio.artist,
                        album = audio.album, genre = audio.genre, createdOn = audio.createdOn,
                        hidden = d.hidden, durationMillis = audio.durationMillis,
                        permanentlyHidden = d.permanentlyHidden, lyricsId = d.lyricsId,
                        year = audio.year, day = audio.day, month = audio.month,
                        volumeName = audio.volumeName, bucketId = audio.bucketId
                    )
                )
            } else insertAudio(audio)
        }
    }

    @Query(
        """
        UPDATE local_audio 
        SET title = :title, artist = :artist, genre = :genre, album = :album, picture = :pictureUri,
        year = :year, day = :day, month = :month, lyrics_id = :lyricsId
        WHERE uri = :uri
        """
    )
    suspend fun updateAudioTags(
        title: String, artist: String, genre: String?, album: String?, pictureUri: Uri?,
        year: Int?, day: Int?, month: Int?, lyricsId: String?, uri: String
    )

    @Transaction
    suspend fun updateAudioTagsAndLyrics(
        title: String, artist: String, genre: String?, album: String?, pictureUri: Uri?,
        year: Int?, day: Int?, month: Int?, lyrics: Lyrics?, uri: String
    ) {
        lyrics?.let { upsertLyrics(it) }
        updateAudioTags(
            title = title, artist = artist, genre = genre, album = album, pictureUri = pictureUri,
            year = year, day = day, month = month, lyricsId = lyrics?.id, uri = uri,
        )
    }

    @Query(
        """UPDATE local_audio SET artist = :artist WHERE uri IN (:uris)"""
    )
    suspend fun updateAudioListArtist(uris: List<Uri>, artist: String)

    @Query(
        """UPDATE local_audio SET album = :album WHERE uri IN (:uris)"""
    )
    suspend fun updateAudioListAlbum(uris: List<Uri>, album: String)

    @Query(
        """UPDATE local_audio SET genre = :genre WHERE uri IN (:uris)"""
    )
    suspend fun updateAudioListGenre(uris: List<Uri>, genre: String)

    @Delete
    suspend fun deletePLSongCrossRef(crossRef: PLSongCrossRef)

    @Transaction
    suspend fun removeSongsFromPL(songIds: List<String>, playlistId: String) {
        songIds.forEach {
            deletePLSongCrossRef(
                PLSongCrossRef(songId = it, playlistId = playlistId)
            )
        }
    }

    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 0 AND permanentlyHidden = 0 ORDER BY title ASC
        """
    )
    fun getFlowOfSongsWithPlsByTitleASC(): Flow<List<AudioWithPls>>

    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 0 AND permanentlyHidden = 0 ORDER BY title DESC
        """
    )
    fun getFlowOfSongsWithPlsByTitleDESC(): Flow<List<AudioWithPls>>

    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 0 AND permanentlyHidden = 0 ORDER BY createdOn ASC
        """
    )
    fun getFlowOfSongsWithPlsByCreatedOnASC(): Flow<List<AudioWithPls>>

    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 0 AND permanentlyHidden = 0 ORDER BY createdOn DESC
        """
    )
    fun getFlowOfSongsWithPlsByCreatedOnDESC(): Flow<List<AudioWithPls>>

    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 0 AND permanentlyHidden = 0 ORDER BY artist ASC
        """
    )
    fun getFlowOfSongsWithPlsByArtistASC(): Flow<List<AudioWithPls>>

    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 0 AND permanentlyHidden = 0 ORDER BY artist DESC
        """
    )
    fun getFlowOfSongsWithPlsByArtistDESC(): Flow<List<AudioWithPls>>


    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 1 OR permanentlyHidden = 1 ORDER BY title ASC
        """
    )
    fun getFlowOfHiddenSongsByTitleASC(): Flow<List<AudioFile>>
}