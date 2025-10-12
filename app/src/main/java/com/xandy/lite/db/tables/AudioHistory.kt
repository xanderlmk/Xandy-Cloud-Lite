package com.xandy.lite.db.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val SONG_ID = "song_id"

@Entity(
    tableName = "audio_history",
    foreignKeys = [
        ForeignKey(
            entity = AudioFile::class,
            parentColumns = [SONG_ID],
            childColumns = [SONG_ID],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class AudioHistory(
    @PrimaryKey(autoGenerate = true)
    val id : Int = 0,
    @ColumnInfo(name = SONG_ID)
    val songId: String,
    @ColumnInfo(name = "mime_type")
    val mimeType: String?,
    @ColumnInfo(name = "failure_category")
    val failureCategory: FailureCategory,
    @ColumnInfo(name = "failure_counter")
    val numOfFailures: Int = 1
)

@Serializable
@SerialName("Audio.FailureCategory")
enum class FailureCategory {
    INTERNAL_VOLUME, METADATA_UNACCESSIBLE, READ_ONLY, UNKNOWN
}
