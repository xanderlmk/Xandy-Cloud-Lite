package com.xandy.lite.db.song.repo

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xandy.lite.controllers.combineBucketKeyWithLocalBucket
import com.xandy.lite.controllers.combinePickedIdxWithPl
import com.xandy.lite.controllers.combinePickedNameWithLocalAlbum
import com.xandy.lite.controllers.combinePickedNameWithLocalArtist
import com.xandy.lite.controllers.combinePickedNameWithLocalGenre
import com.xandy.lite.controllers.groupAudioFilesByArtist
import com.xandy.lite.controllers.groupAudioFilesByGenre
import com.xandy.lite.controllers.groupAudioFilesIntoAlbums
import com.xandy.lite.db.daos.AudioDao
import com.xandy.lite.db.daos.BucketDao
import com.xandy.lite.db.daos.PlaylistDao
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.application.dataStore
import com.xandy.lite.models.ui.order.by.toAudioUIState
import com.xandy.lite.models.ui.order.by.toLocalPls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class LocalLibraryStates(
    audioDao: AudioDao, playlistDao: PlaylistDao, bucketDao: BucketDao,
    mcStates: MediaControllerStates, unknownTrackUri: Uri, private val context: Context
) {
    companion object {
        private val LOCAL_PL = intPreferencesKey("local_playlist")
        private val LOCAL_ALBUM = stringPreferencesKey("local_album_name")
        private val LOCAL_ARTIST = stringPreferencesKey("local_artist_name")
        private val LOCAL_BUCKET_VOL = stringPreferencesKey("local_bucket_vol")
        private val LOCAL_BUCKET_ID = longPreferencesKey("local_bucket_id")
        private val LOCAL_GENRE = stringPreferencesKey("local_genre_name")
        private const val PREFERENCES = "preferences"
        private const val AUTO_UPDATE = "auto_update_enabled"
    }

    private val audioOrderedBy = mcStates.audioOrderedBy
    private val localPlsOrderedBy = mcStates.localPlsOrderedBy
    val localPlaylists = localPlsOrderedBy.flatMapLatest { it.toLocalPls(playlistDao) }
    val audioFiles = audioOrderedBy.flatMapLatest { it.toAudioUIState(audioDao) }
    private val _plIndex = context.dataStore.data.map { preferences -> preferences[LOCAL_PL] ?: -1 }
    val pickedPlaylist = combinePickedIdxWithPl(_plIndex, localPlaylists).flowOn(Dispatchers.IO)
    val bucketsWithAudio = bucketDao.getFlowOfBucketsByNameASC()
        .flowOn(Dispatchers.IO.limitedParallelism(1, "Buckets/Folders"))
    private val _localBucketKey = context.dataStore.data.map { preferences ->
        Pair(preferences[LOCAL_BUCKET_VOL] ?: "", preferences[LOCAL_BUCKET_ID])
    }
    val pickedLocalBucket =
        combineBucketKeyWithLocalBucket(_localBucketKey, bucketsWithAudio)
    val localAlbums = audioFiles.map { state ->
        groupAudioFilesIntoAlbums(state.list.map { it.song }, unknownTrackUri)
    }.flowOn(Dispatchers.IO.limitedParallelism(2, "Albums"))
    private val _localAlbumName = context.dataStore.data.map { preferences ->
        preferences[LOCAL_ALBUM] ?: ""
    }
    val pickedLocalAlbum =
        combinePickedNameWithLocalAlbum(_localAlbumName, localAlbums)

    val localArtists = audioFiles.map { state ->
        groupAudioFilesByArtist(state.list.map { it.song })
    }.flowOn(Dispatchers.IO.limitedParallelism(2, "Artists"))

    private val _localArtistName = context.dataStore.data.map { preferences ->
        preferences[LOCAL_ARTIST] ?: ""
    }
    val pickedLocalArtist =
        combinePickedNameWithLocalArtist(_localArtistName, localArtists)

    val localGenres = audioFiles.map { state ->
        groupAudioFilesByGenre(state.list.map { it.song })
    }.flowOn(Dispatchers.IO.limitedParallelism(2, "Genres"))
    private val _localGenreName = context.dataStore.data.map { preferences ->
        preferences[LOCAL_GENRE] ?: ""
    }
    val pickedLocalGenre =
        combinePickedNameWithLocalGenre(_localGenreName, localGenres)

    suspend fun updateLocalPlIndex(idx: Int) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { settings ->
                settings[LOCAL_PL] = idx
            }
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed updating playlist index: $e")
            return@withContext
        }
    }

    suspend fun updateLocalAlbumName(n: String) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { settings ->
                settings[LOCAL_ALBUM] = n
            }
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed updating album name: $e")
            return@withContext
        }
    }

    suspend fun updateLocalArtistName(n: String) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { settings ->
                settings[LOCAL_ARTIST] = n
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

    suspend fun updateLocalGenreName(n: String) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { settings ->
                settings[LOCAL_GENRE] = n
            }
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed updating genre name: $e")
            return@withContext
        }
    }

    private val appPref = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun autoUpdateEnabled() = appPref.getBoolean(AUTO_UPDATE, false)
    fun toggleAutoUpdate(enabled: Boolean) {
        appPref.edit { putBoolean(AUTO_UPDATE, enabled) }; _autoUpdate.update { enabled }
    }

    private val _autoUpdate = MutableStateFlow(autoUpdateEnabled())
    val autoUpdate = _autoUpdate.asStateFlow()

}