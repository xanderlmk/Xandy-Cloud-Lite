package com.xandy.lite.db.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.xandy.lite.db.tables.AudioWithPls
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenTracksDao {
    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 1 OR permanently_hidden = 1 
        ORDER BY title COLLATE NOCASE ASC
        """
    )
    fun getFlowOfHiddenSongsByTitleASC(): Flow<List<AudioWithPls>>
    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 1 OR permanently_hidden = 1 
        ORDER BY title COLLATE NOCASE DESC
        """
    )
    fun getFlowOfHiddenSongsByTitleDESC(): Flow<List<AudioWithPls>>
    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 1 OR permanently_hidden = 1 
        ORDER BY artist COLLATE NOCASE ASC
        """
    )
    fun getFlowOfHiddenSongsByArtistASC(): Flow<List<AudioWithPls>>
    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 1 OR permanently_hidden = 1 
        ORDER BY artist COLLATE NOCASE DESC
        """
    )
    fun getFlowOfHiddenSongsByArtistDESC(): Flow<List<AudioWithPls>>
    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 1 OR permanently_hidden = 1 
        ORDER BY created_on COLLATE NOCASE ASC
        """
    )
    fun getFlowOfHiddenSongsByCreatedOnASC(): Flow<List<AudioWithPls>>
    @Transaction
    @Query(
        """
        SELECT * FROM local_audio WHERE hidden = 1 OR permanently_hidden = 1 
        ORDER BY created_on COLLATE NOCASE DESC
        """
    )
    fun getFlowOfHiddenSongsByCreatedOnDESC(): Flow<List<AudioWithPls>>
}