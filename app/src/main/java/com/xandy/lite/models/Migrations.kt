package com.xandy.lite.models

import android.util.Log
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
            Log.e("Migration", "Migration 2 to 3 failed", e)
            throw RuntimeException("Migration 2 to 3 failed: ${e.message}")
        } finally {
            db.endTransaction()
        }
    }
}