package com.xandy.lite.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.xandy.lite.db.IsBucketHidden
import com.xandy.lite.db.tables.Bucket
import com.xandy.lite.db.tables.BucketWithAudio
import kotlinx.coroutines.flow.Flow
import kotlin.collections.forEach

@Dao
interface BucketDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBucket(bucket: Bucket)

    @Update
    suspend fun updateBucket(bucket: Bucket)

    @Query("""SELECT * FROM bucket""")
    suspend fun getBuckets(): List<Bucket>

    @Delete
    suspend fun deleteBuckets(buckets: List<Bucket>)

    @Query("""SELECT hidden FROM bucket WHERE id = :id AND volume_name = :volumeName""")
    suspend fun getHidden(id: Long, volumeName: String): IsBucketHidden

    @Transaction
    suspend fun upsertBuckets(buckets: List<Bucket>) {
        val dbBuckets = getBuckets()
        val dbBucketIds = dbBuckets.map { Pair(it.volumeName, it.id) }.toSet()
        val newBucketIds = buckets.map { Pair(it.volumeName, it.id) }.toSet()
        buckets.forEach { bucket ->
            val pair = Pair(bucket.volumeName, bucket.id)
            if (pair in dbBucketIds) updateBucket(
                Bucket(
                    id = pair.second, volumeName = pair.first, name = bucket.name,
                    hidden = getHidden(pair.second, pair.first).hidden,
                    relativePath = bucket.relativePath
                )
            ) else insertBucket(bucket)
        }

        val bucketsToDelete = dbBuckets.filter {
            val pair = Pair(it.volumeName, it.id)
            pair !in newBucketIds
        }

        deleteBuckets(bucketsToDelete)
    }

    @Query("""UPDATE bucket SET hidden = 0""")
    suspend fun setAllBucketsNotHidden()

    @Query("""UPDATE bucket SET hidden = 1 WHERE id = :id AND volume_name = :volumeName""")
    suspend fun hideBucket(id: Long, volumeName: String)

    @Transaction
    suspend fun hideSelectedBuckets(pairs: Set<Pair<String, Long>>) {
        setAllBucketsNotHidden()
        pairs.forEach { hideBucket(it.second, it.first) }
    }

    @Transaction
    @Query("""SELECT * FROM bucket ORDER BY name ASC""")
    fun getFlowOfBucketsByNameASC(): Flow<List<BucketWithAudio>>
}