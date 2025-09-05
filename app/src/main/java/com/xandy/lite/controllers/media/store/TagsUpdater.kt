package com.xandy.lite.controllers.media.store

import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.kyant.taglib.Picture
import com.kyant.taglib.TagLib
import com.kyant.taglib.TagProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID


suspend fun updateTags(
    context: Context, audioUri: Uri, key: String, newValue: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openFileDescriptor(audioUri, "rw")?.use { fd ->
            val metadata = TagLib.getMetadata(fd.dup().detachFd(), readPictures = true) ?: return@withContext false
            val propMap = metadata.propertyMap
            val new = propMap.apply { this[key] = arrayOf(newValue) }
            return@use TagLib.savePropertyMap(fd.dup().detachFd(), new)
        } ?: return@withContext false
    } catch (e: Exception) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException)
            throw e
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && e is FileNotFoundException) {
            throw e
        }
        e.printStackTrace()
        return@withContext false
    }
}

suspend fun updateTitle(
    context: Context, audioUri: Uri, newTitle: String
): Boolean = updateTags(context, audioUri, TagProperty.TITLE, newTitle)

suspend fun updateArtist(
    context: Context, audioUri: Uri, newArtist: String
): Boolean = updateTags(context, audioUri, TagProperty.ARTIST, newArtist)

suspend fun updateGenre(
    context: Context, audioUri: Uri, newGenre: String
): Boolean = updateTags(context, audioUri, TagProperty.GENRE, newGenre)

suspend fun updateAlbum(context: Context, audioUri: Uri, newAlbum: String
): Boolean = updateTags(context, audioUri, TagProperty.ALBUM, newAlbum)

suspend fun updateArtwork(
    context: Context,
    audioUri: Uri,
    imageUri: Uri
): Pair<Boolean, Uri> = withContext(Dispatchers.IO) {
    if (imageUri.scheme == "android.resource"
        && imageUri.path?.contains("drawable/unknown_track") == true
    ) return@withContext Pair(true, imageUri)

    val imgBytes = context.contentResolver.openInputStream(imageUri)!!
        .buffered().use { it.readBytes() }

    val mime = context.contentResolver.getType(imageUri) ?: "image/jpeg"
    val picture = Picture(
        data = imgBytes, description = "cover art", pictureType = "Front Cover", mimeType = mime
    )
    try {
        context.contentResolver
            .openFileDescriptor(audioUri, "rw")?.use { fd ->
                val success = TagLib.savePictures(fd.dup().detachFd(), arrayOf(picture))
                if(success) {
                    val newPicture = TagLib.getPictures(fd.dup().detachFd()).first().data
                    val cacheFile = File(context.cacheDir, "embedded_art_${UUID.randomUUID()}.jpg")
                    cacheFile.outputStream().use { it.write(newPicture) }
                    val newUri = FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", cacheFile
                    )
                    return@use Pair(true, newUri)
                } else return@use Pair(false, imageUri)
            } ?: return@withContext Pair(false, imageUri)
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext Pair(false, imageUri)
    }
}