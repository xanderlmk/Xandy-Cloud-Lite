package com.xandy.lite.navigation

import kotlinx.serialization.Serializable


interface NavDestinations {
    val route: String
}

@Serializable
object PickedSongDestination : NavDestinations {
    override val route = "picked_song"
}

/** Local library of the device */
@Serializable
object LocalMusicDestination : NavDestinations {
    override val route = "local_songs"
}

/** Local Playlist of the device */
@Serializable
object LocalPlDestination : NavDestinations {
    override val route = "local_playlist"
}

@Serializable
object EditAudioDestination : NavDestinations {
    override val route = "edit_local_audio"
}

/** Destination of adding local songs to a local playlist */
@Serializable
object AddToPlDestination : NavDestinations {
    override val route = "add_to_playlist"
}

@Serializable
object LocalAlbumDestination : NavDestinations {
    override val route = "local_album"
}

@Serializable
object LocalArtistDestination : NavDestinations {
    override val route = "local_artist"
}

@Serializable
object LocalBucketDestination : NavDestinations {
    override val route = "local_bucket"
}

@Serializable
object LocalGenreDestination : NavDestinations {
    override val route = "local_genre"
}

@Serializable
object SettingsDestination: NavDestinations {
    override val route = "settings"
}

@Serializable
object LyricsListDestination: NavDestinations {
    override val route = "lyrics_list"
}