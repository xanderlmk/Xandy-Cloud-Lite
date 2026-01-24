package com.xandy.lite.models.application

import com.xandy.lite.models.Theme
import com.xandy.lite.models.media.player.LoadControl
import com.xandy.lite.models.media.player.PlayerControls
import com.xandy.lite.models.ui.XCLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PreferencesManager(
    private val prefRepository: PrefRepository, private val scope: CoroutineScope
) {
    val theme = prefRepository.theme.stateIn(
        scope = scope, started = SharingStarted.Eagerly, initialValue = Theme.Default
    )
    val offloadingEnabled = prefRepository.offloadingEnabled
    val loadControl = prefRepository.loadControl
    val fixPositionEnabled = prefRepository.fixPositionEnabled
    val playerControls = prefRepository.playerControls
    val playbackSpeed = prefRepository.playbackSpeed
    val silenceSkippedEnabled = prefRepository.silenceSkippedEnabled
    val selectedLanguage = prefRepository.selectedLanguage
    fun updatePlayerControls(new: PlayerControls, onSendMsg: () -> Unit) =
        prefRepository.updatePlayerControls(new, onSendMsg)

    suspend fun changeTheme(new: Theme) = prefRepository.changeTheme(new)
    fun toggleOffloading(enabled: Boolean) = prefRepository.toggleOffloading(enabled)
    fun changeLoadControl(new: LoadControl) = prefRepository.changeLoadControl(new)
    fun togglePositionFix(enabled: Boolean) = prefRepository.togglePositionFix(enabled)
    fun updatePlaybackSpeed(new: Float, onSetSpeed: () -> Unit) =
        prefRepository.updatePlaybackSpeed(new, onSetSpeed)

    fun toggleSilenceSkipped() =
        prefRepository.updateSilencedSkippedEnabled()

    fun updateLanguage(new: XCLanguage, onRecreate: () -> Unit) = scope.launch {
        prefRepository.updateSelectedLanguage(new, onRecreate)
    }

    private val _acknowledgeRecreate = MutableStateFlow(prefRepository.getRestartState())
    val acknowledgeRecreate = _acknowledgeRecreate.asStateFlow()
    fun updateAcknowledgement(new: Boolean) =
        _acknowledgeRecreate.update { prefRepository.updateRestartState(new);new }
}
