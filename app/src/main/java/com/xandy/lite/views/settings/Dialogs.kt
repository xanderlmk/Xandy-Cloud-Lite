package com.xandy.lite.views.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xandy.lite.R
import com.xandy.lite.ui.GetUIStyle
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.Details
import kotlinx.coroutines.launch


@Composable
internal fun PlayerOptionsGuide(
    showDialog: Boolean, getUIStyle: GetUIStyle, onDismiss: () -> Unit
) {
    if (showDialog) {
        Dialog(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .background(getUIStyle.dialogBackGroundColor(), RoundedCornerShape(16.dp))
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.player_guide),
                    textAlign = TextAlign.Start, fontSize = 28.sp,
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge
                )
                Details(
                    header = stringResource(R.string.Offloading),
                    text = stringResource(R.string.offloading_guide).align(),
                    headerFontSize = 22.sp, textFontSize = 15.sp
                )
                Details(
                    header = stringResource(R.string.position_fixing),
                    text = stringResource(R.string.position_fixing_guide).align(),
                    headerFontSize = 18.sp, textFontSize = 15.sp
                )
                Details(
                    header = stringResource(R.string.load_control),
                    text = stringResource(R.string.load_control_guide),
                    headerFontSize = 22.sp, textFontSize = 15.sp
                )
                Details(
                    header = stringResource(R.string.playback_speed),
                    text = stringResource(R.string.playback_speed_guide).align(),
                    headerFontSize = 22.sp, textFontSize = 15.sp
                )
                Details(
                    header = stringResource(R.string.silence_skipped),
                    text = stringResource(R.string.silence_skipped_guide).align(),
                    headerFontSize = 22.sp, textFontSize = 15.sp
                )
                Button(onClick = onDismiss) { Text(stringResource(R.string.Close)) }
            }
        }
    }

}

@Composable
internal fun RestartPlayer(onDismiss: () -> Unit, onSubmit: () -> Unit, getUIStyle: GetUIStyle) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.Attention)) },
        confirmButton = {
            Button(
                onClick = { onSubmit() }
            ) { Text(stringResource(R.string.Restart)) }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.Later)) }
        },
        text = {
            Text(
                text = stringResource(R.string.audio_settings_changed),
                modifier = Modifier.fillMaxWidth()
            )
        },
        titleContentColor = getUIStyle.themedOnContainerColor(),
        textContentColor = getUIStyle.themedOnContainerColor(),
        containerColor = getUIStyle.dialogBackGroundColor()
    )
}


@Composable
internal fun NumberPickerTextField(
    value: Int, onValueChange: (Int) -> Unit, ci: ContentIcons,
    modifier: Modifier = Modifier, range: IntRange = 40..100
) {
    var open by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = value.toString(),
        onValueChange = {},
        readOnly = true,
        trailingIcon = {
            ci.ContentIcon(
                Icons.Default.ArrowDropDown, modifier = Modifier
                    .clickable { open = true })
        },
        modifier = modifier
    )

    if (open) {
        // Dialog with a vertically scrollable list
        Dialog(onDismissRequest = { open = false }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .widthIn(min = 150.dp, max = 200.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Select number",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(8.dp)
                    )

                    val numbers = remember { range.toList() }
                    val initialIndex = numbers.indexOf(value).coerceAtLeast(0)
                    val listState = rememberLazyListState(initialIndex)
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(Unit) {
                        // Ensure selected item is visible/centered when opening
                        scope.launch {
                            listState.animateScrollToItem(initialIndex)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .height(260.dp)
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(numbers.size) { idx ->
                            val n = numbers[idx]
                            val selected = n == value
                            val bg =
                                if (selected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else Color.Transparent
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 12.dp)
                                    .background(bg, shape = MaterialTheme.shapes.small)
                                    .clickable {
                                        onValueChange(n)
                                        open = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = n.toString(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
internal fun NumberPickerTextField(
    value: Float, onValueChange: (Float) -> Unit, ci: ContentIcons,
    modifier: Modifier = Modifier, rangeStart: Float, rangeEnd: Float
) {
    var open by rememberSaveable { mutableStateOf(false) }

    val step = 0.5f
    OutlinedTextField(
        value = value.toString(),
        onValueChange = {},
        readOnly = true,
        trailingIcon = {
            ci.ContentIcon(
                Icons.Default.ArrowDropDown, modifier = Modifier
                    .clickable { open = true })
        },
        modifier = modifier
    )

    if (open) {
        Dialog(onDismissRequest = { open = false }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .widthIn(min = 150.dp, max = 200.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "Select number",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(8.dp)
                    )

                    val numbers = remember {
                        generateSequence(rangeStart) { prev ->
                            val next = prev + step
                            if (next <= rangeEnd + 0.0001f) next else null
                        }.toList()
                    }

                    val initialIndex = numbers.indexOfFirst { it == value }.coerceAtLeast(0)
                    val listState = rememberLazyListState(initialIndex)
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(Unit) {
                        // Ensure selected item is visible/centered when opening
                        scope.launch {
                            listState.animateScrollToItem(initialIndex)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .height(260.dp)
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(numbers.size) { idx ->
                            val n = numbers[idx]
                            val selected = n == value
                            val bg =
                                if (selected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else Color.Transparent
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 12.dp)
                                    .background(bg, shape = MaterialTheme.shapes.small)
                                    .clickable { onValueChange(n); open = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "%.1f".format(n),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CustomLoadControlColumn(
    value: Float, onValueChange: (Float) -> Unit,
    rangeStart: Float, rangeEnd: Float, ci: ContentIcons, @StringRes text: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(text), textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 16.sp
        )
        NumberPickerTextField(
            value = value, ci = ci,
            onValueChange = onValueChange,
            modifier = Modifier.widthIn(100.dp, 150.dp),
            rangeStart = rangeStart, rangeEnd = rangeEnd
        )
    }
}

@Composable
internal fun CustomLoadControlColumn(
    value: Int, onValueChange: (Int) -> Unit,
    range: IntRange, ci: ContentIcons, @StringRes text: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(text), textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 16.sp
        )
        NumberPickerTextField(
            value = value, ci = ci,
            onValueChange = onValueChange,
            modifier = Modifier.widthIn(100.dp, 150.dp),
            range = range
        )
    }
}

private fun String.align() = this.trimIndent().replace(Regex("\\s+"), " ")