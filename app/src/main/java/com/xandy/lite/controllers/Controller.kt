package com.xandy.lite.controllers

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.isFavorite
import com.xandy.lite.db.tables.itemKey
import com.xandy.lite.db.tables.toMediaItem
import com.xandy.lite.db.tables.toMediaItems
import com.xandy.lite.models.application.AppStrings


@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@UnstableApi
annotation class PlayNext
object Controller {
    fun addToQueue(
        mediaController: MediaController?, list: List<AudioFile>, appStrings: AppStrings,
        currentQueue: List<MediaItem>, onAddToQueue: (List<String>) -> Unit
    ): Boolean = mediaController?.let { controller ->
        var exists = false
        val idSet = currentQueue.map { it.itemKey() }.toSet()
        val newItems = list.mapNotNull {
            if (it.id in idSet) {
                exists = true
                null
            } else it
        }
        val mediaItems = newItems.toMediaItems(appStrings)
        val newQueueIds = idSet.toList() + newItems.map { it.id }
        controller.addMediaItems(mediaItems)
        onAddToQueue(newQueueIds)
        return@let exists
    } ?: false

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
        ctrl: MediaController, list: List<AudioFile>, song: AudioFile, appStrings: AppStrings,
        onSetQueue: (List<MediaItem>) -> Unit
    ) {
        val songIndex = list.indexOf(song).takeIf { it >= 0 } ?: return
        val beforeSongs = list.subList(0, songIndex)
        val afterSongs =
            if (songIndex == list.lastIndex) emptyList() else list.subList(songIndex + 1, list.size)
        val item = song.toMediaItem(appStrings)
        val beforeItems = beforeSongs.toMediaItems(appStrings)
        val afterItems = afterSongs.toMediaItems(appStrings)
        val list = listOf(song.toMediaItem(appStrings)) +
                afterSongs.toMediaItems(appStrings) +
                beforeSongs.toMediaItems(appStrings)
        ctrl.clearMediaItems()
        ctrl.addMediaItem(item)
        ctrl.addMediaItems(afterItems)
        ctrl.addMediaItems(beforeItems)
        ctrl.prepare()
        ctrl.play()
        onSetQueue(list)
    }

    /** Set the initial queue and return whether the current song is favorite or not */
    fun setInitialQueue(ctrl: Player, list: List<MediaItem>, index: Int, startPosition: Long): Boolean {
        ctrl.clearMediaItems()
        ctrl.setMediaItems(list, index, startPosition)
        ctrl.prepare()
        ctrl.pause()
        return ctrl.currentMediaItem?.isFavorite() ?: false
    }

}