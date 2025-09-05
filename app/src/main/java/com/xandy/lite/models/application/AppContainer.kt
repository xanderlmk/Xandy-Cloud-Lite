package com.xandy.lite.models.application

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.xandy.lite.R
import com.xandy.lite.db.XandyDatabase
import com.xandy.lite.db.song.repo.SongRepository
import com.xandy.lite.db.song.repo.SongRepositoryImpl
import com.xandy.lite.models.ui.drawableResUri
import com.xandy.lite.navigation.UIRepository
import com.xandy.lite.navigation.UIRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlin.getValue

interface AppContainer {
    val songRepository: SongRepository
    val uiRepository: UIRepository
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppDataContainer(
    private val scope: CoroutineScope, private val context: Context
) : AppContainer {
    companion object {
        private const val PREFERENCES = "preferences"
    }
    override val songRepository: SongRepository by lazy {
        SongRepositoryImpl(
            context.drawableResUri(R.drawable.unknown_track), context,
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE),
            XandyDatabase.getDatabase(context, scope).audioDao(),
            XandyDatabase.getDatabase(context, scope).playlistDao(),
            XandyDatabase.getDatabase(context, scope).bucketDao(), scope
        )
    }
    override val uiRepository: UIRepository by lazy {
        UIRepositoryImpl(context)
    }
}