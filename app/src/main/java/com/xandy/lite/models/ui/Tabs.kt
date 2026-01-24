package com.xandy.lite.models.ui

import kotlinx.serialization.Serializable

@Serializable
enum class LocalMusicTabs {
    FAVORITES, LIBRARY, PLAYLIST, ALBUMS, ARTISTS, GENRES, FOLDERS, HIDDEN;

    fun isSelectable() = this == FAVORITES || this == LIBRARY || this == HIDDEN || this == FOLDERS
}