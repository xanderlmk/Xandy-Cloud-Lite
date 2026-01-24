package com.xandy.lite.widget

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.ImageProvider
import  androidx.glance.ImageProvider as ResIdImageProvider
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.text.TextDefaults.defaultTextStyle
import androidx.glance.unit.ColorProvider
import com.xandy.lite.R
import com.xandy.lite.SongViewActivity
import com.xandy.lite.db.XandyDatabase.Companion.getDatabase
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.application.dataStore
import com.xandy.lite.ui.GetUIStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.color.ColorProvider
import androidx.glance.color.isNightMode
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.state.GlanceStateDefinition
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.xandy.lite.controllers.SkipNextHandler
import com.xandy.lite.models.application.AppDataContainer.Companion.PREFERENCES
import com.xandy.lite.models.application.AppStrings
import com.xandy.lite.models.application.mediaControllerBuilder
import com.xandy.lite.models.media.player.ButtonType
import com.xandy.lite.models.media.player.PlaybackService
import com.xandy.lite.views.player.controller.handleSkipPrevious
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import androidx.media3.session.R as AndroidR


@SuppressLint("RestrictedApi", "LocalContextConfigurationRead")
@androidx.annotation.OptIn(UnstableApi::class)
class XandyWidget : GlanceAppWidget() {
    private val scope = CoroutineScope(SupervisorJob())
    private val keyId = stringPreferencesKey("key_id")
    private var controller = flowOf<MediaController?>(null)

