package com.xandy.lite.db.tables

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "bucket", primaryKeys = ["volume_name", "id"])
data class Bucket(
    val id: Long,
    @ColumnInfo(name = "volume_name")
    val volumeName: String,
    val name: String,
    @ColumnInfo(defaultValue = "FALSE")
    val hidden: Boolean = false,
    val relativePath: String
) : Parcelable