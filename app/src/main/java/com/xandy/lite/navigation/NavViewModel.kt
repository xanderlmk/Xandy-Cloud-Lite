package com.xandy.lite.navigation

import android.net.Uri
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.session.MediaController
import com.xandy.lite.controllers.getPlsOrderedBy
import com.xandy.lite.controllers.getSlOrderedBy
import com.xandy.lite.db.song.repo.SongRepository
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.Playlist
import com.xandy.lite.models.AudioIds
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.ui.AudioUIState
import com.xandy.lite.models.ui.LocalAudioStates
import com.xandy.lite.models.ui.LocalMusicTabs
import com.xandy.lite.models.ui.order.by.PlaylistOrder
import com.xandy.lite.models.ui.order.by.SongOrder
import com.xandy.lite.models.ui.order.by.reverseSort
import com.xandy.lite.models.ui.order.by.toOrderedByClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NavViewModel(
    private val savedStateHandle: SavedStateHandle, private val uiRepository: UIRepository,
    private val songRepository: SongRepository,
) : ViewModel() {
    companion object {
        private const val ROUTE = "route"
        private const val TIMEOUT_MILLIS = 4_000L
        private const val LOCAL_AUDIO_URI = "local_audio_uri"
    }

    private val _route = MutableStateFlow(savedStateHandle[ROUTE] ?: LocalMusicDestination.route)
    val route = _route.asStateFlow()
    val isSearching = uiRepository.isSearching

    val songDetails = songRepository.songDetails.stateIn(
        scope = viewModelScope, started = SharingStarted.Lazily,
        initialValue = null
    )

    val mediaController = songRepository.mediaController
    val tracks = songRepository.tracks
    val isPlaying = songRepository.isPlaying
    val isLoading = songRepository.isLoading
    val repeatMode = songRepository.repeatMode.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = Player.REPEAT_MODE_OFF
    )
    val isSelecting = uiRepository.isSelecting
    val isAdding = uiRepository.isAdding
    val query = uiRepository.query
    val querySet = uiRepository.recentQueries.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = setOf()
    )

    fun updateRoute(r: String) = _route.update {
        savedStateHandle[ROUTE] = r; r
    }.also { Log.i(XANDY_CLOUD, "Route : $r") }

    fun turnOnSearch() = uiRepository.turnOnSearch()
    fun turnOffSearch() = uiRepository.turnOffSearch()
    fun resetSearch() = uiRepository.resetSearch()

    fun updateQuery(new: String) {
        uiRepository.updateQuery(new)
    }

    fun updateRecentQuery(new: String) = viewModelScope.launch {
        if (new.length > 2) uiRepository.addQuery(new)
    }

    fun updateMediaController(mc: MediaController) {
        viewModelScope.launch {
            songRepository.updateMediaController(mc)
        }
    }

    fun resetMediaController() = songRepository.resetMediaController()
    fun stopCheckingPosition() = songRepository.stopCheckingPlaybackPosition()

    fun updateTracks(tracks: Tracks) = songRepository.updateTracks(tracks)

    fun updateIsPlaying(isPlaying: Boolean) = songRepository.updateIsPlaying(isPlaying)
    fun updateIsLoading(isLoading: Boolean) = songRepository.updateIsLoading(isLoading)

    fun updatePickedSong(id: String?) {
        viewModelScope.launch {
            id ?: return@launch
            songRepository.updatePickedSong(id)
        }
    }

    fun updateDuration(duration: Long) = songRepository.updateDuration(duration)
    fun updatePosition(position: Long) = songRepository.updatePosition(position)

    fun startAdding(songs: List<AudioFile>) = uiRepository.startAdding(songs)

    /** End the selection, clear the list, and stop adding (if adding) */
    fun endSelect() = uiRepository.endSelect()

    fun getSelectedSongIds() = uiRepository.selectedSongIds.value

    val percentFetched = songRepository.percentFetched
    val audioFiles = songRepository.audioFiles.stateIn(
        scope = viewModelScope, started = SharingStarted.Eagerly,
        initialValue = AudioUIState()
    )
    val plWithAudio = songRepository.pickedPlaylist.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(4_000L),
        initialValue = null
    )
    val artworkList = songRepository.allMediaArtwork.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    val localTab = songRepository.localTab
    val selectedFolders = uiRepository.selectedBuckets
    private val _audioOrderedBy = songRepository.audioOrderedBy
    val alDirection = getSlOrderedBy(viewModelScope, _audioOrderedBy, TIMEOUT_MILLIS)
    private val _localPlsOrderedBy = songRepository.localPlsOrderedBy
    val localPlsDirection = getPlsOrderedBy(viewModelScope, _localPlsOrderedBy, TIMEOUT_MILLIS)

    /** First: isSearching, Second: isSelecting, Third: localAudiosLoading */
    private val uiStates =
        combine(
            isSearching, isSelecting, songRepository.filesLoading,
            songRepository.autoUpdate
        ) { searching, selecting, loading, autoUpdate ->
            UiStates(searching, selecting, loading, autoUpdate)
        }
    val audioStates = combine(
        uiStates, localTab, localPlsDirection, alDirection, percentFetched
    ) { t, tab, plDir, alDir, percent ->
        LocalAudioStates(
            isSearching = t.isSearching, isSelecting = t.isSelecting, tab = tab,
            isLoading = t.localAudiosLoading, plsDirection = plDir, alDirection = alDir,
            percent = percent, autoUpdate = t.autoUpdate
        )
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = LocalAudioStates(
            isSearching = isSearching.value, isSelecting = isSelecting.value,
            tab = localTab.value, isLoading = songRepository.filesLoading.value,
            plsDirection = localPlsDirection.value, alDirection = alDirection.value,
            percent = percentFetched.value, autoUpdate = songRepository.autoUpdateEnabled()
        )
    )
    val writingEnabled = songRepository.idWritingEnabled.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = false
    )

    fun toggleAutoUpdate(enabled: Boolean) = songRepository.toggleAutoUpdate(enabled)

    fun toggleWritingEnabled(enabled: Boolean) = viewModelScope.launch {
        songRepository.toggleIdWriting(enabled)
    }

    fun updateAudioUri(uri: String) {
        savedStateHandle[LOCAL_AUDIO_URI] = uri; songRepository.updateAudioUri(uri)
    }

    /** Add Local Songs to a Local Playlist */
    suspend fun addLocalSongsToPL(songIds: List<String>, playlistId: String) =
        songRepository.addLocalSongsToPl(songIds, playlistId)

    /** Remove Local Songs from a Local Playlist */
    suspend fun removeLocalSongsFromPL(songIds: List<String>, playlistId: String) =
        songRepository.removeLocalSongsFromPl(songIds, playlistId)

    private val _requestEvents = MutableStateFlow<Pair<IntentSenderRequest, List<AudioIds>>?>(null)
    val requestEvents = _requestEvents.asStateFlow()
    fun updateAudioFiles() = viewModelScope.launch(Dispatchers.IO) {
        val pendingIntent = songRepository.updateMediaFiles()
        pendingIntent?.let { pending ->
            _requestEvents.update {
                Pair(
                    IntentSenderRequest.Builder(pending.first.intentSender).build(), pending.second
                )
            }
        }
    }

    suspend fun insertSongIdToMetadata() = _requestEvents.value?.let {
        val result = songRepository.updateSongIdTag(it.second)
        _requestEvents.update { null }
        return@let result
    } ?: true

    suspend fun insertLocalPl(name: String) = songRepository.addLocalPlaylist(name)
    fun updatePlUUID(s: String) = viewModelScope.launch {
        songRepository.updateLocalPlUUID(s)
    }

    fun navToPlaylist(uuid: String) = viewModelScope.launch {
        songRepository.findPlaylistUUID(uuid).takeIf { it.isNotBlank() } ?: return@launch
    }

    fun updateAlbumName(str: String) = viewModelScope.launch {
        songRepository.updateLocalAlbumName(str)
    }

    fun updateArtistName(str: String) = viewModelScope.launch {
        songRepository.updateLocalArtistName(str)
    }

    fun updateBucketKey(s: String, l: Long) = viewModelScope.launch {
        songRepository.updateLocalBucketKey(Pair(s, l))
    }

    fun updateGenreName(str: String) = viewModelScope.launch {
        songRepository.updateLocalGenreName(str)
    }

    fun updateLocalALOrder(songOrder: SongOrder) =
        songRepository.updateLocalALOrder(songOrder.toOrderedByClass(alDirection.value))

    fun reverseALOrder() = songRepository.updateLocalALOrder(_audioOrderedBy.value.reverseSort())

    fun reverseLocalPlsOrder() =
        songRepository.updateLocalPLOrder(_localPlsOrderedBy.value.reverseSort())

    fun updateLocalPLOrder(playlistOrder: PlaylistOrder) =
        songRepository.updateLocalPLOrder(playlistOrder.toOrderedByClass(localPlsDirection.value))

    suspend fun hideFolders(set: Set<Pair<String, Long>>) = songRepository.hideBuckets(set)

    suspend fun hideAudios(uris: List<String>) = songRepository.hideAudioFiles(uris)

    suspend fun showAudios(uris: List<String>) = songRepository.showAudioFiles(uris)

    suspend fun deleteAudios(ids: List<String>) =
        songRepository.deleteLocalAudios(ids)

    fun toggleSong(songId: String) = uiRepository.toggleSong(songId)

    /** Selects all the songs based on the query filter */
    fun selectAllSongs() = viewModelScope.launch {
        val query = query.value
        val isSearching = isSearching.value
        val filtered =
            if (localTab.value == LocalMusicTabs.LIBRARY) audioFiles.value.list.filter { audio ->
                if (query.isBlank() || !isSearching) return@filter true
                audio.song.title.contains(query, ignoreCase = true) ||
                        audio.song.artist.contains(query, ignoreCase = true)
            }.map { it.song } else songRepository.hiddenAudio.first().filter { audio ->
                if (query.isBlank() || !isSearching) return@filter true
                audio.title.contains(query, ignoreCase = true) ||
                        audio.artist.contains(query, ignoreCase = true)
            }

        uiRepository.selectAll(filtered.map { it.id })
    }

    suspend fun updateLocalPlArtwork(pl: Playlist, newPic: Uri) =
        songRepository.updatePlArtwork(name = pl.name, newPic = newPic, currentPic = pl.picture)

    suspend fun changePlName(newName: String, name: String) =
        songRepository.changePlaylistName(newName, name, plWithAudio.value?.playlist?.id)


    fun getMediaController() = songRepository.getMediaController()

    init {
        if (songRepository.autoUpdateEnabled()) viewModelScope.launch { updateAudioFiles() }
        savedStateHandle.get<String?>(LOCAL_AUDIO_URI)?.let {
            songRepository.updateAudioUri(it)
        }
    }

    suspend fun updateArtistsOfAL(ids: List<String>, artist: String) =
        songRepository.updateArtistOfAL(ids, artist)


    suspend fun updateAlbumOfAL(ids: List<String>, album: String) =
        songRepository.updateAlbumOfAL(ids, album)


    suspend fun updateGenreOfAL(ids: List<String>, genre: String) =
        songRepository.updateGenreOfAL(ids, genre)

}


private data class UiStates(
    val isSearching: Boolean, val isSelecting: Boolean, val localAudiosLoading: Boolean,
    val autoUpdate: Boolean
)