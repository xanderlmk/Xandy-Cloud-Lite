package com.xandy.lite.models.media.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.updateAll
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioTrackBufferSizeProvider
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.xandy.lite.MainActivity
import com.xandy.lite.R
import com.xandy.lite.SongViewActivity
import com.xandy.lite.controllers.Controller
import com.xandy.lite.controllers.PlayNext
import com.xandy.lite.controllers.updateIsPlaying
import com.xandy.lite.db.XandyDatabase.Companion.getDatabase
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.isFavorite
import com.xandy.lite.db.tables.itemKey
import com.xandy.lite.db.tables.replaceIsFavorite
import com.xandy.lite.db.tables.toMediaItem
import com.xandy.lite.db.tables.toMediaItems
import com.xandy.lite.db.tables.uri
import com.xandy.lite.models.AppPref
import com.xandy.lite.models.application.AppDataContainer
import com.xandy.lite.models.application.AppStrings
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.application.dataStore
import com.xandy.lite.models.ui.PriorityQueue
import com.xandy.lite.models.ui.drawableResUri
import com.xandy.lite.widget.XandyWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.collections.first


@OptIn(UnstableApi::class)
private fun commandBuilder(icon: Int, name: String, action: String, slot: Int): CommandButton =
    CommandButton.Builder(icon)
        .setDisplayName(name)
        .setSessionCommand(SessionCommand(action, Bundle()))
        .setSlots(slot)
        .build()


@UnstableApi
class PlaybackService : MediaSessionService() {
    private var _mediaSession: MediaSession? = null
    private lateinit var appPref: SharedPreferences
    private var headsetClickCount = 0
    private var headsetClickJob: Job? = null
    private val headsetClickWindowMs = 400L
    private val svcScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    companion object {
        private const val COMMAND_SEEK_TO_NEXT = "Next_Song"
        private const val COMMAND_SEEK_TO_PREV = "Prev_Song"

        // private const val COMMAND_PAUSE = "Pause_Song"
        // private const val COMMAND_PLAY = "Play_Song"
        private const val COMMAND_REPEAT = ButtonType.COMMAND_REPEAT
        private const val COMMAND_SHUFFLE = ButtonType.COMMAND_SHUFFLE
        private const val COMMAND_CHECK_TIME = "Check_Time"
        private const val PREFERENCES = AppDataContainer.PREFERENCES
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = intPreferencesKey("repeat_mode")
        private const val OFFLOADING_ENABLED = "Offloading.Enabled"
        private const val LOAD_CONTROL = "LoadControl"
        private const val FIX_AUDIO_POSITION = "FixAudioPosition"
        private const val CHANGED_PLAYBACK_SETTINGS = "playback_settings_changed"
        private const val BUTTON_LAYOUT = ButtonType.BUTTON_LAYOUT
        private const val COMMAND_FAST_FORWARD = ButtonType.COMMAND_FAST_FORWARD
        private const val COMMAND_REWIND = ButtonType.COMMAND_REWIND
        private const val COMMAND_FAVORITE = ButtonType.COMMAND_FAVORITE
        private const val COMMAND_CHANGE_BUTTON = ButtonType.COMMAND_CHANGE_BUTTON
        private const val COMMAND_CHANGE_LAYOUT = ButtonType.COMMAND_CHANGE_LAYOUT
        private val changeIcon = R.drawable.outline_change_circle
    }

    private val prevButton = commandBuilder(
        CommandButton.ICON_PREVIOUS, "Previous",
        COMMAND_SEEK_TO_PREV, CommandButton.SLOT_BACK
    )
    private val nextButton = commandBuilder(
        CommandButton.ICON_NEXT, "Next",
        COMMAND_SEEK_TO_NEXT, CommandButton.SLOT_FORWARD
    )
    private val repeatButton = commandBuilder(
        CommandButton.ICON_REPEAT_OFF, "Repeat",
        COMMAND_REPEAT, CommandButton.SLOT_OVERFLOW
    )
    private val shuffleButton = commandBuilder(
        CommandButton.ICON_SHUFFLE_OFF, "Shuffle",
        COMMAND_SHUFFLE, CommandButton.SLOT_OVERFLOW
    )

    private val forwardButton = commandBuilder(
        CommandButton.ICON_FAST_FORWARD, "FastForward",
        COMMAND_FAST_FORWARD, CommandButton.SLOT_OVERFLOW
    )

    private val rewindButton = commandBuilder(
        CommandButton.ICON_REWIND, "Rewind",
        COMMAND_REWIND, CommandButton.SLOT_OVERFLOW
    )

    private lateinit var changeButton: CommandButton

    private fun createChangeButton(uri: Uri, slot: Int = CommandButton.SLOT_OVERFLOW) =
        CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName("Change")
            .setSessionCommand(SessionCommand(COMMAND_CHANGE_BUTTON, Bundle()))
            .setSlots(slot)
            .setCustomIconResId(changeIcon)
            .setIconUri(uri)
            .build()

