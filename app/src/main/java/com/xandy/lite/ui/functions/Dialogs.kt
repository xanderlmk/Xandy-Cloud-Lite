package com.xandy.lite.ui.functions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xandy.lite.R
import com.xandy.lite.controllers.media.store.DateParts
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.db.tables.LyricsWithAudio
import com.xandy.lite.ui.functions.item.details.LyricsRow
import com.xandy.lite.ui.GetUIStyle
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.DialogProperties


@Composable
fun AudioDetailsDialog(
    audio: AudioFile?, showDialog: Boolean, onDismiss: () -> Unit, getUIStyle: GetUIStyle
) {
    val unknownText = stringResource(R.string.Unknown)
    if (showDialog) {
        audio?.let { af ->
            val date = DateParts(af.year, af.month, af.day)
            val dateFormat = SimpleDateFormat(
                "EEE MMM dd HH:mm yyyy",
                LocalConfiguration.current.locales.get(0)
            )
            val createdOn = dateFormat.format(af.createdOn)
            val dateModified = dateFormat.format(af.dateModified)
            Dialog(onDismissRequest = onDismiss) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(getUIStyle.dialogBackGroundColor(), RoundedCornerShape(16.dp))
                        .padding(horizontal = 6.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = stringResource(R.string.Details), textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp, style = MaterialTheme.typography.titleLarge
                    )
                    Details(stringResource(R.string.Title), af.title, 12.sp, textFontSize = 15.sp)
                    Details(
                        stringResource(R.string.Artist), af.artist ?: unknownText,
                        12.sp, textFontSize = 15.sp
                    )
                    Details(
                        stringResource(R.string.Album), af.album ?: unknownText,
                        12.sp, textFontSize = 15.sp
                    )
                    Details(
                        stringResource(R.string.Genre), af.genre ?: unknownText,
                        12.sp, textFontSize = 15.sp
                    )
                    Details(
                        stringResource(R.string.release_date),
                        date.toString().takeIf { it.isNotBlank() } ?: unknownText,
                        12.sp, textFontSize = 15.sp
                    )
                    Details(
                        stringResource(R.string.created_on), createdOn,
                        12.sp, textFontSize = 15.sp
                    )
                    Details(
                        stringResource(R.string.last_modified), dateModified,
                        12.sp, textFontSize = 15.sp
                    )
                    Button(onClick = onDismiss, modifier = Modifier.padding(8.dp)) {
                        Text(stringResource(R.string.Close))
                    }
                }
            }
        } ?: Dialog(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(getUIStyle.dialogBackGroundColor(), RoundedCornerShape(16.dp))
                    .padding(horizontal = 4.dp, vertical = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.null_track), textAlign = TextAlign.Center,
                    fontSize = 18.sp, style = MaterialTheme.typography.titleLarge
                )
                Button(onClick = onDismiss) { Text(stringResource(R.string.Close)) }
            }
        }
    }
}

@Composable
fun Details(
    header: String, text: String, headerFontSize: TextUnit, textFontSize: TextUnit
) {
    Text(
        text = header, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth(),
        fontSize = headerFontSize, style = MaterialTheme.typography.headlineMedium
    )
    Text(
        text = text, textAlign = TextAlign.Start,
        fontSize = textFontSize, style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp)
    )
}

@Composable
fun AddPlDialog(
    showDialog: Boolean, onDismiss: () -> Unit, onSubmit: (String) -> Unit, getUIStyle: GetUIStyle,
    enabled: Boolean
) {
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
                    text = stringResource(R.string.enter_playlist_name),
                    textAlign = TextAlign.Center,
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
                    onClick = { onSubmit(name) },
                    enabled = enabled && name.isNotBlank()
                ) { Text(stringResource(R.string.Add)) }
            }
        }
    }
}

@Composable
fun ChangePlNameDialog(
    showDialog: Boolean, onDismiss: () -> Unit, onSubmit: (String) -> Unit, getUIStyle: GetUIStyle,
    enabled: Boolean, originalName: String?
) {
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
                    text = stringResource(R.string.enter_playlist_name),
                    textAlign = TextAlign.Center,
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
                        onSubmit(name)
                    }, enabled = enabled && name.isNotBlank()
                ) { Text(stringResource(R.string.Rename)) }
            }
        }
    }
}

