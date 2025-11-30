package com.xandy.lite.views.lyrics

import android.content.res.Configuration
import android.os.LocaleList
import android.util.Log
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xandy.lite.controllers.view.models.LyricsEditorVM
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.LyricLine
import com.xandy.lite.db.tables.TranslatedLyrics
import com.xandy.lite.db.tables.TranslatedText
import com.xandy.lite.models.XCToast
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.item.details.SongRow
import com.xandy.lite.ui.GetUIStyle
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import kotlin.math.roundToLong


@Composable
fun LyricsEditorView(
    vm: LyricsEditorVM, onUpsert: () -> Unit, getUIStyle: GetUIStyle
) {
    val lyrics by vm.lyrics.collectAsStateWithLifecycle()
    val scrollSet by vm.scrollSet.collectAsStateWithLifecycle()
    val pronunciationSet by vm.pronunciationSet.collectAsStateWithLifecycle()
    val translationSet by vm.translationSet.collectAsStateWithLifecycle()
    val indexListener by vm.index.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val songs by vm.songs.collectAsStateWithLifecycle()
    val controller by vm.controller.collectAsStateWithLifecycle()
    val ci = ContentIcons(getUIStyle)
    var pickedAudio by rememberSaveable { mutableStateOf<AudioFile?>(null) }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val coroutineScope = rememberCoroutineScope()
    val toast = XCToast(LocalContext.current)
    var language by rememberSaveable {
        mutableStateOf(
            try {
                LocaleList.getDefault().get(0).language
            } catch (_: Exception) {
                ""
            }
        )
    }
    val onGetLanguage: () -> Unit = {
        language = try {
            LocaleList.getDefault().get(0).language
        } catch (_: Exception) {
            Log.w(XANDY_CLOUD, "Unable to get Language")
            "en"
        }
    }


    val onPickSong: (AudioFile) -> Unit = {
        vm.updateIndexListener(LyricIndex.AVAILABLE)
        pickedAudio = it; isSearching = false
    }
    val state = rememberScrollState()
    var textFieldHeight by rememberSaveable { mutableIntStateOf(0) }
    var descriptionHeight by rememberSaveable { mutableIntStateOf(0) }
    var plainHeight by rememberSaveable { mutableIntStateOf(0) }
    var syncryonizedHeight by rememberSaveable { mutableIntStateOf(0) }
    var syncryonizedSetHeight by rememberSaveable { mutableIntStateOf(0) }
    var pronunciationHeight by rememberSaveable { mutableIntStateOf(0) }
    var pronunciationSetHeight by rememberSaveable { mutableIntStateOf(0) }
    var landscapeState by rememberSaveable { mutableStateOf(isLandscape) }
    var playerToggle by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (landscapeState != isLandscape) {
            vm.checkPosition()
            landscapeState = isLandscape
        }
    }

    if (pickedAudio == null) {
        @Suppress("KotlinConstantConditions")
        SearchForSong(
            topContent = {
                Text("Pick a song to get started")
                Button(
                    onClick = {
                        vm.updateIndexListener(LyricIndex.UNAVAILABLE)
                        coroutineScope.launch {
                            val audio = vm.getSongOrNullByLyricsId(lyrics.id)
                            if (audio == null) toast.makeMessage("No song available")
                            else {
                                vm.updateIndexListener(LyricIndex.AVAILABLE)
                                pickedAudio = audio
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(if (isLandscape) 0.3f else 0.5f)
                ) {
                    Text(
                        text = "Get first related song", Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            },
            query = query, onQueryChange = { vm.updateQuery(it) },
            getUIStyle = getUIStyle, songs = songs, pickedAudio = pickedAudio,
            onPickSong = {
                if (scrollSet.isEmpty()) onPickSong(it)
                else if (it.durationMillis < scrollSet[scrollSet.lastIndex].range.last)
                    toast.makeMessage("Song is too short")
                else onPickSong(it)
            }
        )
    } else if (!isSearching) {
        LaunchedEffect(indexListener) {
            coroutineScope.launch {
                when (indexListener) {
                    LyricIndex.DESCRIPTION ->
                        state.scrollTo(textFieldHeight)

                    LyricIndex.PLAIN ->
                        state.scrollTo(textFieldHeight + descriptionHeight)

                    LyricIndex.SYNCRYONIZED ->
                        state.scrollTo(textFieldHeight + descriptionHeight + plainHeight)

                    LyricIndex.PRONUNCIATION ->
                        state.scrollTo(
                            textFieldHeight + descriptionHeight + plainHeight
                                    + syncryonizedHeight + syncryonizedSetHeight
                        )

                    LyricIndex.TRANSLATION ->
                        state.scrollTo(
                            textFieldHeight + descriptionHeight + plainHeight
                                    + syncryonizedHeight + syncryonizedSetHeight
                                    + pronunciationHeight + pronunciationSetHeight
                        )

                    else -> return@launch
                }
                vm.updateIndexListener(LyricIndex.AVAILABLE)
            }
        }
        Box {
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (playerToggle) 90.dp else 30.dp)
                    .verticalScroll(state)
            ) {
                OutlinedTextField(
                    value = "Change song: Current - ${pickedAudio?.title ?: "None Selected"}",
                    onValueChange = { },
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = {
                        ci.ContentIcon(
                            Icons.Default.Search,
                            modifier = Modifier
                                .clickable {
                                    vm.updateIndexListener(LyricIndex.UNAVAILABLE)
                                    isSearching = true
                                }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { textFieldHeight = it.size.height },
                    textStyle = TextStyle(fontSize = 18.sp),
                    shape = ShapeDefaults.Large,
                )
                pickedAudio?.let {
                    SimpleItem(
                        "Description", lyrics.description ?: "",
                        onValueChange = { str -> vm.updateDescription(str) },
                        "Enter description of lyrics (e.g. which song, version etc)",
                        modifier = Modifier
                            .onGloballyPositioned { coor -> descriptionHeight = coor.size.height }
                            .onSizeChanged { coor -> descriptionHeight = coor.height }
                    )
                    SimpleItem(
                        "Plain Lyrics", lyrics.plain,
                        onValueChange = { str -> vm.updatePlain(str) }, "Type or paste lyrics here",
                        modifier = Modifier
                            .onGloballyPositioned { coor -> plainHeight = coor.size.height }
                            .onSizeChanged { coor -> plainHeight = coor.height }
                    )
                    SyncryonizedItem(
                        scrollSet, modifier = Modifier
                            .onGloballyPositioned { coor -> syncryonizedHeight = coor.size.height }
                            .onSizeChanged { coor -> syncryonizedHeight = coor.height }
                    ) { vm.addToScrollSet(0, it.durationMillis) }
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .onGloballyPositioned { coor ->
                                syncryonizedSetHeight = coor.size.height
                            }
                            .onSizeChanged { coor -> syncryonizedSetHeight = coor.height }
                    ) {
                        scrollSet.forEachIndexed { index, lyricLine ->
                            ScrollSetItem(scrollSet, index, lyricLine, vm, it.durationMillis)
                        }
                    }
                    CustomizedLyricsItem(
                        scrollSet, onGetLanguage, lyrics.pronunciation,
                        language = language, toast = toast, type = "Pronunciation",
                        onUpdateType = { type, lang -> vm.togglePronunciationType(type, lang) },
                        onTypeToNull = { vm.pronunciationTypeToNull() },
                        modifier = Modifier
                            .onGloballyPositioned { coor -> pronunciationHeight = coor.size.height }
                            .onSizeChanged { coor -> pronunciationHeight = coor.height }
                    )
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .onGloballyPositioned { coor ->
                                pronunciationSetHeight = coor.size.height
                            }
                            .onSizeChanged { coor -> pronunciationSetHeight = coor.height }
                    ) {
                        if (lyrics.pronunciation?.lyrics is TranslatedText.Plain) {
                            val plain by rememberUpdatedState(lyrics.pronunciation?.lyrics as TranslatedText.Plain)
                            TextField(
                                value = plain.t,
                                onValueChange = { str ->
                                    vm.updatePronunciationPlainText(
                                        TranslatedLyrics(
                                            TranslatedText.Plain(str),
                                            lyrics.pronunciation?.language ?: language
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                        } else if (lyrics.pronunciation?.lyrics is TranslatedText.Scroll)
                            pronunciationSet.forEachIndexed { index, lyricLine ->
                                CustomSetItem(lyricLine) { str ->
                                    vm.updatePronunciationSetTextAt(index, str)
                                }
                            }
                    }
                    CustomizedLyricsItem(
                        scrollSet, onGetLanguage, lyrics.translation, language = language,
                        toast = toast, type = "Translated",
                        onUpdateType = { type, lang -> vm.toggleTranslationType(type, lang) },
                        onTypeToNull = { vm.translationTypeToNull() },
                        modifier = Modifier
                    )
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (lyrics.translation?.lyrics is TranslatedText.Plain) {
                            val plain by rememberUpdatedState(lyrics.translation?.lyrics as TranslatedText.Plain)
                            TextField(
                                value = plain.t,
                                onValueChange = { str ->
                                    vm.updateTranslationPlainText(
                                        TranslatedLyrics(
                                            TranslatedText.Plain(str),
                                            lyrics.translation?.language ?: language
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                        } else if (lyrics.translation?.lyrics is TranslatedText.Scroll)
                            translationSet.forEachIndexed { index, lyricLine ->
                                CustomSetItem(lyricLine) { str ->
                                    vm.updateTranslationSetTextAt(index, str)
                                }
                            }
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val result = vm.upsertLyrics(it.uri.toString())
                                if (!result) toast.makeMessage("Failed to upsert lyrics")
                                else onUpsert()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(if (isLandscape) 0.3f else 0.5f)
                    ) {
                        Text(
                            text = "Upsert Lyrics", Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            controller?.let { mc ->
                pickedAudio?.let { af ->
                    PlayerController(
                        mc, vm, af, playerToggle, { playerToggle = it }, getUIStyle, Modifier
                            .align(Alignment.BottomCenter)
                            .height(if (playerToggle) 90.dp else 30.dp)
                    )
                }

            }
        }
    } else SearchForSong(
        query = query, onQueryChange = { vm.updateQuery(it) },
        getUIStyle = getUIStyle, songs = songs, pickedAudio = pickedAudio,
        onPickSong = {
            if (scrollSet.isEmpty()) onPickSong(it)
            else if (it.durationMillis < scrollSet[scrollSet.lastIndex].range.last)
                toast.makeMessage("Song is too short")
            else onPickSong(it)
        }
    )
}

@Composable
private fun SearchForSong(
    topContent: @Composable () -> Unit = {},
    query: String,
    onQueryChange: (String) -> Unit,
    getUIStyle: GetUIStyle,
    onPickSong: (AudioFile) -> Unit,
    songs: List<AudioFile>,
    pickedAudio: AudioFile?
) {
    val state = rememberLazyListState()
    LazyColumnScrollbar(
        state = state,
        settings = ScrollbarSettings(
            thumbUnselectedColor = getUIStyle.unSelectedThumbColor(),
            thumbSelectedColor = getUIStyle.selectedThumbColor()
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = state,
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            item { topContent() }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 18.sp),
                    shape = ShapeDefaults.Large,
                )
            }

            items(songs, key = { it.id }) { s ->
                val isPicked = s.id == pickedAudio?.id
                SongRow(s, getUIStyle, isPicked, LocalContext.current) { onPickSong(s) }
            }
        }
    }
}

@Composable
private fun SimpleItem(
    text: String, value: String, onValueChange: (String) -> Unit, placeholder: String,
    modifier: Modifier
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = text, textDecoration = TextDecoration.Underline,
            style = MaterialTheme.typography.titleLarge
        )
        TextField(
            value = value,
            placeholder = { Text(placeholder) },
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}

@Composable
private fun SyncryonizedItem(scrollSet: List<LyricLine>, modifier: Modifier, onClick: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = "Synchronized Lyrics", textDecoration = TextDecoration.Underline,
            style = MaterialTheme.typography.titleLarge
        )
        if (scrollSet.isEmpty()) Button(onClick = onClick) { Text("Add lyrics line") }
    }
}

@Composable
private fun ScrollSetItem(
    scrollSet: List<LyricLine>, index: Int, lyricLine: LyricLine,
    vm: LyricsEditorVM, durationMillis: Long
) {
    val previousRange =
        rememberUpdatedState(scrollSet).value.takeIf { set ->
            (index - 1) in set.indices
        }?.get(index - 1)?.range
    val nextRange = rememberUpdatedState(scrollSet).value.takeIf { set ->
        (index + 1) in set.indices
    }?.get(index + 1)?.range
    var sliderRange by remember {
        mutableStateOf(
            lyricLine.range.first / durationMillis.toFloat()..
                    lyricLine.range.last / durationMillis.toFloat()
        )
    }
    key(lyricLine.range.first, lyricLine.range.last) {
        sliderRange =
            lyricLine.range.first / durationMillis.toFloat()..
                    lyricLine.range.last / durationMillis.toFloat()
    }

    TextField(
        value = lyricLine.text,
        onValueChange = { str -> vm.updateScrollSetTextAt(index, str) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )
    TrimRangeChooser(
        sliderRange = sliderRange, durationMillis,
        lyricLine.range.first, lyricLine.range.last,
        previousRange = previousRange, nextRange = nextRange
    ) { range ->
        sliderRange =
            range.first / durationMillis.toFloat()..range.last / durationMillis.toFloat()
        vm.updateScrollLineRangeAt(index, range.first, range.last)
    }

    if (index == scrollSet.lastIndex) {
        if (lyricLine.range.last < durationMillis)
            Button(onClick = {
                vm.addToScrollSet(lyricLine.range.last, durationMillis)
            }) { Text("Add lyrics line") }
        Button(onClick = { vm.removeFromScrollSet() }) { Text("Remove lyrics line") }
    }
}

@Composable
private fun CustomizedLyricsItem(
    scrollSet: List<LyricLine>, onGetLanguage: () -> Unit, textType: TranslatedLyrics?,
    onUpdateType: (TranslatedText, String) -> Unit, onTypeToNull: () -> Unit,
    language: String, toast: XCToast, type: String, modifier: Modifier,

    ) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = "$type Lyrics",
            textDecoration = TextDecoration.Underline,
            style = MaterialTheme.typography.titleLarge
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterChip(
                selected = textType?.lyrics is TranslatedText.Plain,
                onClick = {
                    if (textType?.lyrics == null) onGetLanguage()
                    if (language.isBlank()) toast.makeMessage("Must specify language")
                    else if (textType?.lyrics !is TranslatedText.Plain)
                        onUpdateType(TranslatedText.Plain(""), language)
                },
                label = { Text("Plain $type") }
            )

            if (scrollSet.isNotEmpty())
                FilterChip(
                    selected = textType?.lyrics is TranslatedText.Scroll,
                    onClick = {
                        if (textType?.lyrics == null) onGetLanguage()
                        if (language.isBlank()) toast.makeMessage("Must specify language")
                        else if (textType?.lyrics !is TranslatedText.Scroll)
                            onUpdateType(
                                TranslatedText.Scroll(scrollSet.map { set ->
                                    LyricLine(set.range, "")
                                }.toSet()), language
                            )
                    },
                    label = { Text("Synchronized $type") }
                )
        }
        FilterChip(
            selected = textType?.lyrics == null,
            onClick = onTypeToNull,
            label = { Text("No $type") }
        )
    }
}

@Composable
private fun CustomSetItem(lyricLine: LyricLine, onUpdate: (String) -> Unit) {
    TextField(
        value = lyricLine.text,
        onValueChange = onUpdate,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )
    TimeEditRow(lyricLine.range.first, lyricLine.range.last)
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