    private lateinit var handler: Handler
    private lateinit var appStrings: AppStrings
    private val playbackRunnable = object : Runnable {
        override fun run() {
            try {
                val player = _mediaSession?.player ?: return
                val mediaItem = player.currentMediaItem ?: return
                if (player.repeatMode == Player.REPEAT_MODE_ONE) return
                if (!player.isPlaying) return
                val timeLeft = player.duration - player.currentPosition
                if (!::appPref.isInitialized)
                    appPref = try {
                        getAppPrefs()
                    } catch (_: Exception) {
                        return
                    }
                if (!::appStrings.isInitialized)
                    appStrings = try {
                        getAppStrings()
                    } catch (_: Exception) {
                        return
                    }
                val savedIndex = AppPref.getInitSavedIndex(appPref)
                val trashedItemKey = AppPref.getInitItemToTrash(appPref)
                val priorityList = AppPref.getInitialPriorityQueue(appPref).toMutableList()
                if (timeLeft < 625) {
                    val result = onCheckPriorityQueue(
                        player, mediaItem, savedIndex, trashedItemKey, priorityList
                    )
                    if (result is PriorityQueue.Finished) return
                } else if (priorityList.isEmpty() && trashedItemKey.isBlank() &&
                    savedIndex == C.INDEX_UNSET
                ) return
                Log.i(XANDY_CLOUD, "Runner to check again.")
                handler.postDelayed(this, (timeLeft - 175).coerceAtLeast(175))
            } catch (_: Exception) {
                Log.w(XANDY_CLOUD, "Failed get position")
            }
        }
    }

    @OptIn(PlayNext::class)
    private fun onCheckPriorityQueue(
        player: Player, mediaItem: MediaItem, savedIndex: Int, trashedItemKey: String,
        priorityList: MutableList<AudioFile>
    ): PriorityQueue {
        val shuffleEnabled = player.shuffleModeEnabled
        val currentItemKey = mediaItem.itemKey()
        return if (priorityList.isEmpty() && trashedItemKey.isBlank() &&
            savedIndex == C.INDEX_UNSET
        ) PriorityQueue.Finished
        else if (priorityList.isNotEmpty()) {
            val currentIndex = player.currentMediaItemIndex
            val nextIndex = player.nextMediaItemIndex.takeIf { it > C.INDEX_UNSET }
            val nextMedia = priorityList.first().toMediaItem(appStrings)
            player.addMediaItem(currentIndex, nextMedia)
            player.seekToDefaultPosition(currentIndex)
            /**
             * If the current item is to be trashed,
             * it got moved 1 to the right, so remove it.
             */
            if (trashedItemKey == currentItemKey)
                player.removeMediaItem(currentIndex + 1)

            if (trashedItemKey.isBlank()) {
                AppPref.updateCurrentIndex(currentIndex, appPref)
                if (shuffleEnabled)
                    nextIndex?.let { AppPref.updateSavedIndex(it, appPref) }
                else AppPref.updateSavedIndex(currentIndex + 2, appPref)
            }
            priorityList.removeAt(0)
            AppPref.updatePriorityQueue(priorityList.toList(), appPref)
            AppPref.updateTrashedItemKey(nextMedia.itemKey(), appPref)
            PriorityQueue.Sought
        } else if (
            trashedItemKey.isNotBlank() && savedIndex > C.INDEX_UNSET
        ) {
            val currentIndex = AppPref.getInitCurrentIndex(appPref)

            val prospectiveIdx = if (savedIndex == currentIndex + 2)
                player.nextMediaItemIndex.takeIf { it > C.INDEX_UNSET } ?: (currentIndex + 2)
            else savedIndex
            player.seekToDefaultPosition(if (!shuffleEnabled) currentIndex + 2 else prospectiveIdx)
            if (trashedItemKey == currentItemKey && currentIndex > C.INDEX_UNSET)
                player.removeMediaItem(currentIndex)
            AppPref.clearPriorityItemStates(appPref)
            PriorityQueue.Sought
        } else PriorityQueue.Skipped
    }

    fun cleanup() = handler.removeCallbacks(playbackRunnable)

