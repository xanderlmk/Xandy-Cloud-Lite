package com.xandy.lite.db

import android.net.Uri
import androidx.room.ColumnInfo


private const val SONG_ID = "song_id"
private const val PLAYLIST_ID = "playlist_id"

data class IsBucketHidden(val hidden: Boolean)
data class AudioDetails(
    val hidden: Boolean, val permanentlyHidden: Boolean,
    @ColumnInfo(name = "lyrics_id") val lyricsId: String?
)

data class AudioSongId(@ColumnInfo(name = SONG_ID) val id: String)
data class AudioUri(val uri: Uri)
data class PlaylistName(@ColumnInfo(name = PLAYLIST_ID) val name: String)
