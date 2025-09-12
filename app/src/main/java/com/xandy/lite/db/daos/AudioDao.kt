package com.xandy.lite.db.daos

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.AudioWithPls
import com.xandy.lite.db.tables.PLSongCrossRef
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.ui.AudioUri
import com.xandy.lite.models.ui.IsAudioHidden
import kotlinx.coroutines.flow.Flow


@Dao
interface AudioDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAudio(audioFile: AudioFile)

    @Update
    suspend fun updateAudio(audioFile: AudioFile)

    @Delete
    suspend fun deleteAudio(audioFile: AudioFile)

    @Delete(entity = AudioFile::class)
    suspend fun deleteAudioByUri(vararg uri: AudioUri)

    @Delete
    suspend fun deleteAudioFiles(list: List<AudioFile>)

    @Query("""SELECT * FROM local_audio WHERE song_id IN (:ids)""")
    suspend fun getInitialQueue(ids: List<String>): List<AudioFile>

    /** Permanently hide selected audio files. */
    @Query("""UPDATE local_audio SET hidden = 1, permanentlyHidden = 1 WHERE song_id IN (:ids)""")
    suspend fun hideAudioFiles(ids: List<String>)

    @Query("""UPDATE local_audio SET hidden = 0, permanentlyHidden = 0 WHERE song_id in (:ids)""")
    suspend fun showAudioFiles(ids: List<String>)

    @Query("""UPDATE local_audio SET hidden = 1, permanentlyHidden = 1 WHERE song_id = :uri""")
    suspend fun hideAudioFile(uri: String)

    @Query("""UPDATE local_audio SET hidden = 0, permanentlyHidden = 0 WHERE song_id = :uri""")
    suspend fun showAudioFile(uri: String)

    @Query(
        """
        UPDATE local_audio SET hidden = CASE WHEN song_id IN (:uris) THEN 0 ELSE 1 END
        """
    )
    suspend fun updateShownAudios(uris: List<String>)

    @Query("""SELECT * FROM local_audio WHERE song_id IN (:ids)""")
    suspend fun getAudioFiles(ids: List<String>): List<AudioFile>

    @Query("""SELECT * FROM local_audio""")
    suspend fun getAudioFiles(): List<AudioFile>

    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE song_id = :key
        """
    )
    suspend fun getSongWithPls(key: String): AudioWithPls?

    @Query("""SELECT hidden, permanentlyHidden FROM local_audio WHERE song_id = :uri""")
    suspend fun getAudioHidden(uri: Uri): IsAudioHidden

    @Transaction
    @Query("""SELECT * FROM local_audio WHERE song_id = :uri""")
    fun getFlowOfPickedAudio(uri: String): Flow<AudioWithPls?>

    @Transaction
    suspend fun upsertAudios(audios: List<AudioFile>) {
        val originalList = getAudioFiles(audios.map { it.uri.toString() })
        val originalListIds = originalList.map { it.uri }.toSet()
        Log.d(XANDY_CLOUD, "Upserting: ${audios.size} audio files")
        audios.forEach { audio ->
            if (audio.uri in originalListIds) {
                val hidden = getAudioHidden(audio.uri)
                updateAudio(
                    AudioFile(
                        uri = audio.uri, displayName = audio.displayName, picture = audio.picture,
                        title = audio.title, artist = audio.artist, album = audio.album,
                        genre = audio.genre, createdOn = audio.createdOn,
                        hidden = hidden.hidden, durationMillis = audio.durationMillis,
                        permanentlyHidden = hidden.permanentlyHidden,
                        volumeName = audio.volumeName, bucketId = audio.bucketId
                    )
                )
            } else insertAudio(audio)
        }
    }

    @Query(
        """
        UPDATE local_audio 
        SET title = :title, artist = :artist, genre = :genre, album = :album, picture = :pictureUri
        WHERE song_id = :uri
        """
    )
    suspend fun updateAudioTags(
        title: String, artist: String, genre: String?, album: String?, pictureUri: Uri?, uri: String
    )

    @Delete
    suspend fun deletePLSongCrossRef(crossRef: PLSongCrossRef)

    @Transaction
    suspend fun removeSongsFromPL(songIds: List<String>, playlistId: String) {
        songIds.forEach {
            deletePLSongCrossRef(
                PLSongCrossRef(songId = it.toUri(), playlistId = playlistId)
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