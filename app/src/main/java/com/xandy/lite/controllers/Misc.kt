package com.xandy.lite.controllers

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.AudioWithPls
import com.xandy.lite.db.tables.BucketWithAudio
import com.xandy.lite.db.tables.LyricLine
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.db.tables.createdOn
import com.xandy.lite.db.tables.itemKey
import com.xandy.lite.db.tables.toMediaItems
import com.xandy.lite.models.application.AppStrings
import com.xandy.lite.models.application.AppValues
import com.xandy.lite.models.application.dataStore
import com.xandy.lite.models.ui.Album
import com.xandy.lite.models.ui.Artist
import com.xandy.lite.models.ui.AudioUIState
import com.xandy.lite.models.ui.Genre
import com.xandy.lite.models.ui.LocalPlUIState
import com.xandy.lite.models.ui.MediaState
import com.xandy.lite.models.ui.PlaylistWithCount
import com.xandy.lite.models.ui.SongDetails
import com.xandy.lite.models.ui.isBlank
import com.xandy.lite.models.ui.order.by.OrderAlbumsBy
import com.xandy.lite.models.ui.order.by.OrderArtistBy
import com.xandy.lite.models.ui.order.by.OrderGenresBy
import com.xandy.lite.models.ui.order.by.OrderPlsBy
import com.xandy.lite.models.ui.order.by.OrderQueueBy
import com.xandy.lite.models.ui.order.by.OrderSongsBy
import com.xandy.lite.models.ui.order.by.isAscending
import com.xandy.lite.widget.XandyWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.UUID
import kotlin.collections.map

fun shareSingleAudio(context: Context, file: AudioFile) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(Intent.EXTRA_STREAM, file.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share audio via"))
}

fun shareMultipleAudios(context: Context, fileUris: List<String>) {
    val uris = fileUris.map { it.toUri() }
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "audio/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share audios via"))
}

fun copyFileToInternalStorage(
    context: Context, sourceUri: Uri,
    maxWidth: Int = 1280, maxHeight: Int = 720, quality: Int = 95,
): Uri {
    val fileName = "$${System.currentTimeMillis()}-${UUID.randomUUID()}.jpeg"
    // 1. Open input and output streams
    val inputStream = context.contentResolver.openInputStream(sourceUri)
        ?: throw IllegalArgumentException("Cannot open input stream")
    val originalBitmap = BitmapFactory.decodeStream(inputStream)
        ?: throw IllegalArgumentException("Cannot decode bitmap")

    // Resize while keeping aspect ratio
    val resizedBitmap = resizeBitmapMaintainingAspectRatio(originalBitmap, maxWidth, maxHeight)

    val outFile = File(context.filesDir, fileName)
    FileOutputStream(outFile).use { outputStream ->
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    }

    // 3. Return a file:// URI pointing at your private copy
    return Uri.fromFile(outFile)
}


fun Uri.deleteLocalFile() {
    this.takeIf { it.scheme == "file" }?.path?.let { File(it).delete() }
}

private fun resizeBitmapMaintainingAspectRatio(
    bitmap: Bitmap,
    maxWidth: Int,
    maxHeight: Int
): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    val aspectRatio = width.toFloat() / height
    val targetWidth: Int
    val targetHeight: Int

    if (width > height) {
        targetWidth = maxWidth
        targetHeight = (maxWidth / aspectRatio).toInt()
    } else {
        targetHeight = maxHeight
        targetWidth = (maxHeight * aspectRatio).toInt()
    }

    return bitmap.scale(targetWidth, targetHeight)
}

private fun OrderQueueBy.songComparator(): Comparator<MediaItem> =
    Comparator { a, b ->
        when (this) {
            is OrderQueueBy.TitleASC -> a.title()
                .compareTo(b.title(), ignoreCase = true)

            is OrderQueueBy.TitleDESC -> b.title()
                .compareTo(a.title(), ignoreCase = true)

            is OrderQueueBy.CreatedOnASC -> {
                val da: Date = a.createdOn()
                val db: Date = b.createdOn()
                da.compareTo(db)
            }

            is OrderQueueBy.CreatedOnDESC -> {
                val da: Date = a.createdOn()
                val db: Date = b.createdOn()
                db.compareTo(da)
            }

            is OrderQueueBy.ArtistASC -> a.artist().compareTo(b.artist(), ignoreCase = true)

            is OrderQueueBy.ArtistDESC -> b.artist()
                .compareTo(a.artist(), ignoreCase = true)

            is OrderQueueBy.Default -> 0
        }
    }

