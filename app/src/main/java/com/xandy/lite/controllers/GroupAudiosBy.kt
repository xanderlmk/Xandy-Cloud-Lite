package com.xandy.lite.controllers

import android.net.Uri
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.models.ui.Album
import com.xandy.lite.models.ui.Artist
import com.xandy.lite.models.ui.Genre


fun groupAudioFilesIntoAlbums(
    audioFiles: List<AudioFile>, unknownTrackUri: Uri
): List<Album> {
    if (audioFiles.isEmpty()) return emptyList()
    val known = audioFiles
        .filter { !it.album.isNullOrBlank() }
        // key on normalized album + artist
        .groupBy { (it.album!!.trim().lowercase() to it.artist.trim().lowercase()) }
        .map { (_, songs) ->
            // preserve original casing from the first song
            val albumName = songs.first().album!!.trim()
            val artistName = songs.first().artist.trim()
            val picture = songs.first().picture
            Album(
                name = albumName, artist = artistName, songs = songs,
                picture = picture, songCount = songs.size
            )
        }

    val unknown = audioFiles
        .filter { it.album.isNullOrBlank() }
        // group by artist only for unknown-album songs
        .groupBy { it.artist.trim().lowercase() }
        .map { (_, songs) ->
            val artistName = songs.first().artist.trim()
            Album(
                name = "Unknown Album", artist = artistName,
                songs = songs, picture = unknownTrackUri, songCount = songs.size
            )
        }
    // return known first, then unknown-album groups
    return known + unknown
}

fun groupAudioFilesByArtist(audioFiles: List<AudioFile>, unknownTrackUri: Uri): List<Artist> =
    if (audioFiles.isEmpty()) emptyList()
    else audioFiles.groupBy { it.artist.trim().lowercase() }
        .map { (_, songs) ->
            val artist = songs.first().artist.trim()
            val picture = songs.firstOrNull()?.picture ?: unknownTrackUri
            Artist(artist, picture, songs, songs.size)
        }


fun groupAudioFilesByGenre(audioFiles: List<AudioFile>, unknownTrackUri: Uri): List<Genre> {
    if (audioFiles.isEmpty()) return emptyList()
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
    val unknown = if(unknownAudios.isEmpty()) null  else Genre(
        name = "Unknown", picture = picture, songs = unknownAudios, songCount = unknownAudios.size
    )
    return  unknown?.let { known + it } ?: known
}