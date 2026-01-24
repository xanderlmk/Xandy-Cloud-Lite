@file:Suppress("SameParameterValue")

package com.xandy.lite.controllers.media.store

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.database.getIntOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.xandy.lite.R
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import com.kyant.taglib.TagLib
import com.kyant.taglib.TagProperty
import com.xandy.lite.db.daos.AudioDao
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.ui.drawableResUri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.Date
import java.util.UUID


/**
 * @param albumId Album id
 * @param displayName The Display name of the audio file
 * @param year The year of the audio file
 * @param duration Duration of the audio
 * @param contentUri The uri of the file
 * @param dateAddedMs Dated song was added in Milliseconds
 * @param bucketId Bucket id that the file belongs to
 * @param volumeName The volume name of the bucket
 */
private data class MediaRow(
    val id: Long, val albumId: Long, val displayName: String, val year: Int?,
    val duration: Long, val contentUri: Uri,
    val dateAddedMs: Long, val dateModifiedMs: Long, val bucketId: Long, val volumeName: String,
)

data class MediaRowPicture(
    val contentUri: Uri, val albumId: Long
)

data class ImportedAudioDetails(
    val audio: AudioFile, val mrp: MediaRowPicture,
    val songIdNotNull: Boolean, val modified: Boolean
)

data class AudioPicToUpdate(val audio: AudioFile, val art: Uri)

private fun queryMediaRows(context: Context, volumes: List<String>): List<MediaRow> {
    val list = mutableListOf<MediaRow>()

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.YEAR,
        MediaStore.Audio.Media.VOLUME_NAME,
        MediaStore.Audio.Media.BUCKET_ID,
        MediaStore.Audio.Media.DATE_MODIFIED
    )
    for (volume in volumes) {
        val uri = MediaStore.Audio.Media.getContentUri(volume)
            .buildUpon().appendQueryParameter("distinct", "true").build()
        context.contentResolver.query(uri, projection, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_ID)
            val yearIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            while (cursor.moveToNext()) {
                try {
                    val id = cursor.getLong(idCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val contentUri = ContentUris.withAppendedId(uri, id)
                    val dateModified = cursor.getLong(dateModifiedCol) * 1000L
                    val dateAdded = cursor.getLong(dateAddedCol) * 1000L
                    val displayName = cursor.getString(nameCol)
                    val year = cursor.getIntOrNull(yearIdCol)
                    val duration = cursor.getLong(durCol)
                    list += MediaRow(
                        id = id, albumId = albumId, displayName = displayName, duration = duration,
                        contentUri = contentUri, dateAddedMs = dateAdded, year = year,
                        volumeName = volume, bucketId = cursor.getLong(bucketIdCol),
                        dateModifiedMs = dateModified
                    )
                } catch (e: Exception) {
                    Log.e(XANDY_CLOUD, "${e.printStackTrace()}")
                }
            }
        }
    }
    return list
}

private const val CHUNK_SIZE = 20

/**
 * Returns media files
 */
