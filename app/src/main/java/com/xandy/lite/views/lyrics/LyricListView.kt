package com.xandy.lite.views.lyrics

import android.net.Uri
import android.os.Parcelable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xandy.lite.R
import com.xandy.lite.controllers.view.models.LyricsVM
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.models.lyrics.adapter.LyricsXclfAdapter
import com.xandy.lite.models.XCToast
import com.xandy.lite.models.application.XANDY_CLOUD
import com.xandy.lite.models.ellipsize
import com.xandy.lite.models.lyrics.adapter.ExportResult
import com.xandy.lite.models.ui.InsertResult
import com.xandy.lite.ui.functions.DeleteModal
import com.xandy.lite.ui.functions.ExportLyricDialog
import com.xandy.lite.ui.functions.OverwriteItem
import com.xandy.lite.ui.functions.SmallAddButton
import com.xandy.lite.ui.functions.item.details.LyricsRow
import com.xandy.lite.ui.GetUIStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import my.nanihadesuka.compose.LazyColumnScrollbar


@Parcelize
@Serializable
private data class Pair(
    @SerialName(value = "Xandy-Cloud.Full.Lyrics")
    val l: Lyrics?, val b: Boolean
) : Parcelable

@Composable
fun LyricsListView(lyricsVM: LyricsVM, onEdit: (String) -> Unit, getUIStyle: GetUIStyle) {
    val allLyrics by lyricsVM.lyricsList.collectAsStateWithLifecycle()
    val state = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var enabled by rememberSaveable { mutableStateOf(true) }
    var showModal by rememberSaveable { mutableStateOf(Pair(null, false)) }
    var showExportDialog by rememberSaveable {
        mutableStateOf(Pair(null, false))
    }
    var showImportDialog by rememberSaveable {
        mutableStateOf(Pair(null, false))
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

                    is InsertResult.Failure -> toast.makeMessage(toast.importFailed)
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
                Text(stringResource(R.string.no_lyrics_available))
            } else LazyColumn {
                items(allLyrics, key = { it.lyrics.id }) { l ->
                    LyricsRow(
                        l, getUIStyle = getUIStyle,
                        onEdit = { onEdit(l.lyrics.id) },
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
                            onClick = { expanded = false; onEdit("") },
                            text = { Text(stringResource(R.string.Create)) })
                        DropdownMenuItem(
                            onClick = {
                                launcher.launch(
                                    arrayOf("application/octet-stream", LyricsXclfAdapter.MIME_TYPE)
                                )
                            },
                            text = { Text(stringResource(R.string.Import)) }

                        )
                    }
                }
            }
        }

        if (showModal.b) {
            DeleteModal(
                onDismissRequest = { if (enabled) showModal = Pair(null, false) },
                onDelete = {
                    coroutineScope.launch {
                        enabled = false
                        val lyrics = showModal.l
                        if (lyrics == null) {
                            toast.makeMessage(toast.nullLyrics); onFinish()
                            return@launch
                        }
                        val result = lyricsVM.deleteLyrics(lyrics)
                        if (!result)
                            toast.makeMessage("Failed to delete lyrics")
                        onFinish()
                    }
                },
                string = (showModal.l?.description ?: showModal.l?.plain)?.ellipsize()
            )
        }
        if (showExportDialog.b) {
            var submitButtonText by rememberSaveable {
                mutableStateOf(context.getString(R.string.Export))
            }
            var exists by rememberSaveable { mutableStateOf(false) }
            ExportLyricDialog(
                onDismiss = { if (enabled) showExportDialog = Pair(null, false) },
                onSubmit = {
                    coroutineScope.launch {
                        enabled = false
                        val lyrics = showExportDialog.l
                        if (lyrics == null) {
                            toast.makeMessage(toast.nullLyrics); onFinish()
                            return@launch
                        }
                        val result = LyricsXclfAdapter.exportLyricsToXclf(it, lyrics, context)
                        when (val r = result) {
                            ExportResult.Exists -> {
                                Log.w(XANDY_CLOUD, "File already exist.")
                                submitButtonText = "Create New"
                                exists = true; enabled = true
                            }

                            ExportResult.Failed -> {
                                toast.makeMessage("Unable to get save file")
                                onFinish()
                            }

                            is ExportResult.Success -> {
                                if (r.uri == null) toast.makeMessage("Unable to get file path")
                                else toast.makeMessage("Exported file, path = ${r.uri.path}")
                                onFinish()
                            }
                        }
                    }
                },
                onCreateNew = {
                    coroutineScope.launch {
                        enabled = false
                        val lyrics = showExportDialog.l
                        if (lyrics == null) {
                            toast.makeMessage(toast.nullLyrics); onFinish()
                            return@launch
                        }
                        val result = LyricsXclfAdapter.exportNewLyricsToXclf(it, lyrics, context)
                        if (result == null) toast.makeMessage("Unable to get save file or get path")
                        else toast.makeMessage("Exported file, path = ${result.path}")
                        onFinish()
                    }
                },
                onReplace = {
                    coroutineScope.launch {
                        enabled = false
                        val lyrics = showExportDialog.l
                        if (lyrics == null) {
                            toast.makeMessage(toast.nullLyrics); onFinish()
                            return@launch
                        }
                        val name = "${it ?: lyrics.id}.xclf"
                        val result =
                            LyricsXclfAdapter.exportOverwrittenLyrics(name, lyrics, context)
                        if (result == null) toast.makeMessage("Unable to get save file or get path")
                        else toast.makeMessage("Exported file, path = ${result.path}")
                        onFinish()
                    }
                },
                submitButtonText = { submitButtonText }, exists = { exists },
                getUIStyle = getUIStyle, enabled = enabled
            )
        }
        if (showImportDialog.b) {
            OverwriteItem(
                onDismiss = { if (enabled) showImportDialog = Pair(null, false) },
                onSubmit = {
                    coroutineScope.launch {
                        enabled = false
                        val lyrics = showImportDialog.l
                        if (lyrics == null) {
                            toast.makeMessage(toast.nullLyrics); onFinish()
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
                text = "These lyrics already exist!\nOverwriting will replace the existing lyrics.",
                confirmButtonText = stringResource(R.string.Overwrite)
            )
        }
    }
}