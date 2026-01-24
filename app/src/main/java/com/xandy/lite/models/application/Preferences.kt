package com.xandy.lite.models.application

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.util.UnstableApi
import com.xandy.lite.models.AppPref
import com.xandy.lite.models.Theme
import com.xandy.lite.models.media.player.ButtonType
import com.xandy.lite.models.media.player.LoadControl
import com.xandy.lite.models.media.player.PlayerControls
import com.xandy.lite.models.ui.XCLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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
    val playbackSpeed: StateFlow<Float>
    fun updatePlaybackSpeed(new: Float, onSetSpeed: () -> Unit)
    val silenceSkippedEnabled: StateFlow<Boolean>
    fun updateSilencedSkippedEnabled()
    val selectedLanguage: StateFlow<XCLanguage>
    suspend fun updateSelectedLanguage(new: XCLanguage, onRecreate: () -> Unit)
    fun updateRestartState(new: Boolean)
    fun getRestartState(): Boolean
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
        private const val LANGUAGE = "XCLanguage"
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

    private val _playbackSpeed = MutableStateFlow(AppPref.getPlaybackSpeed(appPref))
    override val playbackSpeed = _playbackSpeed.asStateFlow()

    private val _silencedSkippedEnabled = MutableStateFlow(AppPref.getSkipSilenceEnabled(appPref))
    override val silenceSkippedEnabled = _silencedSkippedEnabled.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(
        try {
            appPref.getString(LANGUAGE, null)?.let {
                Json.decodeFromString(XCLanguage.serializer(), it)
            } ?: XCLanguage.Default
        } catch (_: Exception) {
            Log.e(XANDY_CLOUD, "Failed to get language")
            XCLanguage.Default
        }
    )
    override val selectedLanguage = _selectedLanguage.asStateFlow()

    override suspend fun updateSelectedLanguage(new: XCLanguage, onRecreate: () -> Unit) =
        withContext(Dispatchers.IO) {
            appPref.edit { putString(LANGUAGE, Json.encodeToString(XCLanguage.serializer(), new)) }
            AppPref.updateLanguage(new.toLocale(), appPref)
            _selectedLanguage.update { new }
            delay(500)
            onRecreate()
        }

    override fun updateSilencedSkippedEnabled() =
        _silencedSkippedEnabled.update {
            AppPref.updateSkipSilenceEnabled(!it, appPref)
            !it
        }

    override fun updatePlaybackSpeed(new: Float, onSetSpeed: () -> Unit) = _playbackSpeed.update {
        AppPref.updatePlaybackSpeed(new, appPref)
        onSetSpeed()
        new
    }

    override fun updatePlayerControls(new: PlayerControls, onSendMsg: () -> Unit) =
        _playerControls.update {
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

    override fun updateRestartState(new: Boolean) =
        appPref.edit { putBoolean("restart_state", new) }

    override fun getRestartState() = appPref.getBoolean("restart_state", false)
}