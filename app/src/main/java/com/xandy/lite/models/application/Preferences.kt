package com.xandy.lite.models.application

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xandy.lite.models.Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

interface PrefRepository {
    val theme: StateFlow<Theme>
    suspend fun changeTheme(new: Theme)
    val offloadingEnabled: StateFlow<Boolean>
    suspend fun toggleOffloading(enabled: Boolean)
}

class PrefRepositoryImpl(
    private val context: Context, private val scope: CoroutineScope
) : PrefRepository {
    companion object {
        private val THEME = stringPreferencesKey("System.Theme")
        private val OFFLOADING_ENABLED = booleanPreferencesKey("Offloading.Enabled")
    }

    override val theme = context.dataStore.data.map { preferences ->
        preferences[THEME]?.let { Json.decodeFromString(Theme.serializer(), it) } ?: Theme.Default
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = Theme.Default)
    override val offloadingEnabled = context.dataStore.data.map { preferences ->
        preferences[OFFLOADING_ENABLED] ?: false
    }.stateIn(scope = scope, started = SharingStarted.Lazily, initialValue = false)

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

    override suspend fun toggleOffloading(enabled: Boolean) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { settings ->
                settings[OFFLOADING_ENABLED] = enabled
            }
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed updating offloading enabled: $e")
            return@withContext
        }
    }
}