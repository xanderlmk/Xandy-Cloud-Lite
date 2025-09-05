package com.xandy.lite.db.tables

import android.net.Uri
import android.os.Parcelable
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.xandy.lite.models.ui.MediaItemWithCreatedOn
import com.xandy.lite.models.ui.itemKey
import kotlinx.parcelize.Parcelize
import java.util.Date


private const val SONG_ID = "song_id"

/**
 * Local Audio File
 * @param uri The Uri of the file.
 * @param displayName The file name.
 * @param title The title of the song, if it does not exist it'll default to [displayName].
 * @param artist The artist of the song, if it does not exist it'll default to "Unknown Artist".
 * @param album The album the song is in, can be null.
 * @param genre The genre the song is in, can be null.
 * @param durationMillis The duration of the song/audio in Milliseconds.
 * @param picture The artwork of the song, if it does not exist, it'll default to UnknownTrack.png
 * @param hidden Whether the song is hidden by a folder that is hidden.
 * @param permanentlyHidden Whether the song is hidden by the user.
 * We have [hidden] and [permanentlyHidden] because if a user decides to unhide a folder,
 * the hidden audio which might be hidden by the user, can then be unhidden too.
 */
@Parcelize
@Entity(
    tableName = "local_audio",
    foreignKeys = [
        ForeignKey(
            entity = Bucket::class,
            parentColumns = ["volume_name", "id"],
            childColumns = ["volume_name", "bucket_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["volume_name", "bucket_id"], name = "bucket_reference_index")
    ]
)
data class AudioFile(
    @PrimaryKey
    @ColumnInfo(name = SONG_ID)
    val uri: Uri,
    val displayName: String,
    val title: String,
    val artist: String,
    val album: String?,
    val genre: String?,
    val durationMillis: Long,
    val picture: Uri,
    val createdOn: Date,
    @ColumnInfo(defaultValue = "false")
    val hidden: Boolean = false,
    @ColumnInfo(defaultValue = "false")
    val permanentlyHidden: Boolean = false,
    @ColumnInfo(name = "bucket_id")
    val bucketId: Long? = null,
    @ColumnInfo(name = "volume_name")
    val volumeName: String? = null
) : Parcelable

fun List<AudioFile>.toMediaItems() = map { song ->
    MediaItem.Builder()
        .setMediaId(song.uri.toString())
        .setUri(song.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setArtist(song.artist)
                .setTitle(song.title)
                .setGenre(song.genre)
                .setArtworkUri(song.picture)
                .build()
        )
        .build()
}

fun AudioFile.toMediaItem() = MediaItem.Builder()
    .setMediaId(this.uri.toString())
    .setUri(this.uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setArtist(this.artist)
            .setTitle(this.title)
            .setGenre(this.genre)
            .setArtworkUri(this.picture)
            .build()
    )
    .build()

fun AudioFile.toMediaItemWithCreatedOn() =
    MediaItemWithCreatedOn(this.toMediaItem(), this.createdOn)

fun List<AudioFile>.toMediaItemsWithCreatedOn() = this.map { audio ->
    MediaItemWithCreatedOn(audio.toMediaItem(), audio.createdOn)
}

fun MediaItem.toAudioFile(unknownTrackUri: Uri) = AudioFile(
    uri = this.itemKey().toUri(), title = this.title(), artist = this.artist(),
    album = this.album(), durationMillis = this.mediaMetadata.durationMs ?: 0L,
    displayName = displayTitle(), picture = this.artwork() ?: unknownTrackUri,
    genre = this.genre(), createdOn = Date()
)

private const val UNKNOWN = "Unknown Title"
private fun MediaItem.title() = this.mediaMetadata.title?.toString() ?: UNKNOWN
fun MediaItem.artist() = this.mediaMetadata.artist?.toString() ?: "Unknown Artist"
private fun MediaItem.genre() = this.mediaMetadata.genre?.toString()
private fun MediaItem.artwork() = this.mediaMetadata.artworkUri
private fun MediaItem.album() = this.mediaMetadata.albumTitle?.toString()
private fun MediaItem.displayTitle() = this.mediaMetadata.displayTitle?.toString() ?: UNKNOWN