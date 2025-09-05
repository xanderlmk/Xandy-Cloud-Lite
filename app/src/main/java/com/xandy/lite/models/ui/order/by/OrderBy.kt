package com.xandy.lite.models.ui.order.by

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.media3.common.Player
import com.xandy.lite.models.application.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class OrderBy(private val appPref: SharedPreferences, context: Context) {
    companion object {
        private val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        private val REPEAT_MODE = intPreferencesKey("repeat_mode")
    }
    private var laOrderedBy: String
        get() = appPref.getString("local_audio_ordered_by", OBS.TITLE_ASC) ?: OBS.TITLE_ASC
        set(value) = appPref.edit { putString("local_audio_ordered_by", value) }
    private var localPlsOrderedBy: String
        get() = appPref.getString("local_pls_ordered_by", OBS.NAME_ASC) ?: OBS.NAME_ASC
        set(value) = appPref.edit { putString("local_pls_ordered_by", value) }
    private var queueOrderedBy: String
        get() = appPref.getString("queue_ordered_by", OBS.DEFAULT) ?: OBS.DEFAULT
        set(value) = appPref.edit { putString("queue_ordered_by", value) }

    val repeatMode = context.dataStore.data.map { preferences ->
        preferences[REPEAT_MODE] ?: Player.REPEAT_MODE_OFF
    }.flowOn(Dispatchers.IO)
    val shuffleEnabled = context.dataStore.data.map { preferences ->
        preferences[SHUFFLE_ENABLED] == true
    }.flowOn(Dispatchers.IO)


    fun getALOrderToClass() = laOrderedBy.toSongsOrderedByClass()

    fun getLocalPlsOrderToClass() = localPlsOrderedBy.toPlsOrderedByClass()

    fun getQueueOrder() = queueOrderedBy.toQueueOrderedByClass()

    fun updateALOrder(orderSongsBy: OrderSongsBy) {
        laOrderedBy = orderSongsBy.toOrderedString()
    }

    fun updateLocalPLOrder(orderPlsBy: OrderPlsBy) {
        localPlsOrderedBy = orderPlsBy.toOrderedString()
    }

    fun updateQueueOrder(orderQueueBy: OrderQueueBy) {
        queueOrderedBy = orderQueueBy.toOrderedString()
    }
}