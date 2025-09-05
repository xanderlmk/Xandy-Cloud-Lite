package com.xandy.lite.views.edit.audio

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.ui.functions.SelectImageModal
import com.xandy.lite.ui.functions.item.details.Artwork


@Composable
fun EditAudioView(
    audioFile: AudioFile, enabled: Boolean,
    allMediaArtwork: List<Uri>, onUpdate: (AudioFile) -> Unit
) {
    var audio by rememberSaveable { mutableStateOf(audioFile) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape)
        HorizontalEditAudioView(
            audio = audio, enabled = enabled, allMediaArtwork = allMediaArtwork,
            onAudioChange = { audio = it },
            onUpdate = onUpdate
        )
    else VerticalEditAudioView(
        audio = audio, enabled = enabled, allMediaArtwork = allMediaArtwork,
        onAudioChange = { audio = it },
        onUpdate = onUpdate
    )
}

@Composable
fun ImagePicker(
    artworkList: List<Uri>, enabled: Boolean, isLandscape: Boolean,
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
        onSelectGallery = { launcher.launch("image/*") }
    )
    LocalArtwork(
        onDismissRequest = { showDialog = false; showModal = false },
        onSelect = { showDialog = false; showModal = false; onImagePicked(it) },
        list = artworkList, showDialog = showDialog, isLandscape = isLandscape
    )
    Button(
        onClick = { showModal = true },
        enabled = enabled, modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text("Choose Artwork")
    }
}

@Composable
fun LocalArtwork(
    onDismissRequest: () -> Unit, onSelect: (Uri) -> Unit, list: List<Uri>,
    showDialog: Boolean, isLandscape: Boolean
) {

    val rows = list.chunked(if (isLandscape) 4 else 2)
    if (showDialog) {
        if (isLandscape) {
            Dialog(
                onDismissRequest = onDismissRequest,
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.75f),
                    contentPadding = PaddingValues(4.dp)
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
                                    Artwork(image, Modifier.clickable { onSelect(image) })
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
        } else {
            Dialog(onDismissRequest = onDismissRequest) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.90f),
                    contentPadding = PaddingValues(4.dp)
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
                                    Artwork(image, Modifier.clickable { onSelect(image) })
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