    companion object {
        private val SMALL_RECTANGLE = DpSize(120.dp, 80.dp)
        private val MEDIUM_RECTANGLE = DpSize(200.dp, 80.dp)
        private val BIG_RECTANGLE = DpSize(280.dp, 80.dp)
        val playingKey = booleanPreferencesKey("is_playing")
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL_RECTANGLE, MEDIUM_RECTANGLE, BIG_RECTANGLE)
    )


    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val stored = context.dataStore
        val dao = getDatabase(context, scope).audioDao()
        val initialTrack = stored.data.flatMapLatest { preferences ->
            val key = preferences[keyId] ?: ""
            dao.getSongWithPls(key).map { it?.song }
        }.first()
        val initialRM = stored.data.map { preferences ->
            preferences[PlaybackService.REPEAT_MODE] ?: Player.REPEAT_MODE_OFF
        }.first()
        val initialSE = stored.data.map { preferences ->
            preferences[PlaybackService.SHUFFLE_ENABLED] ?: false
        }.first()
        val initialIsPlaying = stored.data.map { preferences ->
            preferences[playingKey] ?: false
        }.first()
        val nullTrack = context.getString(R.string.null_track)
        val sessionToken =
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        mediaControllerBuilder(
            sessionToken = sessionToken, context = context,
            onGetController = { controller = flowOf(it) }
        )


        provideContent {
            stored.data.flatMapLatest { preferences ->
                val key = preferences[keyId] ?: ""
                dao.getSongWithPls(key).map { it?.song }
            }.collectAsState(initialTrack).value?.let {
                val repeatMode = stored.data.map { preferences ->
                    preferences[PlaybackService.REPEAT_MODE] ?: Player.REPEAT_MODE_OFF
                }
                val isPlaying = stored.data.map { preferences ->
                    preferences[playingKey] ?: false
                }
                val shuffleEnabled = stored.data.map { preferences ->
                    preferences[PlaybackService.SHUFFLE_ENABLED] ?: false
                }
                val skipNextHandler = SkipNextHandler(prefs)
                val mc by controller.collectAsState(null)
                Content(
                    af = it, mc,
                    isPlaying = isPlaying.collectAsState(initialIsPlaying).value,
                    repeatMode = repeatMode.collectAsState(initialRM).value,
                    shuffleEnabled = shuffleEnabled.collectAsState(initialSE).value,
                    skipNextHandler
                )
            } ?: Text(
                text = nullTrack,
                modifier = GlanceModifier.padding(12.dp),
                style = defaultTextStyle.copy(
                    color = ColorProvider(
                        if (LocalContext.current.isNightMode) R.color.white else R.color.black
                    ),
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Start
                )
            )
        }
    }


    suspend fun updateMediaKey(context: Context, new: String) =
        context.dataStore.edit { settings -> settings[keyId] = new }


    override fun onCompositionError(
        context: Context, glanceId: GlanceId, appWidgetId: Int, throwable: Throwable
    ) {
        Log.e(XANDY_CLOUD, "Error on showing widget", throwable)
        scope.launch { controller.first()?.release() }
        super.onCompositionError(context, glanceId, appWidgetId, throwable)
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        controller.first()?.release()
        super.onDelete(context, glanceId)
    }

    @Composable
    private fun Content(
        af: AudioFile, controller: MediaController?,
        isPlaying: Boolean, repeatMode: Int, shuffleEnabled: Boolean,
        skipNextHandler: SkipNextHandler
    ) {
        val getUIStyle = GetUIStyle(
            MaterialTheme.colorScheme, LocalContext.current.isNightMode,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        )
        val appStrings = AppStrings(
            LocalContext.current.getString(R.string.unknown_artist),
            LocalContext.current.getString(R.string.Unknown)
        )
        val size = LocalSize.current
        val textAlign = if (size.width < MEDIUM_RECTANGLE.width)
            TextAlign.Center else TextAlign.Start
        val repeatIcon = when (repeatMode) {
            Player.REPEAT_MODE_ONE -> R.drawable.sharp_repeat_one
            Player.REPEAT_MODE_ALL -> R.drawable.sharp_repeat
            else -> R.drawable.repeat_off
        }
        val backgroundColor = ColorProvider(
            getUIStyle.altLightThemeBackgroundColor(), getUIStyle.altDarkThemeBackgroundColor()
        )
        Box(
            modifier = GlanceModifier.fillMaxSize()
                .background(backgroundColor)
                .clickable(actionStartActivity<SongViewActivity>()),
            contentAlignment = Alignment.Center
        ) {

            CustomRow(GlanceModifier.padding(horizontal = 2.dp)) {
                if (size.width >= MEDIUM_RECTANGLE.width)
                    Image(
                        ImageProvider(af.picture), "Song picture",
                        modifier = GlanceModifier.size(75.dp).padding(start = 5.dp)
                    )

                Column(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CustomRow(modifier = GlanceModifier.padding(start = 4.dp)) {
                        Column(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = af.title,
                                modifier = GlanceModifier.padding(1.dp).fillMaxWidth(),
                                style = defaultTextStyle.copy(
                                    color = ColorProvider(Color.Black, Color.White),
                                    textAlign = textAlign, fontSize = 14.sp
                                ),
                                maxLines = 1
                            )

                            Text(
                                text = af.artist
                                    ?: LocalContext.current.getString(R.string.unknown_artist),
                                modifier = GlanceModifier.padding(1.dp).fillMaxWidth(),
                                style = defaultTextStyle.copy(
                                    color = ColorProvider(Color.Black, Color.White),
                                    textAlign = textAlign, fontSize = 14.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                    CustomRow(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (size.width >= BIG_RECTANGLE.width)
                            CircleIconButton(
                                ResIdImageProvider(repeatIcon),
                                contentDescription = "",
                                onClick = {
                                    controller?.sendCustomCommand(
                                        SessionCommand(ButtonType.COMMAND_REPEAT, Bundle()),
                                        Bundle()
                                    )
                                }, backgroundColor = backgroundColor
                            )
                        CircleIconButton(
                            ResIdImageProvider(AndroidR.drawable.media3_icon_previous),
                            contentDescription = "",
                            onClick = {
                                controller?.let { mc -> handleSkipPrevious(repeatMode, mc) }
                            }, backgroundColor = backgroundColor
                        )
                        CircleIconButton(
                            ResIdImageProvider(
                                if (isPlaying) AndroidR.drawable.media3_icon_pause
                                else AndroidR.drawable.media3_icon_play
                            ),
                            contentDescription = "",
                            onClick = {
                                if (isPlaying) controller?.pause()
                                else controller?.play()
                            }, backgroundColor = backgroundColor
                        )
                        CircleIconButton(
                            ResIdImageProvider(AndroidR.drawable.media3_icon_next),
                            contentDescription = "",
                            onClick = {
                                controller?.let { mc ->
                                    skipNextHandler.handleSkipNext(
                                        shuffleEnabled, repeatMode, mc, appStrings
                                    )
                                }
                            }, backgroundColor = backgroundColor
                        )
                        if (size.width >= BIG_RECTANGLE.width)
                            CircleIconButton(
                                ResIdImageProvider(
                                    if (shuffleEnabled) AndroidR.drawable.media3_icon_shuffle_on
                                    else AndroidR.drawable.media3_icon_shuffle_off
                                ),
                                contentDescription = "",
                                onClick = {
                                    controller?.sendCustomCommand(
                                        SessionCommand(ButtonType.COMMAND_SHUFFLE, Bundle()),
                                        Bundle()
                                    )
                                }, backgroundColor = backgroundColor
                            )
                    }
                }
            }
        }
    }

    @Composable
    private fun CustomRow(
        modifier: GlanceModifier = GlanceModifier,
        horizontalAlignment: Alignment.Horizontal = Alignment.Start,
        content: @Composable () -> Unit
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = horizontalAlignment,
            verticalAlignment = Alignment.CenterVertically
        ) { content() }
    }
}