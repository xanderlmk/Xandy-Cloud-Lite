package com.xandy.lite.ui.functions.item.details

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.ui.res.stringResource
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
import com.xandy.lite.models.application.AppStrings
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.GetUIStyle


@Composable
fun SongRow(
    song: AudioFile, appStrings: AppStrings, getUIStyle: GetUIStyle, onClick: () -> Unit,
    onLongPress: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit,
    onToggleHide: () -> Unit = {}, onAdd: () -> Unit, onEnqueue: () -> Unit, onAddNext: () -> Unit,
    onAddFavorite: () -> Unit, onShowDetails: () -> Unit, isSelecting: Boolean, isSelected: Boolean,
    enabled: Boolean, context: Context, isPickedSong: Boolean, onUpsertLyrics: () -> Unit,
    hideAllowed: Pair<Boolean, String> = Pair(false, "")
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
            .border(2.dp, getUIStyle.themedOnContainerColor(), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (isSelecting) {
            ci.ContentIcon(
                if (isSelected) R.drawable.baseline_square else R.drawable.outline_square
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
                    song.artist ?: appStrings.unknownArtist, maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
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
                var addTo by rememberSaveable { mutableStateOf(false) }
                val onDismiss: () -> Unit = { expanded = false; addTo = false }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterEnd)
                        .wrapContentSize(Alignment.CenterEnd)
                ) {
                    IconButton(onClick = { expanded = !expanded }) {
                        ci.ContentIcon(Icons.Default.MoreVert)
                    }
                    DropdownMenu(
                        expanded = expanded, onDismissRequest = onDismiss
                    ) {
                        if (!addTo) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.Add)) },
                                trailingIcon = { ci.ContentIcon(Icons.Default.Add) },
                                onClick = { addTo = true }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.Details)) },
                                trailingIcon = { ci.ContentIcon(R.drawable.format_list_bulleted) },
                                onClick = { onDismiss(); onShowDetails() },
                                enabled = enabled && song.isNotInternal()
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.Edit)) },
                                trailingIcon = { ci.ContentIcon(Icons.Default.Edit) },
                                onClick = { onDismiss(); onEdit() },
                                enabled = enabled && song.isNotInternal()
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.Share)) },
                                trailingIcon = { ci.ContentIcon(Icons.Default.Share) },
                                onClick = { shareSingleAudio(context, song) }
                            )
                            if (hideAllowed.first) {
                                val icon =
                                    if (hideAllowed.second == stringResource(R.string.Hide))
                                        ImageVector.vectorResource(R.drawable.sharp_hide_source)
                                    else Icons.Default.CheckCircle
                                DropdownMenuItem(
                                    text = { Text(hideAllowed.second) },
                                    trailingIcon = { ci.ContentIcon(icon) },
                                    onClick = { onDismiss(); onToggleHide() }, enabled = enabled
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.Delete)) },
                                trailingIcon = { ci.ContentIcon(Icons.Default.Delete) },
                                onClick = onDelete, enabled = enabled && song.isNotInternal()
                            )
                        } else {
                            DropdownMenuItem(
                                text = { },
                                leadingIcon = {
                                    ci.ContentIcon(Icons.AutoMirrored.Default.ArrowBack)
                                },
                                onClick = { addTo = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.Playlist)) },
                                trailingIcon = { ci.ContentIcon(R.drawable.outline_animated_images) },
                                onClick = { onDismiss(); onAdd() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.Enqueue)) },
                                trailingIcon = {
                                    ci.ContentIcon(R.drawable.baseline_add_to_queue)
                                },
                                onClick = { onDismiss(); onEnqueue() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.play_next)) },
                                trailingIcon = {
                                    ci.ContentIcon(R.drawable.queue_play_next)
                                },
                                onClick = { onDismiss(); onAddNext() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.Lyrics)) },
                                trailingIcon = {
                                    ci.ContentIcon(R.drawable.baseline_lyrics)
                                },
                                onClick = { onDismiss(); onUpsertLyrics() },
                                enabled = enabled && song.isNotInternal()
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.Favorites)) },
                                trailingIcon = { ci.ContentIcon(Icons.Default.Favorite) },
                                onClick = { onDismiss(); onAddFavorite() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongRow(
    song: AudioFile, appStrings: AppStrings, getUIStyle: GetUIStyle, onClick: () -> Unit,
    onLongPress: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit, onAdd: () -> Unit,
    onEnqueue: () -> Unit, onAddNext: () -> Unit, onRemoveFavorite: () -> Unit,
    isSelecting: Boolean, isSelected: Boolean, enabled: Boolean, context: Context,
    isPickedSong: Boolean, onUpsertLyrics: () -> Unit,
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
            .border(2.dp, getUIStyle.themedOnContainerColor(), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (isSelecting) {
            ci.ContentIcon(
                if (isSelected) R.drawable.baseline_square else R.drawable.outline_square
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
                    song.artist ?: appStrings.unknownArtist, maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
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
                val onDismiss: () -> Unit = { expanded = false }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterEnd)
                        .wrapContentSize(Alignment.CenterEnd)
                ) {
                    IconButton(onClick = { expanded = !expanded }) {
                        ci.ContentIcon(Icons.Default.MoreVert)
                    }
                    DropdownMenu(
                        expanded = expanded, onDismissRequest = onDismiss
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_lyrics)) },
                            trailingIcon = {
                                ci.ContentIcon(R.drawable.baseline_lyrics)
                            },
                            onClick = { onDismiss(); onUpsertLyrics() },
                            enabled = enabled && song.isNotInternal()
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.Edit)) },
                            trailingIcon = { ci.ContentIcon(Icons.Default.Edit) },
                            onClick = { onDismiss(); onEdit() },
                            enabled = enabled && song.isNotInternal()
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.Share)) },
                            trailingIcon = { ci.ContentIcon(Icons.Default.Share) },
                            onClick = { shareSingleAudio(context, song) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.Delete)) },
                            trailingIcon = { ci.ContentIcon(Icons.Default.Delete) },
                            onClick = onDelete, enabled = enabled && song.isNotInternal()
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.Playlist)) },
                            trailingIcon = { ci.ContentIcon(R.drawable.outline_animated_images) },
                            onClick = { onDismiss(); onAdd() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.Enqueue)) },
                            trailingIcon = {
                                ci.ContentIcon(R.drawable.baseline_add_to_queue)
                            },
                            onClick = { onDismiss(); onEnqueue() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.play_next)) },
                            trailingIcon = {
                                ci.ContentIcon(R.drawable.queue_play_next)
                            },
                            onClick = { onDismiss(); onAddNext() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.Remove)) },
                            trailingIcon = { ci.ContentIcon(Icons.Default.FavoriteBorder) },
                            onClick = { onDismiss(); onRemoveFavorite() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SongRow(
    song: AudioFile, getUIStyle: GetUIStyle, isSelected: Boolean, context: Context,
    isPickedSong: Boolean,appStrings: AppStrings, onClick: () -> Unit
) {
    val ci = ContentIcons(getUIStyle)
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(onClick = onClick)
            .border(2.dp, getUIStyle.themedOnContainerColor(), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        ci.ContentIcon(
            if (isSelected) R.drawable.baseline_square else R.drawable.outline_square
        )

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
                    song.artist ?: appStrings.unknownArtist, maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
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
}

@Composable
fun SongRow(
    song: AudioFile, appStrings: AppStrings, getUIStyle: GetUIStyle, isPickedSong: Boolean,
    context: Context, onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .border(2.dp, getUIStyle.themedOnContainerColor(), RoundedCornerShape(2.dp))
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
                song.artist ?: appStrings.unknownArtist, maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
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
fun SongRow(
    song: AudioFile, appStrings: AppStrings, getUIStyle: GetUIStyle, isPickedSong: Boolean,
    context: Context, onClick: () -> Unit, onAddNext: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val onDismiss: () -> Unit = { expanded = false }

    val ci = ContentIcons(getUIStyle)
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .border(2.dp, getUIStyle.themedOnContainerColor(), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
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
                    song.artist ?: appStrings.unknownArtist, maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isPickedSong) FontWeight.Bold else null,
                    color = if (isPickedSong) getUIStyle.pickedSongColor() else Color.Unspecified,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .fillMaxWidth(.90f)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterEnd)
                    .wrapContentSize(Alignment.CenterEnd)
            ) {
                IconButton(onClick = { expanded = !expanded }) {
                    ci.ContentIcon(Icons.Default.MoreVert)
                }
                DropdownMenu(
                    expanded = expanded, onDismissRequest = onDismiss
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.play_next)) },
                        trailingIcon = { ci.ContentIcon(R.drawable.queue_play_next) },
                        onClick = { onDismiss(); onAddNext() }
                    )
                }
            }
        }
    }
}


@Composable
fun SongRow(
    song: AudioFile, appStrings: AppStrings, getUIStyle: GetUIStyle, isPickedSong: Boolean,
    context: Context
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .border(2.dp, getUIStyle.themedOnContainerColor(), RoundedCornerShape(2.dp))
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
                song.artist ?: appStrings.unknownArtist, maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
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