package com.xandy.lite.models

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
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
import androidx.media3.exoplayer.ExoPlayer
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
import com.xandy.lite.SongViewActivity
import com.xandy.lite.db.XandyDatabase.Companion.getDatabase
import com.xandy.lite.db.tables.toMediaItems
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.application.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json


@androidx.annotation.OptIn(UnstableApi::class)
private fun commandBuilder(icon: Int, name: String, action: String, slot: Int): CommandButton =
    CommandButton.Builder(icon)
        .setDisplayName(name)
        .setSessionCommand(SessionCommand(action, Bundle()))
        .setSlots(slot)
        .build()


@UnstableApi
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var appPrefs: SharedPreferences
    private var headsetClickCount = 0
    private var headsetClickJob: Job? = null
    private val headsetClickWindowMs = 400L
    private val svcScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    companion object {
        private const val COMMAND_SEEK_TO_NEXT = "Next_Song"
        private const val COMMAND_SEEK_TO_PREV = "Prev_Song"
        private const val COMMAND_PAUSE = "Pause_Song"
        private const val COMMAND_PLAY = "Play_Song"
        private const val COMMAND_REPEAT = "Cycle_Repeat"
        private const val COMMAND_SHUFFLE = "Shuffle_Songs"
        private const val PREFERENCES = "preferences"
        private val CURRENT_MI = stringPreferencesKey("current_media_item")
        private const val LAST_POSITION = "last_position"
        private val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        private val REPEAT_MODE = intPreferencesKey("repeat_mode")
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

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                // Pause when headphones/Bluetooth disconnect
                player.playWhenReady = false
            }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        appPrefs = this.applicationContext.getSharedPreferences(PREFERENCES, MODE_PRIVATE)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val isPixel7Pro = Build.MODEL.contains("Pixel 7 Pro", ignoreCase = true)
        Log.i(XANDY_CLOUD, "Is Pixel phone: $isPixel7Pro")
        val audioOffloadPreferences =
            AudioOffloadPreferences.Builder()
                .setAudioOffloadMode(
                    if (isPixel7Pro) AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
                    else AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
                )
                .setIsGaplessSupportRequired(true)
                .build()
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30_000,  // minBufferMs - 50_000 -> 30_000
                60_000,  // maxBufferMs
                1_500,   // bufferForPlaybackMs 1_000 -> 1_500
                3_000    // bufferForPlaybackAfterRebufferMs 2_000 -> 3_000
            )
            .build()
        player = ExoPlayer.Builder(this).setLoadControl(loadControl).build().apply {
            setAudioAttributes(audioAttributes, true)
            playWhenReady = true
            trackSelectionParameters = this.trackSelectionParameters
                .buildUpon()
                .setAudioOffloadPreferences(audioOffloadPreferences)
                .build()
        }

        val forwardingPlayer = object : ForwardingSimpleBasePlayer(player) {
            override fun handleSeek(
                mediaItemIndex: Int,
                positionMs: Long,
                seekCommand: Int
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
        mediaSession =
            MediaSession.Builder(this, forwardingPlayer)
                .setCallback(SessionCallback())
                .setSessionActivity(sessionActivityPendingIntent)
                .setMediaButtonPreferences(buttonLayout)
                .build()
        registerReceiver(
            noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        )
        svcScope.launch {
            val session = mediaSession ?: return@launch
            val repeatMode = setInitRepeatMode(this@PlaybackService)
            val shuffleEnabled = setInitShuffleEnabled(this@PlaybackService)
            val repeatIcon = when (repeatMode) {
                Player.REPEAT_MODE_OFF -> CommandButton.ICON_REPEAT_OFF
                Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE
                Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
                else -> CommandButton.ICON_REPEAT_OFF
            }
            val shuffleIcon =
                if (shuffleEnabled) CommandButton.ICON_SHUFFLE_ON
                else CommandButton.ICON_SHUFFLE_OFF

            val newRepeatButton = commandBuilder(
                repeatIcon, "Repeat", COMMAND_REPEAT, CommandButton.SLOT_OVERFLOW
            )
            val newShuffleButton = commandBuilder(
                shuffleIcon, "Shuffle", COMMAND_SHUFFLE, CommandButton.SLOT_OVERFLOW
            )
            session.setMediaButtonPreferences(
                listOf(
                    prevButton, playButton, pauseButton, nextButton,
                    newRepeatButton, newShuffleButton
                )
            )
            player.repeatMode = repeatMode
            player.shuffleModeEnabled = shuffleEnabled
        }
    }

    override fun onGetSession(controllerInfo: ControllerInfo): MediaSession? {
        val session = mediaSession ?: return null
        svcScope.launch {
            val repeatMode = setInitRepeatMode(this@PlaybackService)
            val shuffleEnabled = setInitShuffleEnabled(this@PlaybackService)
            session.player.repeatMode = repeatMode
            session.player.shuffleModeEnabled = shuffleEnabled
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
            player.currentMediaItem?.let {
                svcScope.launch { updateMediaItem(it.itemKey(), this@PlaybackService) }
                appPrefs.edit { putLong(LAST_POSITION, player.currentPosition) }
            }
            unregisterReceiver(noisyReceiver)
            mediaSession?.release()
            player.release()
            mediaSession = null
        } catch (e: Exception) {
            Log.e(XANDY_CLOUD, "Failed to release resources: ${e.printStackTrace()}")
        }
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
            return MediaSession.ConnectionResult.accept(
                availableSessionCommands.build(), connectionResult.availablePlayerCommands
            )
        }

        override fun onMediaButtonEvent(
            session: MediaSession, controllerInfo: ControllerInfo, intent: Intent
        ): Boolean {
            val keyEvent: KeyEvent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableExtra(KeyEvent::class.java.name)
            if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
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

                }
            }
            return super.onMediaButtonEvent(session, controllerInfo, intent)
        }

        override fun onCustomCommand(
            session: MediaSession, controller: ControllerInfo, customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            super.onCustomCommand(session, controller, customCommand, args)
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
            }
            val repeatIcon = when (session.player.repeatMode) {
                Player.REPEAT_MODE_OFF -> CommandButton.ICON_REPEAT_OFF
                Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE
                Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
                else -> CommandButton.ICON_REPEAT_OFF
            }
            val shuffleIcon =
                if (session.player.shuffleModeEnabled) CommandButton.ICON_SHUFFLE_ON
                else CommandButton.ICON_SHUFFLE_OFF

            val newRepeatButton = commandBuilder(
                repeatIcon, "Repeat", COMMAND_REPEAT, CommandButton.SLOT_OVERFLOW
            )
            val newShuffleButton = commandBuilder(
                shuffleIcon, "Shuffle", COMMAND_SHUFFLE, CommandButton.SLOT_OVERFLOW
            )
            session.setMediaButtonPreferences(
                listOf(
                    prevButton, playButton, pauseButton, nextButton,
                    newRepeatButton, newShuffleButton
                )
            )
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        private fun updateRepeatMode(mode: Int, context: Context) {
            svcScope.launch(Dispatchers.IO) {
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
            svcScope.launch(Dispatchers.IO) {
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
            mediaSession: MediaSession,
            controller: ControllerInfo
        ): ListenableFuture<MediaItemsWithStartPosition> {
            val settable = SettableFuture.create<MediaItemsWithStartPosition>()
            svcScope.launch {
                try {
                    // 1. Restore queue IDs from prefs
                    val queueJson = appPrefs.getString("queue", null)
                    val ids: List<String> = queueJson?.let {
                        Json.decodeFromString(ListSerializer(String.serializer()), it)
                    } ?: emptyList()

                    if (ids.isEmpty()) {
                        settable.set(
                            MediaItemsWithStartPosition(emptyList(), C.INDEX_UNSET, C.TIME_UNSET)
                        )
                        return@launch
                    }
                    val dao = getDatabase(applicationContext, this).audioDao()
                    val audioFiles = dao.getInitialQueue(ids)

                    val ordered = ids.mapNotNull { id ->
                        audioFiles.find { it.uri.toString() == id }
                    }
                    val mediaItems = ordered.toMediaItems()
                    val itemKey = getInitialMediaKey(applicationContext)
                    val first = ordered.find { it.uri.toString() == itemKey }
                    val index = ordered.indexOf(first).takeIf { it >= 0 } ?: 0
                    val startPosition = appPrefs.getLong(LAST_POSITION, 0L)

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
            svcScope.launch { updateMediaItem(it.itemKey(), this@PlaybackService) }
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
            svcScope.launch { updateMediaItem(it.itemKey(), this@PlaybackService) }
        }
    }

    private suspend fun setInitRepeatMode(context: Context) = try {
        context.dataStore.data.map { preferences ->
            preferences[REPEAT_MODE] ?: Player.REPEAT_MODE_OFF
        }.first()
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get repeat mode: $e")
        Player.REPEAT_MODE_OFF
    }


    private suspend fun setInitShuffleEnabled(context: Context) = try {
        context.dataStore.data.map { preferences ->
            preferences[SHUFFLE_ENABLED] == true
        }.first()
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get repeat mode: $e")
        false
    }

    private suspend fun getInitialMediaKey(context: Context) = try {
        context.dataStore.data.map { preferences ->
            preferences[CURRENT_MI] ?: ""
        }.first()
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to get repeat mode: $e")
        ""
    }

    private suspend fun updateMediaItem(key: String, context: Context) = try {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_MI] = key
        }
    } catch (e: Exception) {
        Log.e(XANDY_CLOUD, "Failed to update media key: $e")
    }
}