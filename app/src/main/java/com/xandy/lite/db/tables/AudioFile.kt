package com.xandy.lite.db.tables

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.xandy.lite.models.DateAsLong
import com.xandy.lite.models.UriAsString
import com.xandy.lite.models.itemKey
import com.xandy.lite.models.ui.MediaItemWithCreatedOn
import com.xandy.lite.models.uri
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID


private const val SONG_ID = "song_id"

/**
 * Local Audio File
 * @param id - Internal song id from the metadata, where it can be null.
 * If it's null a uuid will be generated and stored in the database.
 * Then there will be a request to update the metadata property of the song id of the file.
 * We do this because in the future we'll allow users to share playlists that have songs where
 * the uri is not enough to make a connection between a playlist and a song.
 * @param uri The Uri of the file.
 * @param displayName The file name.
 * @param title The title of the song, if it does not exist it'll default to [displayName].
 * @param artist The artist of the song, if it does not exist it'll default to "Unknown Artist".
 * @param album The album the song is in, can be null.
 * @param genre The genre the song is in, can be null.
 * @param durationMillis The duration of the song/audio in Milliseconds.
 * @param year Release year
 * @param day Release day
 * @param month Release month
 * @param picture The artwork of the song, if it does not exist, it'll default to UnknownTrack.png
 * @param createdOn The date the audio file was added/created
 * @param hidden Whether the song is hidden by a folder that is hidden.
 * @param permanentlyHidden Whether the song is hidden by the user.
 * @param bucketId Bucket/folder the song belongs to
 * @param volumeName Volume the song belongs to
 * We have [hidden] and [permanentlyHidden] because if a user decides to unhide a folder,
 * the hidden audio which might be hidden by the user, can then be unhidden too.
 */
@Serializable
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
        ),
        ForeignKey(
            entity = Lyrics::class,
            parentColumns = ["id"],
            childColumns = ["lyrics_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["volume_name", "bucket_id"], name = "bucket_reference_index"),
        Index(value = ["lyrics_id"], name = "lyrics_reference_index"),
        Index(value = ["uri"], name = "uri_index", unique = true)
    ]
)
data class AudioFile(
    @PrimaryKey
    @ColumnInfo(name = SONG_ID)
    val id: String = UUID.randomUUID().toString(),
    @Serializable(with = UriAsString::class)
    val uri: Uri,
    val displayName: String,
    val title: String,
    val artist: String,
    val album: String?,
    val genre: String?,
    val durationMillis: Long,
    @ColumnInfo(defaultValue = "NULL")
    val year: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val day: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val month: Int? = null,
    @Serializable(with = UriAsString::class)
    val picture: Uri,
    @Serializable(with = DateAsLong::class)
    val createdOn: Date,
    @ColumnInfo(defaultValue = "false")
    val hidden: Boolean = false,
    @ColumnInfo(defaultValue = "false")
    val permanentlyHidden: Boolean = false,
    @ColumnInfo(name = "lyrics_id", defaultValue = "NULL")
    val lyricsId: String? = null,
    @ColumnInfo(name = "bucket_id")
    val bucketId: Long? = null,
    @ColumnInfo(name = "volume_name")
    val volumeName: String? = null
) : Parcelable

fun List<AudioFile>.toMediaItems() = map { song -> song.toMediaItem() }

fun AudioFile.toBundle(): Bundle {
    val bundle = Bundle()
    bundle.putString("uri", this.uri.toString())
    return bundle
}
fun AudioFile.toMediaItem() = MediaItem.Builder()
    .setMediaId(this.id)
    .setUri(this.uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setArtist(this.artist)
            .setTitle(this.title)
            .setGenre(this.genre)
            .setArtworkUri(this.picture)
            .setReleaseYear(this.year)
            .setReleaseDay(this.day)
            .setReleaseMonth(this.month)
            .setExtras(this.toBundle())
            .build()
    )
    .build()
fun AudioFile.toMediaItemWithCreatedOn() =
    MediaItemWithCreatedOn(this.toMediaItem(), this.createdOn)

fun List<AudioFile>.toMediaItemsWithCreatedOn() = this.map { audio ->
    MediaItemWithCreatedOn(audio.toMediaItem(), audio.createdOn)
}

/**
 * Convert the year, month, and day into a formatted string such where
 *
 * If none is missing: yyyy-mm-dd
 *
 * If the day is missing: yyyy-mm
 *
 * If the month is missing (and even if the day is included): yyyy
 *
 * Else: null
 */
fun AudioFile.datedString() = when {
    this.year != null && this.month != null && this.day != null ->
        "${this.year}-${this.month}-${this.day}"

    this.year != null && this.month != null -> "${this.year}-${this.month}"
    this.year != null -> "${this.year}"
    else -> null
}

fun AudioFile.isNotInternal() = !this.uri.toString().startsWith("content://media/internal")

fun MediaItem.toAudioFile(unknownTrackUri: Uri) = AudioFile(
    id = this.itemKey() , uri = this.uri(), title = this.title(), artist = this.artist(),
    album = this.album(), durationMillis = this.mediaMetadata.durationMs ?: 0L,
    displayName = displayTitle(), picture = this.artwork() ?: unknownTrackUri,
    genre = this.genre(), createdOn = Date(),
    year = this.year(), day = this.day(), month = this.month()
)

private const val UNKNOWN = "Unknown Title"
private fun MediaItem.title() = this.mediaMetadata.title?.toString() ?: UNKNOWN
fun MediaItem.artist() = this.mediaMetadata.artist?.toString() ?: "Unknown Artist"
private fun MediaItem.genre() = this.mediaMetadata.genre?.toString()
private fun MediaItem.artwork() = this.mediaMetadata.artworkUri
private fun MediaItem.album() = this.mediaMetadata.albumTitle?.toString()
private fun MediaItem.displayTitle() = this.mediaMetadata.displayTitle?.toString() ?: UNKNOWN

private fun MediaItem.year() = this.mediaMetadata.releaseYear
private fun MediaItem.day() = this.mediaMetadata.releaseDay
private fun MediaItem.month() = this.mediaMetadata.releaseMonth