package com.xandy.lite.models

import android.util.Log
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xandy.lite.models.ui.order.by.OBS
import java.util.UUID

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("PRAGMA foreign_keys=OFF;")
            db.beginTransaction()
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS local_playlist_temp(
                    id TEXT NOT NULL,
                    playlist_id TEXT NOT NULL,
                    createdOn INTEGER NOT NULL,
                    picture TEXT,
                    PRIMARY KEY(id)
                )
            """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX pl_name_idx on local_playlist_temp(playlist_id)
            """.trimIndent()
            )
            val cursor = db.query("SELECT playlist_id, createdOn, picture FROM local_playlist")
            while (cursor.moveToNext()) {
                val playlistId = cursor.getString(0)
                val createdOn = cursor.getLong(1)
                val picture = cursor.getString(2)
                val id = UUID.randomUUID().toString()
                db.execSQL(
                    "INSERT INTO local_playlist_temp(id, playlist_id, createdOn, picture) VALUES(?, ?, ?, ?)",
                    arrayOf(id, playlistId, createdOn, picture)
                )
            }
            cursor.close()
            db.execSQL("""DROP TABLE IF EXISTS local_playlist""")
            db.execSQL("""ALTER TABLE local_playlist_temp RENAME TO local_playlist""")
            db.execSQL("PRAGMA foreign_keys=ON;")
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("Migration", "Migration 1 to 2 failed", e)
            throw RuntimeException("Migration 1 to 2 failed: ${e.message}")
        } finally {
            db.endTransaction()
        }
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.beginTransaction()
            db.execSQL("PRAGMA foreign_keys=OFF;")

            db.execSQL(
                """
                INSERT INTO playlist_song_order (playlist_id, orderedBy)
                SELECT p.playlist_id, '${OBS.CREATED_ON_ASC}' FROM local_playlist p
                LEFT JOIN playlist_song_order o ON p.playlist_id = o.playlist_id
                WHERE o.playlist_id IS NULL
            """.trimIndent()
            )
            db.execSQL("PRAGMA foreign_keys=ON;")
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            migrationError(e, 2, 3)
        } finally {
            db.endTransaction()
        }
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {

            db.beginTransaction()
            db.execSQL("PRAGMA foreign_keys=OFF;")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS lyrics(
                    id TEXT NOT NULL,
                    plain TEXT NOT NULL,
                    translation TEXT DEFAULT NULL,
                    pronunciation TEXT DEFAULT NULL, 
                    scroll TEXT DEFAULT NULL,
                    PRIMARY KEY(id)
                )
            """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS local_audio_temp(
                    song_id TEXT NOT NULL,
                    displayName TEXT NOT NULL,
                    title TEXT NOT NULL,
                    artist TEXT NOT NULL,
                    album TEXT, 
                    genre TEXT,
                    durationMillis INTEGER NOT NULL,
                    year INTEGER DEFAULT NULL, 
                    day INTEGER DEFAULT NULL, 
                    month INTEGER DEFAULT NULL,
                    picture TEXT NOT NULL,
                    createdOn INTEGER NOT NULL,
                    hidden INTEGER NOT NULL DEFAULT false,
                    permanentlyHidden INTEGER NOT NULL DEFAULT false,
                    lyrics_id TEXT DEFAULT NULL,
                    bucket_id INTEGER,
                    volume_name TEXT,
                    FOREIGN KEY(volume_name, bucket_id) REFERENCES bucket(volume_name, id)
                    ON DELETE SET NULL ON UPDATE CASCADE 
                    FOREIGN KEY(lyrics_id) REFERENCES lyrics(id)
                    ON DELETE SET NULL ON UPDATE CASCADE
                    PRIMARY KEY(song_id)
                )
            """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO local_audio_temp(
                    song_id, displayName, title, artist, album, genre, durationMillis,
                    year, day, month, picture, createdOn, hidden, permanentlyHidden,
                    volume_name, bucket_id
                )
                SELECT song_id, displayName, title, artist, album, genre, durationMillis,
                    year, day, month, picture, createdOn, hidden, permanentlyHidden,
                    volume_name, bucket_id
                FROM local_audio""".trimIndent()
            )
            db.execSQL("""DROP TABLE IF EXISTS local_audio""")
            db.execSQL(
                """
                CREATE INDEX bucket_reference_index ON local_audio_temp(volume_name, bucket_id)
            """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX lyrics_reference_index ON local_audio_temp(lyrics_id)
            """.trimIndent()
            )
            db.execSQL("""ALTER TABLE local_audio_temp RENAME TO local_audio""")

            db.execSQL("PRAGMA foreign_keys=ON;")
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            migrationError(e, 5, 6)
        } finally {
            db.endTransaction()
        }
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {

            db.beginTransaction()
            db.execSQL("PRAGMA foreign_keys=OFF;")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS local_audio_temp(
                    song_id TEXT NOT NULL,
                    uri TEXT NOT NULL,
                    displayName TEXT NOT NULL,
                    title TEXT NOT NULL,
                    artist TEXT NOT NULL,
                    album TEXT, 
                    genre TEXT,
                    durationMillis INTEGER NOT NULL,
                    year INTEGER DEFAULT NULL, 
                    day INTEGER DEFAULT NULL, 
                    month INTEGER DEFAULT NULL,
                    picture TEXT NOT NULL,
                    createdOn INTEGER NOT NULL,
                    hidden INTEGER NOT NULL DEFAULT false,
                    permanentlyHidden INTEGER NOT NULL DEFAULT false,
                    lyrics_id TEXT DEFAULT NULL,
                    bucket_id INTEGER,
                    volume_name TEXT,
                    FOREIGN KEY(volume_name, bucket_id) REFERENCES bucket(volume_name, id)
                    ON DELETE SET NULL ON UPDATE CASCADE 
                    FOREIGN KEY(lyrics_id) REFERENCES lyrics(id)
                    ON DELETE SET NULL ON UPDATE CASCADE
                    PRIMARY KEY(song_id)
                )
            """.trimIndent()
            )
            val cursor = db.query(
                """
                SELECT song_id, displayName, title, artist, album, genre,
                durationMillis, year, day, month, picture, createdOn,
                hidden, permanentlyHidden, lyrics_id, bucket_id, volume_name
                FROM local_audio
                """.trimIndent()
            )
            val mapping = HashMap<String, String>()
            val colOldSongId = cursor.getColumnIndexOrThrow("song_id")
            val colDisplayName = cursor.getColumnIndexOrThrow("displayName")
            val colTitle = cursor.getColumnIndexOrThrow("title")
            val colArtist = cursor.getColumnIndexOrThrow("artist")
            val colAlbum = cursor.getColumnIndexOrThrow("album")
            val colGenre = cursor.getColumnIndexOrThrow("genre")
            val colDuration = cursor.getColumnIndexOrThrow("durationMillis")
            val colYear = cursor.getColumnIndexOrThrow("year")
            val colDay = cursor.getColumnIndexOrThrow("day")
            val colMonth = cursor.getColumnIndexOrThrow("month")
            val colPicture = cursor.getColumnIndexOrThrow("picture")
            val colCreatedOn = cursor.getColumnIndexOrThrow("createdOn")
            val colHidden = cursor.getColumnIndexOrThrow("hidden")
            val colPermHidden = cursor.getColumnIndexOrThrow("permanentlyHidden")
            val colLyricsId = cursor.getColumnIndexOrThrow("lyrics_id")
            val colBucketId = cursor.getColumnIndexOrThrow("bucket_id")
            val colVolumeName = cursor.getColumnIndexOrThrow("volume_name")

            while (cursor.moveToNext()) {
                val oldSongId =
                    cursor.getString(colOldSongId) // this was the old primary key (content://...)
                val newUuid = UUID.randomUUID().toString()
                mapping[oldSongId] = newUuid

                val displayName = cursor.getString(colDisplayName)
                val title = cursor.getString(colTitle)
                val artist = cursor.getString(colArtist)
                val album = cursor.getStringOrNull(colAlbum)
                val genre = cursor.getStringOrNull(colGenre)
                val durationMillis = cursor.getLong(colDuration)
                val year = cursor.getIntOrNull(colYear)
                val day = cursor.getIntOrNull(colDay)
                val month = cursor.getIntOrNull(colMonth)
                val picture = cursor.getStringOrNull(colPicture)
                val createdOn = cursor.getLong(colCreatedOn) // stored as Long (epoch ms)
                val hidden = cursor.getInt(colHidden) // 0 or 1
                val permanentlyHidden = cursor.getInt(colPermHidden) // 0 or 1
                val lyricsId = cursor.getStringOrNull(colLyricsId)
                val bucketId = cursor.getLongOrNull(colBucketId)
                val volumeName = cursor.getStringOrNull(colVolumeName)

                val args = listOf<Any?>(
                    newUuid, oldSongId, displayName, title, artist,
                    album, genre, durationMillis, year, day, month, picture,
                    createdOn, hidden, permanentlyHidden, lyricsId, bucketId, volumeName
                ).toTypedArray()
                db.execSQL(
                    """
                    INSERT INTO local_audio_temp (
                    song_id, uri, displayName, title, artist, album, genre,
                    durationMillis, year, day, month, picture, createdOn,
                    hidden, permanentlyHidden, lyrics_id, bucket_id, volume_name
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(), args
                )
            }
            cursor.close()
            for ((oldUri, newUuid) in mapping) {
                db.execSQL(
                    "UPDATE local_pl_song_cross_ref SET song_id = ? WHERE song_id = ?",
                    arrayOf(newUuid, oldUri)
                )
            }


            db.execSQL("""DROP TABLE IF EXISTS local_audio""")
            db.execSQL(
                """
                CREATE INDEX bucket_reference_index ON local_audio_temp(volume_name, bucket_id)
            """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX lyrics_reference_index ON local_audio_temp(lyrics_id)
            """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX uri_index on local_audio_temp(uri)
            """.trimIndent()
            )
            db.execSQL("""ALTER TABLE local_audio_temp RENAME TO local_audio""")

            db.execSQL("PRAGMA foreign_keys=ON;")
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            migrationError(e, 7, 8)
        } finally {
            db.endTransaction()
        }
    }
}

val MIGRATION_8_9 = object : Migration(8,9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.beginTransaction()
            db.execSQL("PRAGMA foreign_keys=OFF")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS audio_history(
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    song_id TEXT NOT NULL,
                    mime_type TEXT,
                    failure_category TEXT NOT NULL, 
                    failure_counter INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY(song_id) REFERENCES local_audio(song_id) 
                    ON UPDATE CASCADE ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("PRAGMA foreign_keys=ON;")
            db.setTransactionSuccessful()
        }
        catch (e: Exception) {
            migrationError(e, 8, 9)
        } finally {
            db.endTransaction()
        }
    }
}
private fun migrationError(e: Exception, first: Int, second: Int) {
    Log.e("Migration", "Migration $first to $second failed", e)
    throw RuntimeException("Migration $first to $second failed: ${e.message}")
}