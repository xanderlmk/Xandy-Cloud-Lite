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
import com.xandy.lite.models.MIGRATION_1_2
import com.xandy.lite.models.MIGRATION_2_3
import com.xandy.lite.models.MIGRATION_5_6
import com.xandy.lite.models.MIGRATION_7_8
import com.xandy.lite.models.MIGRATION_8_9
import kotlinx.coroutines.CoroutineScope


@Suppress("unused")
@Database(
    entities = [
        AudioFile::class, Playlist::class, PLSongCrossRef::class, Bucket::class,
        PlaylistSongOrder::class, Lyrics::class, AudioHistory::class
    ], version = 9, exportSchema = true,
    autoMigrations = [AutoMigration(3,4), AutoMigration(4,5), AutoMigration(6,7)]
)
@TypeConverters(
    UriTypeConverter::class, TimestampConverter::class, OrderByConverter::class,
    LyricsConverter::class, FailureCategoryConverter::class
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
                            MIGRATION_8_9
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