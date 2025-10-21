package com.xandy.lite.views.lyrics

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xandy.lite.controllers.view.models.LyricsVM
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.models.LyricsXclfAdapter
import com.xandy.lite.models.XCToast
import com.xandy.lite.models.application.AppVMProvider
import com.xandy.lite.models.ellipsize
import com.xandy.lite.models.ui.InsertResult
import com.xandy.lite.ui.functions.DeleteModal
import com.xandy.lite.ui.functions.ExportLyricDialog
import com.xandy.lite.ui.functions.OverwriteItem
import com.xandy.lite.ui.functions.SmallAddButton
import com.xandy.lite.ui.functions.item.details.LyricsRow
import com.xandy.lite.ui.theme.GetUIStyle
import kotlinx.coroutines.delay
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
    var showExportDialog by rememberSaveable {
        mutableStateOf<Pair<Lyrics?, Boolean>>(Pair(null, false))
    }
    var showImportDialog by rememberSaveable {
        mutableStateOf<Pair<Lyrics?, Boolean>>(Pair(null, false))
    }
    val context = LocalContext.current
    val toast = XCToast(context)
    var show by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val onFinish: () -> Unit = {
        enabled = true
        showExportDialog = Pair(null, false)
        showModal = Pair(null, false)
        showImportDialog = Pair(null, false)
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        // Get display name and validate extension
        val name = context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
        if (name?.endsWith(".xclf", ignoreCase = true) == true) {
            coroutineScope.launch {
                enabled = false
                val lyrics = LyricsXclfAdapter.importLyricsFromXclf(context, uri)
                if (lyrics == null) {
                    toast.makeMessage("Failed to extract lyrics"); onFinish()
                    return@launch
                }
                val result = lyricsVM.importLyrics(lyrics)
                when (result) {
                    is InsertResult.Exists -> {
                        showImportDialog = Pair(lyrics, true)
                        enabled = true
                    }

                    is InsertResult.Failure -> toast.makeMessage("Import Failed.")
                    is InsertResult.Success -> toast.makeMessage("Imported Successfully!")
                }
                if (result !is InsertResult.Exists) onFinish()
            }
        } else {
            toast.makeMessage("Please pick a .xclf file")
        }
    }
    LaunchedEffect(Unit) { delay(100);show = true }
    LazyColumnScrollbar(
        state = state,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (show && allLyrics.isEmpty()) Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text("No lyrics available")
            } else LazyColumn {
                items(allLyrics, key = { it.lyrics.id }) { l ->
                    LyricsRow(
                        l, getUIStyle = getUIStyle,
                        onEdit = {},
                        onExport = { showExportDialog = Pair(l.lyrics, true) },
                        onDelete = { showModal = Pair(l.lyrics, true) }
                    )
                }
            }
            SmallAddButton(
                onClick = { expanded = true },
                getUIStyle = getUIStyle, enabled = enabled,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Bottom
            ) {
                val bottomLeftModifier = Modifier
                    .padding(bottom = 12.dp)
                    .align(Alignment.End)
                Box(
                    modifier = bottomLeftModifier,
                ) {
                    DropdownMenu(
                        expanded = expanded, onDismissRequest = { expanded = false },
                    ) {
                        DropdownMenuItem(
                            onClick = {},
                            text = { Text("Create") })
                        DropdownMenuItem(
                            onClick = { launcher.launch(arrayOf("application/octet-stream")) },
                            text = { Text("Import") }

                        )
                    }
                }
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
                            toast.makeMessage("Null lyrics"); onFinish()
                            return@launch
                        }
                        val result = lyricsVM.deleteLyrics(lyrics)
                        if (!result)
                            toast.makeMessage("Failed to delete lyrics")
                        onFinish()
                    }
                },
                string = (showModal.first?.description ?: showModal.first?.plain)?.ellipsize()
            )
        }
        if (showExportDialog.second) {
            ExportLyricDialog(
                onDismiss = { if (enabled) showExportDialog = Pair(null, false) },
                onSubmit = {
                    coroutineScope.launch {
                        enabled = false
                        val lyrics = showExportDialog.first
                        if (lyrics == null) {
                            toast.makeMessage("Null lyrics"); onFinish()
                            return@launch
                        }
                        val result = LyricsXclfAdapter.exportLyricsToXclf(it, lyrics, context)
                        if (result == null) toast.makeMessage("Unable to export file")
                        else toast.makeMessage("Exported file, Uri = ${result.path}")
                        onFinish()
                    }
                },
                getUIStyle = getUIStyle, enabled = enabled
            )
        }
        if (showImportDialog.second) {
            OverwriteItem(
                onDismiss = { if (enabled) showImportDialog = Pair(null, false) },
                onSubmit = {
                    coroutineScope.launch {
                        enabled = false
                        val lyrics = showImportDialog.first
                        if (lyrics == null) {
                            toast.makeMessage("Null lyrics"); onFinish()
                            return@launch
                        }
                        val result = lyricsVM.updateLyrics(lyrics)
                        if (!result) toast.makeMessage("Unable to overwrite lyrics")
                        else toast.makeMessage("Successfully overwrote lyrics")
                        onFinish()
                    }
                },
                getUIStyle = getUIStyle, enabled = enabled,
                title = "Overwrite lyrics?",
                text = "These lyrics already exist!\nWould you like to overwrite it?",
                confirmButtonText = "Overwrite"
            )
        }
    }
}