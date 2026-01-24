package com.xandy.lite.db

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.TypeConverter
import com.xandy.lite.db.tables.FailureCategory
import com.xandy.lite.db.tables.LyricLine
import com.xandy.lite.db.tables.TranslatedLyrics
import com.xandy.lite.models.ui.order.by.OrderSongsBy
import com.xandy.lite.models.ui.order.by.toOrderedString
import com.xandy.lite.models.ui.order.by.toSongsOrderedByClass
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.json.Json
import java.util.Date

internal class UriTypeConverter {
    @TypeConverter
    fun toString(uri: Uri) = uri.toString()

    @TypeConverter
    fun toUri(string: String) = string.toUri()
}

internal class TimestampConverter {
    @TypeConverter
    fun toDate(long: Long) = Date(long)

    @TypeConverter
    fun toLong(date: Date) = date.time
}

class OrderByConverter {
    @TypeConverter
    fun toString(orderSongsBy: OrderSongsBy) = orderSongsBy.toOrderedString()

    @TypeConverter
    fun toOrderedBy(string: String) = string.toSongsOrderedByClass()
}

private val json = Json

internal class LyricsConverter {
    @TypeConverter
    fun scrollToText(scroll: Set<LyricLine>?): String? =
        if (scroll.isNullOrEmpty()) null
        else json.encodeToString(SetSerializer(LyricLine.serializer()), scroll)

    @TypeConverter
    fun textToScroll(text: String?) =
        if (text.isNullOrBlank()) null
        else json.decodeFromString<Set<LyricLine>>(text)
}

class FailureCategoryConverter {
    @TypeConverter
    fun categoryToText(category: FailureCategory): String =
        json.encodeToString(FailureCategory.serializer(), category)

    @TypeConverter
    fun textToCategory(text: String) =
        json.decodeFromString<FailureCategory>(text)
}

internal class TranslatedLyricsConverter {
    @TypeConverter
    fun toString(value: TranslatedLyrics?): String? =
        value?.let { json.encodeToString(TranslatedLyrics.serializer(), it) }

    @TypeConverter
    fun toTranslatedLyrics(value: String?) =
        value?.let { json.decodeFromString<TranslatedLyrics>(it) }
}