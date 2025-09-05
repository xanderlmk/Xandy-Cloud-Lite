package com.xandy.lite.navigation

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xandy.lite.models.ui.ShowModalFor
import com.xandy.lite.ui.functions.DeleteModal
import com.xandy.lite.ui.functions.SelectImageModal
import com.xandy.lite.ui.functions.item.details.Artwork


@Composable
fun ImagePicker(
    artworkList: List<Uri>, isLandscape: Boolean,
    onImagePicked: (Uri) -> Unit, showModal: ShowModalFor, onDismiss: () -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let(onImagePicked)
    }
    SelectImageModal(
        onDismissRequest = onDismiss, show = showModal !is ShowModalFor.Idle,
        onSelectLocal = { showDialog = true },
        onSelectGallery = { launcher.launch("image/*") }
    )
    LocalArtwork(
        onDismissRequest = { showDialog = false },
        onSelect = { showDialog = false; onImagePicked(it) },
        list = artworkList, showDialog = showDialog, isLandscape = isLandscape
    )
}

@Composable
private fun LocalArtwork(
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

@Composable
fun WithRequestLauncher(
    onResultOk: () -> Unit,
    content: @Composable (ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>) -> Unit
) {
    val context = LocalContext.current
    val writeRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onResultOk()
        } else Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
    }
    content(writeRequestLauncher)
}

@Composable
fun WithDeleteModalAndLauncher(
    onResultOk: () -> Unit, showModal: Boolean, onDismissRequest: () -> Unit, string: String?,
    onDelete: (ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>) -> Unit,
    content: @Composable (ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>) -> Unit
) {
    WithRequestLauncher(
        onResultOk = onResultOk
    ) { writeRequestLauncher ->
        content(writeRequestLauncher)
        if (showModal) {
            DeleteModal(
                onDismissRequest = onDismissRequest,
                onDelete = { onDelete(writeRequestLauncher) }, string = string
            )
        }
    }
}