package com.xandy.lite.db.tables

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xandy.lite.models.LongRangeAsString
import com.xandy.lite.models.application.XANDY_CLOUD
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
@SerialName("$XANDY_CLOUD.Full.Lyrics")
@Parcelize
@Entity(tableName = "lyrics")
data class Lyrics(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val plain: String,
    @ColumnInfo(defaultValue = "NULL")
    val translation: TranslatedLyrics? = null,
    @ColumnInfo(defaultValue = "NULL")
    val pronunciation: TranslatedLyrics? = null,
    @ColumnInfo(defaultValue = "NULL")
    val scroll: Set<LyricLine>? = null,
    @ColumnInfo(defaultValue = "NULL")
    val description: String? = null
) : Parcelable

@Serializable
@SerialName("$XANDY_CLOUD.Lyrics")
@Parcelize
data class LyricLine(
    @Serializable(with = LongRangeAsString::class)
    val range: LongRange, val text: String
) : Parcelable {

    constructor(parcel: Parcel) : this(
        Json.decodeFromString<LongRange>(parcel.readString()!!),
        parcel.readString()!!
    )

    companion object : Parceler<LyricLine> {
        override fun LyricLine.write(parcel: Parcel, flags: Int) {
            parcel.writeString(Json.encodeToString(LongRangeAsString, range))
            parcel.writeString(text)
        }

        override fun create(parcel: Parcel): LyricLine = LyricLine(parcel)
    }
}

@Serializable
@SerialName("$XANDY_CLOUD.Translated.Lyrics")
@Parcelize
data class TranslatedLyrics(val lyrics: TranslatedText, val language: String) : Parcelable

@Serializable
@SerialName("$XANDY_CLOUD.Translated.Interface")
@Parcelize
sealed class TranslatedText : Parcelable {
    @Serializable
    @SerialName("$XANDY_CLOUD.Translated.Interface.Plain")
    @Parcelize
    data class Plain(val t: String) : TranslatedText(), Parcelable

    @Serializable
    @SerialName("$XANDY_CLOUD.Translated.Interface.Scroll")
    @Parcelize
    data class Scroll(val set: Set<LyricLine>) : TranslatedText(), Parcelable
}