package com.xandy.lite.views.settings

import android.os.Bundle
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import com.xandy.lite.models.XCToast
import com.xandy.lite.models.application.PreferencesManager
import com.xandy.lite.models.media.player.ButtonType
import com.xandy.lite.models.media.player.LoadControl
import com.xandy.lite.models.media.player.PlayerControls
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.RestartPlayer
import com.xandy.lite.ui.GetUIStyle
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.ColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings


private const val COMMAND_CHANGE_LAYOUT = ButtonType.COMMAND_CHANGE_LAYOUT

@Composable
fun SettingsView(
    controller: MediaController?, onRestartPlayer: () -> Unit,
    getUIStyle: GetUIStyle, pm: PreferencesManager
) {
    val scope = rememberCoroutineScope()
    val ci = ContentIcons(getUIStyle)
    val offloadingEnabled by pm.offloadingEnabled.collectAsStateWithLifecycle()
    val loadControl by pm.loadControl.collectAsStateWithLifecycle()
    val positionFixEnabled by pm.fixPositionEnabled.collectAsStateWithLifecycle()
    val playerControls by pm.playerControls.collectAsStateWithLifecycle()
    val offloadingText = if (offloadingEnabled) "Offloading enabled" else "Offloading disabled"
    val positionFixText =
        if (positionFixEnabled) "Position fixing enabled" else "Position fixing disabled"
    var showOffloadingHint by rememberSaveable { mutableStateOf(false) }
    var restartPlayerDialog by rememberSaveable { mutableStateOf(false) }
    val state = rememberScrollState()
    ColumnScrollbar(
        state = state,
        modifier = Modifier.fillMaxSize(),
        settings = ScrollbarSettings(
            thumbSelectedColor = getUIStyle.selectedThumbColor(),
            thumbUnselectedColor = getUIStyle.unSelectedThumbColor()
        )
    ) {
        if (restartPlayerDialog)
            RestartPlayer(
                onDismiss = { restartPlayerDialog = false },
                onSubmit = { onRestartPlayer(); restartPlayerDialog = false },
                getUIStyle = getUIStyle
            )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ThemeSettings(getUIStyle, pm) { scope.launch { pm.changeTheme(it) } }
            ColumnContentWithBorder(getUIStyle, "Player options") {
                Text(
                    text = "Offloading", textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 20.sp
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = offloadingText, style = MaterialTheme.typography.titleMedium)
                    ci.ToggleIconButton(
                        onClick = {
                            scope.launch {
                                pm.toggleOffloading(!offloadingEnabled)
                                restartPlayerDialog = true
                            }
                        },
                        isOn = offloadingEnabled
                    )
                }
                Text(
                    text =
                        if (showOffloadingHint) "If you want to save battery, enable offloading, but if it causes issues, disable it."
                        else "Why enable/disable offloading?",
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable { showOffloadingHint = !showOffloadingHint }
                )
                HorizontalDivider(modifier = Modifier.fillMaxWidth(.90f), thickness = 1.5.dp)
                if (offloadingEnabled) {
                    Text(
                        text = "Audio Sink", textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 20.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = positionFixText, style = MaterialTheme.typography.titleMedium)
                        ci.ToggleIconButton(
                            onClick = {
                                scope.launch {
                                    pm.togglePositionFix(!positionFixEnabled)
                                    restartPlayerDialog = true
                                }
                            },
                            isOn = positionFixEnabled
                        )
                    }
                    HorizontalDivider()
                }
                Text(
                    text = "Load Control", textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 20.sp
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = loadControl is LoadControl.Default,
                        onClick = {
                            scope.launch {
                                if (loadControl is LoadControl.Default) return@launch
                                pm.changeLoadControl(LoadControl.Default)
                                restartPlayerDialog = true
                            }
                        },
                        label = { Text("Default") }
                    )
                    FilterChip(
                        selected = loadControl is LoadControl.LowLatency,
                        onClick = {
                            scope.launch {
                                if (loadControl is LoadControl.LowLatency) return@launch
                                pm.changeLoadControl(LoadControl.LowLatency)
                                restartPlayerDialog = true
                            }
                        },
                        label = { Text("Low Latency") }
                    )
                    FilterChip(
                        selected = loadControl is LoadControl.Balanced,
                        onClick = {
                            scope.launch {
                                if (loadControl is LoadControl.Balanced) return@launch
                                pm.changeLoadControl(LoadControl.Balanced)
                                restartPlayerDialog = true
                            }
                        },
                        label = { Text("Balanced") }
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = loadControl is LoadControl.HighStability,
                        onClick = {
                            scope.launch {
                                if (loadControl is LoadControl.HighStability) return@launch
                                pm.changeLoadControl(LoadControl.HighStability)
                                restartPlayerDialog = true
                            }
                        },
                        label = { Text("High Stability") }
                    )
                }

            }
            ColumnContentWithBorder(getUIStyle, "Notification Button Layout") {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = playerControls is PlayerControls.Default,
                        onClick = {
                            scope.launch {
                                if (playerControls is PlayerControls.Default) return@launch
                                pm.updatePlayerControls(PlayerControls.Default) {
                                    controller?.sendCustomCommand(
                                        SessionCommand(COMMAND_CHANGE_LAYOUT, Bundle()), Bundle()
                                    )
                                }
                            }
                        },
                        label = { Text("Default") }
                    )
                    FilterChip(
                        selected = playerControls is PlayerControls.Reversed,
                        onClick = {
                            scope.launch {
                                if (playerControls is PlayerControls.Reversed) return@launch
                                pm.updatePlayerControls(PlayerControls.Reversed) {
                                    controller?.sendCustomCommand(
                                        SessionCommand(COMMAND_CHANGE_LAYOUT, Bundle()), Bundle()
                                    )
                                }
                            }
                        },
                        label = { Text("Reversed") }
                    )
                }
                FilterChip(
                    selected = playerControls is PlayerControls.WithSettings,
                    onClick = {
                        scope.launch {
                            if (playerControls is PlayerControls.WithSettings) return@launch
                            pm.updatePlayerControls(PlayerControls.WithSettings()) {
                                controller?.sendCustomCommand(
                                    SessionCommand(COMMAND_CHANGE_LAYOUT, Bundle()), Bundle()
                                )
                            }
                        }
                    },
                    label = { Text("With Settings") }
                )
            }
        }
    }
}

@Composable
private fun ColumnContentWithBorder(
    getUIStyle: GetUIStyle, text: String, modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .border(
                width = 2.dp, shape = RoundedCornerShape(12.dp),
                color = if (getUIStyle.getIsDarkTheme()) Color.Gray else Color.Black
            )
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = getUIStyle.themedOnContainerColor(),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth()
                .clickable { expanded = !expanded }
        )
        if (expanded) {
            HorizontalDivider(modifier = Modifier.fillMaxWidth(0.95f), thickness = 2.dp)
            content()
        }
    }
}