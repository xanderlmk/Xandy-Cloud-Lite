package com.xandy.lite.controllers

import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.media3.session.MediaController
import com.xandy.lite.controllers.media.store.loadAudioFiles
import com.xandy.lite.controllers.media.store.loadAudioUris
import com.xandy.lite.controllers.media.store.loadBuckets
import com.xandy.lite.controllers.media.store.updateAlbum
import com.xandy.lite.controllers.media.store.updateArtist
import com.xandy.lite.controllers.media.store.updateArtwork
import com.xandy.lite.controllers.media.store.updateGenre
import com.xandy.lite.controllers.media.store.updateReleaseDate
import com.xandy.lite.controllers.media.store.updateSongId
import com.xandy.lite.controllers.media.store.updateTitle
import com.xandy.lite.db.AudioUri
import com.xandy.lite.db.daos.AudioDao
import com.xandy.lite.db.daos.BucketDao
import com.xandy.lite.db.daos.PlaylistDao
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.db.tables.Playlist
import com.xandy.lite.db.tables.datedString
import com.xandy.lite.db.tables.isNotInternal
import com.xandy.lite.db.tables.toAudioFile
import com.xandy.lite.db.tables.toBundle
import com.xandy.lite.models.AudioIds
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.application.dataStore
import com.xandy.lite.models.itemKey
import com.xandy.lite.models.ui.AudioUIState
import com.xandy.lite.models.ui.DeleteResult
import com.xandy.lite.models.ui.InsertResult
import com.xandy.lite.models.ui.UpdateResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.FileNotFoundException
import java.util.Date


