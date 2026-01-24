package com.xandy.lite.navigation

import kotlinx.serialization.Serializable


private interface NavDestinations {
    val route: String
}

@Serializable
internal object PickedSongDestination : NavDestinations {
    override val route = "picked_song"
}

/** Local library of the device */
@Serializable
internal object LocalMusicDestination : NavDestinations {
    override val route = "local_songs"
}

/** Local Playlist of the device */
@Serializable
internal object LocalPlDestination : NavDestinations {
    override val route = "local_playlist"
}

@Serializable
internal object EditAudioDestination : NavDestinations {
    override val route = "edit_local_audio"
}

/** Destination of adding local songs to a local playlist */
@Serializable
internal object AddToPlDestination : NavDestinations {
    override val route = "add_to_playlist/{showAdd}"
    fun createRoute(showAdd: Boolean = false): String {
        return "add_to_playlist/$showAdd"
    }
}

@Serializable
internal object LocalAlbumDestination : NavDestinations {
    override val route = "local_album"
}

@Serializable
internal object LocalArtistDestination : NavDestinations {
    override val route = "local_artist"
}

@Serializable
internal object LocalBucketDestination : NavDestinations {
    override val route = "local_bucket"
}

@Serializable
internal object LocalGenreDestination : NavDestinations {
    override val route = "local_genre"
}

@Serializable
internal object SettingsDestination: NavDestinations {
    override val route = "settings"
}

@Serializable
internal object LyricsListDestination: NavDestinations {
    override val route = "lyrics_list"
}

@Serializable
internal object LyricsEditorDestination: NavDestinations {
    override val route = "lyrics_editor"
}