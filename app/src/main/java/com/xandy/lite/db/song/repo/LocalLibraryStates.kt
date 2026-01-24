package com.xandy.lite.db.song.repo

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xandy.lite.controllers.combineBucketKeyWithLocalBucket
import com.xandy.lite.controllers.combinePickedUUIDWithPl
import com.xandy.lite.controllers.combinePickedNameWithLocalAlbum
import com.xandy.lite.controllers.combinePickedNameWithLocalArtist
import com.xandy.lite.controllers.combinePickedNameWithLocalGenre
import com.xandy.lite.controllers.groupAudioFilesByArtist
import com.xandy.lite.controllers.groupAudioFilesByGenre
import com.xandy.lite.controllers.groupAudioFilesIntoAlbums
import com.xandy.lite.db.daos.AudioDao
import com.xandy.lite.db.daos.BucketDao
import com.xandy.lite.db.daos.PlaylistDao
import com.xandy.lite.models.application.AppValues
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.application.dataStore
import com.xandy.lite.models.application.toStrings
import com.xandy.lite.models.ui.MediaState
import com.xandy.lite.models.ui.order.by.toAudioUIState
import com.xandy.lite.models.ui.order.by.toHiddenUIState
import com.xandy.lite.models.ui.order.by.toLocalPls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@OptIn(ExperimentalCoroutinesApi::class)
internal class LocalLibraryStates(
    private val audioDao: AudioDao, playlistDao: PlaylistDao, bucketDao: BucketDao,
    mcStates: MediaControllerStates, appValues: StateFlow<AppValues>, private val context: Context
) {
    companion object {
        private val LOCAL_PL = stringPreferencesKey("local_playlist_name")
        private val LOCAL_ALBUM = stringPreferencesKey("local_album_name")
        private val LOCAL_ARTIST = stringPreferencesKey("local_artist_name")
        private val LOCAL_BUCKET_VOL = stringPreferencesKey("local_bucket_vol")
        private val LOCAL_BUCKET_ID = longPreferencesKey("local_bucket_id")
        private val LOCAL_GENRE = stringPreferencesKey("local_genre_name")
        private val ID_WRITE_ENABLE = booleanPreferencesKey("enabled_id_writing")
        private const val PREFERENCES = "preferences"
        private const val AUTO_UPDATE = "auto_update_enabled"
    }

    private val scope = CoroutineScope(SupervisorJob())
    private val audioOrderedBy = mcStates.audioOrderedBy
    private val hiddenOrderedBy = mcStates.hiddenOrderedBy
    private val localPlsOrderedBy = mcStates.localPlsOrderedBy
    private val albumsOrderedBy = mcStates.albumsOrderedBy
    private val artistOrderedBy = mcStates.artistOrderedBy
    private val genreOrderedBy = mcStates.genreOrderedBy
    val localPlaylists = localPlsOrderedBy.toLocalPls(playlistDao, appValues.toStrings(scope))
    val audioFiles = audioOrderedBy.flatMapLatest { it.toAudioUIState(audioDao) }
    val hiddenFiles =
        hiddenOrderedBy.flatMapLatest {
            it.toHiddenUIState(audioDao).map { l -> l.list.map { t -> t.song } }
        }
    private val _plUUID = context.dataStore.data.map { preferences -> preferences[LOCAL_PL] ?: "" }
    val pickedPlaylist = combinePickedUUIDWithPl(_plUUID, localPlaylists).flowOn(Dispatchers.IO)
    val bucketsWithAudio = bucketDao.getFlowOfBucketsByNameASC()
        .flowOn(Dispatchers.IO.limitedParallelism(1, "Buckets/Folders"))
    private val _localBucketKey = context.dataStore.data.map { preferences ->
        Pair(preferences[LOCAL_BUCKET_VOL] ?: "", preferences[LOCAL_BUCKET_ID])
    }
    val pickedLocalBucket =
        combineBucketKeyWithLocalBucket(_localBucketKey, bucketsWithAudio)
    val localAlbums = combine(audioFiles, albumsOrderedBy, appValues) { state, order, values ->
        groupAudioFilesIntoAlbums(state.list.map { it.song }, order, values)
    }.flowOn(Dispatchers.IO.limitedParallelism(2, "Albums"))
    private val _localAlbumName = context.dataStore.data.map { preferences ->
        try {
            preferences[LOCAL_ALBUM]?.let { Json.decodeFromString(MediaState.serializer(), it) }
                ?: MediaState("", "")
        } catch (_: Exception) {
            MediaState("", "")
        }
    }
    val pickedLocalAlbum =
        combinePickedNameWithLocalAlbum(_localAlbumName, localAlbums)

    val localArtists = combine(audioFiles, artistOrderedBy, appValues) { state, order, values ->
        groupAudioFilesByArtist(state.list.map { it.song }, order, values)
    }.flowOn(Dispatchers.IO.limitedParallelism(2, "Artists"))

    private val _localArtistName = context.dataStore.data.map { preferences ->
        try {
            preferences[LOCAL_ARTIST]?.let { Json.decodeFromString(MediaState.serializer(), it) }
                ?: MediaState("", "")
        } catch (_: Exception) {
            MediaState("", "")
        }
    }
    val pickedLocalArtist =
        combinePickedNameWithLocalArtist(_localArtistName, localArtists)

    val localGenres = combine(audioFiles, genreOrderedBy, appValues) { state, order, values ->
        groupAudioFilesByGenre(state.list.map { it.song }, order, values.unknownTrackUri)
    }.flowOn(Dispatchers.IO.limitedParallelism(2, "Genres"))
    private val _localGenreName = context.dataStore.data.map { preferences ->
        try {
            preferences[LOCAL_GENRE]?.let { Json.decodeFromString(MediaState.serializer(), it) }
                ?: MediaState("", "")
        } catch (_: Exception) {
            MediaState("", "")
        }
    }
    val pickedLocalGenre =
        combinePickedNameWithLocalGenre(_localGenreName, localGenres)

    suspend fun updateLocalPlUUID(s: String) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { settings -> settings[LOCAL_PL] = s }
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed updating playlist name: $e")
            return@withContext
        }
    }

    suspend fun updateLocalAlbumName(n: MediaState) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { settings ->
                settings[LOCAL_ALBUM] = Json.encodeToString(
                    MediaState.serializer(), n
                )
            }
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed updating album name: $e")
            return@withContext
        }
    }

    suspend fun updateLocalArtistName(n: MediaState) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { settings ->
                settings[LOCAL_ARTIST] = Json.encodeToString(
                    MediaState.serializer(), n
                )
            }
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed updating artist name: $e")
            return@withContext
        }
    }

    suspend fun updateLocalBucketKey(p: Pair<String, Long>) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { settings ->
                settings[LOCAL_BUCKET_VOL] = p.first
                settings[LOCAL_BUCKET_ID] = p.second
            }
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed updating bucket key: $e")
            return@withContext
        }
    }

    suspend fun updateLocalGenreName(n: MediaState) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { settings ->
                settings[LOCAL_GENRE] = Json.encodeToString(
                    MediaState.serializer(), n
                )
            }
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed updating genre name: $e")
            return@withContext
        }
    }

    private val appPref = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun autoUpdateEnabled() = appPref.getBoolean(AUTO_UPDATE, true)
    fun toggleAutoUpdate(enabled: Boolean) {
        appPref.edit { putBoolean(AUTO_UPDATE, enabled) }; _autoUpdate.update { enabled }
    }

    private val _autoUpdate = MutableStateFlow(autoUpdateEnabled())
    val autoUpdate = _autoUpdate.asStateFlow()

    val songIdWritingEnabled = context.dataStore.data.map { preferences ->
        preferences[ID_WRITE_ENABLE] ?: false
    }

    suspend fun updateIdWritingEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { settings ->
                settings[ID_WRITE_ENABLE] = enabled
            }
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed updating id writing enabled: $e")
            return@withContext
        }
    }
}