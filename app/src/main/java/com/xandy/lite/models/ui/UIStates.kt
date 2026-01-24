package com.xandy.lite.models.ui

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.media3.common.MediaItem
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.BucketWithAudio
import com.xandy.lite.db.tables.AudioWithPls
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.db.tables.Playlist
import com.xandy.lite.db.tables.SongWithDateAdded
import com.xandy.lite.models.ui.order.by.OrderQueueBy
import com.xandy.lite.models.ui.order.by.OrderSongsBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Parcelize
data class AudioUIState(
    val list: List<AudioWithPls> = emptyList()
) : Parcelable


data class SongDetails(
    val id: String, val title: String, val artist: String,
    val album: String?, val picture: Any?, val lyrics: Lyrics?
)


@Parcelize
data class LocalAudioStates(
    val isLoading: Boolean, val isSelecting: Boolean, val alDirection: Boolean,
    val hiddenDirection: Boolean, val plsDirection: Boolean, val albumDirection: Boolean,
    val artistDirection: Boolean, val genreDirection: Boolean, val favDirections: Boolean,
    val tab: LocalMusicTabs, val isSearching: Boolean, val gettingPics: Boolean,
    val autoUpdate: Boolean
) : Parcelable

@Parcelize
data class IsDefaultMediaOrder(
    val album: Boolean, val artist: Boolean, val genre: Boolean
) : Parcelable

@Parcelize
data class MediaDirections(
    val alDirection: Boolean, val hiddenDirection: Boolean, val plsDirection: Boolean,
    val albumDirection: Boolean, val artistDirection: Boolean, val genreDirection: Boolean,
    val favDirection: Boolean
) : Parcelable

private const val MEDIA_STATE = "MediaState"

@Serializable
@SerialName(MEDIA_STATE)
data class MediaState(
    /** The key of the given media item, can be album, artist, or genre) */
    @SerialName("$MEDIA_STATE.Key")
    val key: String,
    /** The id of the first song in the list */
    @SerialName("$MEDIA_STATE.First")
    val first: String
)

fun MediaState.isBlank() = key.isBlank() && first.isBlank()

/**
 * @param audioUIState Shown Audio list
 * @param plsWithAudios All playlist, each with their respective songs
 * @param hiddenAudios Hidden audio list
 * @param folders All folders
 */
@Parcelize
data class LocalMediaStates(
    val audioUIState: AudioUIState = AudioUIState(),
    val plsWithAudios: LocalPlUIState = LocalPlUIState(),
    val hiddenAudios: List<AudioFile> = emptyList(),
    val folders: List<BucketWithAudio> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val favorites: List<AudioFile> = emptyList()
) : Parcelable


@Parcelize
data class PlaylistWithCount(
    val playlist: Playlist,
    val order: OrderSongsBy,
    val songs: List<SongWithDateAdded>,
    val songCount: Int
) : Parcelable

@Parcelize
data class LocalPlUIState(
    val list: List<PlaylistWithCount> = emptyList()
) : Parcelable

/**
 * @param audioUIState Shown Audio list
 * @param plsWithAudios All playlist, each with their respective songs
 * @param hiddenAudios Hidden audio list
 * @param folders All folders
 */
@Parcelize
data class MediaStates(
    val audioUIState: AudioUIState,
    val plsWithAudios: LocalPlUIState,
    val hiddenAudios: List<AudioFile>,
    val folders: List<BucketWithAudio>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val genres: List<Genre>
) : Parcelable

@Parcelize
data class Album(
    val name: String, val artist: String, val picture: Uri, val songs: List<AudioFile>,
    val songCount: Int
) : Parcelable

@Parcelize
data class Artist(
    val name: String, val picture: Uri, val songs: List<AudioFile>, val songCount: Int,
    val albums: List<String>, val albumCount: Int
) : Parcelable

@Parcelize
data class Genre(
    val name: String, val picture: Uri, val songs: List<AudioFile>, val songCount: Int
) : Parcelable

fun Flow<LocalPlUIState>.toPlaylists() = this.map { it.list.map { pl -> pl.playlist } }


@Parcelize
sealed class ShowModalFor : Parcelable {
    @Parcelize
    data object Idle : ShowModalFor()

    @Parcelize
    data object LocalPl : ShowModalFor()
}

@Parcelize
sealed class SongToggle : Parcelable {
    @Parcelize
    data object Details : SongToggle()

    @Parcelize
    data class Queue(val priority: Boolean = false) : SongToggle()

    /**
     * @param sync Whether to show plain lyrics or synchronized lyrics,
     * if false show plain lyrics
     */
    @Parcelize
    data class Lyrics(val sync: Boolean = true) : SongToggle()
}

fun Context.drawableResUri(@DrawableRes resId: Int): Uri =
    Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(packageName)
        .appendPath(resources.getResourceTypeName(resId))
        .appendPath(resources.getResourceEntryName(resId))
        .build()

