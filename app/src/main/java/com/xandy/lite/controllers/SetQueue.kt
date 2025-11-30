package com.xandy.lite.controllers

import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.toMediaItem
import com.xandy.lite.db.tables.toMediaItemWithCreatedOn
import com.xandy.lite.db.tables.toMediaItems
import com.xandy.lite.db.tables.toMediaItemsWithCreatedOn
import com.xandy.lite.models.ui.MediaItemWithCreatedOn
import  androidx.media3.common.Player

/** Set the Queue
 * @param list list of AudioFiles
 * @param song Picked AudioFile
 * First, we get the index of the picked song.
 *
 * Second, we get the songs before the picked song and the songs after the picked song.
 * If the picked song is the last song of the list, there is no after songs.
 *
 * Third, Convert the list + the picked song into MediaItems so they can be added.
 *
 *
 * Lastly, we add the picked song, then the after songs and the before songs, and we play :)
 */
fun setQueue(
    ctrl: MediaController, list: List<AudioFile>, song: AudioFile,
    onSetQueue: (List<MediaItemWithCreatedOn>) -> Unit
) {
    val songIndex = list.indexOf(song).takeIf { it >= 0 } ?: return
    val beforeSongs = list.subList(0, songIndex)
    val afterSongs =
        if (songIndex == list.lastIndex) emptyList() else list.subList(songIndex + 1, list.size)
    val item = song.toMediaItem()
    val beforeItems = beforeSongs.toMediaItems()
    val afterItems = afterSongs.toMediaItems()
    val list = listOf(song.toMediaItemWithCreatedOn()) +
            afterSongs.toMediaItemsWithCreatedOn() + beforeSongs.toMediaItemsWithCreatedOn()
    ctrl.clearMediaItems()
    ctrl.addMediaItem(item)
    ctrl.addMediaItems(afterItems)
    ctrl.addMediaItems(beforeItems)
    ctrl.prepare()
    ctrl.play()
    onSetQueue(list)
}

fun setInitialQueue(ctrl: Player, list: List<MediaItem>, index: Int, startPosition: Long) {
    ctrl.clearMediaItems()
    ctrl.setMediaItems(list, index, startPosition)
    ctrl.prepare()
    ctrl.pause()
}