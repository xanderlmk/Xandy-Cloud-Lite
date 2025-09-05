package com.xandy.cloud.ui.functions.item.details

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xandy.lite.R
import com.xandy.lite.db.tables.Playlist
import com.xandy.lite.models.ui.PlaylistWithCount
import com.xandy.lite.models.ui.order.by.OrderSongsBy
import com.xandy.lite.models.ui.order.by.SongOrder
import com.xandy.lite.models.ui.order.by.reverseSort
import com.xandy.lite.models.ui.order.by.toOrderedByClass
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.functions.item.details.Artwork
import com.xandy.lite.ui.theme.GetUIStyle
import androidx.media3.session.R as AndroidR


@Composable
fun PlaylistRow(
    pl: PlaylistWithCount, getUIStyle: GetUIStyle, ci: ContentIcons,
    onDelete: () -> Unit, onClick: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(onClick = onClick)
            .border(2.dp, getUIStyle.themedColor(), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (pl.playlist.picture != null) Artwork(pl.playlist.picture, Modifier.size(50.dp))
        else Artwork(Modifier.size(50.dp))
        Text(
            pl.playlist.name, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth(.8f),
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                "${pl.songCount} tracks", textAlign = TextAlign.End, maxLines = 1, fontSize = 13.sp,
                modifier = Modifier.offset(-(20.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.CenterEnd)
                    .offset(x = 10.dp)
            ) {
                IconButton(onClick = { expanded = !expanded }) {
                    ci.ContentIcon(Icons.Default.MoreVert, modifier = Modifier.fillMaxSize(0.75f))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        trailingIcon = { ci.ContentIcon(Icons.Default.Delete) },
                        onClick = { onDelete(); expanded = false },
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistRow(pl: Playlist, getUIStyle: GetUIStyle, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(onClick = onClick)
            .border(2.dp, getUIStyle.themedColor(), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (pl.picture != null) Artwork(pl.picture, Modifier.size(50.dp))
        else Artwork(Modifier.size(50.dp))
        Text(pl.name, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun PlaylistOrderRow(
    order: OrderSongsBy, ci: ContentIcons, onUpdate: (OrderSongsBy) -> Unit, onShuffle: () -> Unit,
    onPlay: () -> Unit, isPlaying: Boolean
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val icon =
        if (!isPlaying) Icons.Default.PlayArrow else ImageVector.vectorResource(R.drawable.baseline_pause)
    val asc = order is OrderSongsBy.TitleASC || order is OrderSongsBy.CreatedOnASC
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopCenter)
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                ci.ContentIcon(Icons.Default.Menu)
                Text(
                    text = when (order) {
                        OrderSongsBy.CreatedOnASC -> "Oldest first"
                        OrderSongsBy.CreatedOnDESC -> "Newest first"
                        OrderSongsBy.TitleASC -> "Title Asc"
                        OrderSongsBy.TitleDESC -> "Title Desc"
                        OrderSongsBy.ArtistASC -> "Artist Asc"
                        OrderSongsBy.ArtistDESC -> "Artist Desc"
                    },
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
            Row {
                IconButton(onClick = onShuffle) {
                    ci.ContentIcon(painterResource(AndroidR.drawable.media3_icon_shuffle_on))
                }
                IconButton(onClick = onPlay) {
                    ci.ContentIcon(icon)
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(if (asc) "Ascending" else "Descending") },
                onClick = { onUpdate(order.reverseSort()) }
            )
            DropdownMenuItem(
                text = { Text("Date added") },
                onClick = { onUpdate(SongOrder.CreatedOn.toOrderedByClass(asc)) }
            )
            DropdownMenuItem(
                text = { Text("Title") },
                onClick = { onUpdate(SongOrder.Title.toOrderedByClass(asc)) }
            )
            DropdownMenuItem(
                text = { Text("Artist") },
                onClick = { onUpdate(SongOrder.Artist.toOrderedByClass(asc)) }
            )
        }
    }
}