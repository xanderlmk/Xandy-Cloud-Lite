package com.xandy.lite.ui.functions.item.details

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.R as AndroidR
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xandy.lite.R
import com.xandy.lite.models.ui.Album
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.GetUIStyle

@Composable
fun AlbumBox(album: Album, getUIStyle: GetUIStyle, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxHeight(0.825f)
                .border(0.5.dp, getUIStyle.themedOnContainerColor(), RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(album.picture)
                    .crossfade(true)
                    .build(),
                contentScale = ContentScale.FillWidth,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = album.name, fontSize = 16.sp, modifier = Modifier.padding(top = 1.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
        Text(
            text = album.artist, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp), lineHeight = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PlayOptions(
    ci: ContentIcons, onShuffle: () -> Unit, onPlay: () -> Unit, isPlaying: Boolean
) {
    val icon =
        if (!isPlaying) Icons.Default.PlayArrow else ImageVector.vectorResource(R.drawable.baseline_pause)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        IconButton(onClick = onShuffle) {
            ci.ContentIcon(painterResource(AndroidR.drawable.media3_icon_shuffle_on))
        }
        IconButton(onClick = onPlay) {
            ci.ContentIcon(icon)
        }
    }
}