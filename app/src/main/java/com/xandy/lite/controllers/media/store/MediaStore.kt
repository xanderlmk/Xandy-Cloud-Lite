@file:Suppress("SameParameterValue")

package com.xandy.lite.controllers.media.store

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.xandy.lite.R
import androidx.core.net.toUri
import com.kyant.taglib.Metadata
import com.kyant.taglib.TagLib
import com.kyant.taglib.TagProperty
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.ui.MediaItemWithCreatedOn
import com.xandy.lite.models.ui.drawableResUri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.Date
import java.util.UUID


private data class MediaRow(
    val id: Long, val albumId: Long,
    val displayName: String,
    val duration: Long, val contentUri: Uri,
    val dateAddedMs: Long, val albumArtUri: Uri?,
    val bucketId: Long, val volumeName: String,
)

private fun queryMediaRows(
    context: Context, volumes: List<String>, chunkSize: Int, onCalculateTotal: (Int) -> Unit
): Flow<List<MediaRow>> = flow {
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.VOLUME_NAME,
        MediaStore.Audio.Media.BUCKET_ID
    )
    val chunk = ArrayList<MediaRow>(chunkSize)
    for (volume in volumes) {
        val uri = MediaStore.Audio.Media.getContentUri(volume)
            .buildUpon().appendQueryParameter("distinct", "true").build()
        context.contentResolver.query(uri, projection, null, null)?.use { cursor ->
            onCalculateTotal(cursor.count)
        }
    }
    for (volume in volumes) {
        val uri = MediaStore.Audio.Media.getContentUri(volume)
            .buildUpon().appendQueryParameter("distinct", "true").build()
        context.contentResolver.query(uri, projection, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)
                val displayName = cursor.getString(nameCol)
                val duration = cursor.getLong(durCol)
                val dateAdded = cursor.getLong(dateAddedCol) * 1000L
                val contentUri = ContentUris.withAppendedId(uri, id)
                chunk += MediaRow(
                    id = id, albumId = albumId, displayName = displayName, duration = duration,
                    contentUri = contentUri, dateAddedMs = dateAdded,
                    albumArtUri = albumArtOrNull(context, albumId),
                    volumeName = volume, bucketId = cursor.getLong(bucketIdCol)
                )
                if (chunk.size >= chunkSize) {
                    emit(chunk.toList())
                    chunk.clear()
                }
            }
        }
        if (chunk.isNotEmpty()) {
            emit(chunk.toList())
            chunk.clear()
        }
    }
}.flowOn(Dispatchers.IO.limitedParallelism(2, "Query media rows"))

