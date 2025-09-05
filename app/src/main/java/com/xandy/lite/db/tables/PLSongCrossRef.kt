package com.xandy.lite.db.tables

import android.net.Uri
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.parcelize.Parcelize
import java.util.Date

private const val SONG_ID = "song_id"
private const val PLAYLIST_ID = "playlist_id"

@Parcelize
@Entity(
    tableName = "local_pl_song_cross_ref",
    primaryKeys = [PLAYLIST_ID, SONG_ID],
    foreignKeys = [
        ForeignKey(
            entity = AudioFile::class,
            parentColumns = [SONG_ID],
            childColumns = [SONG_ID],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Playlist::class,
            parentColumns = [PLAYLIST_ID],
            childColumns = [PLAYLIST_ID],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = [SONG_ID, PLAYLIST_ID], unique = true, name = "idx_local_pl_song_cross_ref"),
        Index(value = [SONG_ID], name = "idx_local_pl_song_id"),
        Index(value = [PLAYLIST_ID], name = "idx_local_song_pl_id")
    ]
)
data class PLSongCrossRef(
    @ColumnInfo(name = SONG_ID)
    val songId: Uri,
    @ColumnInfo(name = PLAYLIST_ID)
    val playlistId: String,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val dateAdded: Date = Date()
) : Parcelable
