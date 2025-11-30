package com.xandy.lite.models

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.session.CommandButton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class AudioIds(val id: String, val uri: Uri)

private const val SYSTEM_THEME = "SystemTheme"

@Serializable
@SerialName(SYSTEM_THEME)
sealed class Theme {
    @Serializable
    @SerialName("$SYSTEM_THEME.DARK")
    data object Dark : Theme()

    @SerialName("$SYSTEM_THEME.LIGHT")
    @Serializable
    data object Light : Theme()

    @SerialName("$SYSTEM_THEME.DEFAULT")
    @Serializable
    data object Default : Theme()
}

fun String.ellipsize(max: Int = 25): String =
    if (length > max) take(max) + "…" else this

/** Media id which should be the AudioFile UUID */
fun MediaItem.itemKey() = this.mediaId

fun MediaItem.uri() =
    this.localConfiguration?.uri ?: this.requestMetadata.mediaUri
    ?: this.mediaMetadata.extras!!.getString("uri")!!.toUri()


class XCToast(private val context: Context) {
    fun makeMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
