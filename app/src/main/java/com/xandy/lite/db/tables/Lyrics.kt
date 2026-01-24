package com.xandy.lite.db.tables

import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xandy.lite.db.tables.TranslatedLyrics.Companion.write
import com.xandy.lite.db.tables.TranslatedText.Plain
import com.xandy.lite.models.LongRangeAsString
import com.xandy.lite.models.XCToast
import com.xandy.lite.models.application.XANDY_CLOUD
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.collections.map
import kotlin.collections.toList

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
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString()!!,
        plain = parcel.readString()!!,
        translation = if (parcel.readByte() == 1.toByte()) TranslatedLyrics.create(parcel) else null,
        pronunciation = if (parcel.readByte() == 1.toByte()) TranslatedLyrics.create(parcel) else null,
        scroll = parcel.readLyricLineSet(),
        description = parcel.readString()
    )

    companion object : Parceler<Lyrics> {
        override fun Lyrics.write(parcel: Parcel, flags: Int) {
            parcel.writeString(id)
            parcel.writeString(plain)
            translation?.let {
                parcel.writeByte(1)
                it.write(parcel, flags)
            } ?: parcel.writeByte(0)
            pronunciation?.let {
                parcel.writeByte(1)
                it.write(parcel, flags)
            } ?: parcel.writeByte(0)
            scroll?.let { parcel.writeLyricLineSet(it) }
            parcel.writeString(description)
        }

        override fun create(parcel: Parcel): Lyrics = Lyrics(parcel)
    }

    fun isScrollBased() = this.pronunciation?.lyrics is TranslatedText.Scroll ||
            this.translation?.lyrics is TranslatedText.Scroll

    /** Returns a message if it is NOT valid */
    fun isNotValid(
        toast: XCToast, scrollSet: List<LyricLine>,
        translationSet: List<LyricLine>, pronunciationSet: List<LyricLine>
    ) =
        when {
            this.plain.isBlank() -> toast.plainIsBlank
            this.scroll?.all { it.text.isBlank() } ?: scrollSet.takeIf { it.isNotEmpty() }
                ?.all { it.text.isBlank() } ?: false -> toast.scrollIsBlank

            this.translation?.lyrics?.isBlank() ?: translationSet.takeIf { it.isNotEmpty() }
                ?.all { it.text.isBlank() } ?: false -> toast.translatedIsBlank

            this.pronunciation?.lyrics?.isBlank() ?: pronunciationSet.takeIf { it.isNotEmpty() }
                ?.all { it.text.isBlank() } ?: false -> toast.pronunciationIsBlank

            else -> null
        }

}

@Serializable
@SerialName("$XANDY_CLOUD.Lyrics")
@Parcelize
data class LyricLine(
    @Serializable(with = LongRangeAsString::class)
    val range: LongRange, val text: String
) : Parcelable {
    @Serializable
    val id = UUID.randomUUID().toString()

    constructor(parcel: Parcel) : this(
        LongRange(parcel.readLong(), parcel.readLong()),
        parcel.readString()!!
    )

    companion object : Parceler<LyricLine> {
        override fun LyricLine.write(parcel: Parcel, flags: Int) {
            parcel.writeLong(range.first)
            parcel.writeLong(range.last)
            parcel.writeString(text)
        }

        override fun create(parcel: Parcel): LyricLine = LyricLine(parcel)
    }
}

@Serializable
@SerialName("$XANDY_CLOUD.Translated.Lyrics")
@Parcelize
data class TranslatedLyrics(val lyrics: TranslatedText, val language: String) : Parcelable {
    constructor(parcel: Parcel) : this(
        lyrics = if (parcel.readByte() == 0.toByte()) Plain(parcel.readString()!!)
        else TranslatedText.Scroll(parcel.readLyricLineSet()!!),
        language = parcel.readString()!!
    )

    companion object : Parceler<TranslatedLyrics> {
        override fun TranslatedLyrics.write(parcel: Parcel, flags: Int) {
            when (lyrics) {
                is Plain -> {
                    parcel.writeByte(0)
                    parcel.writeString(lyrics.t)
                }

                is TranslatedText.Scroll -> {
                    parcel.writeByte(1)
                    parcel.writeLyricLineSet(lyrics.set)
                }
            }
            parcel.writeString(language)
        }

        override fun create(parcel: Parcel): TranslatedLyrics = TranslatedLyrics(parcel)
    }
}

@Serializable
@SerialName("$XANDY_CLOUD.Translated.Interface")
@Parcelize
sealed class TranslatedText : Parcelable {
    @Serializable
    @SerialName("$XANDY_CLOUD.Translated.Interface.Plain")
    @Parcelize
    data class Plain(val t: String) : TranslatedText(), Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString()!!
        )

        companion object : Parceler<Plain> {
            override fun Plain.write(parcel: Parcel, flags: Int) {
                parcel.writeString(t)
            }

            override fun create(parcel: Parcel): Plain = Plain(parcel)
        }
    }

    @Serializable
    @SerialName("$XANDY_CLOUD.Translated.Interface.Scroll")
    @Parcelize
    data class Scroll(val set: Set<LyricLine>) : TranslatedText(), Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readLyricLineSet()!!
        )

        companion object : Parceler<Scroll> {
            override fun Scroll.write(parcel: Parcel, flags: Int) {
                parcel.writeLyricLineSet(set)
            }

            override fun create(parcel: Parcel): Scroll = Scroll(parcel)
        }
    }

    fun isBlank() = when (this) {
        is Plain -> this.t.isBlank()
        is Scroll -> this.set.all { it.text.isBlank() }
    }
}

private fun Parcel.readLyricLineSet(): Set<LyricLine>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val parcel = this
        arrayListOf<LyricLine>().apply {
            parcel.readList(this, LyricLine::class.java.classLoader, LyricLine::class.java)
        }.toSet()
    } else {
        val b = this.readBundle(LyricLine::class.java.classLoader)!!
        val textList = b.getStringArrayList("text_set")!!
        val firstList = b.getLongArray("range_first")!!.toList()
        val lastList = b.getLongArray("range_last")!!.toList()
        textList.indices.map { i ->
            LyricLine(
                range = firstList[i]..lastList[i],
                text = textList[i]
            )
        }.toSet()
    }
}

private fun Parcel.writeLyricLineSet(set: Set<LyricLine>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        this.writeList(set.toList())
    else {
        val b = Bundle()
        b.putStringArrayList(
            "text_set", ArrayList(set.map { it.text }.toList())
        )
        b.putLongArray(
            "range_first", set.map { it.range.first }.toLongArray()
        )
        b.putLongArray(
            "range_last", set.map { it.range.last }.toLongArray()
        )
        this.writeBundle(b)
    }
}