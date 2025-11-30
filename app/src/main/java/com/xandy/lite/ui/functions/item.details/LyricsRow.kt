package com.xandy.lite.ui.functions.item.details

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xandy.lite.R
import com.xandy.lite.db.tables.LyricsWithAudio
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.GetUIStyle

@Composable
fun LyricsRow(l: LyricsWithAudio, getUIStyle: GetUIStyle, onClick: () -> Unit) {
    var toggleDetails by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() }, onLongPress = { toggleDetails = !toggleDetails }
                )
            }
            .border(2.dp, getUIStyle.themedOnContainerColor(), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        if (!toggleDetails)
            Text(text = l.lyrics.description ?: l.lyrics.plain, maxLines = 2)
        else {
            Text(
                text = "Lyrics", textDecoration = TextDecoration.Underline,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = l.lyrics.plain, textAlign = TextAlign.Start
            )
            HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
            Text(
                text = if (l.lyrics.scroll.isNullOrEmpty()) "No Synchronized Lyrics"
                else "Has Synchronized Lyrics",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
            if (l.audios.isNotEmpty()) {
                Text(
                    text = "Belongs to", textDecoration = TextDecoration.Underline,
                    textAlign = TextAlign.Start, modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
                l.audios.forEach {
                    Text(it.title)
                }
            }
        }
    }
}

@Composable
fun LyricsRow(
    l: LyricsWithAudio, onEdit: () -> Unit, onExport: () -> Unit, onDelete: () -> Unit,
    getUIStyle: GetUIStyle
) {
    val ci = ContentIcons(getUIStyle)
    var toggleDetails by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { toggleDetails = !toggleDetails }
                )
            }
            .border(2.dp, getUIStyle.themedOnContainerColor(), RoundedCornerShape(2.dp))
            .padding(vertical = 4.dp, horizontal = if (!toggleDetails) 0.dp else 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        if (!toggleDetails)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = l.lyrics.description ?: l.lyrics.plain, maxLines = 2,
                    style = MaterialTheme.typography.titleMedium,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .fillMaxWidth(.90f)
                )
                var expanded by rememberSaveable { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterEnd)
                        .wrapContentSize(Alignment.CenterEnd)
                ) {
                    IconButton(onClick = { expanded = !expanded }) {
                        ci.ContentIcon(Icons.Default.MoreVert)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            trailingIcon = { ci.ContentIcon(Icons.Default.Edit) },
                            onClick = { expanded = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Export") },
                            trailingIcon = {
                                ci.ContentIcon(painterResource(R.drawable.outline_file_export))
                            },
                            onClick = { expanded = false; onExport() }
                        )

                        DropdownMenuItem(
                            text = { Text("Delete") },
                            trailingIcon = { ci.ContentIcon(Icons.Default.Delete) },
                            onClick = { expanded = false; onDelete() }
                        )
                    }
                }
            }
        else {
            Text(
                text = "Lyrics", textDecoration = TextDecoration.Underline,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = l.lyrics.plain, textAlign = TextAlign.Start
            )
            HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
            Text(
                text = if (l.lyrics.scroll.isNullOrEmpty()) "No Synchronized Lyrics"
                else "Has Synchronized Lyrics",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
            if (l.audios.isNotEmpty()) {
                Text(
                    text = "Belongs to", textDecoration = TextDecoration.Underline,
                    textAlign = TextAlign.Start, modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
                l.audios.forEach {
                    Text(it.title)
                }
            }
        }
    }
}