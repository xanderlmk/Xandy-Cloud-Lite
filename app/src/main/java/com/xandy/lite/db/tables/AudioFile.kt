package com.xandy.lite.db.tables

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.xandy.lite.models.DateAsLong
import com.xandy.lite.models.UriAsString
import com.xandy.lite.models.application.AppStrings
import com.xandy.lite.models.application.AppValues
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID


private const val SONG_ID = "song_id"
private const val FILE_ID = "file_id"
private const val CREATED_ON = "created_on"
private const val DATE_MODIFIED = "date_modified"

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
        Index(value = ["uri"], name = "uri_index", unique = true),
        Index(value = [FILE_ID], name = "file_id_index")
    ]
)
data class AudioFile(
    @PrimaryKey
    @ColumnInfo(name = SONG_ID)
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = FILE_ID)
    val fileId: Long,
    @Serializable(with = UriAsString::class)
    val uri: Uri,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    val title: String,
    @ColumnInfo(defaultValue = "NULL")
    val artist: String? = null,
    val album: String?,
    val genre: String?,
    @ColumnInfo(name = "duration_millis")
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
    @ColumnInfo(name = CREATED_ON)
    val createdOn: Date,
    @Serializable(with = DateAsLong::class)
    @ColumnInfo(name = DATE_MODIFIED)
    val dateModified: Date,
    @ColumnInfo(defaultValue = "false")
    val hidden: Boolean = false,
    @ColumnInfo(defaultValue = "false")
    val favorite: Boolean = false,
    @ColumnInfo(name = "permanently_hidden", defaultValue = "false")
    val permanentlyHidden: Boolean = false,
    @ColumnInfo(name = "lyrics_id", defaultValue = "NULL")
    val lyricsId: String? = null,
    @ColumnInfo(name = "bucket_id")
    val bucketId: Long? = null,
    @ColumnInfo(name = "volume_name")
    val volumeName: String? = null
) : Parcelable {
    companion object {
        val UNDEFINED = AudioFile(
            fileId = Long.MIN_VALUE,
            uri = Uri.EMPTY,
            displayName = "####",
            title = "####", album = null, genre = null,
            durationMillis = 0,
            picture = Uri.EMPTY,
            createdOn = Date(),
            dateModified =  Date()
        )
    }
}

fun List<AudioFile>.toMediaItems(appStrings: AppStrings) =
    map { song -> song.toMediaItem(appStrings) }

fun AudioFile.toBundle(): Bundle {
    val bundle = Bundle()
    bundle.putString("uri", this.uri.toString())
    bundle.putLong(FILE_ID, this.fileId)
    bundle.putBoolean("favorite", this.favorite)
    bundle.putLong(CREATED_ON, this.createdOn.time)
    bundle.putLong(DATE_MODIFIED, this.dateModified.time)
    bundle.putBoolean("null_artist", this.artist == null)
    return bundle
}

fun AudioFile.toMediaItem(appStrings: AppStrings) = MediaItem.Builder()
    .setMediaId(this.id)
    .setUri(this.uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setArtist(this.artist ?: appStrings.unknownArtist)
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

fun List<AudioFile>.firstId() = this.first().id

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

fun MediaItem.toAudioFile(av: AppValues) = AudioFile(
    id = this.itemKey(), uri = this.uri(), title = this.title(av.unknown),
    artist = this.artist(av.unknownArtist), album = this.album(),
    durationMillis = this.mediaMetadata.durationMs ?: 0L,
    displayName = displayTitle(av.unknown), picture = this.artwork() ?: av.unknownTrackUri,
    genre = this.genre(), createdOn = this.createdOn(), favorite = this.isFavorite(),
    year = this.year(), day = this.day(), month = this.month(), fileId = this.longId(),
    dateModified = this.dateModified()
)

/** Media id which should be the AudioFile UUID */
fun MediaItem.itemKey() = this.mediaId

fun MediaItem.uri() =
    this.localConfiguration?.uri ?: this.requestMetadata.mediaUri
    ?: this.mediaMetadata.extras!!.getString("uri")!!.toUri()

private fun MediaItem.title(str: String) = this.mediaMetadata.title?.toString() ?: str
fun MediaItem.artist(str: String) = this.mediaMetadata.artist?.toString() ?: str
private fun MediaItem.genre() = this.mediaMetadata.genre?.toString()
private fun MediaItem.artwork() = this.mediaMetadata.artworkUri
private fun MediaItem.album() = this.mediaMetadata.albumTitle?.toString()
private fun MediaItem.displayTitle(str: String) = this.mediaMetadata.displayTitle?.toString() ?: str

private fun MediaItem.year() = this.mediaMetadata.releaseYear
private fun MediaItem.day() = this.mediaMetadata.releaseDay
private fun MediaItem.month() = this.mediaMetadata.releaseMonth
fun MediaItem.longId() = this.mediaMetadata.extras?.getLong(FILE_ID) ?: 0
fun MediaItem.isFavorite() = this.mediaMetadata.extras?.getBoolean("favorite") ?: false
fun MediaItem.createdOn() =
    this.mediaMetadata.extras?.getLong(CREATED_ON)?.let { Date(it) } ?: Date()

fun MediaItem.dateModified() =
    this.mediaMetadata.extras?.getLong(DATE_MODIFIED)?.let { Date(it) } ?: Date()


fun MediaItem.updateMetadata(updated: AudioFile): MediaItem {
    val newMetadata = this.mediaMetadata.buildUpon()
        .setTitle(updated.title)
        .setArtist(updated.artist)
        .setGenre(updated.genre)
        .setArtworkUri(updated.picture)
        .setReleaseYear(updated.year)
        .setReleaseMonth(updated.month)
        .setReleaseDay(updated.day)
        .setExtras(updated.toBundle())
        .build()
    return this.buildUpon()
        .setMediaMetadata(newMetadata)
        .build()
}

private fun MediaItem.toUpdatedBundle(isFavorite: Boolean): Bundle {
    val bundle = Bundle()
    bundle.putString("uri", this.uri().toString())
    bundle.putLong(FILE_ID, this.longId())
    bundle.putBoolean("favorite", isFavorite)
    bundle.putLong(CREATED_ON, this.createdOn().time)
    bundle.putLong(DATE_MODIFIED, this.dateModified().time)
    return bundle
}

fun MediaItem.replaceIsFavorite(appStrings: AppStrings): MediaItem {
    val newMetadata = this.mediaMetadata.buildUpon()
        .setTitle(this.title(appStrings.unknown))
        .setArtist(this.artist(appStrings.unknownArtist))
        .setGenre(this.genre())
        .setArtworkUri(this.artwork())
        .setReleaseYear(this.year())
        .setReleaseMonth(this.month())
        .setReleaseDay(this.day())
        .setExtras(this.toUpdatedBundle(!this.isFavorite()))
        .build()
    return this.buildUpon()
        .setMediaMetadata(newMetadata)
        .build()
}
