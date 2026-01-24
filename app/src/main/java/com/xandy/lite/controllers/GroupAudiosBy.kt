package com.xandy.lite.controllers

import android.net.Uri
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.models.application.AppValues
import com.xandy.lite.models.ui.Album
import com.xandy.lite.models.ui.Artist
import com.xandy.lite.models.ui.Genre
import com.xandy.lite.models.ui.order.by.OrderAlbumsBy
import com.xandy.lite.models.ui.order.by.OrderArtistBy
import com.xandy.lite.models.ui.order.by.OrderGenresBy
import com.xandy.lite.models.ui.order.by.comparator


fun groupAudioFilesIntoAlbums(
    audioFiles: List<AudioFile>, order: OrderAlbumsBy, appValues: AppValues
): List<Album> = try {
    if (audioFiles.isEmpty()) emptyList()
    else {
        val known = audioFiles
            .filter { !it.album.isNullOrBlank() }
            // key on normalized album + artist
            .groupBy {
                (it.album!!.trim().lowercase() to
                        (it.artist?.trim()?.lowercase() ?: appValues.unknownArtist))
            }
            .map { (_, songs) ->
                // preserve original casing from the first song
                val albumName = songs.first().album!!.trim()
                val artistName = songs.first().artist?.trim() ?: appValues.unknownArtist
                val picture = songs.first().picture
                Album(
                    name = albumName, artist = artistName, songs = songs,
                    picture = picture, songCount = songs.size
                )
            }

        val unknown = audioFiles
            .filter { it.album.isNullOrBlank() }
            // group by artist only for unknown-album songs
            .groupBy { it.artist?.trim()?.lowercase() ?: appValues.unknownArtist }
            .map { (_, songs) ->
                val artistName = songs.first().artist?.trim() ?: appValues.unknownArtist
                Album(
                    name = "Unknown Album", artist = artistName,
                    songs = songs, picture = appValues.unknownTrackUri, songCount = songs.size
                )
            }
        // return known first, then unknown-album groups
        val list = (known + unknown).sortedWith(order.comparator())
        list
    }
} catch (_: Exception) {
    emptyList()
}


fun groupAudioFilesByArtist(
    audioFiles: List<AudioFile>, order: OrderArtistBy, appValues: AppValues
): List<Artist> = try {
    if (audioFiles.isEmpty()) emptyList()
    else {
        val known = audioFiles
            .filter { !it.artist.isNullOrBlank() }
            .groupBy { it.artist!!.trim().lowercase() }
            .map { (_, songs) ->
                val artist = songs.first().artist!!.trim()
                val picture = songs.firstOrNull()?.picture ?: appValues.unknownTrackUri
                val albums = songs
                    .mapNotNull { s -> s.album?.trim()?.takeIf { it.isNotBlank() } }
                    .distinct()
                Artist(artist, picture, songs, songs.size, albums, albums.size)
            }

        val unknownSongs = audioFiles
            .filter { it.artist.isNullOrBlank() }
        val picture = unknownSongs.firstOrNull()?.picture ?: appValues.unknownTrackUri
        val albums = unknownSongs
            .mapNotNull { s -> s.album?.trim()?.takeIf { it.isNotBlank() } }
            .distinct()
        val unknown = Artist(
            appValues.unknownArtist, picture, unknownSongs, unknownSongs.size, albums, albums.size
        )

        val list = (known + unknown).sortedWith(order.comparator())
        list
    }
} catch (_: Exception) {
    emptyList()
}

fun groupAudioFilesByGenre(
    audioFiles: List<AudioFile>, order: OrderGenresBy, unknownTrackUri: Uri
): List<Genre> = try {
    if (audioFiles.isEmpty()) emptyList()
    else {
        val known = audioFiles
            .filter { !it.genre.isNullOrBlank() }
            .groupBy { it.genre!!.trim().lowercase() }
            .map { (_, songs) ->
                val genre = songs.first().genre!!.trim()
                val picture = songs.first().picture
                Genre(name = genre, picture = picture, songs = songs, songCount = songs.size)
            }
        val unknownAudios = audioFiles
            .filter { it.genre.isNullOrBlank() }
        val picture = unknownAudios.firstOrNull()?.picture ?: unknownTrackUri
        val unknown = if (unknownAudios.isEmpty()) null else Genre(
            name = "Unknown",
            picture = picture,
            songs = unknownAudios,
            songCount = unknownAudios.size
        )
        (unknown?.let { known + it } ?: known).sortedWith(order.comparator())
    }
} catch (_: Exception) {
    emptyList()
}