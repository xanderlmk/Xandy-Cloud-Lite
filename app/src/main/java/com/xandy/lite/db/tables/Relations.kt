package com.xandy.lite.db.tables

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import kotlinx.parcelize.Parcelize


private const val SONG_ID = "song_id"
private const val PLAYLIST_ID = "playlist_id"

@Parcelize
data class SongWithDateAdded(
    @Embedded val crossRef: PLSongCrossRef,
    @Relation(
        parentColumn = SONG_ID,
        entityColumn = SONG_ID,
        entity = AudioFile::class
    )
    val data: AudioFile
) : Parcelable

/**
 * Local playlist with all the audio it and the order it is in.
 * @param playlist The playlist
 * @param songs The songs with the date they were added to the playlist
 * @param order The order the songs will be within the playlist
 * @see PlaylistSongOrder
 */
@Parcelize
data class LocalPlsWithAudio(
    @Embedded val playlist: Playlist,
    @Relation(
        entity = PLSongCrossRef::class,
        parentColumn = PLAYLIST_ID,
        entityColumn = PLAYLIST_ID
    )
    val songs: List<SongWithDateAdded>,
    @Relation(
        entity = PlaylistSongOrder::class,
        parentColumn = PLAYLIST_ID,
        entityColumn = PLAYLIST_ID
    )
    val order: PlaylistSongOrder
) : Parcelable

@Parcelize
data class AudioWithPls(
    @Embedded val song: AudioFile,
    @Relation(
        entity = Playlist::class,
        parentColumn = SONG_ID,
        entityColumn = PLAYLIST_ID,
        associateBy = Junction(
            value = PLSongCrossRef::class,
            parentColumn = SONG_ID,
            entityColumn = PLAYLIST_ID
        )
    )
    val playlists: List<Playlist>
) : Parcelable

@Parcelize
data class BucketWithAudio(
    @Embedded val bucket: Bucket,
    @Relation(
        entity = AudioFile::class,
        parentColumn = "id", entityColumn = "bucket_id"
    )
    val audioList: List<AudioFile>
): Parcelable