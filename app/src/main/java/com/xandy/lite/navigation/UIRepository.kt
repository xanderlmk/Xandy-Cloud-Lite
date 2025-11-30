package com.xandy.lite.navigation

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.Bucket
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.application.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

interface UIRepository {
    val isSelecting: StateFlow<Boolean>
    val selectedSongIds: StateFlow<List<String>>
    val isAdding: StateFlow<Boolean>
    val query: StateFlow<String>
    val recentQueries: Flow<Set<String>>
    val isSearching: StateFlow<Boolean>
    fun updateQuery(new: String)
    fun startAdding()
    fun startSelectingSongs(songId: String)

    /** End the selection, clear the list, and stop adding (if adding) */
    fun endSelect()

    fun toggleSong(songId: String): Boolean
    fun selectAll(list: List<String>)

    fun turnOnSearch()
    fun turnOffSearch()
    fun resetSearch()

    val selectedBuckets: StateFlow<Set<Pair<String, Long>>>
    fun startSelectingFolders(folders: List<Bucket>, selected: Pair<String, Long>)
    fun toggleBucket(id: Pair<String, Long>)
    suspend fun addQuery(str: String)
}

class UIRepositoryImpl(private val context: Context) : UIRepository {
    companion object {
        private val RECENT = stringSetPreferencesKey("recent_queries")
    }

    private val _isSelecting = MutableStateFlow(false)
    override val isSelecting = _isSelecting.asStateFlow()
    private val _selectedSongIds = MutableStateFlow(emptyList<String>())
    override val selectedSongIds = _selectedSongIds.asStateFlow()
    private val _isAdding = MutableStateFlow(false)
    override val isAdding = _isAdding.asStateFlow()
    private val _query = MutableStateFlow("")
    override val query = _query.asStateFlow()
    private val _isSearching = MutableStateFlow(false)
    override val isSearching = _isSearching.asStateFlow()
    override val recentQueries = context.dataStore.data.map { preferences ->
        preferences[RECENT] ?: emptySet()
    }


    override fun updateQuery(new: String) = _query.update { new }


    override fun startAdding() {
        _isSelecting.update { true }; _isAdding.update { true }
        _selectedSongIds.update {emptyList() }
    }

    override fun startSelectingSongs(songId: String) {
        _isSelecting.update { true }; toggleSong(songId)
    }

    override fun endSelect() {
        _isSelecting.update { false }; _selectedSongIds.update { emptyList() }
        _isAdding.update { false }
    }

    /**
     * If the limit has been reached, the user won't be allowed to add more songs.
     * Returns true if limit was reached
     */
    override fun toggleSong(songId: String): Boolean {
        var limitReached = false
        _selectedSongIds.update { ids ->
            val mutableIds = ids.toMutableList()
            if (!mutableIds.contains(songId)) {
                if ((mutableIds.size + 1) > 2_000)
                    limitReached = true
                else mutableIds.add(songId)
            } else mutableIds.remove(songId)
            mutableIds
        }
        return limitReached
    }

    override fun selectAll(list: List<String>) = _selectedSongIds.update { list }

    override fun turnOnSearch() = _isSearching.update { true }
    override fun turnOffSearch() = _isSearching.update { false }
    override fun resetSearch() {
        turnOffSearch(); _query.update { "" }
    }

    private val _selectedBuckets = MutableStateFlow<Set<Pair<String, Long>>>(emptySet())
    override val selectedBuckets = _selectedBuckets.asStateFlow()

    override fun startSelectingFolders(folders: List<Bucket>, selected: Pair<String, Long>) {
        _isSelecting.update { true }
        _selectedBuckets.update {
            val hidden =
                folders.filter { it.hidden }.map { Pair(it.volumeName, it.id) }.toSet()
            if (selected !in hidden) hidden + selected
            else hidden
        }
    }

    override fun toggleBucket(id: Pair<String, Long>) = _selectedBuckets.update { current ->
        if (id in current) current - id else current + id
    }

    override suspend fun addQuery(str: String) = withContext(Dispatchers.IO) {
        try {
            val s = str.trim().lowercase()
            val set = recentQueries.first().map { it.trim().lowercase() }
            val new = when {
                set.isEmpty() -> setOf(s)
                s in set -> {
                    val mutable = set.toMutableList()
                    val index = mutable.indexOf(s).takeIf { it >= 0 } ?: return@withContext
                    val temp = mutable[0]
                    mutable[0] = mutable[index]
                    mutable[index] = temp
                    mutable.toSet()
                }

                else -> {
                    val reversed = set.reversed().toMutableList()
                    if (reversed.size >= 5) {
                        reversed.add(s)
                        reversed.removeAt(0)
                    } else reversed.add(s)
                    reversed.reversed().toSet()
                }
            }
            context.dataStore.edit { settings ->
                settings[RECENT] = new
            }
        } catch (e: Exception) {
            Log.w(XANDY_CLOUD, "Failed to update query set $e")
            return@withContext
        }
    }


}