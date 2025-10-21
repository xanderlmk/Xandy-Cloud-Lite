package com.xandy.lite.views.lyrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xandy.lite.controllers.view.models.EditAudioVM
import com.xandy.lite.controllers.view.models.LyricsEditorVM
import com.xandy.lite.db.tables.LyricLine
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.models.application.AppVMProvider
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.theme.GetUIStyle
import my.nanihadesuka.compose.ColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import kotlin.math.roundToLong


@Composable
fun LyricsEditorView(getUIStyle: GetUIStyle) {
    val editorVM: LyricsEditorVM = viewModel(factory = AppVMProvider.Factory)
    val lyrics by editorVM.lyrics.collectAsStateWithLifecycle()
    val scrollSet by editorVM.scrollSet.collectAsStateWithLifecycle()
    val ci = ContentIcons(getUIStyle)
    val state = rememberScrollState()
    ColumnScrollbar(
        state = state,
        modifier = Modifier.fillMaxSize(),
        settings = ScrollbarSettings(
            thumbSelectedColor = getUIStyle.selectedThumbColor(),
            thumbUnselectedColor = getUIStyle.unSelectedThumbColor()
        )
    ) {
        Column(
            modifier = Modifier.verticalScroll(state),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LyricsOptions(ci, lyrics, 0L, scrollSet, editorVM)
        }
    }
}

@Composable
private fun LyricsOptions(
    ci: ContentIcons, lyrics: Lyrics, durationMs: Long, set: List<LyricLine>,
    lyricsEditorVM: LyricsEditorVM
) {
    Text(text = "Plain Lyrics", textDecoration = TextDecoration.Underline)
    TextField(
        value = lyrics.plain,
        placeholder = { Text("Type or paste lyrics here") },
        onValueChange = { lyricsEditorVM.updatePlain(it) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )
    Text(text = "Synchronized Lyrics", textDecoration = TextDecoration.Underline)
    if (set.isEmpty()) Button(onClick = {
        lyricsEditorVM.addToScrollSet(0, durationMs)
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
            onValueChange = { lyricsEditorVM.updateScrollLineTextAt(index, it) },
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
            lyricsEditorVM.updateScrollLineRangeAt(index, it.first, it.last)
        }

        if (index == set.lastIndex) {
            if (lyricLine.range.last < durationMs)
                Button(onClick = {
                    lyricsEditorVM.addToScrollSet(lyricLine.range.last, durationMs)
                }) { Text("Add lyrics line") }
            Button(onClick = { lyricsEditorVM.removeScrollLast() }) { Text("Remove lyrics line") }
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
