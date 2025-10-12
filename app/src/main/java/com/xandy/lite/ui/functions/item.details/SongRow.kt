package com.xandy.lite.ui.functions.item.details

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xandy.lite.R
import com.xandy.lite.controllers.shareSingleAudio
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.isNotInternal
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.theme.GetUIStyle


@Composable
fun SongRow(
    song: AudioFile, getUIStyle: GetUIStyle, onClick: () -> Unit, onLongPress: () -> Unit,
    onDelete: () -> Unit, onEdit: () -> Unit, onToggleHide: () -> Unit = {}, onAdd: () -> Unit,
    isSelecting: Boolean, isSelected: Boolean, enabled: Boolean, context: Context,
    isPickedSong: Boolean,
    onUpsertLyrics: () -> Unit, hideAllowed: Pair<Boolean, String> = Pair(false, "")
) {
    val ci = ContentIcons(getUIStyle)
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() }, onLongPress = { onLongPress() }
                )
            }
            .border(2.dp, getUIStyle.themedColor(), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (isSelecting) {
            ci.ContentIcon(
                if (isSelected) painterResource(R.drawable.baseline_square)
                else painterResource(R.drawable.outline_square)
            )
        }
        Artwork(song.picture.toString(), context, Modifier.size(50.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    song.title, maxLines = 1, style = MaterialTheme.typography.titleMedium,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isPickedSong) FontWeight.Bold else null,
                    color = if (isPickedSong) getUIStyle.pickedSongColor() else Color.Unspecified,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .fillMaxWidth(.90f), fontSize = 17.sp
                )
                Text(
                    song.artist, maxLines = 1, style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isPickedSong) FontWeight.Bold else null,
                    color = if (isPickedSong) getUIStyle.pickedSongColor() else Color.Unspecified,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .fillMaxWidth(.90f)
                )
            }
            if (!isSelecting) {
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
                            text = { Text("Add") },
                            trailingIcon = { ci.ContentIcon(Icons.Default.Add) },
                            onClick = { expanded = false; onAdd() }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Lyrics") },
                            trailingIcon = {
                                ci.ContentIcon(painterResource(R.drawable.baseline_lyrics))
                            },
                            onClick = { expanded = false; onUpsertLyrics() },
                            enabled = enabled && song.isNotInternal()
                        )
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            trailingIcon = { ci.ContentIcon(Icons.Default.Edit) },
                            onClick = { expanded = false; onEdit() },
                            enabled = enabled && song.isNotInternal()
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            trailingIcon = { ci.ContentIcon(Icons.Default.Share) },
                            onClick = { shareSingleAudio(context, song) }
                        )
                        if (hideAllowed.first) {
                            val icon =
                                if (hideAllowed.second == "Hide")
                                    ImageVector.vectorResource(R.drawable.sharp_hide_source)
                                else Icons.Default.CheckCircle
                            DropdownMenuItem(
                                text = { Text(hideAllowed.second) },
                                trailingIcon = { ci.ContentIcon(icon) },
                                onClick = { expanded = false; onToggleHide() }, enabled = enabled
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            trailingIcon = { ci.ContentIcon(Icons.Default.Delete) },
                            onClick = onDelete, enabled = enabled && song.isNotInternal()
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun SongRow(
    song: AudioFile, getUIStyle: GetUIStyle, isPickedSong: Boolean, context: Context,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .border(2.dp, getUIStyle.themedColor(), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Artwork(song.picture.toString(), context, Modifier.size(50.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                song.title, maxLines = 1, style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isPickedSong) FontWeight.Bold else null,
                color = if (isPickedSong) getUIStyle.pickedSongColor() else Color.Unspecified,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .fillMaxWidth(.90f), fontSize = 17.sp
            )
            Text(
                song.artist, maxLines = 1, style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isPickedSong) FontWeight.Bold else null,
                color = if (isPickedSong) getUIStyle.pickedSongColor() else Color.Unspecified,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .fillMaxWidth(.90f)
            )
        }
    }
}

@Composable
fun Artwork(data: Any, context: Context, modifier: Modifier = Modifier) {
    val image = remember(data) { ImageRequest.Builder(context).data(data).build() }
    AsyncImage(
        model = image,
        contentDescription = "Album art",
        placeholder = painterResource(R.drawable.unknown_track),
        error = painterResource(R.drawable.unknown_track),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun Artwork(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.unknown_track),
        contentDescription = "Unknown Track",
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop
    )
}