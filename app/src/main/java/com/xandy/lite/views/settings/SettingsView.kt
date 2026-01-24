@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.xandy.lite.views.settings

import android.content.ClipData
import android.content.ClipDescription
import android.os.Bundle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import com.xandy.lite.R
import com.xandy.lite.models.XCToast
import com.xandy.lite.models.application.PreferencesManager
import com.xandy.lite.models.media.player.ButtonType
import com.xandy.lite.models.media.player.ConfigCB
import com.xandy.lite.models.media.player.CustomCB
import com.xandy.lite.models.media.player.LoadControl
import com.xandy.lite.models.media.player.PlayerControls
import com.xandy.lite.models.media.player.XCCommandButton
import com.xandy.lite.models.media.player.toCustomCB
import com.xandy.lite.models.ui.XCLanguage
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.GetUIStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import my.nanihadesuka.compose.ColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings


private const val COMMAND_CHANGE_LAYOUT = ButtonType.COMMAND_CHANGE_LAYOUT

@Composable
fun SettingsView(
    controller: MediaController?, onRestartPlayer: () -> Unit, onRecreate: () -> Unit,
    getUIStyle: GetUIStyle, pm: PreferencesManager
) {
    val scope = rememberCoroutineScope()
    val ci = ContentIcons(getUIStyle)
    val offloadingEnabled by pm.offloadingEnabled.collectAsStateWithLifecycle()
    val loadControl by pm.loadControl.collectAsStateWithLifecycle()
    val positionFixEnabled by pm.fixPositionEnabled.collectAsStateWithLifecycle()
    val playerControls by pm.playerControls.collectAsStateWithLifecycle()
    val playbackSpeed by pm.playbackSpeed.collectAsStateWithLifecycle()
    val silencedSkippedEnabled by pm.silenceSkippedEnabled.collectAsStateWithLifecycle()
    val selectedLanguage by pm.selectedLanguage.collectAsStateWithLifecycle()
    val offloadingText = stringResource(
        if (offloadingEnabled) R.string.offloading_enabled else R.string.offloading_disabled
    )
    val positionFixText = stringResource(
        if (positionFixEnabled) R.string.position_fixing_enabled else R.string.position_fixing_disabled
    )
    var showGuideDialog by rememberSaveable { mutableStateOf(false) }
    var restartPlayerDialog by rememberSaveable { mutableStateOf(false) }
    val state = rememberScrollState()
    val toast = XCToast(LocalContext.current)
    val mustHaveTwoButtons = stringResource(R.string.must_have_two_buttons)
    var trashDragging by remember { mutableStateOf(false) }
    var pcButtonDragging by remember { mutableStateOf(false) }
    var pickedIdx by remember { mutableIntStateOf(Int.MIN_VALUE) }
    val onDragging: (Boolean, Int) -> Unit = { bool, int ->
        pcButtonDragging = bool; pickedIdx = int
    }

    val data: (Data) -> DragAndDropTransferData = { d ->
        DragAndDropTransferData(
            ClipData.newPlainText(
                "CustomCB", Json.encodeToString(Data.serializer(), d)
            )
        )
    }
    val acknowledgeRecreate by pm.acknowledgeRecreate.collectAsStateWithLifecycle()
    val theme by pm.theme.collectAsStateWithLifecycle()
    val onUpdateLanguage: (XCLanguage) -> Unit = {
        pm.updateLanguage(it) { onRecreate() }
    }
    LaunchedEffect(Unit, acknowledgeRecreate) {
        if (acknowledgeRecreate) {
            delay(500L)
            restartPlayerDialog = true; pm.updateAcknowledgement(false)
        }
    }
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
        PlayerOptionsGuide(
            showDialog = showGuideDialog, onDismiss = { showGuideDialog = false },
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
            ColumnContentWithBorder(
                getUIStyle, stringResource(R.string.player_options),
                onGuide = { showGuideDialog = true }
            ) {
                Text(
                    text = stringResource(R.string.Offloading), textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 20.sp
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ci.ToggleRow(
                        text = offloadingText,
                        onClick = {
                            scope.launch {
                                pm.toggleOffloading(!offloadingEnabled)
                                restartPlayerDialog = true
                            }
                        },
                        isOn = offloadingEnabled
                    )
                }
                HorizontalDivider(modifier = Modifier.fillMaxWidth(.90f), thickness = 1.5.dp)
                Text(
                    text = stringResource(R.string.audio_sink), textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 20.sp
                )
                ci.ToggleRow(
                    text = positionFixText,
                    onClick = {
                        scope.launch {
                            pm.togglePositionFix(!positionFixEnabled)
                            restartPlayerDialog = true
                        }
                    },
                    isOn = positionFixEnabled, enabled = offloadingEnabled
                )
                HorizontalDivider(modifier = Modifier.fillMaxWidth(.90f), thickness = 1.5.dp)
                Text(
                    text = stringResource(R.string.load_control), textAlign = TextAlign.Center,
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
                        label = { Text(stringResource(R.string.default_language)) }
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
                        label = { Text(stringResource(R.string.low_latency)) }
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
                        label = { Text(stringResource(R.string.Balanced)) }
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
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
                        label = { Text(stringResource(R.string.high_stability)) }
                    )
                    FilterChip(
                        selected = loadControl is LoadControl.Custom,
                        onClick = {
                            scope.launch {
                                if (loadControl is LoadControl.Custom) return@launch
                                pm.changeLoadControl(LoadControl.Custom())
                                restartPlayerDialog = true
                            }
                        },
                        label = { Text(stringResource(R.string.Custom)) }
                    )
                }
                (loadControl as? LoadControl.Custom)?.let { lc ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        CustomLoadControlColumn(
                            value = (lc.minBuffer / 1000), ci = ci,
                            onValueChange = {
                                pm.changeLoadControl(lc.copy(minBuffer = it * 1000))
                            },
                            range = 15..(lc.maxBuffer / 1000).coerceAtMost(60),
                            text = R.string.min_buffer
                        )
                        CustomLoadControlColumn(
                            value = (lc.maxBuffer / 1000), ci = ci,
                            onValueChange = {
                                pm.changeLoadControl(lc.copy(maxBuffer = it * 1000))
                            },
                            range = (lc.minBuffer / 1000).coerceAtLeast(25)..100,
                            text = R.string.max_buffer

                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 4.dp)
                    ) {
                        CustomLoadControlColumn(
                            value = (lc.bufferForPlayback / 1000f), ci = ci,
                            onValueChange = {
                                pm.changeLoadControl(
                                    lc.copy(bufferForPlayback = (it * 1000).toInt())
                                )
                            },
                            rangeStart = 0.50f,
                            rangeEnd = (lc.bufferForRebuffer / 1000f).coerceAtMost(3.00f),
                            text = R.string.buffer_for_playback
                        )
                        CustomLoadControlColumn(
                            value = (lc.bufferForRebuffer / 1000f), ci = ci,
                            onValueChange = {
                                pm.changeLoadControl(
                                    lc.copy(bufferForRebuffer = (it * 1000).toInt())
                                )
                            },
                            rangeStart = (lc.bufferForPlayback / 1000f).coerceAtLeast(1f),
                            rangeEnd = 5.00f,
                            text = R.string.buffer_for_rebuffer
                        )
                    }
                    Button(onClick = onRestartPlayer) {
                        Text(stringResource(R.string.restart_player))
                    }
                }
                HorizontalDivider(modifier = Modifier.fillMaxWidth(.90f), thickness = 1.5.dp)
                Text(
                    text = stringResource(R.string.playback_speed), textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 20.sp
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "0.1", style = MaterialTheme.typography.bodySmall)
                    Text(text = "2.0", style = MaterialTheme.typography.bodySmall)
                }

                Slider(
                    value = playbackSpeed,
                    onValueChange = {
                        val new = "%.2f".format(it).toFloat()
                        pm.updatePlaybackSpeed(new) { controller?.setPlaybackSpeed(new) }
                    },
                    valueRange = 0.1f..2.0f,
                    steps = 18,
                    modifier = Modifier
                        .height(30.dp)
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 18.dp, bottom = 24.dp),
                    thumb = {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            // Tooltip above the thumb
                            Text(
                                text = "%.2f".format(playbackSpeed),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .offset(y = (-25).dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )

                            // Actual thumb
                            SliderDefaults.Thumb(
                                interactionSource = remember { MutableInteractionSource() },
                                thumbSize = DpSize((2.5).dp, 10.dp)
                            )
                        }
                    }
                )
                HorizontalDivider(
                    thickness = 1.5.dp, modifier = Modifier
                        .fillMaxWidth(.90f)
                        .padding(top = 4.dp)
                )
                Text(
                    text = stringResource(R.string.silence_skipped), textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 20.sp
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = silencedSkippedEnabled,
                        onClick = {
                            if (silencedSkippedEnabled) return@FilterChip
                            pm.toggleSilenceSkipped()
                            restartPlayerDialog = true
                        },
                        label = { Text(stringResource(R.string.Enabled)) }
                    )
                    FilterChip(
                        selected = !silencedSkippedEnabled,
                        onClick = {
                            if (!silencedSkippedEnabled) return@FilterChip
                            pm.toggleSilenceSkipped()
                            restartPlayerDialog = true
                        },
                        label = { Text(stringResource(R.string.Disabled)) }
                    )
                }
            }
            ColumnContentWithBorder(
                getUIStyle, stringResource(R.string.notification_button_layout)
            ) {
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
                        label = { Text(stringResource(R.string.default_language)) }
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
                        label = { Text(stringResource(R.string.Reversed)) }
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = playerControls is PlayerControls.Configurable,
                        onClick = {
                            scope.launch {
                                if (playerControls is PlayerControls.Configurable) return@launch
                                pm.updatePlayerControls(PlayerControls.Configurable()) {
                                    controller?.sendCustomCommand(
                                        SessionCommand(COMMAND_CHANGE_LAYOUT, Bundle()), Bundle()
                                    )
                                }
                            }
                        },
                        label = { Text(stringResource(R.string.Configurable)) }
                    )
                    FilterChip(
                        selected = playerControls is PlayerControls.Custom,
                        onClick = {
                            scope.launch {
                                if (playerControls is PlayerControls.Custom) return@launch
                                pm.updatePlayerControls(PlayerControls.Custom()) {
                                    controller?.sendCustomCommand(
                                        SessionCommand(COMMAND_CHANGE_LAYOUT, Bundle()), Bundle()
                                    )
                                }
                            }
                        },
                        label = { Text(stringResource(R.string.Custom)) }
                    )
                }
                (playerControls as? PlayerControls.Configurable)?.let { pc ->
                    HorizontalDivider(modifier = Modifier.fillMaxWidth(.90f), thickness = 1.5.dp)
                    Text(
                        text = stringResource(R.string.button_selection),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 20.sp
                    )
                    Row {
                        XCCommandButton.entries.forEach {
                            FilterChip(
                                selected = pc.included.contains(it),
                                onClick = {
                                    if (!pc.canRemove(it)) {
                                        toast.makeMessage(mustHaveTwoButtons)
                                        return@FilterChip
                                    }
                                    pm.updatePlayerControls(pc.toggleButton(it)) {
                                        controller?.sendCustomCommand(
                                            SessionCommand(COMMAND_CHANGE_LAYOUT, Bundle()),
                                            Bundle()
                                        )
                                    }
                                },
                                label = {
                                    ci.ContentIcon(
                                        it.icon, tint = getUIStyle.themedOnContainerColor()
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
                (playerControls as? PlayerControls.Custom)?.let { pc ->
                    val list = pc.toList()
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(.90f),
                                thickness = 1.5.dp
                            )
                            Text(
                                text = stringResource(R.string.button_selection),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                lineHeight = 20.sp
                            )
                            HorizontalDivider(
                                thickness = 1.0.dp, modifier = Modifier
                                    .fillMaxWidth(.75f)
                                    .padding(top = 2.dp, bottom = 8.dp)
                            )
                        }
                        if (trashDragging) TrashBin(
                            ci, color = getUIStyle.altThemedOnContainerColor(),
                            onFinish = {
                                trashDragging = false; onDragging(false, Int.MIN_VALUE)
                            }) { str ->
                            val idx = str.idx
                            val new = pc.toNull(idx)
                            pm.updatePlayerControls(new) {
                                controller?.sendCustomCommand(
                                    SessionCommand(COMMAND_CHANGE_LAYOUT, Bundle()),
                                    Bundle()
                                )
                            }
                            true
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        list.forEachIndexed { idx, button ->
                            val text = when (idx) {
                                PlayerControls.Custom.OVERFLOW_ONE -> stringResource(R.string.overflow_one)
                                PlayerControls.Custom.PREV -> stringResource(R.string.backward)
                                PlayerControls.Custom.NEXT -> stringResource(R.string.forward)
                                PlayerControls.Custom.OVERFLOW_TWO -> stringResource(R.string.overflow_two)
                                else -> null
                            }
                            key(
                                theme, (idx to button), pcButtonDragging, pickedIdx,
                                pc.customConfig.button, pc.customConfig.included
                            ) {
                                val disabledColor = getUIStyle.disabledThemedColor()
                                val borderColor =
                                    if (!pcButtonDragging) getUIStyle.pickedSongColor()
                                    else if (pc.canChange(pickedIdx, idx) &&
                                        pc.configChangeable(list, idx, pickedIdx)
                                    ) getUIStyle.greenBorderColor()
                                    else disabledColor
                                val configColor = if (pc.isSelectedConfigIdx(idx))
                                    getUIStyle.configTintColor()
                                else getUIStyle.themedOnContainerColor()
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    text?.let {
                                        Text(
                                            text = it, textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp, lineHeight = 11.sp
                                        )
                                    }
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .background(
                                                color = if (getUIStyle.getIsDarkTheme()) Color.DarkGray
                                                else Color.LightGray,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                BorderStroke(1.5.dp, borderColor),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 4.dp)
                                            .width(52.5.dp)
                                            .dragAndDropSource(transferData = { _: Offset ->
                                                if (pc.isInOverflow(idx))
                                                    button?.name?.let { name ->
                                                        onDragging(true, idx)
                                                        trashDragging = true; data(Data(name, idx))
                                                    }
                                                else {
                                                    onDragging(true, idx)
                                                    data(Data(button!!.name, idx))
                                                }

                                            }
                                            )
                                            .dragAndDropTarget(
                                                shouldStartDragAndDrop = { event ->
                                                    event.mimeTypes()
                                                        .contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                                                },
                                                target = object : DragAndDropTarget {
                                                    override fun onDrop(event: DragAndDropEvent): Boolean {
                                                        val data =
                                                            Json.decodeFromString(
                                                                Data.serializer(),
                                                                event.toAndroidDragEvent().clipData
                                                                    .getItemAt(0).text.toString()
                                                            )
                                                        onDragging(false, Int.MIN_VALUE)
                                                        val cb =
                                                            runCatching {
                                                                CustomCB.valueOf(data.str)
                                                            }.getOrNull() ?: return false
                                                        val canChange =
                                                            pc.canChange(cb, idx)
                                                        if (!canChange) return false
                                                        val (new, result) =
                                                            pc.onChange(cb, idx)
                                                        if (!result) return false
                                                        pm.updatePlayerControls(new) {
                                                            controller?.sendCustomCommand(
                                                                SessionCommand(
                                                                    COMMAND_CHANGE_LAYOUT, Bundle()
                                                                ), Bundle()
                                                            )
                                                        }
                                                        return true
                                                    }

                                                    override fun onEnded(event: DragAndDropEvent) {
                                                        onDragging(false, Int.MIN_VALUE)
                                                        super.onEnded(event)
                                                    }
                                                }
                                            ),
                                    ) {
                                        button?.icon?.let {
                                            ci.ContentIcon(it, tint = configColor)
                                        } ?: ci.ContentIcon(Icons.Default.Clear)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CustomCB.entries
                            .filter { it !in list }
                            .forEachIndexed { idx, cb ->
                                key(list.size, (idx to cb), theme) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (getUIStyle.getIsDarkTheme()) Color.DarkGray
                                                else Color.LightGray,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 3.dp)
                                            .width(40.dp)
                                            .dragAndDropSource(transferData = { _: Offset ->
                                                onDragging(true, -1); data(Data(cb.name, idx))
                                            }
                                            )
                                    ) {
                                        ci.ContentIcon(
                                            cb.icon, modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                    }
                    if (pc.hasConfigButton())
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = stringResource(R.string.config_button_selection),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                textDecoration = TextDecoration.Underline,
                                fontSize = 15.sp,
                                lineHeight = 17.sp
                            )
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ConfigCB.entries.filter { it.toCustomCB() !in list }.forEach {
                                    FilterChip(
                                        selected = pc.customConfig.included.contains(it),
                                        onClick = {
                                            if (!pc.canRemove(it)) {
                                                toast.makeMessage(mustHaveTwoButtons)
                                                return@FilterChip
                                            }
                                            pm.updatePlayerControls(pc.toggleButton(it)) {
                                                controller?.sendCustomCommand(
                                                    SessionCommand(COMMAND_CHANGE_LAYOUT, Bundle()),
                                                    Bundle()
                                                )
                                            }
                                        },
                                        label = {
                                            ci.ContentIcon(
                                                it.icon, tint = getUIStyle.themedOnContainerColor()
                                            )
                                        },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                }

            }
            ColumnContentWithBorder(getUIStyle, stringResource(R.string.Language)) {
                FilterChip(
                    selected = selectedLanguage.isDefault(),
                    onClick = {
                        if (!selectedLanguage.isDefault())
                            onUpdateLanguage(XCLanguage.Default)
                    },
                    label = {
                        Text(
                            stringResource(R.string.default_language),
                            textAlign = TextAlign.Center
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp)
                )
                FilterChip(
                    selected = selectedLanguage.isEnglish(),
                    onClick = {
                        if (!selectedLanguage.isEnglish())
                            onUpdateLanguage(XCLanguage.English)
                    },
                    label = { Text(XCLanguage.English.toString(), textAlign = TextAlign.Center) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp)
                )
                FilterChip(
                    selected = selectedLanguage.isSpanish(),
                    onClick = {
                        if (!selectedLanguage.isSpanish())
                            onUpdateLanguage(XCLanguage.Spanish)
                    },
                    label = { Text(XCLanguage.Spanish.toString(), textAlign = TextAlign.Center) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp)
                )
            }
        }
    }
}

@Serializable
private data class Data(val str: String, val idx: Int)

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

@Composable
private fun ColumnContentWithBorder(
    getUIStyle: GetUIStyle, text: String, onGuide: () -> Unit,
    modifier: Modifier = Modifier, content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val ci = ContentIcons(getUIStyle)
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
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = text,
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                color = getUIStyle.themedOnContainerColor(),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            )
            ci.ContentIcon(
                R.drawable.help_center,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .align(Alignment.CenterEnd)
                    .clickable { onGuide() })
        }
        if (expanded) {
            HorizontalDivider(modifier = Modifier.fillMaxWidth(0.95f), thickness = 2.dp)
            content()
        }
    }
}

@Composable
private fun TrashBin(
    ci: ContentIcons, color: Color, onFinish: () -> Unit,
    onDropRemove: (payloadText: Data) -> Boolean
) {
    var hover by remember { mutableStateOf(false) }
    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                hover = true
                super.onEntered(event)
            }

            override fun onExited(event: DragAndDropEvent) {
                hover = false
                super.onExited(event)
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                hover = false
                onFinish()
                val payload =
                    Json.decodeFromString(
                        Data.serializer(),
                        event.toAndroidDragEvent().clipData.getItemAt(0).text.toString()
                    )
                return onDropRemove(payload)
            }

            override fun onEnded(event: DragAndDropEvent) {
                super.onEnded(event)
                onFinish()
            }

            override fun onChanged(event: DragAndDropEvent) {
                super.onChanged(event)
                onFinish()
            }
        }
    }

    Card(
        shape = RectangleShape,
        colors = CardDefaults.cardColors().copy(
            containerColor = if (hover) Color.Red.copy(alpha = 0.12f)
            else CardDefaults.cardColors().containerColor
        ),
        modifier = Modifier
            .width(55.dp)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                },
                target = dropTarget
            ),
    ) {
        ci.ContentIcon(
            Icons.Default.Delete,
            contentDescription = "Trash",
            tint = if (hover) color else ci.defaultColor(),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}