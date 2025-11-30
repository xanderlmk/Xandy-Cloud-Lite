package com.xandy.lite.models.media.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingSimpleBasePlayer
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
import com.xandy.lite.controllers.setInitialQueue
import com.xandy.lite.db.XandyDatabase.Companion.getDatabase
import com.xandy.lite.db.tables.toMediaItems
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.application.dataStore
import com.xandy.lite.models.itemKey
import com.xandy.lite.models.media.player.PlaybackService.Companion.COMMAND_CHANGE_BUTTON
import com.xandy.lite.models.ui.drawableResUri
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
        private const val COMMAND_PAUSE = "Pause_Song"
        private const val COMMAND_PLAY = "Play_Song"
        private const val COMMAND_REPEAT = ButtonType.COMMAND_REPEAT
        private const val COMMAND_SHUFFLE = ButtonType.COMMAND_SHUFFLE
        private const val PREFERENCES = "preferences"
        private const val CURRENT_MI = "current_media_item"
        private const val LAST_POSITION = "last_position"
        private val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        private val REPEAT_MODE = intPreferencesKey("repeat_mode")
        private val QUEUE_SET = stringPreferencesKey("queue_json")
        private const val OFFLOADING_ENABLED = "Offloading.Enabled"
        private const val LOAD_CONTROL = "LoadControl"
        private const val FIX_AUDIO_POSITION = "FixAudioPosition"
        private const val CHANGED_PLAYBACK_SETTINGS = "playback_settings_changed"
        private const val BUTTON_LAYOUT = ButtonType.BUTTON_LAYOUT
        private const val COMMAND_FAST_FORWARD = ButtonType.COMMAND_FAST_FORWARD
        private const val COMMAND_REWIND = ButtonType.COMMAND_REWIND
        private const val COMMAND_CHANGE_BUTTON = ButtonType.COMMAND_CHANGE_BUTTON
        private const val COMMAND_CHANGE_LAYOUT = ButtonType.COMMAND_CHANGE_LAYOUT
        private val changeIcon = R.drawable.outline_change_circle
    }

    private val prevButton = commandBuilder(
        CommandButton.ICON_PREVIOUS, "Previous",
        COMMAND_SEEK_TO_PREV, CommandButton.SLOT_BACK
    )
    private val playButton = commandBuilder(
        CommandButton.ICON_PLAY, "Play",
        COMMAND_PLAY, CommandButton.SLOT_CENTRAL
    )
    private val pauseButton = commandBuilder(
        CommandButton.ICON_PAUSE, "Pause",
        COMMAND_PAUSE, CommandButton.SLOT_CENTRAL
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

    private fun createChangeButton(uri: Uri) =
        CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName("Change")
            .setSessionCommand(SessionCommand(COMMAND_CHANGE_BUTTON, Bundle()))
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .setCustomIconResId(changeIcon)
            .setIconUri(uri)
            .build()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        appPref = getAppPrefs()
        changeButton = createChangeButton(this.drawableResUri(changeIcon))
        val renderersFactory = getRenderers(this)
        val loadControl = loadControlBuilder()
        val audioOffloadPreferences = audioOffloadBuilder()
        val player = buildExpoPlayer(
            this, loadControl, audioOffloadPreferences, audioAttributes(), renderersFactory
        )
        val forwardingPlayer = forwardingPlayer(player)
        val buttonLayout = listOf(
            prevButton, playButton, pauseButton, nextButton, repeatButton, shuffleButton
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
        /**
         *  Override the player as well as the notification buttons
         */
        _mediaSession =
            MediaSession.Builder(this, forwardingPlayer)
                .setCallback(SessionCallback())
                .setSessionActivity(sessionActivityPendingIntent)
                .setMediaButtonPreferences(buttonLayout)
                .build()
        svcScope.launch {
            try {
                val session = _mediaSession ?: return@launch
                val repeatMode = getInitRepeatMode(this@PlaybackService)
                val shuffleEnabled = getInitShuffleEnabled(this@PlaybackService)
                val repeatIcon = getRepeatIconButton(repeatMode)
                val shuffleIcon =
                    if (shuffleEnabled) CommandButton.ICON_SHUFFLE_ON
                    else CommandButton.ICON_SHUFFLE_OFF

                val playerControls = getInitButtonLayout(appPref)
                val (newSlot1Button, newSlot2Button) =
                    getSlotButtons(playerControls, repeatIcon, shuffleIcon)

                setButtonLayout(playerControls, session, newSlot1Button, newSlot2Button)
                session.player.repeatMode = repeatMode
                session.player.shuffleModeEnabled = shuffleEnabled
                setMediaItems(this@PlaybackService, appPref, session, this)
            } catch (_: Exception) {
                Log.e(XANDY_CLOUD, "Failed to retrieve media session contents")
            }
        }
    }

    override fun onGetSession(controllerInfo: ControllerInfo): MediaSession? {
        val session = _mediaSession ?: return null
        appPref = this.applicationContext.getSharedPreferences(PREFERENCES, MODE_PRIVATE)

        try {
            val changedSettings = appPref.getBoolean(CHANGED_PLAYBACK_SETTINGS, false)
            if (changedSettings) {
                val loadControl = loadControlBuilder()
                val audioOffloadPreferences = audioOffloadBuilder()
                val renderersFactory = getRenderers(this)
                val newPlayer =
                    buildExpoPlayer(
                        this, loadControl, audioOffloadPreferences,
                        audioAttributes(), renderersFactory
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
                    updateMediaItem(it.itemKey(), appPref)
                    updateLastPosition(position, appPref)
                }
            }
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
            pauseButton.sessionCommand?.let(availableSessionCommands::add)
            playButton.sessionCommand?.let(availableSessionCommands::add)
            repeatButton.sessionCommand?.let(availableSessionCommands::add)
            shuffleButton.sessionCommand?.let(availableSessionCommands::add)
            forwardButton.sessionCommand?.let(availableSessionCommands::add)
            rewindButton.sessionCommand?.let(availableSessionCommands::add)
            changeButton.sessionCommand?.let(availableSessionCommands::add)
            availableSessionCommands.add(SessionCommand(COMMAND_CHANGE_LAYOUT, Bundle()))
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
                session.player.currentMediaItem?.let {
                    val position = session.player.currentPosition
                    val key = it.itemKey()
                    if (::appPref.isInitialized) {
                        updateMediaItem(key, appPref)
                        updateLastPosition(position, appPref)
                    }
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
            changeButton = createChangeButton(this@PlaybackService.drawableResUri(changeIcon))
            var playerControls = getInitButtonLayout(appPref)
            when (customCommand.customAction) {
                COMMAND_SEEK_TO_NEXT -> handleSkipNext(session.player)
                COMMAND_SEEK_TO_PREV -> handleSkipPrevious(session.player)
                COMMAND_PAUSE -> session.player.pause()
                COMMAND_PLAY -> session.player.play()
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
                COMMAND_CHANGE_LAYOUT -> {
                    val repeatIcon = getRepeatIconButton(session.player.repeatMode)
                    val shuffleIcon =
                        if (session.player.shuffleModeEnabled) CommandButton.ICON_SHUFFLE_ON
                        else CommandButton.ICON_SHUFFLE_OFF
                    val (newSlot1Button, newSlot2Button) =
                        getSlotButtons(playerControls, repeatIcon, shuffleIcon)

                    setButtonLayout(playerControls, session, newSlot1Button, newSlot2Button)
                }

                COMMAND_CHANGE_BUTTON -> {
                    if (playerControls is PlayerControls.WithSettings) {
                        val button = playerControls.button
                        val new = playerControls.copy(button = button.switchButton())
                        updateButtonLayout(new, appPref)
                    }
                }
            }
            playerControls = getInitButtonLayout(appPref)
            val repeatIcon = getRepeatIconButton(session.player.repeatMode)
            val shuffleIcon =
                if (session.player.shuffleModeEnabled) CommandButton.ICON_SHUFFLE_ON
                else CommandButton.ICON_SHUFFLE_OFF
            val (newSlot1Button, newSlot2Button) =
                getSlotButtons(playerControls, repeatIcon, shuffleIcon)

            setButtonLayout(playerControls, session, newSlot1Button, newSlot2Button)
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        private fun updateButtonLayout(
            new: PlayerControls.WithSettings,
            appPref: SharedPreferences
        ) =
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
                    val ids = applicationContext.dataStore.data.map { preferences ->
                        try {
                            preferences[QUEUE_SET]?.let { Json.decodeFromString<List<String>>(it) }
                        } catch (_: Exception) {
                            emptyList()
                        } ?: emptyList()
                    }.first()

                    if (ids.isEmpty()) {
                        settable.set(
                            MediaItemsWithStartPosition(emptyList(), C.INDEX_UNSET, C.TIME_UNSET)
                        )
                        return@launch
                    }
                    val dao = getDatabase(applicationContext, this).audioDao()
                    val audioFiles = dao.getInitialQueue(ids)
                    val ordered = ids.mapNotNull { id -> audioFiles.find { it.id == id } }
                    val mediaItems = ordered.toMediaItems()
                    val itemKey = getInitialMediaKey(appPref)
                    val first = ordered.find { it.id == itemKey }
                    val index = ordered.indexOf(first).takeIf { it >= 0 } ?: 0
                    val startPosition = getLastPosition(appPref)
                    val result = MediaItemsWithStartPosition(
                        mediaItems, index.coerceIn(mediaItems.indices), startPosition
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
        playerControls: PlayerControls, repeatIcon: Int, shuffleIcon: Int
    ): Pair<CommandButton, CommandButton> {
        val newSlot1Button = if (playerControls is PlayerControls.WithSettings)
            playerControls.toButton(repeatIcon, shuffleIcon) else commandBuilder(
            repeatIcon, "Repeat", COMMAND_REPEAT, CommandButton.SLOT_OVERFLOW
        )
        val newSlot2Button = if (playerControls is PlayerControls.WithSettings)
            changeButton else commandBuilder(
            shuffleIcon, "Shuffle", COMMAND_SHUFFLE, CommandButton.SLOT_OVERFLOW
        )
        return Pair(newSlot1Button, newSlot2Button)
    }

    private fun PlayerControls.WithSettings.toButton(
        repeatIcon: Int, shuffleIcon: Int
    ): CommandButton = when (this.button) {
        XCCommandButton.FastForward -> forwardButton
        XCCommandButton.Rewind -> rewindButton
        XCCommandButton.Repeat -> getRepeatButton(repeatIcon)
        XCCommandButton.Shuffle -> getShuffleButton(shuffleIcon)
    }

    private fun getRepeatButton(repeatIcon: Int) = commandBuilder(
        repeatIcon, "Repeat", COMMAND_REPEAT, CommandButton.SLOT_OVERFLOW
    )

    private fun getShuffleButton(shuffleIcon: Int) = commandBuilder(
        shuffleIcon, "Shuffle", COMMAND_SHUFFLE, CommandButton.SLOT_OVERFLOW
    )

    private fun getRepeatIconButton(repeatMode: Int) = when (repeatMode) {
        Player.REPEAT_MODE_OFF -> CommandButton.ICON_REPEAT_OFF
        Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE
        Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
        else -> CommandButton.ICON_REPEAT_OFF
    }

    private fun setButtonLayout(
        playerControls: PlayerControls, session: MediaSession,
        newSlot1Button: CommandButton, newSlot2Button: CommandButton
    ) {
        when (playerControls) {
            PlayerControls.Default -> session.setMediaButtonPreferences(
                listOf(
                    prevButton, playButton, pauseButton, nextButton,
                    newSlot1Button, newSlot2Button
                )
            )

            PlayerControls.Reversed -> session.setMediaButtonPreferences(
                listOf(
                    prevButton, playButton, pauseButton, nextButton,
                    newSlot2Button, newSlot1Button
                )
            )

            is PlayerControls.WithSettings -> session.setMediaButtonPreferences(
                listOf(
                    prevButton, playButton, pauseButton, nextButton,
                    newSlot1Button, newSlot2Button
                )
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
                updateMediaItem(it.itemKey(), appPref)
        }
    }

    private fun handleSkipNext(player: Player) {
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
                updateMediaItem(it.itemKey(), appPref)
        }
    }

    private fun getAppPrefs() = this.applicationContext.getSharedPreferences(
        PREFERENCES, MODE_PRIVATE
    )

    private suspend fun getInitRepeatMode(context: Context) = try {
        context.dataStore.data.map { preferences ->
            preferences[REPEAT_MODE] ?: Player.REPEAT_MODE_OFF
        }.first()
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get repeat mode: $e")
        Player.REPEAT_MODE_OFF
    }


    private suspend fun getInitShuffleEnabled(context: Context) = try {
        context.dataStore.data.map { preferences ->
            preferences[SHUFFLE_ENABLED] == true
        }.first()
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

    private fun getInitialMediaKey(appPref: SharedPreferences) = try {
        appPref.getString(CURRENT_MI, "") ?: ""
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get repeat mode: $e")
        ""
    }

    private fun getLastPosition(appPref: SharedPreferences) = try {
        appPref.getLong(LAST_POSITION, 0L)
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get repeat mode: $e")
        0L
    }

    private fun updateMediaItem(key: String, appPref: SharedPreferences) = try {
        appPref.edit { putString(CURRENT_MI, key) }
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to update media key: $e")
    }

    private fun updateLastPosition(position: Long, appPref: SharedPreferences) = try {
        appPref.edit { putLong(LAST_POSITION, position) }
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to update last position: $e")
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

                else -> super.handleSeek(mediaItemIndex, positionMs, seekCommand)
            }
        }
    }

    private suspend fun setMediaItems(
        context: Context, appPref: SharedPreferences, session: MediaSession, scope: CoroutineScope
    ) {
        val ids = context.dataStore.data.map { preferences ->
            try {
                preferences[QUEUE_SET]?.let { Json.decodeFromString<List<String>>(it) }
            } catch (_: Exception) {
                emptyList()
            } ?: emptyList()
        }.first()
        val startPosition = getLastPosition(appPref)
        val dao = getDatabase(context, scope).audioDao()
        val queue = dao.getInitialQueue(ids).toMediaItems()

        if (queue.isEmpty() || session.player.mediaItemCount > 0) return
        val itemsByKey = queue.associateBy { it.itemKey() }
        val orderedQueue = ids.mapNotNull { id -> itemsByKey[id] }
        val itemKey = getInitialMediaKey(appPref)
        val first = orderedQueue.find { it.itemKey() == itemKey }
        val index = orderedQueue.indexOf(first).takeIf { it >= 0 } ?: 0
        setInitialQueue(session.player, orderedQueue, index, startPosition)
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
        renderersFactory: DefaultRenderersFactory = DefaultRenderersFactory(context)
    ) = ExoPlayer.Builder(context, renderersFactory)
        .setHandleAudioBecomingNoisy(true)
        .setLoadControl(loadControl)
        .setAudioAttributes(audioAttributes, true)
        .build().apply {
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