package com.xandy.lite.db.song.repo

import android.app.PendingIntent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.BucketWithAudio
import com.xandy.lite.db.tables.AudioWithPls
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.db.tables.Playlist
import com.xandy.lite.db.tables.PlaylistSongOrder
import com.xandy.lite.models.AudioIds
import com.xandy.lite.models.application.AppValues
import com.xandy.lite.models.ui.Album
import com.xandy.lite.models.ui.Artist
import com.xandy.lite.models.ui.AudioUIState
import com.xandy.lite.models.ui.DeleteResult
import com.xandy.lite.models.ui.Genre
import com.xandy.lite.models.ui.InsertResult
import com.xandy.lite.models.ui.LocalMusicTabs
import com.xandy.lite.models.ui.LocalPlUIState
import com.xandy.lite.models.ui.MediaState
import com.xandy.lite.models.ui.PlaylistWithCount
import com.xandy.lite.models.ui.SongDetails
import com.xandy.lite.models.ui.UpdateResult
import com.xandy.lite.models.ui.order.by.OrderAlbumsBy
import com.xandy.lite.models.ui.order.by.OrderArtistBy
import com.xandy.lite.models.ui.order.by.OrderGenresBy
import com.xandy.lite.models.ui.order.by.OrderPlsBy
import com.xandy.lite.models.ui.order.by.OrderQueueBy
import com.xandy.lite.models.ui.order.by.OrderSongsBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SongRepository {
    val scope: CoroutineScope
    val appValues: StateFlow<AppValues>
    val pickedSong: Flow<AudioWithPls?>

    /* <-- Player related stuff --> */
    val mediaController: StateFlow<MediaController?>
    val isPlaying: StateFlow<Boolean>
    val isLoading: StateFlow<Boolean>
    val repeatMode: Flow<Int>
    val shuffleEnabled: Flow<Boolean>
    val durationMs: StateFlow<Long>
    fun handleSkipNext(shuffleEnabled: Boolean, repeatMode: Int, mc: MediaController)

    /** Song position in milliseconds */
    val positionMs: StateFlow<Long>

    /** Sorted Queue based on [OrderQueueBy] */
    val sortedQueue: Flow<List<MediaItem>>

    /**
     * An unsorted Queue to properly play the correct song upon picking a song within the queue
     * when it is sorted in a different manner
     */
    val unsortedQueue: Flow<List<MediaItem>>
    fun updatePosition(position: Long)
    fun updateDuration(duration: Long)
    fun updateMediaController(mc: MediaController)
    fun resetMediaController()
    fun getMediaController(): MediaController?
    fun updateIsPlaying(isPlaying: Boolean)
    fun updateIsLoading(isLoading: Boolean)

    /** Start Checking the position of the song */
    fun checkPlaybackPosition(): Boolean

    /** Stop Checking the position of the song */
    fun stopCheckingPlaybackPosition()
    fun updateLastestPlayerInfo()
    /* <-- End of Player related stuff --> */

    /** Song Details of the current media item or picked song */
    val songDetails: Flow<SongDetails?>

    fun updatePickedSong(id: String)

    /* <-- Local audio files --> */
    val audioOrderedBy: StateFlow<OrderSongsBy>
    val hiddenOrderedBy: StateFlow<OrderSongsBy>
    val favOrderedBy: StateFlow<OrderSongsBy>
    val localPlsOrderedBy: StateFlow<OrderPlsBy>
    val albumOrderedBy: StateFlow<OrderAlbumsBy>
    val artistOrderedBy: StateFlow<OrderArtistBy>
    val genreOrderedBy: StateFlow<OrderGenresBy>
    val pickedQueueName: Flow<String>

    /** Update the order of the local audio list */
    fun updateLocalALOrder(orderSongsBy: OrderSongsBy)

    /** Update the order of the hidden audio list */
    fun updateHiddenOrder(orderSongsBy: OrderSongsBy)

    fun updateFavoriteOrder(orderSongsBy: OrderSongsBy)

    /** Update the order of the local playlist */
    fun updateLocalPLOrder(orderPlsBy: OrderPlsBy)

    /** Update the order of the albums */
    fun updateAlbumOrder(orderAlbumsBy: OrderAlbumsBy)

    /** Update the order of the artists */
    fun updateArtistOrder(orderArtistBy: OrderArtistBy)

    fun updateGenreOrder(orderGenresBy: OrderGenresBy)

    val audioFiles: Flow<AudioUIState>
    val hiddenAudios: Flow<List<AudioFile>>
    /** A list of the favorite tracks */
    val favorites: Flow<List<AudioFile>>

    /** Local playlists with their songs, and the count of songs it has */
    val localPlaylists: Flow<LocalPlUIState>
    val filesLoading: StateFlow<Boolean>
    val gettingAudioPics: StateFlow<Boolean>

    suspend fun updateMediaFiles(): Pair<PendingIntent, List<AudioIds>>?

    suspend fun updateSongIdTag(ids: List<AudioIds>): Boolean

    /** Update local playlist artwork */
    suspend fun updatePlArtwork(name: String, newPic: Uri, currentPic: Uri?)
    val localTab: StateFlow<LocalMusicTabs>
    fun updateLocalTab(tab: LocalMusicTabs)
    suspend fun addLocalPlaylist(name: String): InsertResult
    val pickedPlaylist: Flow<PlaylistWithCount?>

    /** Update local playlist name to get the picked playlist */
    suspend fun updateLocalPlUUID(n: String)
    suspend fun updateLocalAlbumName(n: MediaState)
    suspend fun updateLocalArtistName(n: MediaState)
    suspend fun updateLocalGenreName(n: MediaState)
    suspend fun updateLocalBucketKey(p: Pair<String, Long>)

    suspend fun findPlaylistUUID(name: String): String
    suspend fun addLocalSongsToPl(songIds: List<String>, playlistId: String): Boolean
    suspend fun removeLocalSongsFromPl(songIds: List<String>, playlistId: String): Boolean
    val bucketsWithAudio: Flow<List<BucketWithAudio>>
    val pickedLocalBucket: Flow<BucketWithAudio?>
    suspend fun hideBuckets(set: Set<Pair<String, Long>>): Boolean
    val localAlbums: Flow<List<Album>>
    val pickedLocalAlbum: Flow<Album?>
    val localArtists: Flow<List<Artist>>
    val pickedLocalArtist: Flow<Artist?>

    val localGenres: Flow<List<Genre>>
    val pickedLocalGenre: Flow<Genre?>

    fun getAfsByIds(ids: List<String>): Flow<List<AudioFile>>

    /** Delete the audio file from the system and the db */
    suspend fun deleteLocalAudios(list: List<String>): DeleteResult

    /** Permanently hide selected audio files. */
    suspend fun hideAudioFiles(ids: List<String>): Boolean

    /** Show the hidden audio files */
    suspend fun showAudioFiles(ids: List<String>): Boolean

    /** Permanently hide the selected audio file */
    suspend fun hideAudioFile(uri: String): Boolean

    /** Show the hidden file */
    suspend fun showAudioFile(uri: String): Boolean

    /** Add to favorites */
    suspend fun addToFavorites(uri: Uri): InsertResult

    suspend fun removeFromFavorites(uri: Uri): Boolean

    suspend fun updateAudioTags(newAudio: AudioFile, lyrics: Lyrics?): UpdateResult

    /** Update picked audio uri */
    fun updateAudioUri(uri: String)

    /** Maps the lastest value of the audioUri */
    val pickedAudioToEdit: Flow<AudioWithPls?>
    suspend fun deleteLocalPlaylist(playlist: Playlist): Boolean
    suspend fun addPlWithSongs(songIds: List<String>, name: String): Pair<InsertResult, String>
    suspend fun updatePLSongOrder(order: PlaylistSongOrder)
    val allMediaArtwork: Flow<List<Uri>>
    val queueOrder: StateFlow<OrderQueueBy>
    val priorityList: StateFlow<List<AudioFile>>
    suspend fun setNewQueue(list: List<MediaItem>, name: String)
    suspend fun updateQueue(newQueueIds: List<String>)
    fun updateQueueOrder(orderQueueBy: OrderQueueBy)
    fun addItemToPriorityQueue(af: AudioFile): InsertResult

    fun getCurrentPriorityItem(): Pair<String, Int>
    fun clearPriorityItemStates()

    fun addItemsToPriorityQueue(afs: List<AudioFile>): InsertResult

    suspend fun changePlaylistName(
        newName: String, name: String, pickedPlUUID: String?
    ): InsertResult

    fun autoUpdateEnabled(): Boolean
    fun toggleAutoUpdate(enabled: Boolean)
    val autoUpdate: StateFlow<Boolean>

    /** Update artist of the selected song/audio list */
    suspend fun updateArtistOfAL(ids: List<String>, artist: String): UpdateResult

    /** Update album of the selected song/audio list */
    suspend fun updateAlbumOfAL(ids: List<String>, album: String): UpdateResult

    /** Update genre of the selected song/audio list */
    suspend fun updateGenreOfAL(ids: List<String>, genre: String): UpdateResult

    val idWritingEnabled: Flow<Boolean>
    suspend fun toggleIdWriting(e: Boolean)
}