private fun MediaItem.title() = this.mediaMetadata.title?.toString() ?: "Unknown Title"
private fun MediaItem.artist() = this.mediaMetadata.artist?.toString() ?: "Unknown Artist"

fun combineMCWithPickedSong(
    mediaController: StateFlow<MediaController?>, pickedSong: Flow<AudioWithPls?>,
    appValues: StateFlow<AppValues>
): Flow<SongDetails?> = combine(mediaController, pickedSong, appValues) { controller, a, values ->
    val mediaMetadata = controller?.currentMediaItem?.mediaMetadata
    val id =
        a?.song?.id ?: controller?.currentMediaItem?.itemKey() ?: return@combine null
    if (mediaMetadata == null && a == null) null
    else {
        val sd = combineSongWithMediaMetadata(a, mediaMetadata, values, id)
        //Log.i("Xandy-Cloud", "$sd")
        sd
    }
}.flowOn(Dispatchers.Main.limitedParallelism(1, "Song Details"))

private fun combineSongWithMediaMetadata(
    af: AudioWithPls?, item: MediaMetadata?, appValues: AppValues, id: String
): SongDetails {
    val title = (af?.song?.title ?: item?.title)?.takeIf { it.isNotBlank() } ?: appValues.unknown
    val artist = af?.song?.artist ?: appValues.unknownArtist
    val album = af?.song?.album ?: item?.albumTitle
    val artwork = af?.song?.picture ?: item?.artworkUri ?: appValues.unknownTrackUri
    // Log.i(XANDY_CLOUD, "${af?.lyrics}")
    return SongDetails(
        id, title.toString(), artist, album?.toString(), artwork,
        af?.lyrics?.refineScroll()
    )
}

private fun Lyrics.refineScroll() = this.copy(scroll = scroll.insertGapPlaceholders())

private fun Set<LyricLine>?.insertGapPlaceholders(thresholdMs: Long = 3_000L): Set<LyricLine> {
    val sorted = this?.sortedBy { it.range.first } ?: return emptySet()
    if (sorted.isEmpty()) return emptySet()

    val out = ArrayList<LyricLine>(sorted.size * 2)
    for (i in sorted.indices) {
        val curr = sorted[i]
        out.add(curr)

        val next = sorted.getOrNull(i + 1) ?: continue

        val gapStart = curr.range.last + 1
        val gapEnd = next.range.first - 1

        if (gapStart <= gapEnd) {
            val gapLength = (gapEnd - gapStart + 1)
            if (gapLength >= thresholdMs) {
                // create a transient placeholder LyricLine; not saved to DB
                val placeholder = LyricLine(LongRange(gapStart, gapEnd), "$;12345$")
                out.add(placeholder)
            }
        }
    }
    return out.toSet()
}


fun combinePickedUUIDWithPl(
    uuid: Flow<String>, localPlaylists: Flow<LocalPlUIState>
): Flow<PlaylistWithCount?> = combine(uuid, localPlaylists) { id, pls ->
    if (id.isBlank() || pls.list.isEmpty()) null
    else pls.list.find { it.playlist.id == id }
}

fun combinePickedNameWithLocalAlbum(
    state: Flow<MediaState>, localAlbums: Flow<List<Album>>
): Flow<Album?> = combine(state, localAlbums) { n, albums ->
    if (n.isBlank() || albums.isEmpty()) null
    else albums.find { it.name == n.key.trim() && it.songs.first().id == n.first }
}

fun combineBucketKeyWithLocalBucket(
    key: Flow<Pair<String, Long?>>, buckets: Flow<List<BucketWithAudio>>
): Flow<BucketWithAudio?> = combine(key, buckets) { k, b ->
    if (k.first.isEmpty() || k.second == null || b.isEmpty()) null
    else b.find { it.bucket.id == k.second && it.bucket.volumeName == k.first }
}

