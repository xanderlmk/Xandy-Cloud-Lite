package com.xandy.lite.db

import android.net.Uri
import androidx.room.ColumnInfo
import com.xandy.lite.db.tables.LyricLine
import java.util.Date


private const val SONG_ID = "song_id"
private const val PLAYLIST_ID = "playlist_id"

data class IsBucketHidden(val hidden: Boolean)
data class AudioDetails(
    val hidden: Boolean, @ColumnInfo(name = "permanently_hidden") val permanentlyHidden: Boolean,
    @ColumnInfo(name = "lyrics_id") val lyricsId: String?, val picture: Uri
)

data class AudioSongId(@ColumnInfo(name = SONG_ID) val id: String)
data class AudioDateModified(@ColumnInfo(name = "date_modified") val dateModified: Date)
data class AudioUri(val uri: Uri)
data class PlaylistName(@ColumnInfo(name = PLAYLIST_ID) val name: String)
data class LyricsId(val id: String)
data class AudioFavorite(val favorite: Boolean)
