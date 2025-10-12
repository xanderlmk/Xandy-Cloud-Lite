package com.xandy.lite.models

import android.net.Uri
import androidx.core.net.toUri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Date

object DateAsLong : KSerializer<Date> {
    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) =
        encoder.encodeLong(value.time)

    override fun deserialize(decoder: Decoder) =
        Date(decoder.decodeLong())
}

object LongRangeAsString : KSerializer<LongRange> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LongRange", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LongRange) =
        encoder.encodeString("${value.first}--${value.last}")

    override fun deserialize(decoder: Decoder): LongRange {
        val raw = decoder.decodeString().trim()
        if (raw.isBlank()) return LongRange(0L, 0L)
        val parts = raw.split("--", limit = 2)
        if (parts.size != 2) return LongRange(0L, 0L)
        val first = parts[0].trim()
        val second = parts[1].trim()
        return try {
            val f = first.toLong()
            val s = second.toLong()
            if (f <= s) LongRange(f, s) else LongRange(s, f)
        } catch (_: Exception) {
            LongRange(0L, 0L)
        }
    }
}

object UriAsString : KSerializer<Uri> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UriSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Uri) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Uri =
        decoder.decodeString().toUri()
}