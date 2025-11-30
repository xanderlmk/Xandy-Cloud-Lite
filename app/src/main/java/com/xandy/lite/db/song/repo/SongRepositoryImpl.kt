package com.xandy.lite.db.song.repo

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.media3.common.Tracks
import androidx.media3.session.MediaController
import com.xandy.lite.controllers.Functions
import com.xandy.lite.controllers.combineAllMediaItems
import com.xandy.lite.controllers.combineMCWithPickedSong
import com.xandy.lite.controllers.combineQueueMediaItems
import com.xandy.lite.controllers.media.store.ImportedAudioDetails
import com.xandy.lite.controllers.media.store.getAllImages
import com.xandy.lite.db.daos.AudioDao
import com.xandy.lite.db.daos.BucketDao
import com.xandy.lite.db.daos.PlaylistDao
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.db.tables.Playlist
import com.xandy.lite.db.tables.PlaylistSongOrder
import com.xandy.lite.models.AudioIds
import com.xandy.lite.models.itemKey
import com.xandy.lite.models.ui.LocalMusicTabs
import com.xandy.lite.models.ui.MediaItemWithCreatedOn
import com.xandy.lite.models.ui.order.by.OrderPlsBy
import com.xandy.lite.models.ui.order.by.OrderQueueBy
import com.xandy.lite.models.ui.order.by.OrderSongsBy
import com.xandy.lite.models.ui.toPlaylists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SongRepositoryImpl(
    unknownTrackUri: Uri, context: Context,
    appPref: SharedPreferences, private val audioDao: AudioDao,
    private val playlistDao: PlaylistDao, bucketDao: BucketDao,
    private val scope: CoroutineScope
) : SongRepository {

    override fun getMediaController() = mcStates.mediaController.value

    private val mcStates = MediaControllerStates(appPref, context)
    private val functions = Functions(audioDao, playlistDao, bucketDao, context)
    private val llStates =
        LocalLibraryStates(audioDao, playlistDao, bucketDao, mcStates, unknownTrackUri, context)

    override val pickedSong = mcStates.itemKey.flatMapLatest { key ->
        audioDao.getSongWithPls(key)
    }

    override val mediaController = mcStates.mediaController

    override val songDetails =
        combineMCWithPickedSong(mediaController, pickedSong, unknownTrackUri)

    override fun updatePickedSong(id: String) = mcStates.updateMediaItem(id)

    override val tracks = mcStates.tracks
    override val isPlaying = mcStates.isPlaying
    override val isLoading = mcStates.isLoading
    override val repeatMode = mcStates.repeatMode
    override val shuffleEnabled = mcStates.shuffleEnabled
    override val positionMs = mcStates.positionMs

    override val durationMs = mcStates.durationMs

    override fun updatePosition(position: Long) = mcStates.updatePosition(position)

    override fun updateDuration(duration: Long) = mcStates.updateDuration(duration)

    override fun updateTracks(tracks: Tracks) = mcStates.updateTracks(tracks)

    override fun updateIsPlaying(isPlaying: Boolean) = mcStates.updateIsPlaying(isPlaying)

    override fun updateIsLoading(isLoading: Boolean) = mcStates.updateIsLoading(isLoading)

    override fun updateMediaController(mc: MediaController) =
        mcStates.updateMediaController(mc)


    override fun resetMediaController() = mcStates.resetMediaController()

    override val audioOrderedBy = mcStates.audioOrderedBy
    override val localPlsOrderedBy = mcStates.localPlsOrderedBy
    override val localPlaylists = llStates.localPlaylists

    override val audioFiles = llStates.audioFiles
    override val hiddenAudio = audioDao.getFlowOfHiddenSongsByTitleASC()
    private val _localTab = MutableStateFlow(LocalMusicTabs.LIBRARY)
    override val localTab = _localTab.asStateFlow()
    override val pickedPlaylist = llStates.pickedPlaylist
    override val bucketsWithAudio = bucketDao.getFlowOfBucketsByNameASC()
    override val pickedLocalBucket = llStates.pickedLocalBucket
    override val pickedQueueName = mcStates.pickedQueueName

    private val _filesLoading = MutableStateFlow(false)
    override val filesLoading = _filesLoading.asStateFlow()
    private val _gettingAudioPics = MutableStateFlow(false)
    override val gettingAudioPics = _gettingAudioPics.asStateFlow()
    override val localAlbums = llStates.localAlbums
    override val pickedLocalAlbum = llStates.pickedLocalAlbum

    override val localArtists = llStates.localArtists
    override val pickedLocalArtist = llStates.pickedLocalArtist

    override val localGenres = llStates.localGenres
    override val pickedLocalGenre = llStates.pickedLocalGenre

    override val idWritingEnabled = llStates.songIdWritingEnabled

    override fun updateLocalALOrder(orderSongsBy: OrderSongsBy) =
        mcStates.updateLocalALOrder(orderSongsBy)

    override fun updateLocalPLOrder(orderPlsBy: OrderPlsBy) =
        mcStates.updateLocalPLOrder(orderPlsBy)

    override suspend fun getMediaFiles() =
        functions.getMediaFiles(
            onGettingPics = { getting -> _gettingAudioPics.update { getting } },
            onUpdate = { loading -> _filesLoading.update { loading } },
        )

    override suspend fun updateMediaFiles(iad: List<ImportedAudioDetails>) =
        functions.updateMediaFiles(
            onGettingPics = { getting -> _gettingAudioPics.update { getting } },
            onUpdate = { loading -> _filesLoading.update { loading } }, iad
        )

    override suspend fun updateSongIdTag(ids: List<AudioIds>) = functions.updateSongIdTag(ids)

    override fun updateLocalTab(tab: LocalMusicTabs) = _localTab.update { tab }

    override suspend fun addLocalPlaylist(name: String) = functions.addLocalPlaylist(name.trim())

    override suspend fun updateLocalPlUUID(n: String) = llStates.updateLocalPlUUID(n)
    override suspend fun updateLocalAlbumName(n: String) = llStates.updateLocalAlbumName(n)
    override suspend fun updateLocalArtistName(n: String) = llStates.updateLocalArtistName(n)
    override suspend fun updateLocalBucketKey(p: Pair<String, Long>) =
        llStates.updateLocalBucketKey(p)

    override suspend fun updateLocalGenreName(n: String) = llStates.updateLocalGenreName(n)

    override suspend fun toggleIdWriting(e: Boolean) = llStates.updateIdWritingEnabled(e)

    override suspend fun findPlaylistUUID(name: String) =
        functions.findPlaylistUUID(name, localPlaylists.toPlaylists().first().toSet()) { n ->
            updateLocalPlUUID(n)
        }

    override suspend fun addLocalSongsToPl(songIds: List<String>, playlistId: String) =
        functions.addLocalSongsToPl(songIds, playlistId)

    override suspend fun removeLocalSongsFromPl(songIds: List<String>, playlistId: String) =
        functions.removeLocalSongsFromPl(songIds, playlistId)

    override suspend fun hideBuckets(set: Set<Pair<String, Long>>) =
        functions.hideBuckets(set) { loading -> _filesLoading.update { loading } }

    override suspend fun deleteLocalAudios(list: List<String>) =
        functions.deleteAudioFiles(list, mediaController.value, unsortedQueue.first())

    override suspend fun hideAudioFiles(ids: List<String>) = functions.hideAudioFiles(ids)
    override suspend fun showAudioFiles(ids: List<String>) = functions.showAudioFiles(ids)
    override suspend fun hideAudioFile(uri: String) = functions.hideAudioFile(uri)
    override suspend fun showAudioFile(uri: String) = functions.showAudioFile(uri)

    private val audioUri = MutableStateFlow("")
    override fun updateAudioUri(uri: String) = audioUri.update { uri }

    override val pickedAudio = audioUri.flatMapLatest { uri ->
        if (uri.isEmpty()) flowOf(null)
        else audioDao.getFlowOfPickedAudio(uri)
    }

    override suspend fun updateAudioTags(newAudio: AudioFile, lyrics: Lyrics?) =
        functions.updateAudioTags(newAudio, mediaController.value, lyrics, unsortedQueue.first()) {
            updatePickedSong(it.id)
        }

    override suspend fun deleteLocalPlaylist(playlist: Playlist) =
        functions.deleteLocalPlaylist(playlist)


    override suspend fun addPlWithSongs(songIds: List<String>, name: String) =
        functions.addPlWithSongs(songIds, name)

    override fun checkPlaybackPosition() = mcStates.checkPlaybackPosition()

    override fun stopCheckingPlaybackPosition() = mcStates.stopCheckingPlaybackPosition()
    override fun updateLastestPlayerInfo() = mcStates.updateLastestPlayerInfo()

    /*
    *   <-- Queue stuff -->
    */
    private val allMediaItems = combineAllMediaItems(audioFiles, hiddenAudio)
        .flowOn(Dispatchers.IO.limitedParallelism(2, "All Media Items"))
    override val allMediaArtwork = allMediaItems.map { it.getAllImages() }
        .flowOn(Dispatchers.IO.limitedParallelism(1, "All Images"))

    override val queueOrder = mcStates.queueOrderedBy
    override val sortedQueue = combineQueueMediaItems(mcStates.queue, allMediaItems, queueOrder)
        .flowOn(Dispatchers.IO.limitedParallelism(2, "Sorted Queue"))
    override val unsortedQueue = combineQueueMediaItems(mcStates.queue, allMediaItems)
        .flowOn(Dispatchers.IO.limitedParallelism(2, "Unsorted Queue"))

    override suspend fun setNewQueue(list: List<MediaItemWithCreatedOn>, name: String) =
        mcStates.updateQueue(list.map { it.mediaItem.itemKey() }, name)

    override fun updateQueueOrder(orderQueueBy: OrderQueueBy) =
        mcStates.updateQueueOrder(orderQueueBy)

    override suspend fun addToQueue(newQueueIds: List<String>) = mcStates.updateQueue(newQueueIds)

    override suspend fun updatePlArtwork(name: String, newPic: Uri, currentPic: Uri?) =
        functions.updatePlArtwork(name, newPic, currentPic)

    override suspend fun updatePLSongOrder(order: PlaylistSongOrder) =
        playlistDao.updatePLOrder(order)

    override suspend fun changePlaylistName(newName: String, name: String, pickedPlUUID: String?) =
        functions.changePlaylistName(newName, name) {
            scope.launch {
                if (name == pickedPlUUID) updateLocalPlUUID(newName)
            }
        }

    override suspend fun updateArtistOfAL(ids: List<String>, artist: String) =
        functions.updateArtistOfAL(ids, artist)

    override suspend fun updateAlbumOfAL(ids: List<String>, album: String) =
        functions.updateAlbumOfAL(ids, album)

    override suspend fun updateGenreOfAL(ids: List<String>, genre: String) =
        functions.updateGenreOfAL(ids, genre)

    override fun autoUpdateEnabled() = llStates.autoUpdateEnabled()
    override fun toggleAutoUpdate(enabled: Boolean) = llStates.toggleAutoUpdate(enabled)
    override val autoUpdate = llStates.autoUpdate
}