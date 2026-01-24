package com.xandy.lite.views.edit.audio

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xandy.lite.R
import com.xandy.lite.controllers.view.models.EditAudioVM
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.db.tables.LyricLine
import com.xandy.lite.db.tables.Lyrics
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.GetUIStyle

@Composable
internal fun VerticalEditAudioView(
    audio: AudioFile, onAudioChange: (AudioFile) -> Unit, enabled: Boolean, lyrics: Lyrics,
    onLyricsChange: (Lyrics) -> Unit, onUpdate: (AudioFile, Lyrics?) -> Unit,
    allMediaArtwork: List<Uri>, getUIStyle: GetUIStyle
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.Title), textDecoration = TextDecoration.Underline)
        TextField(
            value = audio.title, onValueChange = { onAudioChange(audio.copy(title = it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Text(text = stringResource(R.string.Artist), textDecoration = TextDecoration.Underline)
        TextField(
            value = audio.artist ?: "", onValueChange = { onAudioChange(audio.copy(artist = it)) },
            placeholder = { Text(stringResource(R.string.no_artist_set_yet)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Text(text = stringResource(R.string.Genre), textDecoration = TextDecoration.Underline)
        TextField(
            value = audio.genre ?: "",
            placeholder = { Text(stringResource(R.string.no_genre_set_yet)) },
            onValueChange = { onAudioChange(audio.copy(genre = it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences
            )
        )
        Text(text = stringResource(R.string.Album), textDecoration = TextDecoration.Underline)
        TextField(
            value = audio.album ?: "",
            placeholder = { Text(stringResource(R.string.no_album_set_yet)) },
            onValueChange = { onAudioChange(audio.copy(album = it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Text(
            text = stringResource(R.string.release_date), textDecoration = TextDecoration.Underline
        )
        DateChooser(
            minYear = 1900, getUIStyle = getUIStyle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            selectedDay = audio.day, selectedYear = audio.year, selectedMonth = audio.month,
            onYearSelected = { onAudioChange(audio.copy(year = it)) },
            onDaySelected = { if (audio.month != null) onAudioChange(audio.copy(day = it)) },
            onMonthSelected = { onAudioChange(audio.copy(month = it)) }
        )
        Text(
            text = stringResource(R.string.cover_image), textDecoration = TextDecoration.Underline
        )
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(audio.picture)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.cover_image),
                placeholder = painterResource(R.drawable.unknown_track),
                error = painterResource(R.drawable.unknown_track),
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(6.dp),
                contentScale = ContentScale.Crop
            )
            ImagePicker(
                allMediaArtwork, enabled, isLandscape = false, getUIStyle = getUIStyle
            ) { picture ->
                onAudioChange(audio.copy(picture = picture))
            }
        }
        LyricsOptions(lyrics) { onLyricsChange(it) }
        Spacer(Modifier.padding(vertical = 10.dp))
        HorizontalDivider(Modifier.fillMaxWidth())
        Button(
            onClick = { onUpdate(audio, lyrics.takeIf { ls -> ls.plain.isNotBlank() }) },
            modifier = Modifier.padding(8.dp)
        ) { Text(stringResource(R.string.update_track)) }
    }
}