fun loadAudioFiles(
    context: Context,
    volumes: List<String>,
    audioDao: AudioDao
): Flow<List<ImportedAudioDetails>> = flow {
    val mutableList = ArrayList<ImportedAudioDetails>(CHUNK_SIZE)
    val pictureUri = context.drawableResUri(R.drawable.unknown_track)
    val onEmit: suspend () -> Unit = {
        try {
            val pairs = mutableList.map { Pair(it.audio, it.modified) }
            audioDao.upsertAudios(pairs)
        } catch (e: Exception) {
            Log.e(
                XANDY_CLOUD,
                "Failed to upsert audio files to DB: ${e.printStackTrace()}"
            )
        }
        emit(mutableList.toList())
        mutableList.clear()
    }
    try {
        val rows = queryMediaRows(context, volumes)
        for (row in rows) {
            val date = Date(row.dateAddedMs)
            val dateModified = Date(row.dateModifiedMs)
            val dbDateModified = audioDao.getAudioDateModified(row.contentUri, row.id)?.dateModified
            val uuid =
                audioDao.getAudioId(row.contentUri, row.id)?.id ?: UUID.randomUUID().toString()
            try {
                val metadata =
                    if (dbDateModified != dateModified) context.contentResolver.openFileDescriptor(
                        row.contentUri, "r"
                    )?.use { fd ->
                        return@use TagLib.getMetadata(
                            fd = fd.dup().detachFd()
                        )
                    } else null
                val title = metadata?.propertyMap[TagProperty.TITLE]?.singleOrNull()
                val artist = metadata?.propertyMap[TagProperty.ARTIST]?.singleOrNull()
                val album = metadata?.propertyMap[TagProperty.ALBUM]?.singleOrNull()
                val genre = metadata?.propertyMap[TagProperty.GENRE]?.singleOrNull()
                val dateRelease = metadata?.propertyMap[TagProperty.DATE]?.singleOrNull()
                val songId = metadata?.propertyMap[XANDY_SONG_ID]?.singleOrNull()
                val dateParts = parseTagDate(dateRelease)
                mutableList += ImportedAudioDetails(
                    audio = AudioFile(
                        id = songId ?: uuid,
                        fileId = row.id,
                        uri = row.contentUri,
                        displayName = row.displayName,
                        title = title ?: row.displayName, artist = artist,
                        album = album, genre = genre,
                        year = row.year ?: dateParts.year,
                        day = dateParts.day, month = dateParts.month,
                        durationMillis = row.duration,
                        picture = pictureUri, createdOn = date,
                        bucketId = row.bucketId, volumeName = row.volumeName,
                        dateModified = dateModified
                    ),
                    mrp = MediaRowPicture(
                        contentUri = row.contentUri, albumId = row.albumId
                    ),
                    songIdNotNull = songId != null, modified = dbDateModified != dateModified
                )

            } catch (e: Exception) {
                Log.w(
                    XANDY_CLOUD,
                    "Failed to get metadata from ${row.displayName}: ${e.printStackTrace()}"
                )
                mutableList += ImportedAudioDetails(
                    audio = AudioFile(
                        id = uuid, fileId = row.id,
                        uri = row.contentUri,
                        displayName = row.displayName,
                        title = row.displayName, artist = null,
                        album = null, genre = null, year = row.year, day = null, month = null,
                        durationMillis = row.duration,
                        picture = pictureUri,
                        createdOn = date, dateModified = dateModified,
                        bucketId = row.bucketId, volumeName = row.volumeName
                    ),
                    mrp = MediaRowPicture(
                        contentUri = row.contentUri, albumId = row.albumId
                    ),
                    songIdNotNull = false, dbDateModified != dateModified
                )
            }
            if (mutableList.size >= CHUNK_SIZE) onEmit()
        }
    } catch (e: Exception) {
        Log.e(
            XANDY_CLOUD, "Failed to load local media at ${mutableList[mutableList.lastIndex]}: $e"
        )
    }
    onEmit()
}

data class DateParts(val year: Int?, val month: Int?, val day: Int?) {
    override fun toString(): String {
        return when {
            month == null && day == null && year == null -> ""
            month == null && day == null -> "$year"
            day == null -> "$month-$year"
            else -> "$month-$day-$year"
        }
    }
}


private fun parseTagDate(dateStr: String?): DateParts {
    val s = dateStr?.trim() ?: return DateParts(null, null, null)
    if (s.equals("null", ignoreCase = true) || s.isEmpty()) return DateParts(null, null, null)

    // Match "yyyy", or "yyyy-sep-mm", or "yyyy-sep-mm-sep-dd" where sep is the separator
    // and can be -, /, ., , or space
    val re = Regex("""^(\d{4})(?:[ \-/,.](\d{1,2})(?:[ \-/,.](\d{1,2}))?)?$""")
    re.matchEntire(s)?.let { m ->
        val y = m.groupValues[1].toIntOrNull()
        val mo = m.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() }?.toIntOrNull()
        val d = m.groupValues.getOrNull(3)?.takeIf { it.isNotEmpty() }?.toIntOrNull()
        return DateParts(y, mo, d)
    }

    // Fallback: extract the first 1..3 numbers found (handles things like "2025 9 19"
    // or weird formats)
    val ints = Regex("""\d+""").findAll(s).map { it.value.toIntOrNull() }.filterNotNull().toList()
    return when (ints.size) {
        0 -> DateParts(null, null, null)
        1 -> DateParts(ints[0].takeIf { it > 32 }, null, null)
        2 -> DateParts(ints[0], ints[1], null)
        else -> DateParts(ints[0], ints[1], ints[2])
    }
}

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

fun getArtData(context: Context, albumId: Long, uri: Uri) =
    albumArtOrNull(context, albumId) ?: getArtData(uri, context)


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

private fun getArtData(uri: Uri, context: Context): Uri? {
    val picture = try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
            val pic = TagLib.getFrontCover(fd = fd.dup().detachFd())
            return@use pic?.data
        }
    } catch (_: Exception) {
        return null
    } ?: return null
    val cacheFile = File(context.cacheDir, "embedded_art_${UUID.randomUUID()}.jpg")
    cacheFile.outputStream().use { it.write(picture) }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
}

fun List<MediaItem>.getAllImages() = this.mapNotNull {
    if (it.mediaMetadata.artworkUri?.scheme == "android.resource"
        && it.mediaMetadata.artworkUri?.path?.contains("drawable/unknown_track") == true
    ) null
    else it.mediaMetadata.artworkUri
}
