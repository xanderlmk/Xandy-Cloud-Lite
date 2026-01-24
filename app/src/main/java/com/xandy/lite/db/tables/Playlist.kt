package com.xandy.lite.db.tables

import android.net.Uri
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.xandy.lite.models.application.AppStrings
import com.xandy.lite.models.ui.PlaylistWithCount
import com.xandy.lite.models.ui.order.by.OBS
import com.xandy.lite.models.ui.order.by.OrderSongsBy
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.UUID

private const val PLAYLIST_ID = "playlist_id"

@Parcelize
@Entity(
    tableName = "local_playlist",
    indices = [Index(value = [PLAYLIST_ID], name = "pl_name_idx", unique = true)])
data class Playlist(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = PLAYLIST_ID)
    val name: String,
    val createdOn: Date,
    val picture: Uri?,
) : Parcelable

@Parcelize
@Entity(
    tableName = "playlist_song_order",
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = [PLAYLIST_ID],
            childColumns = [PLAYLIST_ID],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = [PLAYLIST_ID], name = "idx_pl_song_order_id")]
)
data class PlaylistSongOrder(
    @PrimaryKey
    @ColumnInfo(name = PLAYLIST_ID)
    val name: String,
    @ColumnInfo(defaultValue = OBS.CREATED_ON_ASC)
    val orderedBy: OrderSongsBy = OrderSongsBy.CreatedOnASC
) : Parcelable

private fun songComparator(order: OrderSongsBy, unknown: String): Comparator<SongWithDateAdded> =
    Comparator { a, b ->
        when (order) {
            is OrderSongsBy.TitleASC -> a.data.title.compareTo(b.data.title, ignoreCase = true)
            is OrderSongsBy.TitleDESC -> b.data.title.compareTo(a.data.title, ignoreCase = true)
            is OrderSongsBy.CreatedOnASC -> {
                val da: Date = a.crossRef.dateAdded
                val db: Date = b.crossRef.dateAdded
                da.compareTo(db)
            }

            is OrderSongsBy.CreatedOnDESC -> {
                val da: Date = a.crossRef.dateAdded
                val db: Date = b.crossRef.dateAdded
                db.compareTo(da)
            }

            is OrderSongsBy.ArtistASC ->
                (a.data.artist ?: unknown).compareTo(b.data.artist?: unknown, ignoreCase = true)

            is OrderSongsBy.ArtistDESC ->
                (b.data.artist ?: unknown).compareTo(a.data.artist?: unknown, ignoreCase = true)
        }
    }

fun List<LocalPlsWithAudio>.toPlaylistWithCount(appStrings: AppStrings) = this.map {
    val order = it.order.orderedBy
    val sortedSongs = it.songs.sortedWith(songComparator(order, appStrings.unknownArtist))
    PlaylistWithCount(
        playlist = it.playlist,
        songs = sortedSongs,
        songCount = sortedSongs.size,
        order = it.order.orderedBy
    )
}
