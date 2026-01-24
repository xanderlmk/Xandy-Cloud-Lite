package com.xandy.lite.models.ui.order.by

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringDef
import androidx.core.content.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.media3.common.Player
import com.xandy.lite.models.application.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.lang.annotation.RetentionPolicy

class OrderBy(private val appPref: SharedPreferences, context: Context) {
    companion object {
        private val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        private val REPEAT_MODE = intPreferencesKey("repeat_mode")
    }

    private var audiosOrderedBy: String
        get() = appPref.getString("local_audio_ordered_by", OBS.TITLE_ASC) ?: OBS.TITLE_ASC
        set(value) = appPref.edit { putString("local_audio_ordered_by", value) }
    private var hiddenOrderedBy: String
        get() = appPref.getString("hidden_audio_ordered_by", OBS.TITLE_ASC) ?: OBS.TITLE_ASC
        set(value) = appPref.edit { putString("hidden_audio_ordered_by", value) }
    private var favOrderedBy: String
        get() = appPref.getString("favorite_audio_ordered_by", OBS.TITLE_ASC) ?: OBS.TITLE_ASC
        set(value) = appPref.edit { putString("favorite_audio_ordered_by", value) }
    private var playlistsOrderedBy: String
        get() = appPref.getString("local_pls_ordered_by", OBS.NAME_ASC) ?: OBS.NAME_ASC
        set(value) = appPref.edit { putString("local_pls_ordered_by", value) }
    private var albumsOrderedBy: String
        get() = appPref.getString("local_album_ordered_by", OBS.DEFAULT) ?: OBS.DEFAULT
        set(value) = appPref.edit { putString("local_album_ordered_by", value) }
    private var artistOrderedBy: String
        get() = appPref.getString("local_artist_ordered_by", OBS.DEFAULT) ?: OBS.DEFAULT
        set(value) = appPref.edit { putString("local_artist_ordered_by", value) }
    private var genresOrderedBy: String
        get() = appPref.getString("local_genre_ordered_by", OBS.DEFAULT) ?: OBS.DEFAULT
        set(value) = appPref.edit { putString("local_genre_ordered_by", value) }
    private var queueOrderedBy: String
        get() = appPref.getString("queue_ordered_by", OBS.DEFAULT) ?: OBS.DEFAULT
        set(value) = appPref.edit { putString("queue_ordered_by", value) }
    val repeatMode = context.dataStore.data.map { preferences ->
        preferences[REPEAT_MODE] ?: Player.REPEAT_MODE_OFF
    }.flowOn(Dispatchers.IO)
    val shuffleEnabled = context.dataStore.data.map { preferences ->
        preferences[SHUFFLE_ENABLED] == true
    }.flowOn(Dispatchers.IO)

    fun getALOrderToClass() = audiosOrderedBy.toSongsOrderedByClass()

    fun getHiddenOrderToClass() = hiddenOrderedBy.toSongsOrderedByClass()

    fun getFavoriteOrderToClass() = favOrderedBy.toSongsOrderedByClass()

    fun getLocalPlsOrderToClass() = playlistsOrderedBy.toPlsOrderedByClass()

    fun getAlbumsOrderToClass() = albumsOrderedBy.toAlbumsOrderedByClass()

    fun getArtistOrderToClass() = artistOrderedBy.toArtistOrderedByClass()

    fun getGenreOrderToClass() = genresOrderedBy.toGenresOrderedByClass()

    fun getQueueOrder() = queueOrderedBy.toQueueOrderedByClass()

    fun updateALOrder(orderSongsBy: OrderSongsBy) {
        audiosOrderedBy = orderSongsBy.toOrderedString()
    }

    fun updateHiddenOrder(orderSongsBy: OrderSongsBy) {
        hiddenOrderedBy = orderSongsBy.toOrderedString()
    }

    fun updateFavoriteOrder(orderSongsBy: OrderSongsBy) {
        favOrderedBy = orderSongsBy.toOrderedString()
    }

    fun updateLocalPLOrder(orderPlsBy: OrderPlsBy) {
        playlistsOrderedBy = orderPlsBy.toOrderedString()
    }

    fun updateAlbumOrder(orderAlbumsBy: OrderAlbumsBy) {
        albumsOrderedBy = orderAlbumsBy.toOrderedString()
    }

    fun updateArtistOrder(orderArtistBy: OrderArtistBy) {
        artistOrderedBy = orderArtistBy.toOrderedString()
    }

    fun updateGenreOrder(orderGenresBy: OrderGenresBy) {
        genresOrderedBy = orderGenresBy.toOrderedString()
    }

    fun updateQueueOrder(orderQueueBy: OrderQueueBy) {
        queueOrderedBy = orderQueueBy.toOrderedString()
    }
}