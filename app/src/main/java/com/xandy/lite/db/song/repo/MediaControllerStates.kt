package com.xandy.lite.db.song.repo

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.edit
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.MediaItem
import androidx.media3.common.Tracks
import androidx.media3.session.MediaController
import com.xandy.lite.controllers.setInitialQueue
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
import kotlinx.coroutines.flow.first
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
        private val CURRENT_MI = stringPreferencesKey("current_media_item")
        private val QUEUE_NAME = stringPreferencesKey("picked_queue_name")
    }

    private var _initialQueue: String?
        get() = appPref.getString("queue", null)
        set(value) = appPref.edit { putString("queue", value) }
    private val _queue = MutableStateFlow(
        _initialQueue?.let {
            Json.Default.decodeFromString(
                ListSerializer(String.Companion.serializer()), it
            )
        }
            ?: emptyList()
    )
    val queue = _queue.asStateFlow()
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

    suspend fun updateMediaController(mc: MediaController, queue: List<MediaItem>) {
        _mediaController.update { mc }
        if (queue.isEmpty() || mc.mediaItemCount > 0) return
        val itemKey = getInitialMediaKey(context)
        val first = queue.find { it.itemKey() == itemKey }
        val index = queue.indexOf(first).takeIf { it >= 0 } ?: 0
        _mediaController.value?.let { setInitialQueue(it, queue, index) }
    }

    fun resetMediaController() {
        try {
            _mediaController.update { it?.release(); null }
            Log.i(XANDY_CLOUD, "Released MC")
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
            val currentPosition = _mediaController.value?.currentPosition ?: return
            _positionMs.update { currentPosition }
            handler.postDelayed(this, DELAYED_CHECK)
        }
    }

    fun checkPlaybackPosition() =
        handler.postDelayed(playbackRunnable, DELAYED_CHECK)

    fun stopCheckingPlaybackPosition() = handler.removeCallbacks(playbackRunnable)

    suspend fun updateQueue(songIds: List<String>, name: String) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { settings -> settings[QUEUE_NAME] = name }
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed to update name to data store: ${e.printStackTrace()}")
        }
        _queue.update {
            _initialQueue =
                Json.Default.encodeToString(ListSerializer(String.serializer()), songIds)
            songIds
        }
        return@withContext
    }
    private suspend fun getInitialMediaKey(context: Context) = try {
        context.dataStore.data.map { preferences -> preferences[CURRENT_MI] ?: "" }.first()
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get repeat mode: $e")
        ""
    }
}