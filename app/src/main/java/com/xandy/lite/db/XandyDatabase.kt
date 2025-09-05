package com.xandy.lite.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.xandy.lite.db.daos.AudioDao
import com.xandy.lite.db.daos.BucketDao
import com.xandy.lite.db.daos.PlaylistDao
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.Bucket
import com.xandy.lite.db.tables.PlaylistSongOrder
import com.xandy.lite.db.tables.PLSongCrossRef
import com.xandy.lite.db.tables.Playlist
import kotlinx.coroutines.CoroutineScope


@Database(
    entities = [
        AudioFile::class, Playlist::class, PLSongCrossRef::class, Bucket::class,
        PlaylistSongOrder::class
    ], version = 1, exportSchema = true,
)
@TypeConverters(
    UriTypeConverter::class, TimestampConverter::class,
    OrderByConverter::class
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
                        .addMigrations()
                        .fallbackToDestructiveMigration(false)
                        .build().also { Instance = it }
                Instance = instance
                // return instance
                instance
            }
        }
    }
}