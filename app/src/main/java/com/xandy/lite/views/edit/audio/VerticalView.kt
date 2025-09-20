package com.xandy.lite.views.edit.audio

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xandy.lite.R
import com.xandy.lite.db.tables.AudioFile
import com.xandy.lite.ui.theme.GetUIStyle


@Composable
fun VerticalEditAudioView(
    audio: AudioFile, onAudioChange: (AudioFile) -> Unit, enabled: Boolean,
    onUpdate: (AudioFile) -> Unit, allMediaArtwork: List<Uri>, getUIStyle: GetUIStyle
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Title", textDecoration = TextDecoration.Underline)
        TextField(
            value = audio.title, onValueChange = { onAudioChange(audio.copy(title = it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Text(text = "Artist", textDecoration = TextDecoration.Underline)
        TextField(
            value = audio.artist, onValueChange = { onAudioChange(audio.copy(artist = it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Text(text = "Genre", textDecoration = TextDecoration.Underline)
        TextField(
            value = audio.genre ?: "",
            placeholder = { Text("No genre set yet") },
            onValueChange = { onAudioChange(audio.copy(genre = it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences
            )
        )
        Text(text = "Album", textDecoration = TextDecoration.Underline)
        TextField(
            value = audio.album ?: "",
            placeholder = { Text("No album set yet") },
            onValueChange = { onAudioChange(audio.copy(album = it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Text(text = "Release Date", textDecoration = TextDecoration.Underline)
        DateChooser(
            minYear = 1900, getUIStyle = getUIStyle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            selectedDay = audio.day, selectedYear = audio.year, selectedMonth = audio.month,
            onYearSelected = { onAudioChange(audio.copy(year = it)) },
            onDaySelected = { if(audio.month != null) onAudioChange(audio.copy(day = it)) },
            onMonthSelected = { onAudioChange(audio.copy(month = it)) }
        )
        Text(text = "Picture", textDecoration = TextDecoration.Underline)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(audio.picture)
                .crossfade(true)
                .build(),
            contentDescription = "Album art",
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
        Spacer(Modifier.padding(vertical = 20.dp))
        Button(onClick = { onUpdate(audio) }, modifier = Modifier.padding(8.dp)) {
            Text("Update Song")
        }
    }
}