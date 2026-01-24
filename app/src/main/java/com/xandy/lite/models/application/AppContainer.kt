package com.xandy.lite.models.application

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.xandy.lite.R
import com.xandy.lite.db.XandyDatabase
import com.xandy.lite.db.lyrics.repo.LyricsRepository
import com.xandy.lite.db.lyrics.repo.OfflineLyricsRepo
import com.xandy.lite.db.song.repo.SongRepository
import com.xandy.lite.db.song.repo.SongRepositoryImpl
import com.xandy.lite.models.AppPref
import com.xandy.lite.models.ui.drawableResUri
import com.xandy.lite.navigation.UIRepository
import com.xandy.lite.navigation.UIRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import kotlin.getValue

interface AppContainer {
    val songRepository: SongRepository
    val lyricsRepository: LyricsRepository
    val uiRepository: UIRepository
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

internal class AppDataContainer(
    private val scope: CoroutineScope, private val context: Context
) : AppContainer {
    companion object {
        const val PREFERENCES = "preferences"
    }

    private val _appValues = MutableStateFlow(
        AppValues(
            context.drawableResUri(R.drawable.unknown_track),
            context.getString(R.string.unknown_artist),
            context.getString(R.string.Unknown)
        )
    )
    private val appValues = _appValues.asStateFlow()
    private val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    private val onUpdateValues: () -> Unit = {
        val language = AppPref.getLanguage(prefs) ?: Locale.getDefault().language
        val locale = Locale.forLanguageTag(language)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        val new = context.createConfigurationContext(config)
        _appValues.update {
            AppValues(
                new.drawableResUri(R.drawable.unknown_track),
                new.getString(R.string.unknown_artist),
                new.getString(R.string.Unknown)
            )
        }
    }
    override val songRepository: SongRepository by lazy {
        SongRepositoryImpl(
            appValues, context, prefs,
            XandyDatabase.getDatabase(context, scope).audioDao(),
            XandyDatabase.getDatabase(context, scope).playlistDao(),
            XandyDatabase.getDatabase(context, scope).bucketDao(), scope
        )
    }
    override val lyricsRepository by lazy {
        OfflineLyricsRepo(
            XandyDatabase.getDatabase(context, scope).audioDao(), context
        )
    }
    override val uiRepository: UIRepository by lazy {
        UIRepositoryImpl(context, onUpdateValues)
    }

    init { onUpdateValues() }
}