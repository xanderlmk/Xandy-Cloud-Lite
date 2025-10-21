package com.xandy.lite.models

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.encryptor.Encryptor
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

object LyricsXclfAdapter {
    private fun saveToDownloadsXandyLite(
        context: Context, filename: String, bytes: ByteArray
    ): Uri? {
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/XandyLite"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
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
            val dir = File(downloads, "XandyLite")
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, filename)
            FileOutputStream(outFile).use { it.write(bytes) }
            Uri.fromFile(outFile)
        }
    }

    /** Export (serialize -> compress -> encrypt -> write .xclf) */
    fun exportLyricsToXclf(fileName: String?, lyrics: Lyrics, context: Context): Uri? = try {
        val json = Json.encodeToString(Lyrics.serializer(), lyrics)

        val payloadString =
            Base64.getEncoder().encodeToString(gzipCompress(json.toByteArray(UTF_8)))

        val encrypted = Encryptor.encryptString(payloadString)

        val payloadBytes = encrypted.toByteArray(UTF_8)
        val headerAndPayload = ByteArrayOutputStream().use { bAOS ->
            val dos = DataOutputStream(bAOS)
            dos.write("XCLF".toByteArray(UTF_8))             // magic
            dos.writeByte(VERSION.toInt())                   // version
            dos.writeInt(payloadBytes.size)                  // payload length
            dos.write(payloadBytes)                          // payload
            bAOS.toByteArray()
        }
        saveToDownloadsXandyLite(context, "${fileName ?: lyrics.id}.xclf", headerAndPayload)
    } catch (_: Exception) {
        Log.e(XANDY_CLOUD, "Failed to export lyrics")
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

}