package com.xandy.lite.controllers.media.store

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import com.xandy.lite.db.tables.Bucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


suspend fun loadBuckets(context: Context): List<Bucket> = withContext(Dispatchers.IO) {
    val buckets = mutableListOf<Bucket>()
    val projection = arrayOf(
        MediaStore.Audio.Media.VOLUME_NAME,
        MediaStore.Audio.Media.RELATIVE_PATH,
        MediaStore.Audio.Media.BUCKET_ID,
        MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
    )
    val volumes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        listOf(MediaStore.VOLUME_EXTERNAL, MediaStore.VOLUME_INTERNAL)
    } else {
        listOf("external", "internal")
    }
    for (volume in volumes) {
        val uri = MediaStore.Audio.Media
            .getContentUri(volume)
            .buildUpon()
            .appendQueryParameter("distinct", "true")
            .build()

        context.contentResolver.query(uri, projection, null, null, null)
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_ID)
                val nameCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
                while (cursor.moveToNext()) {
                    val path = cursor.getStringOrNull(pathCol) ?: "unknown_path"
                    buckets += Bucket(
                        id = cursor.getLong(idCol),
                        volumeName = volume,
                        name = cursor.getString(nameCol),
                        relativePath = path
                    )
                }
            }
    }
    return@withContext buckets.distinctBy { it.volumeName to it.id }
}