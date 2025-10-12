package com.xandy.lite.views

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xandy.lite.controllers.view.models.LyricsVM
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.models.XCToast
import com.xandy.lite.models.application.AppVMProvider
import com.xandy.lite.models.ellipsize
import com.xandy.lite.ui.functions.DeleteModal
import com.xandy.lite.ui.functions.item.details.LyricsRow
import com.xandy.lite.ui.theme.GetUIStyle
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyColumnScrollbar

@Composable
fun LyricsListView(getUIStyle: GetUIStyle) {
    val lyricsVM: LyricsVM = viewModel(factory = AppVMProvider.Factory)
    val allLyrics by lyricsVM.lyricsList.collectAsStateWithLifecycle()
    val state = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var enabled by rememberSaveable { mutableStateOf(true) }
    var showModal by rememberSaveable { mutableStateOf<Pair<Lyrics?, Boolean>>(Pair(null, false)) }
    val toast = XCToast(LocalContext.current)

    LazyColumnScrollbar(
        state = state,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn {
            items(allLyrics) { lyrics ->
                LyricsRow(lyrics, onEdit = {}, onDelete = {
                    showModal = Pair(lyrics.lyrics, true)
                }, getUIStyle)
            }
        }
        if (showModal.second) {
            DeleteModal(
                onDismissRequest = { if (enabled) showModal = Pair(null, false) },
                onDelete = {
                    coroutineScope.launch {
                        enabled = false
                        val lyrics = showModal.first
                        if (lyrics == null) {
                            toast.makeMessage("Null lyrics")
                            return@launch
                        }
                        val result = lyricsVM.deleteLyrics(lyrics)
                        if(!result)
                            toast.makeMessage("Failed to delete lyrics")
                        enabled = true
                        showModal = Pair(null, false)
                    }
                },
                string = (showModal.first?.description ?: showModal.first?.plain)?.ellipsize()
            )
        }
    }
}