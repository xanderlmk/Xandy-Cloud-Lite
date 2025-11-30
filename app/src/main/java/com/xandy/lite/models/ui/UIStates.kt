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
import java.util.Date


@Parcelize
data class AudioUIState(
    val list: List<AudioWithPls> = emptyList()
) : Parcelable


data class SongDetails(
    val id: String, val title: String, val artist: String,
    val album: String?, val picture: Any?, val lyrics: Lyrics?
)

data class PickedSongVMStates(
    val song: SongDetails?, val isPlaying: Boolean,
    val isLoading: Boolean, val repeatMode: Int, val shuffleMode: Boolean,
    val sortedQueue: List<MediaItemWithCreatedOn>, val unsortedQueue: List<MediaItemWithCreatedOn>,
    val queueSize: Int, val queueAsc: Boolean, val queueOrder: OrderQueueBy

)

@Parcelize
data class LocalAudioStates(
    val isLoading: Boolean, val alDirection: Boolean, val isSelecting: Boolean,
    val plsDirection: Boolean, val tab: LocalMusicTabs, val isSearching: Boolean,
    val gettingPics: Boolean, val autoUpdate: Boolean
) : Parcelable

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
    val genres: List<Genre> = emptyList()
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
    val name: String, val picture: Uri, val songs: List<AudioFile>, val songCount: Int
) : Parcelable

@Parcelize
data class Genre(
    val name: String, val picture: Uri, val songs: List<AudioFile>, val songCount: Int
) : Parcelable

fun Flow<LocalPlUIState>.toPlaylists() = this.map { it.list.map { pl -> pl.playlist } }

data class MediaItemWithCreatedOn(val mediaItem: MediaItem, val createdOn: Date)

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
    data object Queue : SongToggle()

    @Parcelize
    data object Lyrics : SongToggle()
}

fun Context.drawableResUri(@DrawableRes resId: Int): Uri =
    Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(packageName)
        .appendPath(resources.getResourceTypeName(resId))
        .appendPath(resources.getResourceEntryName(resId))
        .build()

