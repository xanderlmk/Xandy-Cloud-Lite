package com.xandy.lite.models.media.player

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val PLAYER_CONTROLS = "PlayerControls"
private const val XC_COMMAND_BUTTON = "XCCommandButton"


object ButtonType {
    const val BUTTON_LAYOUT = "button_layout"
    const val COMMAND_REPEAT = "Cycle_Repeat"
    const val COMMAND_SHUFFLE = "Shuffle_Songs"
    const val COMMAND_FAST_FORWARD = "Fast_Forward"
    const val COMMAND_REWIND = "Rewind_Xandy"
    const val COMMAND_CHANGE_BUTTON = "Change_Button"
    const val COMMAND_CHANGE_LAYOUT = "Change_Button_Layout"
}


@Serializable
@SerialName(PLAYER_CONTROLS)
sealed class PlayerControls {
    @Serializable
    @SerialName("$PLAYER_CONTROLS.DEFAULT")
    data object Default : PlayerControls()

    @Serializable
    @SerialName("$PLAYER_CONTROLS.REVERSED")
    data object Reversed : PlayerControls()

    @Serializable
    @SerialName("$PLAYER_CONTROLS.WITH_SETTINGS")
    data class WithSettings(val button: XCCommandButton = XCCommandButton.Repeat) : PlayerControls()
}


@Serializable
@SerialName(XC_COMMAND_BUTTON)
enum class XCCommandButton {
    FastForward, Rewind, Repeat, Shuffle;

    fun switchButton() = when (this) {
        FastForward -> Rewind
        Rewind -> Repeat
        Repeat -> Shuffle
        Shuffle -> FastForward
    }
}


