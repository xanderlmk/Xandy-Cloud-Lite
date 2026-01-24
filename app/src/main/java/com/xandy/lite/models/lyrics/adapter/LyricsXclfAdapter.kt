package com.xandy.lite.models.lyrics.adapter

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.database.getStringOrNull
import com.xandy.lite.BuildConfig
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.encryptor.Encryptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.text.Charsets.UTF_8


object LyricsXclfAdapter {
    const val MIME_TYPE = "application/x-xclf"
    private val DOWNLOADS_RELATIVE_PATH =
        if (BuildConfig.DEBUG) "${Environment.DIRECTORY_DOWNLOADS}/XandyDebugLite"
        else "${Environment.DIRECTORY_DOWNLOADS}/XandyLite"

    // --- Constants ---
    private val MAGIC = "XCLF".toByteArray(UTF_8)
    private const val VERSION: Byte = 1

    // --- Helpers ---
    private fun gzipCompress(input: ByteArray): ByteArray =
        ByteArrayOutputStream().use { ba ->
            GZIPOutputStream(ba).use { it.write(input) }
            ba.toByteArray()
        }

    private fun gzipDecompress(input: ByteArray): ByteArray =
        GZIPInputStream(input.inputStream()).use { it.readBytes() }

    private fun saveToDownloadsXandyLite(
        context: Context, filename: String, bytes: ByteArray
    ): Uri? {
        val relativePath = DOWNLOADS_RELATIVE_PATH
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    relativePath
                ) // creates XandyLite under Downloads
            }
            val resolver = context.contentResolver
            val uri =
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return null
            uri
        } else {
            // Legacy path (API < 29). Requires WRITE_EXTERNAL_STORAGE if target SDK < 29 or runtime permission.
            val downloads =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val dir = File(downloads, if (BuildConfig.DEBUG) "XandyDebugLite" else "XandyLite")
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, filename)
            FileOutputStream(outFile).use { it.write(bytes) }
            Uri.fromFile(outFile)
        }
    }

    /** Export (serialize -> compress -> encrypt -> write .xclf) */
    suspend fun exportLyricsToXclf(
        inputName: String?, lyrics: Lyrics, context: Context
    ): ExportResult =
        try {
            val json = Json.encodeToString(Lyrics.serializer(), lyrics)
            val fileName = "${inputName ?: lyrics.id}.xclf"
            val payloadString =
                Base64.getEncoder().encodeToString(gzipCompress(json.toByteArray(UTF_8)))

            val encrypted = Encryptor.encryptString(payloadString)

            val payloadBytes = encrypted.toByteArray(UTF_8)
            val headerAndPayload = ByteArrayOutputStream().use { bAOS ->
                val dos = DataOutputStream(bAOS)
                dos.write(MAGIC)             // magic
                dos.writeByte(VERSION.toInt())                   // version
                dos.writeInt(payloadBytes.size)                  // payload length
                dos.write(payloadBytes)                          // payload
                bAOS.toByteArray()
            }
            val existing = findExistingFileUriInDownloadsXandyLite(context, fileName)
            Log.i(XANDY_CLOUD, "$existing")
            if (existing != null) ExportResult.Exists
            else ExportResult.Success(saveToDownloadsXandyLite(context, fileName, headerAndPayload))
        } catch (_: Exception) {
            Log.e(XANDY_CLOUD, "Failed to export lyrics")
            ExportResult.Failed
        }

    /** Export (serialize -> compress -> encrypt -> write .xclf) */
    fun exportNewLyricsToXclf(inputName: String?, lyrics: Lyrics, context: Context): Uri? = try {
        val json = Json.encodeToString(Lyrics.serializer(), lyrics)
        val fileName = "${inputName ?: lyrics.id}.xclf"
        val payloadString =
            Base64.getEncoder().encodeToString(gzipCompress(json.toByteArray(UTF_8)))

        val encrypted = Encryptor.encryptString(payloadString)

        val payloadBytes = encrypted.toByteArray(UTF_8)
        val headerAndPayload = ByteArrayOutputStream().use { bAOS ->
            val dos = DataOutputStream(bAOS)
            dos.write(MAGIC)             // magic
            dos.writeByte(VERSION.toInt())                   // version
            dos.writeInt(payloadBytes.size)                  // payload length
            dos.write(payloadBytes)                          // payload
            bAOS.toByteArray()
        }
        saveToDownloadsXandyLite(context, fileName, headerAndPayload)
    } catch (_: Exception) {
        Log.e(XANDY_CLOUD, "Failed to export lyrics")
        null
    }

    suspend fun exportOverwrittenLyrics(fileName: String, lyrics: Lyrics, context: Context) = try {
        val json = Json.encodeToString(Lyrics.serializer(), lyrics)
        val existing =
            findExistingFileUriInDownloadsXandyLite(context, fileName) ?: return null
        val payloadString =
            Base64.getEncoder().encodeToString(gzipCompress(json.toByteArray(UTF_8)))

        val encrypted = Encryptor.encryptString(payloadString)

        val payloadBytes = encrypted.toByteArray(UTF_8)
        val headerAndPayload = ByteArrayOutputStream().use { bAOS ->
            val dos = DataOutputStream(bAOS)
            dos.write(MAGIC)             // magic
            dos.writeByte(VERSION.toInt())                   // version
            dos.writeInt(payloadBytes.size)                  // payload length
            dos.write(payloadBytes)                          // payload
            bAOS.toByteArray()
        }
        // overwrite existing entry (MediaStore or file)
        context.contentResolver.openOutputStream(existing, "rwt")
            ?.use { it.write(headerAndPayload) }
        existing
    } catch (_: Exception) {
        Log.e(XANDY_CLOUD, "Failed to export and overwrite lyrics.")
        null
    }

    /** Import (read .xclf -> decrypt -> base64-> unzip -> deserialize) */
    fun importLyricsFromXclf(context: Context, uri: Uri): Lyrics? = try {
        context.contentResolver.openInputStream(uri)?.use { raw ->
            DataInputStream(raw).use { input ->
                val magic = ByteArray(4)
                input.readFully(magic)
                require(String(magic, UTF_8) == String(MAGIC, UTF_8)) { "Not an XCLF file" }

                val version = input.readByte()
                require(version.toInt() == VERSION.toInt()) { "Unsupported XCLF version $version" }
                val length = input.readInt()
                val payloadBytes = ByteArray(length)
                input.readFully(payloadBytes)

                val encryptedString = String(payloadBytes, UTF_8)
                val decryptedPayload = Encryptor.decryptString(encryptedString)

                val json =
                    String(gzipDecompress(Base64.getDecoder().decode(decryptedPayload)), UTF_8)
                Json.decodeFromString(Lyrics.serializer(), json)
            }
        }
    } catch (_: Exception) {
        Log.e(XANDY_CLOUD, "Failed to import lyrics")
        null
    }

    private suspend fun findExistingFileUriInDownloadsXandyLite(
        context: Context, filename: String
    ): Uri? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // Try exact RELATIVE_PATH matches first (with and without trailing slash)
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
                val projection = arrayOf(
                    MediaStore.MediaColumns._ID, MediaStore.MediaColumns.RELATIVE_PATH,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                )
                val selection =
                    "${MediaStore.Downloads.RELATIVE_PATH} = ? AND ${MediaStore.Downloads.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(
                    "$DOWNLOADS_RELATIVE_PATH/",  // note trailing slash for folder path
                    filename
                )
                context.contentResolver.query(
                    collection, projection, selection, selectionArgs, null
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameIdx =
                        cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIdx)
                        val displayName = cursor.getStringOrNull(nameIdx)
                        Log.i(XANDY_CLOUD, displayName ?: "NULL NAME")
                        if (displayName == filename)
                            return@withContext ContentUris.withAppendedId(collection, id)
                    }
                    if (cursor.moveToFirst()) {
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                        val id = cursor.getLong(idCol)
                        // Construct the Uri for the existing file:
                        return@withContext ContentUris.withAppendedId(collection, id)
                    }
                }


                return@withContext null
            } catch (e: Exception) {
                Log.e(XANDY_CLOUD, "Failed to look for file", e)
                return@withContext null
            }

        } else {
            // Legacy path
            val downloads =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val dir = File(downloads,if (BuildConfig.DEBUG) "XandyDebugLite" else "XandyLite")
            val outFile = File(dir, filename)
            return@withContext if (outFile.exists()) Uri.fromFile(outFile) else null
        }
    }
}