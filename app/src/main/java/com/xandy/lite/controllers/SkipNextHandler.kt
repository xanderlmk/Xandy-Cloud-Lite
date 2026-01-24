package com.xandy.lite.controllers

import android.content.SharedPreferences
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.xandy.lite.db.tables.itemKey
import com.xandy.lite.db.tables.toMediaItem
import com.xandy.lite.models.AppPref
import com.xandy.lite.models.application.AppStrings
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.ui.PriorityQueue


class SkipNextHandler(
    private val appPref: SharedPreferences
) {
    fun handleSkipNext(
        shuffleEnabled: Boolean, repeatMode: Int, mc: MediaController, appStrings: AppStrings
    ) {
        var priorityQueue = false
        mc.currentMediaItem?.let {
            val result = onCheckPriorityQueue(shuffleEnabled, mc, it, appStrings)
            priorityQueue = result is PriorityQueue.Sought
        }
        if (priorityQueue) return
        if (repeatMode == Player.REPEAT_MODE_OFF || repeatMode == Player.REPEAT_MODE_ONE) {
            if (!mc.hasNextMediaItem()) {
                if (shuffleEnabled) {
                    val songCount = mc.mediaItemCount.takeIf { it > 0 } ?: return
                    val index = (0 until songCount).random().takeIf {
                        it != mc.currentMediaItemIndex
                    } ?: 0
                    mc.seekToDefaultPosition(index)
                } else mc.seekToDefaultPosition(0)
            } else {
                mc.seekToNext()
            }
        } else {
            // REPEAT_MODE_ALL
            mc.seekToNext()
        }
    }

    private fun onCheckPriorityQueue(
        shuffleEnabled: Boolean, mc: MediaController, mediaItem: MediaItem, appStrings: AppStrings
    ): PriorityQueue {
        val currentItemKey = mediaItem.itemKey()
        val savedIndex = AppPref.getInitSavedIndex(appPref)
        val trashedItemKey = AppPref.getInitItemToTrash(appPref)
        val priorityList = AppPref.getInitialPriorityQueue(appPref).toMutableList()
        return if (priorityList.isEmpty() && trashedItemKey.isBlank() &&
            savedIndex == C.INDEX_UNSET
        ) PriorityQueue.Finished
        else if (priorityList.isNotEmpty()) {
            val currentIndex = mc.currentMediaItemIndex
            val nextIndex = mc.nextMediaItemIndex.takeIf { it > C.INDEX_UNSET }
            val nextMedia = priorityList.first().toMediaItem(appStrings)
            mc.addMediaItem(currentIndex, nextMedia)
            mc.seekToDefaultPosition(currentIndex)
            /**
             * If the current item is to be trashed,
             * it got moved 1 to the right, so remove it.
             */
            if (trashedItemKey == currentItemKey)
                mc.removeMediaItem(currentIndex + 1)

            if (trashedItemKey.isBlank()) {
                AppPref.updateCurrentIndex(currentIndex, appPref)
                if (shuffleEnabled)
                    nextIndex?.let { AppPref.updateSavedIndex(it, appPref) }
                else AppPref.updateSavedIndex(currentIndex + 2, appPref)
            }
            priorityList.removeAt(0)
            AppPref.updatePriorityQueue(priorityList.toList(), appPref)
            AppPref.updateTrashedItemKey(nextMedia.itemKey(), appPref)
            Log.i(XANDY_CLOUD, "Priority Queue.")
            PriorityQueue.Sought
        } else if (
            trashedItemKey.isNotBlank() && savedIndex > C.INDEX_UNSET
        ) {
            val currentIndex = AppPref.getInitCurrentIndex(appPref)
            AppPref.updateTrashedItemKey("", appPref)
            AppPref.updateSavedIndex(C.INDEX_UNSET, appPref)
            mc.seekToDefaultPosition(if (!shuffleEnabled) currentIndex + 2 else savedIndex)
            if (trashedItemKey == currentItemKey && currentIndex > C.INDEX_UNSET)
                mc.removeMediaItem(currentIndex)
            AppPref.updateCurrentIndex(C.INDEX_UNSET, appPref)
            Log.i(
                XANDY_CLOUD, "No items in queue, removing saved index and trashed item."
            )
            PriorityQueue.Sought
        } else PriorityQueue.Skipped
    }
}