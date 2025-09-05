package com.xandy.lite.controllers

import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.media3.session.MediaController
import com.xandy.lite.controllers.media.store.loadAudioFiles
import com.xandy.lite.controllers.media.store.loadAudioUris
import com.xandy.lite.controllers.media.store.loadBuckets
import com.xandy.lite.controllers.media.store.updateAlbum
import com.xandy.lite.controllers.media.store.updateArtist
import com.xandy.lite.controllers.media.store.updateArtwork
import com.xandy.lite.controllers.media.store.updateGenre
import com.xandy.lite.controllers.media.store.updateTitle
import com.xandy.lite.db.daos.AudioDao
import com.xandy.lite.db.daos.BucketDao
import com.xandy.lite.db.daos.PlaylistDao
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.Playlist
import com.xandy.lite.db.tables.toAudioFile
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.ui.AudioUIState
import com.xandy.lite.models.ui.AudioUri
import com.xandy.lite.models.ui.DeleteResult
import com.xandy.lite.models.ui.InsertResult
import com.xandy.lite.models.ui.UpdateResult
import com.xandy.lite.models.ui.itemKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.FileNotFoundException
import java.util.Date


class Functions(
    private val audioDao: AudioDao, private val playlistDao: PlaylistDao,
    private val bucketDao: BucketDao, private val context: Context,
) {


    suspend fun updatePlArtwork(name: String, newPic: Uri, currentPic: Uri?) =
        withContext(Dispatchers.IO) {
            try {
                val newCopiedPic = copyFileToInternalStorage(context, newPic)
                playlistDao.updatePlArtwork(name, newCopiedPic)
                currentPic?.deleteLocalFile()
                return@withContext
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to update artwork.", Toast.LENGTH_SHORT).show()
                Log.e(XANDY_CLOUD, "${e.printStackTrace()}")
                return@withContext
            }
        }

    suspend fun removeLocalSongsFromPl(songIds: List<String>, playlistId: String) =
        withContext(Dispatchers.IO) {
            try {
                audioDao.removeSongsFromPL(songIds, playlistId)
                true
            } catch (_: Exception) {
                false
            }
        }

    suspend fun addLocalSongsToPl(songIds: List<String>, playlistId: String) =
        withContext(Dispatchers.IO) {
            try {
                playlistDao.addSongsToPl(songIds, playlistId)
                true
            } catch (_: Exception) {
                false
            }
        }

    suspend fun addLocalPlaylist(name: String) = withContext(Dispatchers.IO) {
        try {
            val result = playlistDao.checkIfPlExists(name)
            if (result > 0) return@withContext InsertResult.Exists
            val new = Playlist(name = name, createdOn = Date(), picture = null)
            playlistDao.insertPL(new)
            InsertResult.Success
        } catch (_: Exception) {
            InsertResult.Failure
        }
    }

    suspend fun findPlaylistIdx(
        name: String, pls: Set<Playlist>, onUpdate: suspend (Int) -> Unit
    ) =
        withContext(Dispatchers.IO) {
            val pl = pls.find { it.name == name } ?: return@withContext -1
            val index = pls.indexOf(pl).takeIf { it >= 0 } ?: return@withContext -1
            onUpdate(index)
            return@withContext index
        }

    suspend fun hideBuckets(
        set: Set<Pair<String, Long>>, onUpdate: (Boolean) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            onUpdate(true)
            bucketDao.hideSelectedBuckets(set)
            Log.i(XANDY_CLOUD, "hidden buckets: $set")
            val shownUris = loadAudioUris(context, set)
            Log.i(XANDY_CLOUD, "Shown Uris count: ${shownUris.size}")
            audioDao.updateShownAudios(shownUris.map { it.toString() })
            true
        } catch (e: Exception) {
            Log.e(XANDY_CLOUD, "Failed to hide buckets to DB: $e")
            false
        } finally {
            onUpdate(false)
        }
    }

    suspend fun updateMediaFiles(
        onProgress: (Int) -> Unit, onUpdate: (Boolean) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            /** A minute to load all media */
            withTimeout(60_000L) {
                val audios = mutableListOf<AudioFile>()
                try {
                    onUpdate(true)
                    Log.i("Xandy-Cloud", "Getting files")
                    val buckets = loadBuckets(context)
                    bucketDao.upsertBuckets(buckets)
                    loadAudioFiles(
                        context, 50, onProgress = { onProgress(it) }).collect { flow ->
                        audios += flow
                        audioDao.upsertAudios(flow)
                    }
                    Log.i(XANDY_CLOUD, "Successfully updated list.")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(XANDY_CLOUD, "Failed to upsert audio files to DB: $e")
                } finally {
                    val newListIds = audios.map { it.uri }.toSet()
                    val originalList = audioDao.getAudioFiles()
                    val audioToDelete =
                        originalList.filter { original -> original.uri !in newListIds }
                    try {
                        audioDao.deleteAudioFiles(audioToDelete)
                    } catch (e: Exception) {
                        Log.e(XANDY_CLOUD, "Failed to delete audio files from DB: $e")
                    }
                    onUpdate(false)
                    onProgress(0)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(XANDY_CLOUD, "Time exceeded: $e")
        }
        return@withContext
    }

    suspend fun hideAudioFiles(uris: List<String>) = withContext(Dispatchers.IO) {
        try {
            audioDao.hideAudioFiles(uris)
            true
        } catch (e: Exception) {
            Log.e("Xandy-Cloud", "Failed to hide audio files: $e")
            false
        }
    }

    suspend fun hideAudioFile(uri: String) = withContext(Dispatchers.IO) {
        try {
            audioDao.hideAudioFile(uri)
            true
        } catch (e: Exception) {
            Log.e("Xandy-Cloud", "Failed to hide audio file: $e")
            false
        }
    }

    suspend fun showAudioFile(uri: String) = withContext(Dispatchers.IO) {
        try {
            audioDao.showAudioFile(uri)
            true
        } catch (e: Exception) {
            Log.e("Xandy-Cloud", "Failed to show audio file: $e")
            false
        }
    }

    suspend fun updateAudioTags(
        newAudio: AudioFile, mediaController: MediaController?,
        onUpdateSong: (AudioFile) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val title = newAudio.title.trim()
            val artist = newAudio.artist.trim()
            val genre = newAudio.genre?.trim()
            val album = newAudio.album?.trim()
            val result1 = updateTitle(context, newAudio.uri, title)
            val result2 = updateArtist(context, newAudio.uri, artist)
            val result3 = genre?.let { updateGenre(context, newAudio.uri, it) } ?: true
            val result4 = album?.let { updateAlbum(context, newAudio.uri, it) } ?: true
            val (result5, newArtwork) = updateArtwork(context, newAudio.uri, newAudio.picture)
            if (!result1 || !result2 || !result3 || !result4 || !result5)
                return@withContext UpdateResult.Failure
            audioDao.updateAudioTags(
                title = title, artist = artist, genre = genre, album = album,
                pictureUri = newArtwork, uri = newAudio.uri.toString()
            )
            withContext(Dispatchers.Main) {
                val controller = mediaController ?: return@withContext
                val current = controller.currentMediaItem ?: return@withContext
                val updated = newAudio.copy(
                    title = title, artist = artist, genre = genre, album = album,
                    picture = newArtwork
                )
                if (current.itemKey() == updated.uri.toString()) {
                    try {
                        val newMetadata = current.mediaMetadata.buildUpon()
                            .setTitle(updated.title)
                            .setArtist(updated.artist)
                            .setGenre(updated.genre)
                            .setArtworkUri(updated.picture)
                            .build()
                        val updatedItem = current.buildUpon()
                            .setMediaMetadata(newMetadata)
                            .build()
                        controller.replaceMediaItem(
                            controller.currentMediaItemIndex, updatedItem
                        )
                        onUpdateSong(updated)
                    } catch (e: Exception) {
                        Log.w(
                            XANDY_CLOUD,
                            "On update current media item: ${e.stackTraceToString()}"
                        )
                    }
                }
            }
            return@withContext UpdateResult.Success
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException)
                return@withContext UpdateResult.SecurityException(e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && e is FileNotFoundException) {
                val uris = listOf(newAudio.uri) // or multiple URIs
                val editRequest = MediaStore.createWriteRequest(context.contentResolver, uris)
                return@withContext UpdateResult.FileException(editRequest)
            }
            Log.e(XANDY_CLOUD, "Failed to update audio: $e")
            return@withContext UpdateResult.Failure
        }
    }

    suspend fun deleteAudioFiles(list: List<Uri>, mediaController: MediaController?) =
        withContext(Dispatchers.IO) {
            try {
                val deleted = mutableListOf<Uri>()
                val failed = mutableListOf<Uri>()
                try {
                    withContext(Dispatchers.Main) {
                        mediaController?.let { mc ->
                            val current = runCatching { mc.currentMediaItem }.getOrNull()
                            val currentUri = current?.itemKey()
                            val uriSet = list.toSet().map { it.toString() }
                            if (currentUri != null && currentUri in uriSet) {
                                runCatching { mc.pause() }
                                runCatching {
                                    val idx = mc.currentMediaItemIndex
                                    if (idx >= 0) mc.removeMediaItem(idx)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(
                        XANDY_CLOUD,
                        "Failed to remove picked song from controller: ${e.printStackTrace()}"
                    )
                }
                for (uri in list) {
                    try {
                        val rowsDeleted = try {
                            context.contentResolver.delete(uri, null, null)
                        } catch (e: Exception) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException)
                                return@withContext DeleteResult.SecurityException(e)
                            return@withContext DeleteResult.Failure
                        }
                        if (rowsDeleted > 0) {
                            audioDao.deleteAudioByUri(AudioUri(uri))
                            deleted += uri
                        } else {
                            failed += uri
                        }
                    } catch (e: Exception) {
                        Log.w(XANDY_CLOUD, "Failed to delete $uri: ${e.printStackTrace()}")
                        failed += uri
                    }
                }
                return@withContext when {
                    failed.isEmpty() -> DeleteResult.Success
                    deleted.isEmpty() -> DeleteResult.Failure
                    else -> DeleteResult.Partial(deleted = deleted, failed = failed)
                }
            } catch (e: Exception) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException)
                    return@withContext DeleteResult.SecurityException(e)
                Log.e(XANDY_CLOUD, "Failed to delete audios: ${e.printStackTrace()}")
                return@withContext DeleteResult.Failure
            }
        }

    suspend fun deleteLocalPlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        try {
            playlist.picture?.deleteLocalFile()
            playlistDao.deletePlaylist(playlist)
            true
        } catch (e: Exception) {
            Log.e(XANDY_CLOUD, "Failed to delete playlist $e")
            false
        }
    }

    suspend fun updatePickedSong(
        songId: String, delay: Long, unknownTrackUri: Uri,
        audioFiles: Flow<AudioUIState>, mediaController: MediaController?,
        onUpdate: (AudioFile?) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            delay(delay)
            val listOfAL = audioFiles.first().list
            val item = try {
                mediaController?.currentMediaItem
            } catch (_: Exception) {
                null
            }
            val song = listOfAL.find { it.song.uri.toString() == songId }?.song
                ?: item?.toAudioFile(unknownTrackUri)
            if (song == null) Log.w(XANDY_CLOUD, "NULL song.")
            onUpdate(song)
            return@withContext
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed to find track/song: $e")
            return@withContext
        }
    }

    suspend fun addPlWithSongs(songIds: List<String>, name: String) =
        withContext(Dispatchers.IO) {
            try {
                val result = playlistDao.checkIfPlExists(name)
                if (result > 0) return@withContext InsertResult.Exists
                val new = Playlist(name = name, createdOn = Date(), picture = null)
                playlistDao.addPlWithSongs(songIds, new)
                return@withContext InsertResult.Success
            } catch (_: Exception) {
                return@withContext InsertResult.Failure
            }
        }

    suspend fun changePlaylistName(newName: String, name: String) =
        withContext(Dispatchers.IO) {
            try {
                val result = playlistDao.checkIfPlExists(newName)
                if (result > 0) return@withContext InsertResult.Exists
                playlistDao.updatePlaylistName(newName, name)
                return@withContext InsertResult.Success
            } catch (_: Exception) {
                return@withContext InsertResult.Failure
            }
        }
}