@Composable
fun UpdateAudioListMetadata(
    showDialog: Boolean, onDismiss: () -> Unit, onSubmit: (String) -> Unit, getUIStyle: GetUIStyle,
    enabled: Boolean, metadataToUpdate: String
) {
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
                        onSubmit(name)
                    }, enabled = enabled && name.isNotBlank()
                ) { Text(stringResource(R.string.Rename)) }
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
                        Text(stringResource(R.string.no_lyrics_available))
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
fun LyricDialog(
    show: Boolean, enabled: Boolean, replace: Boolean, onDismiss: () -> Unit, l: Lyrics?,
    onImport: () -> Unit, onReplace: () -> Unit, getUIStyle: GetUIStyle
) {
    if (show)
        Dialog(
            onDismissRequest = { if (enabled) onDismiss() },
            properties = DialogProperties(
                dismissOnClickOutside = false
            )
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(getUIStyle.dialogBackGroundColor(), RoundedCornerShape(16.dp))
                    .border(2.dp, getUIStyle.themedOnContainerColor(), RoundedCornerShape(16.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                if (l != null && !replace) {
                    Text(
                        text = stringResource(R.string.Lyrics),
                        textDecoration = TextDecoration.Underline,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = l.plain, textAlign = TextAlign.Start
                    )
                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
                    Text(
                        text = if (l.scroll.isNullOrEmpty()) "No Synchronized Lyrics"
                        else "Has Synchronized Lyrics",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
                    l.pronunciation?.let {
                        Text(
                            text = "Has Pronunciation Lyrics",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            text = "Language: ${Locale.forLanguageTag(it.language).displayLanguage}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } ?: Text(
                        text = "No Pronunciation Lyrics",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
                    l.translation?.let {
                        Text(
                            text = "Has Translation Lyrics",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            text =
                                "Language: ${Locale.forLanguageTag(it.language).displayLanguage}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } ?: Text(
                        text = "No Translation Lyrics",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = onDismiss, enabled = enabled) {
                            Text(stringResource(R.string.Cancel))
                        }
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Button(
                            onClick = onImport, enabled = enabled
                        ) { Text(stringResource(R.string.Import)) }
                    }
                } else if (l != null) {
                    Text(
                        text = "Overwrite lyrics?",
                        textDecoration = TextDecoration.Underline,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "These lyrics already exist!\nWould you like to overwrite it?",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = onDismiss, enabled = enabled) {
                            Text(stringResource(R.string.Cancel))
                        }
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Button(
                            onClick = onReplace, enabled = enabled
                        ) { Text(stringResource(R.string.Overwrite)) }

                    }
                } else Text(
                    text = "No lyrics",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
}

@Composable
fun ExportLyricDialog(
    onDismiss: () -> Unit, onSubmit: (String?) -> Unit, submitButtonText: () -> String,
    exists: () -> Boolean, getUIStyle: GetUIStyle, enabled: Boolean,
    onCreateNew: (String?) -> Unit, onReplace: (String?) -> Unit,
) {
    var fileName by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text("Export lyrics?")
        }, confirmButton = {
            Button(
                onClick = {
                    if (!exists()) onSubmit(fileName.takeIf { it.isNotBlank() })
                    else onCreateNew(fileName.takeIf { it.isNotBlank() })
                }, enabled = enabled && filenameRegex.matches(fileName)
            ) { Text(submitButtonText()) }
        },
        dismissButton = {
            if (!exists()) Button(onClick = onDismiss, enabled = enabled) {
                Text(stringResource(R.string.Cancel))
            }
            else Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onDismiss, enabled = enabled) {
                    Text(stringResource(R.string.Cancel))
                }
                Spacer(Modifier.padding(horizontal = 4.dp))
                Button(
                    onClick = { onReplace(fileName.takeIf { it.isNotBlank() }) }, enabled = enabled
                ) { Text("Replace") }

            }
        }, text = {
            if (!exists()) TextField(
                value = fileName,
                onValueChange = { fileName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                suffix = { Text(stringResource(R.string.XCLF)) }
            ) else Text("Lyrics already exists, please select an option")
        },
        titleContentColor = getUIStyle.themedOnContainerColor(),
        textContentColor = getUIStyle.themedOnContainerColor(),
        containerColor = getUIStyle.dialogBackGroundColor()
    )
}

private val filenameRegex = Regex(
    "(?i)^(?!^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?$)" +  // not a reserved name (with optional extension)
            "[^<>:\"/\\\\|?*\\u0000-\\u001F]+" +                         // no forbidden chars or control chars
            "(?<![ .])$"                                                 // does not end with space or dot
)

@Composable
fun OverwriteItem(
    onDismiss: () -> Unit, onSubmit: () -> Unit, title: String, confirmButtonText: String,
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
            Button(onClick = onDismiss, enabled = enabled) { Text(stringResource(R.string.Cancel)) }
        },
        text = { Text(text = text, modifier = Modifier.fillMaxWidth()) },
        titleContentColor = getUIStyle.themedOnContainerColor(),
        textContentColor = getUIStyle.themedOnContainerColor(),
        containerColor = getUIStyle.dialogBackGroundColor()
    )
}