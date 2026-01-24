package com.xandy.lite.navigation

import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.xandy.lite.controllers.getAlbumOrderedBy
import com.xandy.lite.controllers.getArtistOrderedBy
import com.xandy.lite.controllers.getGenreOrderedBy
import com.xandy.lite.controllers.getPlsOrderedBy
import com.xandy.lite.controllers.getSlOrderedBy
import com.xandy.lite.db.lyrics.repo.LyricsRepository
import com.xandy.lite.db.song.repo.SongRepository
import com.xandy.lite.db.tables.Playlist
import com.xandy.lite.models.AudioIds
import com.xandy.lite.models.ui.AudioUIState
import com.xandy.lite.models.ui.IsDefaultMediaOrder
import com.xandy.lite.models.ui.LocalAudioStates
import com.xandy.lite.models.ui.LocalMusicTabs
import com.xandy.lite.models.ui.MediaDirections
import com.xandy.lite.models.ui.MediaState
import com.xandy.lite.models.ui.order.by.AlbumOrder
import com.xandy.lite.models.ui.order.by.ArtistOrder
import com.xandy.lite.models.ui.order.by.GenreOrder
import com.xandy.lite.models.ui.order.by.OrderAlbumsBy
import com.xandy.lite.models.ui.order.by.OrderArtistBy
import com.xandy.lite.models.ui.order.by.OrderGenresBy
import com.xandy.lite.models.ui.order.by.PlaylistOrder
import com.xandy.lite.models.ui.order.by.SongOrder
import com.xandy.lite.models.ui.order.by.reverseSort
import com.xandy.lite.models.ui.order.by.toOrderedByClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NavViewModel(
    private val savedStateHandle: SavedStateHandle, private val uiRepository: UIRepository,
    private val songRepository: SongRepository, private val lyricsRepository: LyricsRepository
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
    val isPlaying = songRepository.isPlaying
    val isLoading = songRepository.isLoading
    val repeatMode = songRepository.repeatMode.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = Player.REPEAT_MODE_OFF
    )
    val shuffleEnabled = songRepository.shuffleEnabled.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = false
    )
    val isSelecting = uiRepository.isSelecting
    val isAdding = uiRepository.isAdding
    val query = uiRepository.query
    val querySet = uiRepository.recentQueries.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = setOf()
    )
    val queue = songRepository.unsortedQueue.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    fun updateRoute(r: String) = _route.update {
        savedStateHandle[ROUTE] = r; r
    }

    fun getRoute() = _route.value

    fun turnOnSearch() = uiRepository.turnOnSearch()
    fun turnOffSearch() = uiRepository.turnOffSearch()
    fun resetSearch() = uiRepository.resetSearch()

    fun updateQuery(new: String) {
        uiRepository.updateQuery(new)
    }

    fun updateRecentQuery(new: String) = viewModelScope.launch {
        if (new.length > 2) uiRepository.addQuery(new)
    }

    fun updateLanguage() {
        uiRepository.onUpdateLanguage()
    }


    fun updateMediaController(mc: MediaController) = try {
        songRepository.updateMediaController(mc)
    } catch (_: Exception) {
    }

    fun resetMediaController() = songRepository.resetMediaController()
    fun stopCheckingPosition() = songRepository.stopCheckingPlaybackPosition()

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
    fun updateLastestPlayerInfo() = songRepository.updateLastestPlayerInfo()

    fun startAdding() = uiRepository.startAdding()

    /** End the selection, clear the list, and stop adding (if adding) */
    fun endSelect() = uiRepository.endSelect()

    fun getSelectedSongIds() = uiRepository.selectedSongIds.value
    val listNotEmpty = uiRepository.selectedSongIds.map { it.isNotEmpty() }.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = true
    )

    val gettingAudioPics = songRepository.gettingAudioPics
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
    private val alDirection = getSlOrderedBy(viewModelScope, _audioOrderedBy, TIMEOUT_MILLIS)
    private val _hiddenOrderedBy = songRepository.hiddenOrderedBy
    private val hiddenDirection = getSlOrderedBy(viewModelScope, _hiddenOrderedBy, TIMEOUT_MILLIS)
    private val _favOrderedBy = songRepository.favOrderedBy
    private val favDirection = getSlOrderedBy(viewModelScope, _favOrderedBy, TIMEOUT_MILLIS)
    private val _localPlsOrderedBy = songRepository.localPlsOrderedBy
    private val localPlsDirection =
        getPlsOrderedBy(viewModelScope, _localPlsOrderedBy, TIMEOUT_MILLIS)
    private val _albumOrderBy = songRepository.albumOrderedBy
    private val albumDirection = getAlbumOrderedBy(viewModelScope, _albumOrderBy, TIMEOUT_MILLIS)
    private val _artistOrderBy = songRepository.artistOrderedBy
    private val artistDirection = getArtistOrderedBy(viewModelScope, _artistOrderBy, TIMEOUT_MILLIS)
    private val _genreOrderBy = songRepository.genreOrderedBy
    private val genreDirection = getGenreOrderedBy(viewModelScope, _genreOrderBy, TIMEOUT_MILLIS)

    val defaultMediaDir =
        combine(_albumOrderBy, _artistOrderBy, _genreOrderBy) { album, artist, genre ->
            IsDefaultMediaOrder(
                album = album is OrderAlbumsBy.Default,
                artist = artist is OrderArtistBy.Default,
                genre = genre is OrderGenresBy.Default
            )
        }.stateIn(
            viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue =
                IsDefaultMediaOrder(
                    album = _albumOrderBy.value is OrderAlbumsBy.Default,
                    artist = _artistOrderBy.value is OrderArtistBy.Default,
                    genre = _genreOrderBy.value is OrderGenresBy.Default
                )
        )
    private val directions = combine(
        alDirection, hiddenDirection, localPlsDirection,
        albumDirection, artistDirection, genreDirection, favDirection
    ) { values ->
        MediaDirections(
            alDirection = values[0], hiddenDirection = values[1], plsDirection = values[2],
            albumDirection = values[3], artistDirection = values[4], genreDirection = values[5],
            favDirection = values[6]
        )
    }

    /** First: isSearching, Second: isSelecting, Third: localAudiosLoading */
    private val uiStates =
        combine(
            isSearching, isSelecting, songRepository.filesLoading,
            songRepository.autoUpdate, songRepository.gettingAudioPics
        ) { searching, selecting, loading, autoUpdate, getting ->
            UiStates(searching, selecting, loading, autoUpdate, getting)
        }
    val audioStates = combine(
        uiStates, localTab, directions
    ) { t, tab, dir ->
        LocalAudioStates(
            isSearching = t.isSearching, isSelecting = t.isSelecting, tab = tab,
            isLoading = t.localAudiosLoading, plsDirection = dir.plsDirection,
            alDirection = dir.alDirection, albumDirection = dir.albumDirection,
            artistDirection = dir.artistDirection, genreDirection = dir.genreDirection,
            gettingPics = t.gettingPics, autoUpdate = t.autoUpdate,
            hiddenDirection = dir.hiddenDirection, favDirections = dir.favDirection
        )
    }.stateIn(
        viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = LocalAudioStates(
            isSearching = isSearching.value, isSelecting = isSelecting.value,
            tab = localTab.value, isLoading = songRepository.filesLoading.value,
            plsDirection = localPlsDirection.value, alDirection = alDirection.value,
            albumDirection = albumDirection.value, artistDirection = artistDirection.value,
            genreDirection = genreDirection.value, gettingPics = gettingAudioPics.value,
            hiddenDirection = hiddenDirection.value, favDirections = favDirection.value,
            autoUpdate = songRepository.autoUpdateEnabled()
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
                    IntentSenderRequest.Builder(pending.first.intentSender).build(),
                    pending.second
                )
            }
        }
    }


    fun onCreateVM() {
        if (songRepository.autoUpdateEnabled()) updateAudioFiles()
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

    fun updateAlbumName(str: MediaState) = viewModelScope.launch {
        songRepository.updateLocalAlbumName(str)
    }

    fun updateArtistName(str: MediaState) = viewModelScope.launch {
        songRepository.updateLocalArtistName(str)
    }

    fun updateBucketKey(s: String, l: Long) = viewModelScope.launch {
        songRepository.updateLocalBucketKey(Pair(s, l))
    }

    fun updateGenreName(str: MediaState) = viewModelScope.launch {
        songRepository.updateLocalGenreName(str)
    }

    fun updateLocalALOrder(songOrder: SongOrder) =
        songRepository.updateLocalALOrder(songOrder.toOrderedByClass(alDirection.value))

    fun reverseALOrder() =
        songRepository.updateLocalALOrder(_audioOrderedBy.value.reverseSort())

    fun updateHiddenOrder(songOrder: SongOrder) =
        songRepository.updateHiddenOrder(songOrder.toOrderedByClass(hiddenDirection.value))

    fun reverseHiddenOrder() =
        songRepository.updateHiddenOrder(_hiddenOrderedBy.value.reverseSort())

    fun updateFavoriteOrder(songOrder: SongOrder) =
        songRepository.updateFavoriteOrder(songOrder.toOrderedByClass(favDirection.value))

    fun reverseFavoriteOrder() =
        songRepository.updateFavoriteOrder(_favOrderedBy.value.reverseSort())

    fun reverseLocalPlsOrder() =
        songRepository.updateLocalPLOrder(_localPlsOrderedBy.value.reverseSort())

    fun updateLocalPLOrder(playlistOrder: PlaylistOrder) =
        songRepository.updateLocalPLOrder(playlistOrder.toOrderedByClass(localPlsDirection.value))

    fun reverseAlbumOrder() = songRepository.updateAlbumOrder(_albumOrderBy.value.reverseSort())

    fun updateAlbumOrder(albumOrder: AlbumOrder) =
        songRepository.updateAlbumOrder(albumOrder.toOrderedByClass(albumDirection.value))

    fun reverseArtistOrder() =
        songRepository.updateArtistOrder(_artistOrderBy.value.reverseSort())

    fun updateArtistOrder(artistOrder: ArtistOrder) =
        songRepository.updateArtistOrder(artistOrder.toOrderedByClass(artistDirection.value))

    fun reverseGenreOrder() = songRepository.updateGenreOrder(_genreOrderBy.value.reverseSort())
    fun updateGenreOrder(genreOrder: GenreOrder) =
        songRepository.updateGenreOrder(genreOrder.toOrderedByClass(genreDirection.value))

    suspend fun hideFolders(set: Set<Pair<String, Long>>) = songRepository.hideBuckets(set)

    suspend fun hideAudios(ids: List<String>) = songRepository.hideAudioFiles(ids)

    suspend fun showAudios(ids: List<String>) = songRepository.showAudioFiles(ids)

    suspend fun deleteAudios(ids: List<String>) =
        songRepository.deleteLocalAudios(ids)

    fun toggleSong(songId: String) = uiRepository.toggleSong(songId)

    /** Selects all the songs based on the query filter */
    fun selectAllSongs(onMaxReached: () -> Unit) = viewModelScope.launch {
        val query = query.value
        val isSearching = isSearching.value
        val filtered =
            when (localTab.value) {
                LocalMusicTabs.LIBRARY -> audioFiles.value.list.filter { audio ->
                    if (query.isBlank() || !isSearching) return@filter true
                    audio.song.title.contains(query, ignoreCase = true) ||
                            audio.song.artist?.contains(query, ignoreCase = true) ?: false
                }.map { it.song }

                LocalMusicTabs.FAVORITES -> songRepository.favorites.first().filter { audio ->
                    if (query.isBlank() || !isSearching) return@filter true
                    audio.title.contains(query, ignoreCase = true) ||
                            audio.artist?.contains(query, ignoreCase = true) ?: false
                }

                else -> songRepository.hiddenAudios.first().filter { audio ->
                    if (query.isBlank() || !isSearching) return@filter true
                    audio.title.contains(query, ignoreCase = true) ||
                            audio.artist?.contains(query, ignoreCase = true) ?: false
                }
            }
        if (filtered.size >= 2000) onMaxReached()
        uiRepository.selectAll(filtered.map { it.id }.take(2_000))
    }

    suspend fun updateLocalPlArtwork(pl: Playlist, newPic: Uri) =
        songRepository.updatePlArtwork(name = pl.name, newPic = newPic, currentPic = pl.picture)

    suspend fun changePlName(newName: String, name: String) =
        songRepository.changePlaylistName(newName, name, plWithAudio.value?.playlist?.id)


    fun getMediaController() = songRepository.getMediaController()
    fun handleSkipNext(shuffleEnabled: Boolean, repeatMode: Int, mc: MediaController) =
        songRepository.handleSkipNext(shuffleEnabled, repeatMode, mc)

    init {
        savedStateHandle.get<String?>(LOCAL_AUDIO_URI)
            ?.let { songRepository.updateAudioUri(it) }
    }

    suspend fun updateArtistsOfAL(ids: List<String>, artist: String) =
        songRepository.updateArtistOfAL(ids, artist)


    suspend fun updateAlbumOfAL(ids: List<String>, album: String) =
        songRepository.updateAlbumOfAL(ids, album)


    suspend fun updateGenreOfAL(ids: List<String>, genre: String) =
        songRepository.updateGenreOfAL(ids, genre)

    fun updatePickedLyrics(id: String) =
        viewModelScope.launch { lyricsRepository.updatePickedLyrics(id) }

    fun updateIndexListener(idx: Int) = lyricsRepository.updateIndex(idx)
    val lyricEditorIdx = lyricsRepository.indexListener.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = lyricsRepository.indexListener.value
    )
}

private data class UiStates(
    val isSearching: Boolean, val isSelecting: Boolean, val localAudiosLoading: Boolean,
    val autoUpdate: Boolean, val gettingPics: Boolean
)