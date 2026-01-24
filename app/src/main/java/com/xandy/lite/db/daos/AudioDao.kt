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
import com.xandy.lite.controllers.media.store.AudioPicToUpdate
import com.xandy.lite.db.AudioDateModified
import com.xandy.lite.db.AudioUri
import com.xandy.lite.db.AudioDetails
import com.xandy.lite.db.AudioFavorite
import com.xandy.lite.db.AudioSongId
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.AudioWithPls
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.db.tables.PLSongCrossRef
import com.xandy.lite.models.application.XANDY_CLOUD
import kotlinx.coroutines.flow.Flow


@Dao
interface AudioDao : LyricsDao, HiddenTracksDao {
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

    @Query("""SELECT * FROM local_audio WHERE song_id = :id""")
    suspend fun getTrashedItemOrNull(id: String): AudioFile?

    /** Permanently hide selected audio files. */
    @Query("""UPDATE local_audio SET hidden = 1, permanently_hidden = 1 WHERE song_id IN (:ids)""")
    suspend fun hideAudioFiles(ids: List<String>)

    @Query("""UPDATE local_audio SET hidden = 0, permanently_hidden = 0 WHERE song_id in (:ids)""")
    suspend fun showAudioFiles(ids: List<String>)

    @Query("""UPDATE local_audio SET hidden = 1, permanently_hidden = 1 WHERE uri = :uri""")
    suspend fun hideAudioFile(uri: String)

    @Query("""UPDATE local_audio SET hidden = 0, permanently_hidden = 0 WHERE uri = :uri""")
    suspend fun showAudioFile(uri: String)

    @Query(
        """
        UPDATE local_audio SET hidden = CASE WHEN uri IN (:uris) THEN 0 ELSE 1 END
        """
    )
    suspend fun updateShownAudios(uris: List<String>)

    @Query("""SELECT * FROM local_audio WHERE song_id IN (:ids) OR uri IN (:uris)""")
    suspend fun getAudioFiles(ids: List<String>, uris: List<Uri>): List<AudioFile>
    @Query("""SELECT * FROM local_audio WHERE song_id IN (:ids)""")
    fun getAudioFiles(ids: List<String>): Flow<List<AudioFile>>

    @Query("""SELECT uri FROM local_audio WHERE song_id IN (:ids)""")
    suspend fun getAudioUris(ids: List<String>): List<AudioUri>

    @Query("""SELECT * FROM local_audio""")
    suspend fun getAudioFiles(): List<AudioFile>

    @Transaction
    @Query("""SELECT * FROM local_audio WHERE song_id = :key""")
    fun getSongWithPls(key: String): Flow<AudioWithPls?>

    @Query(
        """
        SELECT hidden, permanently_hidden, lyrics_id, picture
        FROM local_audio WHERE song_id = :id OR uri = :uri
        """
    )
    suspend fun getAudioDetails(id: String, uri: Uri): AudioDetails

    @Query(
        """SELECT song_id FROM local_audio WHERE uri = :uri OR file_id = :fileId"""
    )
    suspend fun getAudioId(uri: Uri, fileId: Long): AudioSongId?

    @Query(
        """SELECT date_modified FROM local_audio WHERE uri = :uri OR file_id = :fileId"""
    )
    suspend fun getAudioDateModified(uri: Uri, fileId: Long): AudioDateModified?

    @Transaction
    @Query("""SELECT * FROM local_audio WHERE uri = :uri""")
    fun getFlowOfPickedAudio(uri: String): Flow<AudioWithPls?>


