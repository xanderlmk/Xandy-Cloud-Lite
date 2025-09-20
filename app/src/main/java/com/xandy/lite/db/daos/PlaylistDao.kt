package com.xandy.lite.db.daos

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.xandy.lite.db.tables.PlaylistSongOrder
import com.xandy.lite.db.tables.LocalPlsWithAudio
import com.xandy.lite.db.tables.PLSongCrossRef
import com.xandy.lite.db.tables.Playlist
import com.xandy.lite.models.ui.PlaylistName
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Transaction
    @Query("""SELECT * FROM local_playlist ORDER BY playlist_id ASC""")
    fun getFlowOfPlsWithSongsByNameASC(): Flow<List<LocalPlsWithAudio>>

    @Transaction
    @Query("""SELECT * FROM local_playlist ORDER BY playlist_id DESC""")
    fun getFlowOfPlsWithSongsByNameDESC(): Flow<List<LocalPlsWithAudio>>

    @Transaction
    @Query("""SELECT * FROM local_playlist ORDER BY createdOn ASC""")
    fun getFlowOfPlsWithSongsByCreatedOnASC(): Flow<List<LocalPlsWithAudio>>

    @Transaction
    @Query("""SELECT * FROM local_playlist ORDER BY createdOn DESC""")
    fun getFlowOfPlsWithSongsByCreatedOnDESC(): Flow<List<LocalPlsWithAudio>>

    @Query(
        """
        UPDATE local_playlist SET picture = :picture WHERE playlist_id = :name
    """
    )
    suspend fun updatePlArtwork(name: String, picture: Uri)

    @Update
    suspend fun updatePLOrder(order: PlaylistSongOrder)

    @Update
    suspend fun updatePlaylist(localPlaylist: Playlist)

    @Query("""SELECT COUNT(*) FROM local_playlist where playlist_id = :string""")
    suspend fun checkIfPlExists(string: String): Int
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPLOrderBy(ref: PlaylistSongOrder)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Transaction
    suspend fun insertPL(playlist: Playlist) {
        insertPlaylist(playlist)
        insertPLOrderBy(PlaylistSongOrder(playlist.name))
    }

    /**
     * Insert a new row for the Cross reference.
     * If it exists, ignore.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(crossRef: PLSongCrossRef)

    @Query("""SELECT playlist_id FROM local_playlist where id = :id""")
    suspend fun getPlaylistNameById(id: String): PlaylistName

    @Transaction
    suspend fun addSongsToPl(songIds: List<String>, playlistId: String) {
        val n = getPlaylistNameById(playlistId)
        songIds.forEach {
            addSongToPlaylist(
                PLSongCrossRef(songId = it.toUri(), playlistId = n.name)
            )
        }
    }
    @Transaction
    suspend fun addPlWithSongs(songIds: List<String>, pl: Playlist) {
        insertPlaylist(pl)
        insertPLOrderBy(PlaylistSongOrder(pl.name))
        songIds.forEach {
            addSongToPlaylist(
                PLSongCrossRef(songId = it.toUri(), playlistId = pl.name)
            )
        }
    }

    @Query("""UPDATE local_playlist SET playlist_id = :newName WHERE playlist_id = :name""")
    suspend fun updatePlaylistName(newName:String, name: String)
}