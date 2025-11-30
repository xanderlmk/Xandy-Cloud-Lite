package com.xandy.lite.models.application

import com.xandy.lite.models.Theme
import com.xandy.lite.models.media.player.LoadControl
import com.xandy.lite.models.media.player.PlayerControls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class PreferencesManager(
    private val prefRepository: PrefRepository, scope: CoroutineScope
) {
    val theme = prefRepository.theme.stateIn(
        scope = scope, started = SharingStarted.Eagerly, initialValue = Theme.Default
    )
    val offloadingEnabled = prefRepository.offloadingEnabled
    val loadControl = prefRepository.loadControl
    val fixPositionEnabled = prefRepository.fixPositionEnabled
    val playerControls = prefRepository.playerControls
    fun updatePlayerControls(new: PlayerControls, onSendMsg: () -> Unit) =
        prefRepository.updatePlayerControls(new, onSendMsg)

    suspend fun changeTheme(new: Theme) = prefRepository.changeTheme(new)
    fun toggleOffloading(enabled: Boolean) = prefRepository.toggleOffloading(enabled)
    fun changeLoadControl(new: LoadControl) = prefRepository.changeLoadControl(new)
    fun togglePositionFix(enabled: Boolean) = prefRepository.togglePositionFix(enabled)
}