    private fun initLateVars() {
        appPref = getAppPrefs()
        changeButton = createChangeButton(this.drawableResUri(changeIcon))
        appStrings = getAppStrings()
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        initLateVars()
        val renderersFactory = getRenderers(this)
        val loadControl = loadControlBuilder()
        val audioOffloadPreferences = audioOffloadBuilder()
        val playbackSpeed = AppPref.getPlaybackSpeed(appPref)
        val silenceSkipEnabled = AppPref.getSkipSilenceEnabled(appPref)
        val player = buildExpoPlayer(
            this, loadControl, audioOffloadPreferences, audioAttributes(), playbackSpeed,
            silenceSkipEnabled, renderersFactory
        )
        val forwardingPlayer = forwardingPlayer(player)

        val buttonLayout = listOf(
            prevButton, /*playButton, pauseButton,*/ nextButton, repeatButton, shuffleButton
        )
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val songViewIntent = Intent(this, SongViewActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val sessionActivityPendingIntent = PendingIntent.getActivities(
            this, 0, arrayOf(mainIntent, songViewIntent),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val onCallHandler: () -> Unit = {
            handler.removeCallbacks(playbackRunnable)
            handler.postDelayed(playbackRunnable, 50)
        }
        /**
         *  Override the player as well as the notification buttons
         */
        _mediaSession =
            MediaSession.Builder(this, forwardingPlayer)
                .setCallback(SessionCallback())
                .setSessionActivity(sessionActivityPendingIntent)
                .setMediaButtonPreferences(buttonLayout)
                .build()

        _mediaSession?.player?.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                super.onEvents(player, events)
                if (events.contains(Player.EVENT_PLAYER_ERROR)) {
                    val error = player.playerError
                    error?.let { Log.e(XANDY_CLOUD, "Failed to play/continue track: ${it.cause}") }
                    player.prepare(); player.seekToNextMediaItem(); player.pause()
                }

                if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                    if (!player.isPlaying) handler.removeCallbacks(playbackRunnable)
                    else onCallHandler()
                    val isPlaying = player.isPlaying
                    svcScope.launch {
                        this@PlaybackService.updateIsPlaying(isPlaying)
                        XandyWidget().updateAll(this@PlaybackService)
                    }
                }


                if (events.contains(Player.EVENT_REPEAT_MODE_CHANGED))
                    if (player.repeatMode == Player.REPEAT_MODE_ONE)
                        handler.removeCallbacks(playbackRunnable)
                    else onCallHandler()


                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    val playerControls =
                        getInitButtonLayout(appPref).takeIf { it.isCustomBased() } ?: return
                    val session = _mediaSession ?: return
                    if (!::appPref.isInitialized)
                        appPref = try {
                            getAppPrefs()
                        } catch (_: Exception) {
                            return
                        }
                    val repeatMode = player.repeatMode
                    val shuffleEnabled = player.shuffleModeEnabled
                    val isFavorite = player.currentMediaItem?.isFavorite() ?: false

                    val repeatIcon = getRepeatIconButton(repeatMode)
                    val shuffleIcon =
                        if (shuffleEnabled) CommandButton.ICON_SHUFFLE_ON
                        else CommandButton.ICON_SHUFFLE_OFF
                    setButtonLayout(
                        playerControls, session, repeatIcon, shuffleIcon, isFavorite
                    )
                    svcScope.launch {
                        player.currentMediaItem?.let {
                            XandyWidget().updateMediaKey(this@PlaybackService, it.itemKey())
                            XandyWidget().updateAll(this@PlaybackService)
                        }
                    }
                }

                val position = player.currentPosition
                AppPref.updateLastPosition(position, appPref)
            }
        }
        )
        svcScope.launch {
            try {
                val session = _mediaSession ?: return@launch
                handler = Handler(session.player.applicationLooper)
                handler.postDelayed(playbackRunnable, 1_000L)
                val repeatMode = getInitRepeatMode(this@PlaybackService)
                val shuffleEnabled = getInitShuffleEnabled(this@PlaybackService)
                val repeatIcon = getRepeatIconButton(repeatMode)
                val shuffleIcon =
                    if (shuffleEnabled) CommandButton.ICON_SHUFFLE_ON
                    else CommandButton.ICON_SHUFFLE_OFF
                session.player.repeatMode = repeatMode
                session.player.shuffleModeEnabled = shuffleEnabled

                val isFavorite = setMediaItems(this@PlaybackService, appPref, session, this)
                val playerControls = getInitButtonLayout(appPref)
                setButtonLayout(playerControls, session, repeatIcon, shuffleIcon, isFavorite)

            } catch (e: Exception) {
                Log.e(
                    XANDY_CLOUD,
                    "Failed to connect to media player successfully: ${e.printStackTrace()}"
                )
            }
            Log.i(XANDY_CLOUD, "onCreate session")

        }
    }

    override fun onGetSession(controllerInfo: ControllerInfo): MediaSession? {
        val session = _mediaSession ?: return null
        initLateVars()
        try {
            val changedSettings = appPref.getBoolean(CHANGED_PLAYBACK_SETTINGS, false)
            if (changedSettings) {
                val loadControl = loadControlBuilder()
                val audioOffloadPreferences = audioOffloadBuilder()
                val renderersFactory = getRenderers(this)
                val playbackSpeed = AppPref.getPlaybackSpeed(appPref)
                val silenceSkipEnabled = AppPref.getSkipSilenceEnabled(appPref)
                val newPlayer =
                    buildExpoPlayer(
                        this, loadControl, audioOffloadPreferences, audioAttributes(),
                        playbackSpeed, silenceSkipEnabled, renderersFactory
                    )
                val forwardingPlayer = forwardingPlayer(newPlayer)
                session.player.release()
                session.player = forwardingPlayer
            }
            svcScope.launch {
                if (changedSettings) {
                    setMediaItems(this@PlaybackService, appPref, session, this)
                    appPref.edit { putBoolean(CHANGED_PLAYBACK_SETTINGS, false) }
                }
                val repeatMode = getInitRepeatMode(this@PlaybackService)
                val shuffleEnabled = getInitShuffleEnabled(this@PlaybackService)
                session.player.repeatMode = repeatMode
                session.player.shuffleModeEnabled = shuffleEnabled

                Log.i(XANDY_CLOUD, "onGet session")

            }
        } catch (_: Exception) {
            Log.e(XANDY_CLOUD, "Cannot resume session")
        }
        return session
    }

    override fun onDestroy() {
        try {
            headsetClickJob?.cancel()
            svcScope.cancel()
        } catch (e: Exception) {
            Log.e(XANDY_CLOUD, "Failed to cancel jobs: ${e.printStackTrace()}")
        }
        try {
            _mediaSession?.player?.currentMediaItem?.let {
                val position = _mediaSession?.player?.currentPosition ?: 0L
                if (::appPref.isInitialized) {
                    AppPref.updateMediaItem(it.itemKey(), appPref)
                    AppPref.updateLastPosition(position, appPref)
                }
            }
            cleanup()
        } catch (_: Exception) {
            Log.e(XANDY_CLOUD, "Failed to get position and item key")
        }
        try {
            _mediaSession?.player?.release()
            _mediaSession?.release()
        } catch (_: Exception) {
        }
        _mediaSession = null
        super.onDestroy()
    }


    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
            nextButton.sessionCommand?.let(availableSessionCommands::add)
            prevButton.sessionCommand?.let(availableSessionCommands::add)
            // pauseButton.sessionCommand?.let(availableSessionCommands::add)
            // playButton.sessionCommand?.let(availableSessionCommands::add)
            repeatButton.sessionCommand?.let(availableSessionCommands::add)
            shuffleButton.sessionCommand?.let(availableSessionCommands::add)
            forwardButton.sessionCommand?.let(availableSessionCommands::add)
            rewindButton.sessionCommand?.let(availableSessionCommands::add)
            changeButton.sessionCommand?.let(availableSessionCommands::add)
            availableSessionCommands.add(SessionCommand(COMMAND_CHANGE_LAYOUT, Bundle()))
            availableSessionCommands.add(SessionCommand(COMMAND_CHECK_TIME, Bundle()))
            availableSessionCommands.add(SessionCommand(COMMAND_FAVORITE, Bundle()))
            return MediaSession.ConnectionResult.accept(
                availableSessionCommands.build(), connectionResult.availablePlayerCommands
            )
        }

        override fun onMediaButtonEvent(
            session: MediaSession, controllerInfo: ControllerInfo, intent: Intent
        ): Boolean {
            val keyEvent: KeyEvent? = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableExtra(KeyEvent::class.java.name)
            } catch (_: Exception) {
                Log.e(XANDY_CLOUD, "Failed to get key event")
                null
            }
            if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                try {
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            handleSkipNext(session.player)
                            return true
                        }

                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            handleSkipPrevious(session.player)
                            return true
                        }

                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                keyEvent.device.isExternal
                            ) {
                                headsetClickCount = (headsetClickCount + 1).coerceAtMost(3)
                                // cancel any pending job and schedule a new one
                                headsetClickJob?.cancel()
                                headsetClickJob = svcScope.launch {
                                    delay(headsetClickWindowMs)
                                    val p = session.player
                                    when (headsetClickCount) {
                                        1 -> if (p.isPlaying) p.pause() else p.play()
                                        2 -> handleSkipNext(p)
                                        3 -> handleSkipPrevious(p)
                                    }
                                    headsetClickCount = 0
                                }
                                return true
                            } else return super.onMediaButtonEvent(session, controllerInfo, intent)
                        }

                        else -> return super.onMediaButtonEvent(session, controllerInfo, intent)
                    }
                } catch (e: Exception) {
                    Log.e(XANDY_CLOUD, "Error: ${e.printStackTrace()}")
                    return super.onMediaButtonEvent(session, controllerInfo, intent)
                }
            }
            return super.onMediaButtonEvent(session, controllerInfo, intent)
        }

        override fun onDisconnected(session: MediaSession, controller: ControllerInfo) {
            try {
                val position = session.player.currentPosition
                val key = session.player.currentMediaItem?.itemKey()
                val isPlaying = session.player.isPlaying
                svcScope.launch {
                    this@PlaybackService.updateIsPlaying(isPlaying)
                    key?.let { XandyWidget().updateMediaKey(this@PlaybackService, it) }
                    XandyWidget().updateAll(this@PlaybackService)
                }
                if (::appPref.isInitialized) {
                    key?.let { AppPref.updateMediaItem(it, appPref) }
                    AppPref.updateLastPosition(position, appPref)
                }

            } catch (_: Exception) {
                Log.w(XANDY_CLOUD, "Failed to update position at Root")
            }
            super.onDisconnected(session, controller)
        }

        override fun onCustomCommand(
            session: MediaSession, controller: ControllerInfo, customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            super.onCustomCommand(session, controller, customCommand, args)
            appPref = getAppPrefs()
            appStrings = getAppStrings()
            changeButton = createChangeButton(this@PlaybackService.drawableResUri(changeIcon))
            var playerControls = getInitButtonLayout(appPref)
            when (customCommand.customAction) {
                COMMAND_SEEK_TO_NEXT -> handleSkipNext(session.player)
                COMMAND_SEEK_TO_PREV -> handleSkipPrevious(session.player)
                //COMMAND_PAUSE -> session.player.pause()
                //COMMAND_PLAY -> session.player.play()
                COMMAND_REPEAT -> {
                    when (session.player.repeatMode) {
                        Player.REPEAT_MODE_ALL -> {
                            session.player.repeatMode = Player.REPEAT_MODE_ONE
                            updateRepeatMode(Player.REPEAT_MODE_ONE, this@PlaybackService)
                        }

                        Player.REPEAT_MODE_OFF -> {
                            session.player.repeatMode = Player.REPEAT_MODE_ALL
                            updateRepeatMode(Player.REPEAT_MODE_ALL, this@PlaybackService)
                        }

                        Player.REPEAT_MODE_ONE -> {
                            session.player.repeatMode = Player.REPEAT_MODE_OFF
                            updateRepeatMode(Player.REPEAT_MODE_OFF, this@PlaybackService)
                        }
                    }
                }

                COMMAND_SHUFFLE -> {
                    val enabled = !session.player.shuffleModeEnabled
                    updateShuffleEnabled(enabled, this@PlaybackService)
                    session.player.shuffleModeEnabled = enabled
                }

                COMMAND_FAST_FORWARD -> session.player.seekForward()
                COMMAND_REWIND -> session.player.seekBack()
//                COMMAND_CHANGE_LAYOUT -> setSessionButtonLayout(session, playerControls)


                COMMAND_CHANGE_BUTTON -> {
                    if (playerControls is PlayerControls.Configurable) {
                        val new = playerControls.switchButton()
                        updateButtonLayout(new, appPref)
                    } else if (playerControls is PlayerControls.Custom &&
                        playerControls.hasConfigButton()
                    ) {
                        val new = playerControls.switchButton()
                        updateButtonLayout(new, appPref)
                    }
                }

                COMMAND_CHECK_TIME -> {
                    handler.removeCallbacks(playbackRunnable)
                    handler.postDelayed(playbackRunnable, 100L)
                }

                COMMAND_FAVORITE -> {
                    session.player.currentMediaItem?.let { item ->
                        svcScope.launch {
                            val dao = getDatabase(this@PlaybackService, this).audioDao()
                            if (item.isFavorite()) dao.unfavoriteSong(item.uri())
                            else dao.favoriteSong(item.uri())
                            val newItem = item.replaceIsFavorite(appStrings)
                            session.player.replaceMediaItem(
                                session.player.currentMediaItemIndex, newItem
                            )
                            setSessionButtonLayout(session, playerControls)
                        }
                    }
                }
            }
            playerControls = getInitButtonLayout(appPref)
            setSessionButtonLayout(session, playerControls)
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        private fun setSessionButtonLayout(session: MediaSession, playerControls: PlayerControls) {
            val repeatIcon = getRepeatIconButton(session.player.repeatMode)
            val shuffleIcon =
                if (session.player.shuffleModeEnabled) CommandButton.ICON_SHUFFLE_ON
                else CommandButton.ICON_SHUFFLE_OFF
            val isFavorite = session.player.currentMediaItem?.isFavorite() ?: false

            setButtonLayout(playerControls, session, repeatIcon, shuffleIcon, isFavorite)
        }

        private fun updateButtonLayout(new: PlayerControls, appPref: SharedPreferences) =
            try {
                appPref.edit {
                    putString(
                        BUTTON_LAYOUT, Json.encodeToString(PlayerControls.serializer(), new)
                    )
                }
            } catch (_: Exception) {
                Log.e(XANDY_CLOUD, "Failed to update button layout")
            }

        private fun updateRepeatMode(mode: Int, context: Context) {
            svcScope.launch {
                try {
                    context.dataStore.edit { settings ->
                        settings[REPEAT_MODE] = mode
                    }
                } catch (e: Exception) {
                    Log.e(XANDY_CLOUD, "${e.printStackTrace()}")
                }
            }
        }

        private fun updateShuffleEnabled(enabled: Boolean, context: Context) {
            svcScope.launch {
                try {
                    context.dataStore.edit { settings ->
                        settings[SHUFFLE_ENABLED] = enabled
                    }
                } catch (e: Exception) {
                    Log.e(XANDY_CLOUD, "${e.printStackTrace()}")
                }
            }
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession, controller: ControllerInfo
        ): ListenableFuture<MediaItemsWithStartPosition> {
            val settable = SettableFuture.create<MediaItemsWithStartPosition>()
            if (_mediaSession == null)
                _mediaSession = mediaSession
            svcScope.launch {
                try {
                    appPref = getAppPrefs()
                    // 1. Restore queue IDs from prefs
                    val ids = AppPref.getQueue(applicationContext).first()
                    if (ids.isEmpty()) {
                        settable.set(
                            MediaItemsWithStartPosition(emptyList(), C.INDEX_UNSET, C.TIME_UNSET)
                        )
                        return@launch
                    }
                    val dao = getDatabase(applicationContext, this).audioDao()
                    val mediaItems = dao.getInitialQueue(ids).toMediaItems(appStrings)
                    val itemsByKey = mediaItems.associateBy { it.itemKey() }
                    val orderedQueue = ids.mapNotNull { id -> itemsByKey[id] }.toMutableList()
                    val itemKey = AppPref.getInitialMediaKey(appPref)
                    val trashedItemKey = AppPref.getInitItemToTrash(appPref)
                    val currentIndex = AppPref.getInitCurrentIndex(appPref)
                    val trashedItem = dao.getTrashedItemOrNull(trashedItemKey)
                    val first = orderedQueue.find { it.itemKey() == itemKey }
                    val priorityItemExists =
                        currentIndex > C.INDEX_UNSET && trashedItemKey == itemKey && trashedItem != null
                    val index = currentIndex.takeIf { priorityItemExists }
                        ?: orderedQueue.indexOf(first)
                            .takeIf { it >= 0 } ?: 0
                    if (priorityItemExists)
                        orderedQueue.add(index, trashedItem.toMediaItem(appStrings))
                    val startPosition = AppPref.getLastPosition(appPref)
                    val result = MediaItemsWithStartPosition(
                        orderedQueue, index.coerceIn(orderedQueue.indices), startPosition
                    )
                    settable.set(result)
                } catch (e: Exception) {
                    Log.e(XANDY_CLOUD, "Resumption failed", e)
                    settable.set(
                        MediaItemsWithStartPosition(emptyList(), C.INDEX_UNSET, C.TIME_UNSET)
                    )

                }
            }
            return settable
        }
    }

    private fun getSlotButtons(
        playerControls: PlayerControls, repeatIcon: Int, shuffleIcon: Int, isFavorite: Boolean
    ): Pair<CommandButton, CommandButton> {
        val newSlot1Button = if (playerControls is PlayerControls.Configurable)
            playerControls.toButton(repeatIcon, shuffleIcon, isFavorite) else commandBuilder(
            repeatIcon, "Repeat", COMMAND_REPEAT, CommandButton.SLOT_OVERFLOW
        )
        val newSlot2Button = if (playerControls is PlayerControls.Configurable)
            changeButton else commandBuilder(
            shuffleIcon, "Shuffle", COMMAND_SHUFFLE, CommandButton.SLOT_OVERFLOW
        )
        return Pair(newSlot1Button, newSlot2Button)
    }

    private fun PlayerControls.Configurable.toButton(
        repeatIcon: Int, shuffleIcon: Int, isFavorite: Boolean
    ): CommandButton = when (this.button) {
        XCCommandButton.FastForward -> forwardButton
        XCCommandButton.Rewind -> rewindButton
        XCCommandButton.Repeat -> getRepeatButton(repeatIcon)
        XCCommandButton.Shuffle -> getShuffleButton(shuffleIcon)
        XCCommandButton.Favorite -> getFavoriteButton(isFavorite)
    }

    private fun PlayerControls.Custom.toButtons(
        repeatIcon: Int,
        shuffleIcon: Int,
        isFavorite: Boolean
    ) = this.toListOfNotNull().map {
        it.first.toButton(repeatIcon, shuffleIcon, isFavorite, it.second)
    } //+ listOf(playButton, pauseButton)


    private fun CustomCB.toButton(
        repeatIcon: Int, shuffleIcon: Int, isFavorite: Boolean, slot: Int
    ) = when (this) {
        CustomCB.FastForward -> commandBuilder(
            forwardButton.icon, forwardButton.displayName.toString(), COMMAND_FAST_FORWARD, slot
        )

        CustomCB.Rewind -> commandBuilder(
            rewindButton.icon, rewindButton.displayName.toString(), COMMAND_REWIND, slot
        )

        CustomCB.Repeat -> getRepeatButton(repeatIcon, slot)
        CustomCB.Shuffle -> getShuffleButton(shuffleIcon, slot)
        CustomCB.Favorite -> getFavoriteButton(isFavorite, slot)
        CustomCB.Next -> commandBuilder(
            nextButton.icon, nextButton.displayName.toString(), COMMAND_SEEK_TO_NEXT, slot
        )

        CustomCB.Previous -> commandBuilder(
            prevButton.icon, prevButton.displayName.toString(), COMMAND_SEEK_TO_PREV, slot
        )

        CustomCB.Config ->
            createChangeButton(this@PlaybackService.drawableResUri(changeIcon), slot)
    }


    private fun getRepeatButton(repeatIcon: Int, slot: Int? = null) = commandBuilder(
        repeatIcon, "Repeat", COMMAND_REPEAT, slot ?: CommandButton.SLOT_OVERFLOW
    )

    private fun getShuffleButton(shuffleIcon: Int, slot: Int? = null) = commandBuilder(
        shuffleIcon, "Shuffle", COMMAND_SHUFFLE, slot ?: CommandButton.SLOT_OVERFLOW
    )

    private fun getFavoriteButton(isFavorite: Boolean, slot: Int? = null) = commandBuilder(
        if (isFavorite) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED,
        "Favorite", COMMAND_FAVORITE, slot ?: CommandButton.SLOT_OVERFLOW
    )

    private fun getRepeatIconButton(repeatMode: Int) = when (repeatMode) {
        Player.REPEAT_MODE_OFF -> CommandButton.ICON_REPEAT_OFF
        Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE
        Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
        else -> CommandButton.ICON_REPEAT_OFF
    }

    private fun setButtonLayout(
        playerControls: PlayerControls, session: MediaSession,
        repeatIcon: Int, shuffleIcon: Int, isFavorite: Boolean
    ) {
        val (newSlot1Button, newSlot2Button) =
            getSlotButtons(playerControls, repeatIcon, shuffleIcon, isFavorite)
        when (playerControls) {
            is PlayerControls.Default -> session.setMediaButtonPreferences(
                listOf(
                    prevButton, /*playButton, pauseButton,*/ nextButton,
                    newSlot1Button, newSlot2Button
                )
            )

            is PlayerControls.Reversed -> session.setMediaButtonPreferences(
                listOf(
                    prevButton, /*playButton, pauseButton,*/ nextButton,
                    newSlot2Button, newSlot1Button
                )
            )

            is PlayerControls.Configurable -> session.setMediaButtonPreferences(
                listOf(
                    prevButton, /*playButton, pauseButton,*/ nextButton,
                    newSlot1Button, newSlot2Button
                )
            )

            is PlayerControls.Custom ->
                session.setMediaButtonPreferences(
                    playerControls.toButtons(repeatIcon, shuffleIcon, isFavorite)
                )
        }
    }

    private fun handleSkipPrevious(player: Player) {
        val repeat = player.repeatMode
        val pos = player.currentPosition
        if (repeat == Player.REPEAT_MODE_OFF || repeat == Player.REPEAT_MODE_ONE) {
            if (pos > 5_000) {
                player.seekTo(0)
            } else {
                if (!player.hasPreviousMediaItem())
                    player.seekToDefaultPosition(player.mediaItemCount - 1)
                else player.seekToPrevious()
            }
        } else {
            // REPEAT_MODE_ALL
            if (pos > 5_000) player.seekTo(0) else player.seekToPrevious()
        }
        player.currentMediaItem?.let {
            if (::appPref.isInitialized)
                AppPref.updateMediaItem(it.itemKey(), appPref)
        }
    }

    private fun handleSkipNext(player: Player) {
        var priorityQueue = false
        if (::appPref.isInitialized) player.currentMediaItem?.let {
            val savedIndex = AppPref.getInitSavedIndex(appPref)
            val trashedItemKey = AppPref.getInitItemToTrash(appPref)
            val priorityList = AppPref.getInitialPriorityQueue(appPref).toMutableList()
            val result = onCheckPriorityQueue(player, it, savedIndex, trashedItemKey, priorityList)
            priorityQueue = result is PriorityQueue.Sought
        }
        if (priorityQueue) return
        val repeat = player.repeatMode
        if (repeat == Player.REPEAT_MODE_OFF || repeat == Player.REPEAT_MODE_ONE) {
            if (!player.hasNextMediaItem()) {
                if (player.shuffleModeEnabled) {
                    val songCount = player.mediaItemCount.takeIf { it > 0 } ?: return
                    val index = (0 until songCount).random().takeIf {
                        it != player.currentMediaItemIndex
                    } ?: 0
                    player.seekToDefaultPosition(index)
                } else player.seekToDefaultPosition(0)
            } else {
                player.seekToNext()
            }
        } else {
            // REPEAT_MODE_ALL
            player.seekToNext()
        }
        player.currentMediaItem?.let {
            if (::appPref.isInitialized)
                AppPref.updateMediaItem(it.itemKey(), appPref)
        }
    }

    private fun getAppPrefs() = this.applicationContext.getSharedPreferences(
        PREFERENCES, MODE_PRIVATE
    )

    private fun getAppStrings(): AppStrings {
        val language = AppPref.getLanguage(appPref) ?: Locale.getDefault().language
        val locale = Locale.forLanguageTag(language)
        val config = Configuration(this.resources.configuration)
        config.setLocale(locale)
        val new = createConfigurationContext(config)
        return AppStrings(
            unknownArtist = new.getString(R.string.unknown_artist),
            new.getString(R.string.Unknown)
        )
    }


    private suspend fun getInitRepeatMode(context: Context) = try {
        context.dataStore.data.map { preferences ->
            preferences[REPEAT_MODE] ?: Player.REPEAT_MODE_OFF
        }.first()
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get repeat mode: $e")
        Player.REPEAT_MODE_OFF
    }

    private suspend fun getInitShuffleEnabled(context: Context) = try {
        context.dataStore.data.map { preferences -> preferences[SHUFFLE_ENABLED] == true }.first()
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get repeat mode: $e")
        false
    }

    private fun getInitButtonLayout(appPref: SharedPreferences) = try {
        appPref.getString(BUTTON_LAYOUT, null)?.let {
            Json.decodeFromString(PlayerControls.serializer(), it)
        } ?: PlayerControls.Default
    } catch (_: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get button layout")
        PlayerControls.Default
    }


    private fun forwardingPlayer(player: Player) = object : ForwardingSimpleBasePlayer(player) {
        override fun handleSeek(
            mediaItemIndex: Int, positionMs: Long, seekCommand: Int
        ): ListenableFuture<*> {
            return when (seekCommand) {
                COMMAND_SEEK_TO_NEXT -> {
                    handleSkipNext(player)
                    Futures.immediateFuture(null)
                }

                COMMAND_SEEK_TO_PREVIOUS -> {
                    handleSkipPrevious(player)
                    Futures.immediateFuture(null)
                }

                COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> {
                    handler.removeCallbacks(playbackRunnable)
                    handler.post(playbackRunnable)
                    super.handleSeek(mediaItemIndex, positionMs, seekCommand)
                }

                else -> super.handleSeek(mediaItemIndex, positionMs, seekCommand)
            }
        }
    }

    private suspend fun setMediaItems(
        context: Context, appPref: SharedPreferences, session: MediaSession, scope: CoroutineScope
    ): Boolean {
        val isFavorite = try {
            val ids = AppPref.getQueue(context).first()
            val startPosition = AppPref.getLastPosition(appPref)
            val dao = getDatabase(context, scope).audioDao()
            val queue = dao.getInitialQueue(ids).toMediaItems(appStrings)
            val trashedItemKey = AppPref.getInitItemToTrash(appPref)
            val currentIndex = AppPref.getInitCurrentIndex(appPref)
            val trashedItem = dao.getTrashedItemOrNull(trashedItemKey)
            val itemsByKey = queue.associateBy { it.itemKey() }
            val orderedQueue = ids.mapNotNull { id -> itemsByKey[id] }.toMutableList()
            val itemKey = AppPref.getInitialMediaKey(appPref)
            val first =
                orderedQueue.find { it.itemKey() == itemKey }
                    ?: dao.getSongWithPls(itemKey).first()?.song?.toMediaItem(appStrings)
            if (orderedQueue.isEmpty() && first != null) orderedQueue.add(first)
            val songIds = orderedQueue.map { it.itemKey() }
            AppPref.updateQueue(context, songIds)
            if (orderedQueue.isEmpty() || session.player.mediaItemCount > 0) return false

            val priorityItemExists =
                currentIndex > C.INDEX_UNSET && trashedItemKey == itemKey && trashedItem != null
            val index = currentIndex.takeIf { priorityItemExists } ?: orderedQueue.indexOf(first)
                .takeIf { it >= 0 } ?: 0
            if (priorityItemExists) {
                orderedQueue.add(index, trashedItem.toMediaItem(appStrings))
                XandyWidget().updateMediaKey(this, trashedItem.id)
            } else XandyWidget().updateMediaKey(this, itemKey)
            Controller.setInitialQueue(session.player, orderedQueue, index, startPosition)
        } catch (_: Exception) {
            Log.e(XANDY_CLOUD, "Failed to retrieve media session contents")
            false
        }
        return isFavorite
    }

    private fun audioOffloadBuilder(): AudioOffloadPreferences {
        val offloadingEnabled = appPref.getBoolean(OFFLOADING_ENABLED, false)
        return AudioOffloadPreferences.Builder()
            .setAudioOffloadMode(
                if (offloadingEnabled) AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
                else AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
            )
            .setIsSpeedChangeSupportRequired(true)
            .setIsGaplessSupportRequired(true)
            .build()
    }

    private fun loadControlBuilder(): DefaultLoadControl {
        val loadControlSettings = try {
            appPref.getString(LOAD_CONTROL, null)?.let {
                Json.decodeFromString<LoadControl>(it)
            } ?: LoadControl.Default
        } catch (_: Exception) {
            LoadControl.Default
        }
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                // minBufferMs
                loadControlSettings.minBuffer(),
                // maxBufferMs
                loadControlSettings.maxBuffer(),
                // bufferForPlaybackMs
                loadControlSettings.bufferForPlayback(),
                // bufferForPlaybackAfterRebufferMs
                loadControlSettings.bufferForPlaybackAfterRebuffer()
            )
            .build()
    }

    private fun audioAttributes() = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    private fun buildExpoPlayer(
        context: Context, loadControl: DefaultLoadControl,
        audioOffloadPreferences: AudioOffloadPreferences, audioAttributes: AudioAttributes,
        playbackSpeed: Float, silenceSkipEnabled: Boolean,
        renderersFactory: DefaultRenderersFactory = DefaultRenderersFactory(context)
    ) = ExoPlayer.Builder(context, renderersFactory)
        .setHandleAudioBecomingNoisy(true)
        .setLoadControl(loadControl)
        .setAudioAttributes(audioAttributes, true)
        .setSkipSilenceEnabled(silenceSkipEnabled)
        .build().apply {
            setPlaybackSpeed(playbackSpeed)
            playWhenReady = true
            trackSelectionParameters = this.trackSelectionParameters
                .buildUpon()
                .setAudioOffloadPreferences(audioOffloadPreferences)
                .build()

        }


    private fun audioSink(
        context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean
    ): DefaultAudioSink {
        val enableAudioPositionFix = appPref.getBoolean(FIX_AUDIO_POSITION, true)
        return DefaultAudioSink.Builder(context)
            .setAudioTrackBufferSizeProvider(
                DefaultAudioTrackBufferSizeProvider.Builder()
                    .setOffloadBufferDurationUs(50_000_000)
                    .setPassthroughBufferDurationUs(250_000)
                    .build()
            )
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableOnAudioPositionAdvancingFix(enableAudioPositionFix)
            .build()
    }

    private fun getRenderers(context: Context) =
        object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink? {
                return audioSink(context, enableFloatOutput, enableAudioTrackPlaybackParams)
            }
        }
}