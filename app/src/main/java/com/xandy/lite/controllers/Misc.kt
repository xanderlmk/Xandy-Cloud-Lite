package com.xandy.lite.controllers

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.AudioWithPls
import com.xandy.lite.db.tables.BucketWithAudio
import com.xandy.lite.db.tables.toMediaItemsWithCreatedOn
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.itemKey
import com.xandy.lite.models.ui.Album
import com.xandy.lite.models.ui.Artist
import com.xandy.lite.models.ui.AudioUIState
import com.xandy.lite.models.ui.Genre
import com.xandy.lite.models.ui.LocalPlUIState
import com.xandy.lite.models.ui.MediaItemWithCreatedOn
import com.xandy.lite.models.ui.PlaylistWithCount
import com.xandy.lite.models.ui.SongDetails
import com.xandy.lite.models.ui.order.by.OrderPlsBy
import com.xandy.lite.models.ui.order.by.OrderQueueBy
import com.xandy.lite.models.ui.order.by.OrderSongsBy
import com.xandy.lite.models.ui.order.by.isAscending
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

fun songComparator(order: OrderQueueBy): Comparator<MediaItemWithCreatedOn> =
    Comparator { a, b ->
        when (order) {
            is OrderQueueBy.TitleASC -> a.mediaItem.title()
                .compareTo(b.mediaItem.title(), ignoreCase = true)

            is OrderQueueBy.TitleDESC -> b.mediaItem.title()
                .compareTo(a.mediaItem.title(), ignoreCase = true)

            is OrderQueueBy.CreatedOnASC -> {
                val da: Date = a.createdOn
                val db: Date = b.createdOn
                da.compareTo(db)
            }

            is OrderQueueBy.CreatedOnDESC -> {
                val da: Date = a.createdOn
                val db: Date = b.createdOn
                db.compareTo(da)
            }

            is OrderQueueBy.ArtistASC -> a.mediaItem.artist()
                .compareTo(b.mediaItem.artist(), ignoreCase = true)

            is OrderQueueBy.ArtistDESC -> b.mediaItem.artist()
                .compareTo(a.mediaItem.artist(), ignoreCase = true)

            is OrderQueueBy.Default -> 0
        }
    }

private fun MediaItem.title() = this.mediaMetadata.title?.toString() ?: "Unknown Title"
private fun MediaItem.artist() = this.mediaMetadata.artist?.toString() ?: "Unknown Artist"

fun combineMCWithPickedSong(
    mediaController: StateFlow<MediaController?>, pickedSong: Flow<AudioWithPls?>,
    unknownTrackUri: Uri
): Flow<SongDetails?> = combine(mediaController, pickedSong) { controller, a ->
    val mediaMetadata = controller?.currentMediaItem?.mediaMetadata
    val id =
        a?.song?.id ?: controller?.currentMediaItem?.itemKey() ?: return@combine null
    if (mediaMetadata == null && a == null) null
    else {
        val sd = combineSongWithMediaMetadata(a, mediaMetadata, unknownTrackUri, id)
        Log.i("Xandy-Cloud", "$sd")
        sd
    }
}.flowOn(Dispatchers.Main.limitedParallelism(1, "Song Details"))

private fun combineSongWithMediaMetadata(
    af: AudioWithPls?, item: MediaMetadata?, unknownTrackUri: Uri, id: String
): SongDetails {
    val title = af?.song?.title ?: item?.title ?: "Unknown Title"
    val artist = af?.song?.artist ?: item?.artist ?: "Unknown Artist"
    val album = af?.song?.album ?: item?.albumTitle
    val artwork = af?.song?.picture ?: item?.artworkUri ?: unknownTrackUri
    Log.i(XANDY_CLOUD, "${af?.lyrics}")

    return SongDetails(
        id, title.toString(), artist.toString(), album?.toString(), artwork, af?.lyrics
    )
}

fun combinePickedUUIDWithPl(
    name: Flow<String>, localPlaylists: Flow<LocalPlUIState>
): Flow<PlaylistWithCount?> = combine(name, localPlaylists) { n, pls ->
    if (n.isBlank() || pls.list.isEmpty()) null
    else pls.list.find { it.playlist.id == n }
}

fun combinePickedNameWithLocalAlbum(
    name: Flow<String>, localAlbums: Flow<List<Album>>
): Flow<Album?> = combine(name, localAlbums) { n, albums ->
    if (n.isBlank() || albums.isEmpty()) null
    else albums.find { it.name == n.trim() }
}

fun combineBucketKeyWithLocalBucket(
    key: Flow<Pair<String, Long?>>, buckets: Flow<List<BucketWithAudio>>
): Flow<BucketWithAudio?> = combine(key, buckets) { k, b ->
    if (k.first.isEmpty() || k.second == null || b.isEmpty()) null
    else b.find { it.bucket.id == k.second && it.bucket.volumeName == k.first }
}

fun combinePickedNameWithLocalGenre(
    name: Flow<String>, localGenres: Flow<List<Genre>>
): Flow<Genre?> = combine(name, localGenres) { n, genres ->
    if (n.isEmpty() || genres.isEmpty()) null
    else genres.find { it.name == n.trim() }
}

fun combinePickedNameWithLocalArtist(
    name: Flow<String>, localArtists: Flow<List<Artist>>
): Flow<Artist?> = combine(name, localArtists) { n, artists ->
    if (n.isEmpty() || artists.isEmpty()) null
    else artists.find { it.name == n.trim() }
}

/** Sorted queue */
fun combineQueueMediaItems(
    queue: StateFlow<List<String>>,
    allMediaItems: Flow<List<MediaItemWithCreatedOn>>, queueOrder: StateFlow<OrderQueueBy>
) = combine(queue, allMediaItems, queueOrder) { queueIds, items, order ->
    val itemsByKey = items.associateBy { it.mediaItem.itemKey() }
    queueIds.mapNotNull { id -> itemsByKey[id] }.sortedWith(songComparator(order))
}

/** Unsorted queue */
fun combineQueueMediaItems(
    queue: StateFlow<List<String>>, allMediaItems: Flow<List<MediaItemWithCreatedOn>>
) = combine(queue, allMediaItems) { queueIds, items ->
    val itemsByKey = items.associateBy { it.mediaItem.itemKey() }
    queueIds.mapNotNull { id -> itemsByKey[id] }
}

/** combine all audio, remote songs, and hidden songs into one list */
fun combineAllMediaItems(
    audioFiles: Flow<AudioUIState>, hiddenAudio: Flow<List<AudioFile>>
) = combine(audioFiles, hiddenAudio) { shown, hidden ->
    val list = shown.list.map { it.song }.toMediaItemsWithCreatedOn() +
            hidden.toMediaItemsWithCreatedOn()
    val distinctList = list.distinctBy { it.mediaItem.itemKey() }
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

fun getQueueOrderedBy(
    viewModelScope: CoroutineScope, order: StateFlow<OrderQueueBy>, timeout: Long
) = order.map { it.isAscending() }.stateIn(
    scope = viewModelScope, started = SharingStarted.WhileSubscribed(timeout),
    initialValue = order.value.isAscending()
)

