package com.xandy.lite.ui.functions

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xandy.lite.db.tables.LyricsWithAudio
import com.xandy.lite.ui.functions.item.details.LyricsRow
import com.xandy.lite.ui.theme.GetUIStyle


@Composable
fun AddPlDialog(
    showDialog: Boolean, onDismiss: () -> Unit, onSubmit: (String) -> Unit, getUIStyle: GetUIStyle,
    enabled: Boolean
) {
    val context = LocalContext.current
    if (showDialog) {
        var name by rememberSaveable { mutableStateOf("") }
        Dialog(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(getUIStyle.dialogBackGroundColor(), RoundedCornerShape(16.dp))
                    .padding(horizontal = 4.dp, vertical = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Enter playlist name", textAlign = TextAlign.Center,
                    fontSize = 18.sp, style = MaterialTheme.typography.titleLarge
                )
                TextField(
                    value = name, onValueChange = { name = it },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                )
                Button(
                    onClick = {
                        if (name.isEmpty()) Toast.makeText(
                            context, "Name can't be empty", Toast.LENGTH_LONG
                        ).show()
                        else onSubmit(name)
                    }, enabled = enabled
                ) { Text("Add") }
            }
        }
    }
}

@Composable
fun ChangePlNameDialog(
    showDialog: Boolean, onDismiss: () -> Unit, onSubmit: (String) -> Unit, getUIStyle: GetUIStyle,
    enabled: Boolean, originalName: String?
) {
    val context = LocalContext.current
    if (showDialog) {
        var name by rememberSaveable { mutableStateOf(originalName ?: "") }
        Dialog(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(getUIStyle.dialogBackGroundColor(), RoundedCornerShape(16.dp))
                    .padding(horizontal = 4.dp, vertical = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Enter playlist name", textAlign = TextAlign.Center,
                    fontSize = 18.sp, style = MaterialTheme.typography.titleLarge
                )
                TextField(
                    value = name, onValueChange = { name = it },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                )
                Button(
                    onClick = {
                        if (name.isEmpty()) Toast.makeText(
                            context, "Name can't be empty", Toast.LENGTH_LONG
                        ).show()
                        else onSubmit(name)
                    }, enabled = enabled
                ) { Text("Rename") }
            }
        }
    }
}

@Composable
fun UpdateAudioListMetadata(
    showDialog: Boolean, onDismiss: () -> Unit, onSubmit: (String) -> Unit, getUIStyle: GetUIStyle,
    enabled: Boolean, metadataToUpdate: String
) {
    val context = LocalContext.current
    if (showDialog) {
        var name by rememberSaveable { mutableStateOf("") }
        val string = metadataToUpdate.lowercase().replaceFirstChar { it.uppercase() }
        Dialog(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(getUIStyle.dialogBackGroundColor(), RoundedCornerShape(16.dp))
                    .padding(horizontal = 4.dp, vertical = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Enter $string", textAlign = TextAlign.Center,
                    fontSize = 18.sp, style = MaterialTheme.typography.titleLarge
                )
                TextField(
                    value = name, onValueChange = { name = it },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                )
                Button(
                    onClick = {
                        if (name.isEmpty()) Toast.makeText(
                            context, "$string can't be empty", Toast.LENGTH_LONG
                        ).show()
                        else onSubmit(name)
                    }, enabled = enabled
                ) { Text("Rename") }
            }
        }
    }
}

@Composable
fun LyricsListDialog(
    showDialog: Boolean, onDismiss: () -> Unit, onSubmit: (String) -> Unit, getUIStyle: GetUIStyle,
    enabled: Boolean, list: List<LyricsWithAudio>
) {
    if (showDialog) {
        Dialog(onDismissRequest = {
            if (enabled) onDismiss()
        }) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(getUIStyle.dialogBackGroundColor(), RoundedCornerShape(16.dp))
                    .padding(horizontal = 4.dp, vertical = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                item {
                    if (list.isEmpty())
                        Text("No lyrics available")
                    else
                        Text("Long Press to expand lyrics details")
                }
                items(list) { item ->
                    LyricsRow(item, getUIStyle) { onSubmit(item.lyrics.id) }
                }
            }
        }
    }
}

@Composable
fun ExportLyricDialog(
    onDismiss: () -> Unit, onSubmit: (String?) -> Unit, getUIStyle: GetUIStyle, enabled: Boolean
) {
    var fileName by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text("Export lyrics?")
        }, confirmButton = {
            Button(
                onClick = { onSubmit(fileName.takeIf { it.isNotBlank() }) }, enabled = enabled
            ) { Text("Export") }
        },
        dismissButton = {
            Button(onClick = onDismiss, enabled = enabled) { Text("Cancel") }
        }, text = {
            TextField(
                value = fileName,
                onValueChange = { fileName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                suffix = { Text(".xclf") }
            )
        },
        titleContentColor = getUIStyle.themedOnContainerColor(),
        textContentColor = getUIStyle.themedOnContainerColor(),
        containerColor = getUIStyle.dialogBackGroundColor()
    )
}

@Composable
fun OverwriteItem(
    onDismiss: () -> Unit, onSubmit: () -> Unit, title: String,  confirmButtonText: String,
    text: String, getUIStyle: GetUIStyle, enabled: Boolean
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(title) },
        confirmButton = {
            Button(
                onClick = { onSubmit() }, enabled = enabled
            ) { Text(confirmButtonText) }
        },
        dismissButton = {
            Button(onClick = onDismiss, enabled = enabled) { Text("Cancel") }
        },
        text = { Text(text = text, modifier = Modifier.fillMaxWidth()) },
        titleContentColor = getUIStyle.themedOnContainerColor(),
        textContentColor = getUIStyle.themedOnContainerColor(),
        containerColor = getUIStyle.dialogBackGroundColor()
    )
}