fun combinePickedNameWithLocalGenre(
    state: Flow<MediaState>, localGenres: Flow<List<Genre>>
): Flow<Genre?> = combine(state, localGenres) { n, genres ->
    if (n.isBlank() || genres.isEmpty()) null
    else genres.find { it.name == n.key.trim() && it.songs.first().id == n.first }
}

fun combinePickedNameWithLocalArtist(
    state: Flow<MediaState>, localArtists: Flow<List<Artist>>
): Flow<Artist?> = combine(state, localArtists) { n, artists ->
    if (n.isBlank() || artists.isEmpty()) null
    else artists.find { it.name == n.key.trim() && it.songs.first().id == n.first }
}

/** Sorted queue */
fun combineQueueMediaItems(
    queue: Flow<List<String>>,
    allMediaItems: Flow<List<MediaItem>>, queueOrder: StateFlow<OrderQueueBy>
) = combine(queue, allMediaItems, queueOrder) { queueIds, items, order ->
    val itemsByKey = items.associateBy { it.itemKey() }
    queueIds.mapNotNull { id -> itemsByKey[id] }.sortedWith(order.songComparator())
}

/** Unsorted queue */
fun combineQueueMediaItems(
    queue: Flow<List<String>>, allMediaItems: Flow<List<MediaItem>>
) = combine(queue, allMediaItems) { queueIds, items ->
    val itemsByKey = items.associateBy { it.itemKey() }
    queueIds.mapNotNull { id -> itemsByKey[id] }
}

/** combine all audio and hidden songs into one list */
fun combineAllMediaItems(
    audioFiles: Flow<AudioUIState>, hiddenAudio: Flow<List<AudioFile>>,
    favorites: Flow<List<AudioFile>>, appStrings: StateFlow<AppStrings>
) = combine(audioFiles, hiddenAudio, favorites, appStrings) { shown, hidden, fav, strings ->
    val list = shown.list.map { it.song }.toMediaItems(strings) + fav.toMediaItems(strings) +
            hidden.toMediaItems(strings)
    val distinctList = list.distinctBy { it.itemKey() }
    distinctList
}

fun getSlOrderedBy(
    viewModelScope: CoroutineScope, songsBy: StateFlow<OrderSongsBy>, timeout: Long
) = songsBy.map { it.isAscending() }.stateIn(
    scope = viewModelScope, started = SharingStarted.WhileSubscribed(timeout),
    initialValue = songsBy.value.isAscending()
)

fun getPlsOrderedBy(
    viewModelScope: CoroutineScope, plsBy: StateFlow<OrderPlsBy>, timeout: Long
) = plsBy.map { it.isAscending() }.stateIn(
    scope = viewModelScope, started = SharingStarted.WhileSubscribed(timeout),
    initialValue = plsBy.value.isAscending()
)

fun getAlbumOrderedBy(
    viewModelScope: CoroutineScope, albumsBy: StateFlow<OrderAlbumsBy>, timeout: Long
) = albumsBy.map { it.isAscending() }.stateIn(
    scope = viewModelScope, started = SharingStarted.WhileSubscribed(timeout),
    initialValue = albumsBy.value.isAscending()
)

fun getArtistOrderedBy(
    viewModelScope: CoroutineScope, artistBy: StateFlow<OrderArtistBy>, timeout: Long
) = artistBy.map { it.isAscending() }.stateIn(
    scope = viewModelScope, started = SharingStarted.WhileSubscribed(timeout),
    initialValue = artistBy.value.isAscending()
)

fun getGenreOrderedBy(
    viewModelScope: CoroutineScope, order: StateFlow<OrderGenresBy>, timeout: Long
) = order.map { it.isAscending() }.stateIn(
    scope = viewModelScope, started = SharingStarted.WhileSubscribed(timeout),
    initialValue = order.value.isAscending()
)

fun getQueueOrderedBy(
    viewModelScope: CoroutineScope, order: StateFlow<OrderQueueBy>, timeout: Long
) = order.map { it.isAscending() }.stateIn(
    scope = viewModelScope, started = SharingStarted.WhileSubscribed(timeout),
    initialValue = order.value.isAscending()
)

suspend fun Context.updateIsPlaying(isPlaying: Boolean) =
    this.dataStore.edit { settings -> settings[XandyWidget.playingKey] = isPlaying }