    @Transaction
    suspend fun upsertAudios(pairs: List<Pair<AudioFile, Boolean>>) {
        val originalList = getAudioFiles(pairs.map { it.first.id }, pairs.map { it.first.uri })
        val originalIds = originalList.map { it.id }
        val originalUris = originalList.map { it.uri }
        //Log.i(XANDY_CLOUD, "ids: ${pairs.map { it.first.id }}")
        pairs.forEach { pair ->
            val audio = pair.first
            val modified = pair.second
            val songId = getAudioId(audio.uri, audio.fileId)?.id
            if (audio.uri in originalUris || audio.id in originalIds || songId != null) {
                if (modified) {
                    val d = getAudioDetails(audio.id, audio.uri)
                    updateAudio(
                        AudioFile(
                            id = songId ?: audio.id, uri = audio.uri,
                            displayName = audio.displayName, picture = d.picture,
                            title = audio.title, artist = audio.artist, album = audio.album,
                            genre = audio.genre, createdOn = audio.createdOn, hidden = d.hidden,
                            durationMillis = audio.durationMillis,
                            permanentlyHidden = d.permanentlyHidden, lyricsId = d.lyricsId,
                            year = audio.year, day = audio.day, month = audio.month,
                            volumeName = audio.volumeName, bucketId = audio.bucketId,
                            fileId = audio.fileId, dateModified = audio.dateModified
                        )
                    )
                }
            } else insertAudio(audio)
        }
    }

    @Query("""UPDATE local_audio SET picture = :picture WHERE song_id = :id OR file_id = :fileId""")
    suspend fun updateAudioMedia(picture: Uri, id: String, fileId: Long)

    @Transaction
    suspend fun updateAudioMedia(list: List<AudioPicToUpdate>) {
        list.forEach {
            val songId = getAudioId(it.audio.uri, it.audio.fileId)?.id
            updateAudioMedia(it.art, songId ?: it.audio.id, it.audio.fileId)
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
        title: String, artist: String?, genre: String?, album: String?, pictureUri: Uri?,
        year: Int?, day: Int?, month: Int?, lyricsId: String?, uri: String
    )

    @Transaction
    suspend fun updateAudioTagsAndLyrics(
        title: String, artist: String?, genre: String?, album: String?, pictureUri: Uri?,
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

    @Query("""SELECT favorite FROM local_audio WHERE uri = :uri""")
    suspend fun getIsFavorite(uri: Uri): AudioFavorite

    @Query("""UPDATE local_audio SET favorite = 1 WHERE uri = :uri""")
    suspend fun favoriteSong(uri: Uri)

    @Query("""UPDATE local_audio SET favorite = 0 WHERE uri = :uri""")
    suspend fun unfavoriteSong(uri: Uri)

    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 0 AND permanently_hidden = 0 
        ORDER BY title COLLATE NOCASE ASC
        """
    )
    fun getFlowOfSongsWithPlsByTitleASC(): Flow<List<AudioWithPls>>

    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 0 AND permanently_hidden = 0 
        ORDER BY title COLLATE NOCASE DESC
        """
    )
    fun getFlowOfSongsWithPlsByTitleDESC(): Flow<List<AudioWithPls>>

    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 0 AND permanently_hidden = 0 ORDER BY created_on ASC
        """
    )
    fun getFlowOfSongsWithPlsByCreatedOnASC(): Flow<List<AudioWithPls>>

    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 0 AND permanently_hidden = 0 ORDER BY created_on DESC
        """
    )
    fun getFlowOfSongsWithPlsByCreatedOnDESC(): Flow<List<AudioWithPls>>

    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 0 AND permanently_hidden = 0 
        ORDER BY artist COLLATE NOCASE ASC
        """
    )
    fun getFlowOfSongsWithPlsByArtistASC(): Flow<List<AudioWithPls>>

    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 0 AND permanently_hidden = 0 
        ORDER BY artist COLLATE NOCASE DESC
        """
    )
    fun getFlowOfSongsWithPlsByArtistDESC(): Flow<List<AudioWithPls>>




    @Query(
        """
        SELECT * FROM local_audio WHERE favorite = 1 ORDER BY title COLLATE NOCASE ASC
    """
    )
    fun getFlowOfFavoritesTitleASC(): Flow<List<AudioFile>>

    @Query(
        """
            SELECT * FROM local_audio WHERE 
            (title LIKE '%' || :query || '%' COLLATE NOCASE)
            OR (artist LIKE '%' || :query || '%' COLLATE NOCASE)
        """
    )
    fun searchForSong(query: String): Flow<List<AudioFile>>
}