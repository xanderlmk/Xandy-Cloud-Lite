package com.xandy.lite.db

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.TypeConverter
import com.xandy.lite.models.ui.order.by.OrderSongsBy
import com.xandy.lite.models.ui.order.by.toOrderedString
import com.xandy.lite.models.ui.order.by.toSongsOrderedByClass
import java.util.Date

class UriTypeConverter {
    @TypeConverter
    fun toString(uri: Uri) = uri.toString()

    @TypeConverter
    fun toUri(string: String) = string.toUri()
}

class TimestampConverter {
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