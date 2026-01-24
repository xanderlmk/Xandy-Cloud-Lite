package com.xandy.lite.models

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.widget.Toast
import com.xandy.lite.R
import com.xandy.lite.db.tables.AudioFile
import kotlinx.parcelize.Parcelize
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

class XCToast(private val context: Context) {
    fun makeMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    val unableToGet2kPlusFiles = context.getString(R.string.UNABLE_TO_SELECT_2K_FILES)
    val trackAlreadyInQueue = context.getString(R.string.track_already_in_queue)
    val trackAddedToQueue = context.getString(R.string.track_added_to_queue)
    val failedToDeleteTrack = context.getString(R.string.failed_to_delete_track)
    val failedToDeleteTracks = context.getString(R.string.failed_to_delete_tracks)
    val trackAlreadyInPlayNext = context.getString(R.string.track_already_in_play_next)
    val trackAddedToPlayNext = context.getString(R.string.track_added_to_play_next)
    val undefinedPlaylist = context.getString(R.string.undefined_playlist)
    val nameAlreadyExists = context.getString(R.string.name_already_exist)
    val failedToAddPl = context.getString(R.string.failed_to_add_playlist)
    val trackAlreadyInFavorites = context.getString(R.string.track_already_in_favorites)
    val failedToAddToFavorites = context.getString(R.string.failed_to_add_to_favorites)
    val failedToShowTrack = context.getString(R.string.failed_to_show_track)
    val failedToHideTrack = context.getString(R.string.failed_to_hide_track)
    val failedToAddTrackNullMC = context.getString(R.string.failed_to_add_track_null_controller)
    val failedToUnfavoriteTrack = context.getString(R.string.failed_to_unfavorite_track)
    val nullTrack = context.getString(R.string.null_track)
    val failedToAddTracks = context.getString(R.string.failed_to_add_tracks)
    val failedToUpdateTags = context.getString(R.string.failed_to_update_tags)
    val titleCantBeBlank = context.getString(R.string.title_cant_be_blank)
    val permissionDenied = context.getString(R.string.permission_denied)
    val updateFailed = context.getString(R.string.update_failed)
    val emptyList = context.getString(R.string.empty_list)
    val unknownProperty = context.getString(R.string.unknown_property)
    val nullLyrics = context.getString(R.string.null_lyrics)
    fun failedToAddLyricsTo(songId: String) =
        context.getString(R.string.failed_to_add_lyrics_to_, songId)

    fun deletedTrack(songName: String) = context.getString(R.string.deleted_track, songName)
    fun deletedTracks(count: Int) = context.getString(R.string.deleted_num_tracks, count)

    val importFailed = context.getString(R.string.import_failed)
    val nullType = context.getString(R.string.NULL)

    val plainIsBlank = context.getString(R.string.plain_lyrics_is_blank)
    val scrollIsBlank = context.getString(R.string.synchronized_lyrics_are_blank)
    val translatedIsBlank = context.getString(R.string.translation_lyrics_is_blank)
    val pronunciationIsBlank = context.getString(R.string.pronunciation_lyrics_is_blank)
}

@Parcelize
class AudioDialog(val af: AudioFile? = null, val show: Boolean = false) : Parcelable


enum class LyricType { Pronunciation, Translated }