class Functions(
    private val audioDao: AudioDao, private val playlistDao: PlaylistDao,
    private val bucketDao: BucketDao, private val context: Context,
) {

    companion object {
        private val ID_WRITE_ENABLE = booleanPreferencesKey("enabled_id_writing")

    }

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
                Log.i(XANDY_CLOUD, "$songIds")
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

    suspend fun findPlaylistUUID(
        uuid: String, pls: Set<Playlist>, onUpdate: suspend (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val pl = pls.find { it.id == uuid } ?: return@withContext ""
        onUpdate(pl.id)
        return@withContext pl.id
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
    ) = withContext(Dispatchers.IO.limitedParallelism(2, "Update media files")) {
        return@withContext try {
            /** A minute to load all media */
            withTimeout(60_000L) {
                val audios = mutableListOf<Pair<AudioFile, Boolean>>()
                try {
                    onUpdate(true)
                    Log.i("Xandy-Cloud", "Getting files")
                    val buckets = loadBuckets(context)
                    bucketDao.upsertBuckets(buckets)
                    loadAudioFiles(
                        context, 50, audioDao = audioDao, onProgress = { onProgress(it) }
                    ).collect { flow ->
                        val flowAudios = flow.map { it.first }
                        try {
                            audioDao.upsertAudios(flowAudios)
                            audios += flow
                        } catch (e: Exception) {
                            Log.e(
                                XANDY_CLOUD,
                                "Failed to upsert ${flow.size} audio files to DB: ${e.printStackTrace()}"
                            )
                        }
                    }
                    Log.i(XANDY_CLOUD, "Successfully updated list.")
                } catch (e: Exception) {
                    Log.e(XANDY_CLOUD, "Failed to upsert audio files to DB: ${e.printStackTrace()}")
                } finally {
                    val newListIds = audios.map { it.first.uri }.toSet()
                    val originalList = audioDao.getAudioFiles()
                    val audioToDelete =
                        originalList.filter { original -> original.uri !in newListIds }
                    try {
                        audioDao.deleteAudioFiles(audioToDelete)
                    } catch (e: Exception) {
                        Log.e(XANDY_CLOUD, "Failed to delete audio files from DB: $e")
                    }
                }
                val writeEnabled = context.dataStore.data.map { preferences ->
                    preferences[ID_WRITE_ENABLE] ?: false
                }.first()
                if (!writeEnabled) return@withTimeout null
                /**
                 *  If the audio doesn't have a uuid already set in the metadata,
                 *  ask to insert one in the metadata.
                 *
                 *  If it's an internal media item from the device itself, we can't edit it.
                 */
                val newAudioIds =
                    audios.filter {
                        !it.second &&
                                !it.first.isNotInternal()
                    }.map { AudioIds(it.first.id, it.first.uri) }
                val uris = newAudioIds.map { it.uri }
                Log.i(XANDY_CLOUD, "$uris")
                return@withTimeout if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && newAudioIds.isNotEmpty())
                    Pair(MediaStore.createWriteRequest(context.contentResolver, uris), newAudioIds)
                else null
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(XANDY_CLOUD, "Time exceeded: $e")
            return@withContext null
        } finally {
            onUpdate(false)
            onProgress(0)
        }
    }

    suspend fun hideAudioFiles(uris: List<String>) = withContext(Dispatchers.IO) {
        try {
            audioDao.hideAudioFiles(uris)
            true
        } catch (e: Exception) {
            Log.e(XANDY_CLOUD, "Failed to hide audio files: $e")
            false
        }
    }

    suspend fun showAudioFiles(uris: List<String>) = withContext(Dispatchers.IO) {
        try {
            audioDao.showAudioFiles(uris)
            true
        } catch (e: Exception) {
            Log.e(XANDY_CLOUD, "Failed to show audio files: $e")
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
        newAudio: AudioFile, mediaController: MediaController?, lyrics: Lyrics?,
        onUpdateSong: (AudioFile) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val title = newAudio.title.trim()
            val artist = newAudio.artist.trim()
            val genre = newAudio.genre?.trim()
            val album = newAudio.album?.trim()
            val date = newAudio.datedString()
            val result1 = updateTitle(context, newAudio.uri, title)
            val result2 = updateArtist(context, newAudio.uri, artist)
            val result3 = genre?.let { updateGenre(context, newAudio.uri, it) } ?: true
            val result4 = album?.let { updateAlbum(context, newAudio.uri, it) } ?: true
            val (result5, newArtwork) = updateArtwork(context, newAudio.uri, newAudio.picture)
            val result6 = date?.let { updateReleaseDate(context, newAudio.uri, it) } ?: true
            if (!result1 || !result2 || !result3 || !result4 || !result5 || !result6)
                return@withContext UpdateResult.Failure
            audioDao.updateAudioTagsAndLyrics(
                title = title, artist = artist, genre = genre, album = album,
                year = newAudio.year, day = newAudio.day, month = newAudio.month,
                pictureUri = newArtwork, uri = newAudio.uri.toString(), lyrics = lyrics
            )
            withContext(Dispatchers.Main) {
                val controller = mediaController ?: return@withContext
                val current = controller.currentMediaItem ?: return@withContext
                val updated = newAudio.copy(
                    title = title, artist = artist, genre = genre, album = album,
                    picture = newArtwork
                )
                if (current.itemKey() == updated.id) {
                    try {
                        val newMetadata = current.mediaMetadata.buildUpon()
                            .setTitle(updated.title)
                            .setArtist(updated.artist)
                            .setGenre(updated.genre)
                            .setArtworkUri(updated.picture)
                            .setReleaseYear(updated.year)
                            .setReleaseMonth(updated.month)
                            .setReleaseDay(updated.day)
                            .setExtras(updated.toBundle())
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
                            XANDY_CLOUD, "On update current media item: ${e.printStackTrace()}"
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

    suspend fun deleteAudioFiles(list: List<String>, mediaController: MediaController?) =
        withContext(Dispatchers.IO) {
            val uriList = idsToUris(list)
            try {
                val deleted = mutableListOf<Uri>()
                val failed = mutableListOf<Uri>()
                try {
                    withContext(Dispatchers.Main) {
                        mediaController?.let { mc ->
                            val current = runCatching { mc.currentMediaItem }.getOrNull()
                            val currentId = current?.itemKey()
                            val idSet = list.toSet()
                            if (currentId != null && currentId in idSet) {
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
                for (uri in uriList) {
                    try {
                        val rowsDeleted = try {
                            context.contentResolver.delete(uri, null, null)
                        } catch (e: Exception) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                                (e is RecoverableSecurityException || e is FileNotFoundException)
                            ) {
                                val editRequest =
                                    MediaStore.createWriteRequest(context.contentResolver, uriList)
                                return@withContext DeleteResult.FileException(editRequest)
                            }
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    (e is RecoverableSecurityException || e is FileNotFoundException)
                ) {
                    val editRequest =
                        MediaStore.createWriteRequest(context.contentResolver, uriList)
                    return@withContext DeleteResult.FileException(editRequest)
                }
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
                if (result > 0) return@withContext Pair(InsertResult.Exists, "")
                val new = Playlist(name = name, createdOn = Date(), picture = null)
                playlistDao.addPlWithSongs(songIds, new)
                return@withContext Pair(InsertResult.Success, new.id)
            } catch (_: Exception) {
                return@withContext Pair(InsertResult.Failure, "")
            }
        }

    suspend fun changePlaylistName(newName: String, name: String, onUpdate: () -> Unit) =
        withContext(Dispatchers.IO) {
            try {
                val result = playlistDao.checkIfPlExists(newName)
                if (result > 0) return@withContext InsertResult.Exists
                playlistDao.updatePlaylistName(newName, name)
                onUpdate()
                return@withContext InsertResult.Success
            } catch (_: Exception) {
                return@withContext InsertResult.Failure
            }
        }

    suspend fun updateArtistOfAL(ids: List<String>, artist: String) =
        withContext(Dispatchers.IO) {
            val uris = idsToUris(ids)
            try {
                uris.forEachIndexed { index, it ->
                    val result = updateArtist(context, it, artist)
                    if (!result) Toast.makeText(
                        context,
                        "Failed to update artist of song at ${index + 1}", Toast.LENGTH_SHORT
                    ).show()
                }
                audioDao.updateAudioListArtist(uris, artist)
                return@withContext UpdateResult.Success
            } catch (e: Exception) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    (e is RecoverableSecurityException || e is FileNotFoundException)
                ) {
                    val editRequest = MediaStore.createWriteRequest(context.contentResolver, uris)
                    return@withContext UpdateResult.FileException(editRequest)
                }
                return@withContext UpdateResult.Failure
            }
        }

    suspend fun updateAlbumOfAL(ids: List<String>, album: String) =
        withContext(Dispatchers.IO) {
            val uris = idsToUris(ids)
            try {
                uris.forEachIndexed { index, it ->
                    val result = updateAlbum(context, it, album)
                    if (!result) Toast.makeText(
                        context,
                        "Failed to update album of song at ${index + 1}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                audioDao.updateAudioListAlbum(uris, album)
                return@withContext UpdateResult.Success
            } catch (e: Exception) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    (e is RecoverableSecurityException || e is FileNotFoundException)
                ) {
                    val editRequest = MediaStore.createWriteRequest(context.contentResolver, uris)
                    return@withContext UpdateResult.FileException(editRequest)
                }
                return@withContext UpdateResult.Failure
            }
        }

    suspend fun updateGenreOfAL(ids: List<String>, genre: String) =
        withContext(Dispatchers.IO) {
            val uris = idsToUris(ids)
            try {
                uris.forEachIndexed { index, it ->
                    val result = updateGenre(context, it, genre)
                    if (!result) Toast.makeText(
                        context,
                        "Failed to update genre of song at ${index + 1}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                audioDao.updateAudioListGenre(uris, genre)
                return@withContext UpdateResult.Success
            } catch (e: Exception) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    (e is RecoverableSecurityException || e is FileNotFoundException)
                ) {
                    val editRequest = MediaStore.createWriteRequest(context.contentResolver, uris)
                    return@withContext UpdateResult.FileException(editRequest)
                }
                return@withContext UpdateResult.Failure
            }
        }

    suspend fun updateSongIdTag(ids: List<AudioIds>) = withContext(Dispatchers.IO) {
        try {
            ids.forEach {
                try {
                    updateSongId(context, it.uri, it.id)
                } catch (e: Exception) {
                    Log.w(XANDY_CLOUD, "Failed to update audio ${it.uri}: ${e.printStackTrace()}")
                }
            }
            return@withContext true
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed to update audios: ${e.printStackTrace()}")
            return@withContext false
        }
    }

    suspend fun updateSongLyrics(lyricsId: String, songUri: String) = withContext(Dispatchers.IO) {
        try {
            audioDao.updateLyricsOfSong(lyricsId, songUri = songUri)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun updateLyrics(lyrics: Lyrics) = withContext(Dispatchers.IO) {
        try {
            audioDao.updateLyrics(lyrics)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteLyrics(lyrics: Lyrics) = withContext(Dispatchers.IO) {
        try {
            audioDao.deleteLyrics(lyrics)
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun idsToUris(ids: List<String>) = audioDao.getAudioUris(ids).map { it.uri }
}