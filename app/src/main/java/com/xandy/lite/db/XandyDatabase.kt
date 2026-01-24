package com.xandy.lite.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.xandy.lite.db.daos.AudioDao
import com.xandy.lite.db.daos.BucketDao
import com.xandy.lite.db.daos.PlaylistDao
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.AudioHistory
import com.xandy.lite.db.tables.Bucket
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.db.tables.PlaylistSongOrder
import com.xandy.lite.db.tables.PLSongCrossRef
import com.xandy.lite.db.tables.Playlist
import kotlinx.coroutines.CoroutineScope


@Suppress("unused")
@Database(
    entities = [
        AudioFile::class, Playlist::class, PLSongCrossRef::class, Bucket::class,
        PlaylistSongOrder::class, Lyrics::class, AudioHistory::class
    ], version = 13, exportSchema = true,
    autoMigrations =
        [AutoMigration(3, 4), AutoMigration(4, 5), AutoMigration(6, 7), AutoMigration(11, 12)]
)
@TypeConverters(
    UriTypeConverter::class, TimestampConverter::class, OrderByConverter::class,
    LyricsConverter::class, FailureCategoryConverter::class, TranslatedLyricsConverter::class
)
abstract class XandyDatabase : RoomDatabase() {
    abstract fun audioDao(): AudioDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun bucketDao(): BucketDao

    companion object {
        @Volatile
        private var Instance: XandyDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): XandyDatabase {
            return Instance ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(context, XandyDatabase::class.java, "xandy_lite_database")
                        .addMigrations(
                            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_5_6, MIGRATION_7_8,
                            MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_12_13
                        )
                        .fallbackToDestructiveMigration(false)
                        .build().also { Instance = it }
                Instance = instance
                // return instance
                instance
            }
        }
    }
}