fun loadAudioFiles(
    context: Context, chunkSize: Int, onProgress: (Int) -> Unit
): Flow<List<AudioFile>> = flow {
    val mutableList = ArrayList<AudioFile>(chunkSize)
    var total = 0
    var i = 0
    val volumes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        listOf(MediaStore.VOLUME_EXTERNAL, MediaStore.VOLUME_INTERNAL)
    else listOf("external", "internal")
    try {
        queryMediaRows(
            context, volumes, chunkSize = 100, onCalculateTotal = { total += it }
        ).collect { rows ->
            for (row in rows) {
                val date = Date(row.dateAddedMs)
                try {
                    val metadata = context.contentResolver.openFileDescriptor(
                        row.contentUri, "r"
                    )?.use { fd ->
                        return@use TagLib.getMetadata(
                            fd = fd.dup().detachFd(),
                            readPictures = true
                        )
                    }
                    val title = metadata?.propertyMap[TagProperty.TITLE]?.singleOrNull()
                    val artist = metadata?.propertyMap[TagProperty.ARTIST]?.singleOrNull()
                    val album = metadata?.propertyMap[TagProperty.ALBUM]?.singleOrNull()
                    val genre = metadata?.propertyMap[TagProperty.GENRE]?.singleOrNull()
                    val pictureUri = row.albumArtUri ?: getArtData(metadata, context)
                    ?: context.drawableResUri(R.drawable.unknown_track)
                    mutableList += AudioFile(
                        uri = row.contentUri,
                        displayName = row.displayName,
                        title = title ?: row.displayName, artist = artist ?: "Unknown Artist",
                        album = album, genre = genre,
                        durationMillis = row.duration,
                        picture = pictureUri, createdOn = date,
                        bucketId = row.bucketId, volumeName = row.volumeName
                    )
                } catch (e: Exception) {
                    Log.w(XANDY_CLOUD, "Failed to get metadata from ${row.displayName}: $e")
                    mutableList += AudioFile(
                        uri = row.contentUri,
                        displayName = row.displayName,
                        title = row.displayName, artist = "Unknown Artist",
                        album = null, genre = null,
                        durationMillis = row.duration,
                        picture = context.drawableResUri(R.drawable.unknown_track),
                        createdOn = date,
                        bucketId = row.bucketId, volumeName = row.volumeName
                    )
                }
                i++
                val percent = (i * 100) / total
                onProgress(percent)
                if (mutableList.size >= chunkSize) {
                    emit(mutableList.toList())
                    mutableList.clear()
                }
            }
        }
        if (mutableList.isNotEmpty()) {
            emit(mutableList.toList())
            mutableList.clear()
        }
    } catch (e: Exception) {
        Log.e(
            XANDY_CLOUD, "Failed to load local media at ${mutableList[mutableList.lastIndex]}: $e"
        )
        emit(mutableList.toList())
        mutableList.clear()
        onProgress(0)
    }
}.flowOn(Dispatchers.IO.limitedParallelism(2, "File  Retrieval"))

suspend fun loadAudioUris(
    context: Context, bucketFilter: Set<Pair<String, Long>> = emptySet()
): List<Uri> = withContext(Dispatchers.IO) {
    try {
        val list = mutableListOf<Uri>()
        val bucketsByVolume = bucketFilter.groupBy { it.first }
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val volumes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            listOf(MediaStore.VOLUME_EXTERNAL, MediaStore.VOLUME_INTERNAL)
        else listOf("external", "internal")

        for (volume in volumes) {
            val uri = MediaStore.Audio.Media
                .getContentUri(volume)
                .buildUpon()
                .appendQueryParameter("distinct", "true")
                .build()

            /** Filtered Queries */
            val fq = bucketsByVolume[volume]?.let { pairs ->
                mutableListOf(
                    "${MediaStore.Audio.Media.BUCKET_ID} NOT IN (${
                        pairs.map { it.second }.joinToString()
                    })"
                )
            } ?: mutableListOf()
            val selection = fq.joinToString(" AND ")
            context.contentResolver.query(
                uri, projection, selection, null, null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(uri, id)
                    list += contentUri
                }
            }
        }
        list
    } catch (e: Exception) {
        Log.e("Xandy-Cloud", "failed to load local media: $e")
        emptyList()
    }
}

private fun albumArtOrNull(context: Context, albumId: Long): Uri? {
    val uri = getAlbumArtUri(albumId)
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.close()
        uri
    } catch (_: Exception) {
        null
    }
}

private fun getAlbumArtUri(albumId: Long): Uri =
    "content://media/external/audio/albumart/$albumId".toUri()

private fun getArtData(metadata: Metadata?, context: Context): Uri? {
    val picture = try {
        metadata?.pictures?.singleOrNull()?.data ?: return null
    } catch (_: Exception) {
        return null
    }
    val cacheFile = File(context.cacheDir, "embedded_art_${UUID.randomUUID()}.jpg")
    cacheFile.outputStream().use { it.write(picture) }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
}

fun List<MediaItemWithCreatedOn>.getAllImages() = this.mapNotNull {
    if (it.mediaItem.mediaMetadata.artworkUri?.scheme == "android.resource"
        && it.mediaItem.mediaMetadata.artworkUri?.path?.contains("drawable/unknown_track") == true
    ) null
    else it.mediaItem.mediaMetadata.artworkUri
}


