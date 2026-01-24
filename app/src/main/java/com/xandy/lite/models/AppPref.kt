package com.xandy.lite.models

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.C
import com.xandy.lite.db.song.repo.MediaControllerStates
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.application.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object AppPref {
    private const val CURRENT_MI = "current_media_item"
    private const val LAST_POSITION = "last_position"
    private const val PRIORITY_QUEUE = "priority_queue"
    private const val SAVED_INDEX = "saved_index"
    private const val ITEM_TO_TRASH_KEY = "item_to_trash_key"
    private const val CURRENT_INDEX = "current_index"
    private const val PLAYBACK_SPEED = "playback_speed"
    private const val SILENCED_SKIPPED_ENABLED = "silence_skipped_enabled"
    private const val GET_LANGUAGE = "get_language"
    private val QUEUE_SET = stringPreferencesKey("queue_json")

    internal fun getInitialMediaKey(appPref: SharedPreferences) = try {
        appPref.getString(CURRENT_MI, "") ?: ""
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get repeat mode: $e")
        ""
    }

    internal fun updateMediaItem(key: String, appPref: SharedPreferences) = try {
        appPref.edit { putString(CURRENT_MI, key) }
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to update media key: $e")
    }

    fun getLastPosition(appPref: SharedPreferences) = try {
        appPref.getLong(LAST_POSITION, 0L)
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get repeat mode: $e")
        0L
    }

    fun updateLastPosition(position: Long, appPref: SharedPreferences) = try {
        appPref.edit { putLong(LAST_POSITION, position) }
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to update last position: $e")
    }

    fun getInitialPriorityQueue(appPref: SharedPreferences) = try {
        appPref.getString(PRIORITY_QUEUE, "")?.let {
            Json.decodeFromString(ListSerializer(AudioFile.serializer()), it)
        } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    fun updatePriorityQueue(list: List<AudioFile>, appPref: SharedPreferences) = try {
        appPref.edit {
            putString(
                PRIORITY_QUEUE,
                Json.encodeToString(ListSerializer(AudioFile.serializer()), list)
            )
        }
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to update priority queue: $e")
    }

    /** Get the itemKey of the trashed item from the priority queue, can be the current media item. */
    fun getInitItemToTrash(appPref: SharedPreferences) = try {
        appPref.getString(ITEM_TO_TRASH_KEY, "") ?: ""
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get trashed item key: $e")
        ""
    }

    fun updateTrashedItemKey(key: String, appPref: SharedPreferences) = try {
        appPref.edit { putString(ITEM_TO_TRASH_KEY, key) }
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get trashed item key: $e")
    }

    fun getInitSavedIndex(appPref: SharedPreferences) = try {
        appPref.getInt(SAVED_INDEX, C.INDEX_UNSET)
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get saved index: $e")
        C.INDEX_UNSET
    }

    fun updateSavedIndex(index: Int, appPref: SharedPreferences) = try {
        appPref.edit { putInt(SAVED_INDEX, index) }
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to update saved index: $e")
    }

    fun clearPriorityItemStates(appPref: SharedPreferences) = try {
        updateTrashedItemKey("", appPref)
        updateSavedIndex(C.INDEX_UNSET, appPref)
        updateCurrentIndex(C.INDEX_UNSET, appPref)
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to clear items")
    }

    fun getInitCurrentIndex(appPref: SharedPreferences) = try {
        appPref.getInt(CURRENT_INDEX, C.INDEX_UNSET)
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get saved index: $e")
        C.INDEX_UNSET
    }

    fun updateCurrentIndex(index: Int, appPref: SharedPreferences) = try {
        appPref.edit { putInt(CURRENT_INDEX, index) }
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to update current media index: $e")
    }

    fun getPlaybackSpeed(appPref: SharedPreferences) = try {
        appPref.getFloat(PLAYBACK_SPEED, 1.0f)
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get playback speed: $e")
        1.0f
    }

    fun updatePlaybackSpeed(new: Float, appPref: SharedPreferences) = try {
        appPref.edit { putFloat(PLAYBACK_SPEED, new) }
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to update playback speed: $e")
    }

    fun getSkipSilenceEnabled(appPref: SharedPreferences) = try {
        appPref.getBoolean(SILENCED_SKIPPED_ENABLED, false)
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get silenced skipped enabled: $e")
        false
    }

    fun updateSkipSilenceEnabled(new: Boolean, appPref: SharedPreferences) = try {
        appPref.edit { putBoolean(SILENCED_SKIPPED_ENABLED, new) }
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to update silenced skipped enabled: $e")
    }

    fun getLanguage(appPref: SharedPreferences) = try {
        appPref.getString(GET_LANGUAGE, null)
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get language: $e")
        null
    }

    fun getQueue(context: Context) = context.dataStore.data.map { preferences ->
        try {
            preferences[QUEUE_SET]?.let { Json.decodeFromString<List<String>>(it) }
        } catch (_: Exception) {
            emptyList()
        } ?: emptyList()
    }

    suspend fun updateQueue(
        context: Context, songIds: List<String>
    ) = withContext(Dispatchers.IO) {
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

    internal fun updateLanguage(new: String?, appPref: SharedPreferences) = try {
        if (!new.isNullOrBlank())
            appPref.edit { putString(GET_LANGUAGE, new) }
        else appPref.edit { remove(GET_LANGUAGE) }
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to update language: $e")
    }
}