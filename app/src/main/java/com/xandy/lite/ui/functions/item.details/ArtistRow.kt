package com.xandy.lite.ui.functions.item.details

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xandy.lite.R
import com.xandy.lite.models.ui.Artist
import com.xandy.lite.ui.GetUIStyle

@Composable
fun ArtistRow(artist: Artist, getUIStyle: GetUIStyle, onClick: () -> Unit) {
    val trackCount = if (artist.songCount == 1) stringResource(R.string.one_track)
    else stringResource(R.string.num_tracks, artist.songCount)
    val albumText = stringResource(if (artist.albumCount == 1) R.string.album else R.string.albums)
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
        Artwork(artist.picture, LocalContext.current, Modifier.size(50.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                artist.name, maxLines = 1, style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .fillMaxWidth(.90f), fontSize = 17.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    trackCount,
                    maxLines = 1, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(start = 4.dp)
                )
                Text(
                    "| ${artist.albumCount} $albumText",
                    maxLines = 1, style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}