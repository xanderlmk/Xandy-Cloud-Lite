package com.xandy.lite.views.edit.audio

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xandy.lite.controllers.view.models.EditAudioVM
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.AudioWithPls
import com.xandy.lite.db.tables.LyricLine
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.SelectImageModal
import com.xandy.lite.ui.functions.item.details.Artwork
import com.xandy.lite.ui.theme.GetUIStyle
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import java.time.Month
import java.time.Year
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToLong


@Composable
fun EditAudioView(
    audioFile: AudioWithPls, enabled: Boolean, getUIStyle: GetUIStyle, editAudioVM: EditAudioVM,
    allMediaArtwork: List<Uri>, onUpdate: (AudioFile, Lyrics?) -> Unit
) {
    var audio by rememberSaveable { mutableStateOf(audioFile.song) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var lyrics by rememberSaveable { mutableStateOf(audioFile.lyrics ?: Lyrics(plain = "")) }
    val set by editAudioVM.set.collectAsStateWithLifecycle()
    if (isLandscape)
        HorizontalEditAudioView(
            audio = audio, enabled = enabled, allMediaArtwork = allMediaArtwork,
            onAudioChange = { audio = it }, lyrics = lyrics, onLyricsChange = { lyrics = it },
            onUpdate = onUpdate, getUIStyle = getUIStyle, set = set, editAudioVM = editAudioVM
        )
    else VerticalEditAudioView(
        audio = audio, enabled = enabled, allMediaArtwork = allMediaArtwork,
        onAudioChange = { audio = it },
        lyrics = lyrics, onLyricsChange = { lyrics = it },
        onUpdate = onUpdate, getUIStyle = getUIStyle, set = set, editAudioVM = editAudioVM
    )
}

@Composable
fun ImagePicker(
    artworkList: List<Uri>, enabled: Boolean, isLandscape: Boolean, getUIStyle: GetUIStyle,
    onImagePicked: (Uri) -> Unit
) {
    var showModal by rememberSaveable { mutableStateOf(false) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let(onImagePicked).also { showModal = false }
    }
    SelectImageModal(
        onDismissRequest = { showModal = false }, show = showModal,
        onSelectLocal = { showDialog = true },
        onSelectGallery = { launcher.launch("image/*") },
        localEnabled = artworkList.isNotEmpty()
    )
    LocalArtwork(
        onDismissRequest = { showDialog = false; showModal = false },
        onSelect = { showDialog = false; showModal = false; onImagePicked(it) },
        list = artworkList, showDialog = showDialog, isLandscape = isLandscape,
        getUIStyle = getUIStyle
    )
    Button(
        onClick = { showModal = true },
        enabled = enabled, modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text("Choose Artwork")
    }
}

@Composable
private fun LocalArtwork(
    onDismissRequest: () -> Unit, onSelect: (Uri) -> Unit, list: List<Uri>,
    showDialog: Boolean, isLandscape: Boolean, getUIStyle: GetUIStyle
) {

    val rows = list.chunked(if (isLandscape) 4 else 2)
    if (showDialog) {
        if (isLandscape) {
            Dialog(
                onDismissRequest = onDismissRequest,
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                val state = rememberLazyListState()
                LazyColumnScrollbar(
                    state = state,
                    settings = ScrollbarSettings(
                        thumbSelectedColor = getUIStyle.selectedThumbColor(),
                        thumbUnselectedColor = getUIStyle.unSelectedThumbColor()
                    )
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.75f),
                        contentPadding = PaddingValues(4.dp), state = state
                    ) {
                        items(rows) { rowItems ->
                            Row(modifier = Modifier.fillMaxSize()) {
                                rowItems.forEach { image ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                    ) {
                                        Artwork(
                                            image, LocalContext.current,
                                            Modifier.clickable { onSelect(image) }
                                        )
                                    }
                                }
                                when (rowItems.size) {
                                    1 -> Spacer(modifier = Modifier.weight(3f))
                                    2 -> Spacer(modifier = Modifier.weight(2f))
                                    3 -> Spacer(modifier = Modifier.weight(1f))

                                }
                            }
                        }
                    }
                }
            }
        } else {
            Dialog(onDismissRequest = onDismissRequest) {
                val state = rememberLazyListState()
                LazyColumnScrollbar(
                    state = state,
                    settings = ScrollbarSettings(
                        thumbSelectedColor = getUIStyle.selectedThumbColor(),
                        thumbUnselectedColor = getUIStyle.unSelectedThumbColor()
                    )
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.90f),
                        contentPadding = PaddingValues(4.dp), state = state
                    ) {
                        items(rows) { rowItems ->
                            Row(modifier = Modifier.fillMaxSize()) {
                                rowItems.forEach { image ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                    ) {
                                        Artwork(
                                            image, LocalContext.current,
                                            Modifier.clickable { onSelect(image) })
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateChooser(
    minYear: Int, maxYear: Int = Year.now().value, getUIStyle: GetUIStyle,
    selectedYear: Int?, selectedMonth: Int?, selectedDay: Int?,
    onYearSelected: (Int?) -> Unit, onMonthSelected: (Int?) -> Unit, onDaySelected: (Int?) -> Unit,
    modifier: Modifier
) {
    val years = (minYear..maxYear).toList().reversed()
    val monthNames =
        Month.entries.map { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }

    // compute days in selected month/year (if both present)
    val daysInMonth = try {
        if (selectedYear != null && selectedMonth != null && selectedMonth in 1..12)
            YearMonth.of(selectedYear, selectedMonth).lengthOfMonth() else null
    } catch (_: Exception) {
        null
    }
    Row(modifier = modifier) {
        SimpleDropdown(
            label = "Year", options = years.map { it.toString() },
            selectedIndex = selectedYear?.let { years.indexOf(it) } ?: -1,
            onSelectIndex = { idx -> onYearSelected(if (idx >= 0) years[idx] else null) },
            modifier = Modifier.weight(1f)
        )
        SimpleDropdown(
            label = "Month",
            options = listOf("—") + monthNames, // index 0 = none, indexes 1..12 = months
            selectedIndex = selectedMonth?.let {
                if (it in 1..12) it else -1
            }?.let { if (it == -1) -1 else it } ?: (0),
            onSelectIndex = { idx ->
                onMonthSelected(
                    when (idx) {
                        0 -> null
                        in 1..12 -> idx
                        else -> null
                    }
                )
            },
            tint = if (selectedYear == null) getUIStyle.disabledThemedColor()
            else getUIStyle.themedColor(),
            enabled = selectedYear != null,
            modifier = Modifier.weight(1f)
        )

        val dayOptions =
            if (daysInMonth != null) listOf("—") + (1..daysInMonth).map { it.toString() }
            else listOf()

        SimpleDropdown(
            label = "Day",
            tint = if (dayOptions.isEmpty()) getUIStyle.disabledThemedColor()
            else getUIStyle.themedColor(),
            enabled = dayOptions.isNotEmpty(),
            options = dayOptions,
            selectedIndex = selectedDay ?: 0,
            onSelectIndex = { idx -> onDaySelected(if (idx == 0) null else idx) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SimpleDropdown(
    label: String, options: List<String>, selectedIndex: Int, onSelectIndex: (Int) -> Unit,
    enabled: Boolean = true, tint: Color = LocalContentColor.current, modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val displayText = if (selectedIndex in options.indices) options[selectedIndex] else "—"

    Box(
        modifier = modifier
            .padding(4.dp)
            .wrapContentSize(Alignment.Center)
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = { },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .clickable(enabled = enabled) { expanded = !expanded }
                )
            },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 500.dp),
            properties = PopupProperties(clippingEnabled = false)
        ) {
            options.forEachIndexed { idx, opt ->
                DropdownMenuItem(
                    onClick = { onSelectIndex(idx); expanded = false },
                    text = { Text(opt, style = MaterialTheme.typography.bodyMedium) }
                )
            }
        }
    }
}

@Composable
fun LyricsOptions(
    ci: ContentIcons, lyrics: Lyrics, durationMs: Long, set: List<LyricLine>,
    editAudioVM: EditAudioVM, onLyricsChange: (Lyrics) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val icon = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
    Text(text = "Lyrics", textDecoration = TextDecoration.Underline)
    TextField(
        value = lyrics.plain,
        placeholder = { Text("Type or paste lyrics here") },
        onValueChange = { onLyricsChange(lyrics.copy(plain = it)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )
    if (lyrics.plain.isNotBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("More options")
            IconButton(onClick = { expanded = !expanded }) { ci.ContentIcon(icon) }
        }
        if (expanded) {
            Text(text = "Translation for Lyrics", textDecoration = TextDecoration.Underline)
            TextField(
                value = lyrics.translation ?: "",
                placeholder = { Text("Type or paste translation here") },
                onValueChange = { onLyricsChange(lyrics.copy(translation = it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
            Text(text = "Synchronized Lyrics", textDecoration = TextDecoration.Underline)
            if (set.isEmpty()) Button(onClick = {
                editAudioVM.addToSet(0, durationMs)
            }) { Text("Add lyrics line") }
            else set.forEachIndexed { index, lyricLine ->
                val previousRange = rememberUpdatedState(set).value.takeIf {
                    (index - 1) in it.indices
                }?.get(index - 1)?.range
                val nextRange = rememberUpdatedState(set).value.takeIf {
                    (index + 1) in it.indices
                }?.get(index + 1)?.range
                var sliderRange by remember {
                    mutableStateOf(
                        lyricLine.range.first / durationMs.toFloat()..lyricLine.range.last / durationMs.toFloat()
                    )
                }
                key(lyricLine.range.first, lyricLine.range.last) {
                    sliderRange =
                        lyricLine.range.first / durationMs.toFloat()..lyricLine.range.last / durationMs.toFloat()
                }

                TextField(
                    value = lyricLine.text,
                    onValueChange = { editAudioVM.updateLineTextAt(index, it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
                TrimRangeChooser(
                    sliderRange = sliderRange, durationMs,
                    lyricLine.range.first, lyricLine.range.last,
                    previousRange = previousRange, nextRange = nextRange
                ) {
                    sliderRange = it.first / durationMs.toFloat()..it.last / durationMs.toFloat()
                    editAudioVM.updateLineRangeAt(index, it.first, it.last)
                }

                if (index == set.lastIndex) {
                    if (lyricLine.range.last < durationMs)
                        Button(onClick = {
                            editAudioVM.addToSet(lyricLine.range.last, durationMs)
                        }) { Text("Add lyrics line") }
                    Button(onClick = { editAudioVM.removeLast() }) { Text("Remove lyrics line") }
                }
            }
        }
    }

}


/**
 * Range chooser for a media timeline.
 *
 * @param durationMs total duration in milliseconds (must be > 0)
 * @param startMs starting ms
 * @param endMs ending ms
 * @param minGapMs minimum allowed gap between start and end in ms (defaults 100 ms)
 * @param onRangeChanged called when user finished dragging with the chosen LongRange (inclusive start..end)
 */
@Composable
private fun TrimRangeChooser(
    sliderRange: ClosedFloatingPointRange<Float>, durationMs: Long,
    startMs: Long, endMs: Long, minGapMs: Long = 100L,
    previousRange: LongRange?, nextRange: LongRange?, onRangeChanged: (LongRange) -> Unit
) {
    val duration = durationMs.coerceAtLeast(1L)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        TimeEditRow(startMs, endMs)
        RangeSlider(
            value = sliderRange,
            onValueChange = { newRange ->
                val minGapFrac = minGapMs / duration.toFloat()
                val startF = newRange.start.coerceIn(0f, 1f)
                val endF = newRange.endInclusive.coerceIn(0f, 1f)
                val previousLast = previousRange?.last
                val nextFirst = nextRange?.first
                var tempStart =
                    (startF * duration).roundToLong().coerceIn(0L, duration)
                var tempEnd = (endF * duration).roundToLong().coerceIn(0L, duration)
                if (endF - startF < minGapFrac) return@RangeSlider
                if (previousLast != null && tempStart < previousLast) tempStart = previousLast
                if (nextFirst != null && nextFirst < tempEnd) tempEnd = nextFirst
                onRangeChanged(tempStart..tempEnd)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(48.dp)
        )
    }
}

@Composable
private fun TimeEditRow(startMs: Long, endMs: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = formatMs(startMs), style = MaterialTheme.typography.bodyLarge)
        Text(text = formatMs(endMs), style = MaterialTheme.typography.bodyLarge)
    }
}

/** Returns "M:SS.mmm" */
private fun formatMs(ms: Long): String {
    val total = ms.coerceAtLeast(0L)
    val minutes = total / 60_000
    val seconds = (total % 60_000) / 1000
    val millis = total % 1000
    return "%d:%02d.%03d".format(minutes, seconds, millis)
}
