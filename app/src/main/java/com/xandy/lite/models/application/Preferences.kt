package com.xandy.lite.models.application

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.util.UnstableApi
import com.xandy.lite.models.Theme
import com.xandy.lite.models.media.player.ButtonType
import com.xandy.lite.models.media.player.LoadControl
import com.xandy.lite.models.media.player.PlaybackService
import com.xandy.lite.models.media.player.PlayerControls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

interface PrefRepository {
    val theme: Flow<Theme>
    suspend fun changeTheme(new: Theme)
    val offloadingEnabled: StateFlow<Boolean>
    fun toggleOffloading(enabled: Boolean)
    val loadControl: StateFlow<LoadControl>
    fun changeLoadControl(new: LoadControl)
    val fixPositionEnabled: StateFlow<Boolean>
    fun togglePositionFix(enabled: Boolean)
    val playerControls: StateFlow<PlayerControls>
    fun updatePlayerControls(new: PlayerControls, onSendMsg: () -> Unit)
}

@UnstableApi
class PrefRepositoryImpl(
    private val context: Context, private val appPref: SharedPreferences
) : PrefRepository {
    companion object {
        private val THEME = stringPreferencesKey("System.Theme")
        private const val OFFLOADING_ENABLED = "Offloading.Enabled"
        private const val LOAD_CONTROL = "LoadControl"
        private const val FIX_AUDIO_POSITION = "FixAudioPosition"
        private const val BUTTON_LAYOUT = ButtonType.BUTTON_LAYOUT
    }

    override val theme: Flow<Theme> = context.dataStore.data.map { preferences ->
        preferences[THEME]?.let { Json.decodeFromString(Theme.serializer(), it) } ?: Theme.Default
    }
    private val _offloadingEnabled = MutableStateFlow(appPref.getBoolean(OFFLOADING_ENABLED, false))
    override val offloadingEnabled = _offloadingEnabled.asStateFlow()
    private val _loadControl = MutableStateFlow(
        try {
            appPref.getString(LOAD_CONTROL, null)?.let {
                Json.decodeFromString<LoadControl>(it)
            } ?: LoadControl.Default
        } catch (_: Exception) {
            LoadControl.Default
        }
    )
    override val loadControl = _loadControl.asStateFlow()
    private val _fixPositionEnabled = MutableStateFlow(appPref.getBoolean(FIX_AUDIO_POSITION, true))
    override val fixPositionEnabled = _fixPositionEnabled.asStateFlow()

    private val _playerControls = MutableStateFlow(
        try {
            appPref.getString(BUTTON_LAYOUT, null)?.let {
                Json.decodeFromString(PlayerControls.serializer(), it)
            } ?: PlayerControls.Default
        } catch (_: Exception) {
            Log.e(XANDY_CLOUD, "Failed to get button layout")
            PlayerControls.Default
        }
    )
    override val playerControls = _playerControls.asStateFlow()

    override fun updatePlayerControls(new: PlayerControls, onSendMsg: () -> Unit) = _playerControls.update {
        appPref.edit {
            putString(
                BUTTON_LAYOUT, Json.encodeToString(PlayerControls.serializer(), new)
            )
        }
        onSendMsg()
        new
    }

    override suspend fun changeTheme(new: Theme) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { settings ->
                settings[THEME] = Json.encodeToString(Theme.serializer(), new)
            }
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed updating theme: $e")
            return@withContext
        }
    }

    override fun toggleOffloading(enabled: Boolean) = _offloadingEnabled.update {
        appPref.edit { putBoolean(OFFLOADING_ENABLED, enabled) }
        enabled
    }

    override fun changeLoadControl(new: LoadControl) = _loadControl.update {
        appPref.edit {
            putString(
                LOAD_CONTROL, Json.encodeToString(LoadControl.serializer(), new)
            )
        }
        new
    }

    override fun togglePositionFix(enabled: Boolean) = _fixPositionEnabled.update {
        appPref.edit { putBoolean(FIX_AUDIO_POSITION, enabled) }
        enabled
    }
}