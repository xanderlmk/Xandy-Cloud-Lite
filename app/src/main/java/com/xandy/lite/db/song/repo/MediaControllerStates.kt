package com.xandy.lite.db.song.repo

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.edit
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.media3.common.Tracks
import androidx.media3.session.MediaController
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.application.dataStore
import com.xandy.lite.models.itemKey
import com.xandy.lite.models.ui.order.by.OrderBy
import com.xandy.lite.models.ui.order.by.OrderPlsBy
import com.xandy.lite.models.ui.order.by.OrderQueueBy
import com.xandy.lite.models.ui.order.by.OrderSongsBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class MediaControllerStates(private val appPref: SharedPreferences, private val context: Context) {

    companion object {
        private const val DELAYED_CHECK = 425L
        private val QUEUE_NAME = stringPreferencesKey("picked_queue_name")
        private val QUEUE_SET = stringPreferencesKey("queue_json")
        private const val CURRENT_MI = "current_media_item"
        private const val LAST_POSITION = "last_position"
    }

    val queue = context.dataStore.data.map { preferences ->
        try {
            preferences[QUEUE_SET]?.let { Json.decodeFromString<List<String>>(it) }
        } catch (_: Exception) {
            emptyList()
        } ?: emptyList()
    }
    private val _mediaController = MutableStateFlow<MediaController?>(null)
    val mediaController = _mediaController.asStateFlow()
    private val orderedClass = OrderBy(appPref, context)

    private val _audioOrderedBy = MutableStateFlow(orderedClass.getALOrderToClass())
    val audioOrderedBy = _audioOrderedBy.asStateFlow()
    private val _localPlsOrderedBy = MutableStateFlow(orderedClass.getLocalPlsOrderToClass())
    val localPlsOrderedBy = _localPlsOrderedBy.asStateFlow()

    private val _queueOrderedBy = MutableStateFlow(orderedClass.getQueueOrder())
    val queueOrderedBy = _queueOrderedBy.asStateFlow()

    val pickedQueueName = context.dataStore.data.map { preferences ->
        preferences[QUEUE_NAME] ?: ""
    }.flowOn(Dispatchers.IO)

    private val _itemKey = MutableStateFlow(appPref.getString(CURRENT_MI, "") ?: "")
    val itemKey = _itemKey.asStateFlow()

    fun updateLocalALOrder(orderSongsBy: OrderSongsBy) {
        _audioOrderedBy.update { orderSongsBy }; orderedClass.updateALOrder(orderSongsBy)
    }

    fun updateLocalPLOrder(orderPlsBy: OrderPlsBy) {
        _localPlsOrderedBy.update { orderPlsBy }; orderedClass.updateLocalPLOrder(orderPlsBy)
    }

    fun updateQueueOrder(orderQueueBy: OrderQueueBy) {
        _queueOrderedBy.update { orderQueueBy }; orderedClass.updateQueueOrder(orderQueueBy)
    }

    private val _tracks = MutableStateFlow(Tracks.EMPTY)
    val tracks = _tracks.asStateFlow()
    private val _isPlaying = MutableStateFlow(_mediaController.value?.isPlaying ?: false)
    val isPlaying = _isPlaying.asStateFlow()
    private val _isLoading = MutableStateFlow(_mediaController.value?.isLoading ?: false)
    val isLoading = _isLoading.asStateFlow()
    val repeatMode = orderedClass.repeatMode
    private val _positionMs = MutableStateFlow(0L)
    val positionMs = _positionMs.asStateFlow()
    private val _durationMs = MutableStateFlow(0L)
    val durationMs = _durationMs.asStateFlow()
    val shuffleEnabled = orderedClass.shuffleEnabled

    fun updateMediaController(mc: MediaController) = _mediaController.update { mc }

    fun resetMediaController() {
        try {
            _mediaController.value?.release()
            mediaController.value?.release()
            _mediaController.update { null }
            Log.i(XANDY_CLOUD, "Released SongRepo MC")
        } catch (_: Exception) {
            _mediaController.update { null }
        }
    }

    fun updatePosition(position: Long) = _positionMs.update { position }

    fun updateDuration(duration: Long) = _durationMs.update { duration }

    fun updateTracks(tracks: Tracks) = _tracks.update { tracks }

    fun updateIsPlaying(isPlaying: Boolean) = _isPlaying.update { isPlaying }

    fun updateIsLoading(isLoading: Boolean) = _isLoading.update { isLoading }

    private val handler = Handler(Looper.getMainLooper())
    private val playbackRunnable = object : Runnable {
        override fun run() {
            try {
                val currentPosition = _mediaController.value?.currentPosition ?: return
                _positionMs.update { currentPosition }
                _mediaController.value?.currentMediaItem?.itemKey()?.let {
                        updateMediaItem(it)
                }
                handler.postDelayed(this, DELAYED_CHECK)
            } catch (_: Exception) {
                Log.w(XANDY_CLOUD, "Failed to update position")
            }
        }
    }

    fun checkPlaybackPosition() =
        handler.postDelayed(playbackRunnable, DELAYED_CHECK)

    fun stopCheckingPlaybackPosition() = handler.removeCallbacks(playbackRunnable)

    suspend fun updateQueue(songIds: List<String>, name: String) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { settings ->
                settings[QUEUE_NAME] = name
                settings[QUEUE_SET] =
                    Json.encodeToString(ListSerializer(String.serializer()), songIds)
            }
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed to update name to data store: ${e.printStackTrace()}")
        }
        return@withContext
    }

    suspend fun updateQueue(songIds: List<String>) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { settings ->
                settings[QUEUE_SET] =
                    Json.encodeToString(ListSerializer(String.serializer()), songIds)
            }
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed to update name to data store: ${e.printStackTrace()}")
        }
        return@withContext
    }

    fun updateLastestPlayerInfo() {
        try {
            _mediaController.value?.let { mc ->
                val itemKey = mc.currentMediaItem?.mediaId ?: return@let
                Log.i(XANDY_CLOUD, "Updating lastest info, position: ${mc.currentPosition}")
                updateLastPosition(mc.currentPosition)
                updateMediaItem(itemKey)
            } ?: Log.w(XANDY_CLOUD, "Null controller")
        } catch (_: Exception) {
            Log.w(XANDY_CLOUD, "Failed to update lastest player info")
        }
    }

    fun updateMediaItem(key: String) = _itemKey.update {
        appPref.edit { putString(CURRENT_MI, key) }; key
    }

    private fun updateLastPosition(position: Long) = try {
        appPref.edit { putLong(LAST_POSITION, position) }
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to update media key: $e")
    }

}