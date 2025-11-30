package com.xandy.lite.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.xandy.lite.R
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.GetUIStyle
import com.xandy.lite.views.lyrics.LyricIndex

class ModalContent(
    private val navController: NavHostController,
    private val getUIStyle: GetUIStyle, private val navVM: NavViewModel,
    private val cr: String, private val onClose: () -> Unit
) {
    private val onNavigate: (String) -> Unit = {
        if (cr != it) {
            navVM.updateIndexListener(LyricIndex.UNAVAILABLE)
            navVM.updateRoute(it)
            navController.navigate(it)
            onClose()
        }
    }
    private val mdModifier = Modifier
        .padding(horizontal = 4.dp)
        .size(28.dp)
        .background(
            color = getUIStyle.getColorScheme().primaryContainer,
            shape = RoundedCornerShape(12.dp)
        )
        .zIndex(2f)
    private val ci = ContentIcons(getUIStyle)

    @Composable
    fun Home() {
        val fontSize = returnFontSizeBasedOnDp()
        CustomRow(onClick = { onNavigate(LocalMusicDestination.route) }) {
            Text(
                text = "Home", fontSize = fontSize, color = getUIStyle.themedOnContainerColor(),
                style = MaterialTheme.typography.bodyLarge
            )
            ci.ContentIcon(
                Icons.Filled.Home, "Home", mdModifier,
                tint = getUIStyle.getColorScheme().onPrimaryContainer
            )
        }
    }

    @Composable
    fun LyricList() {
        val fontSize = returnFontSizeBasedOnDp()
        CustomRow(onClick = { onNavigate(LyricsListDestination.route)}) {
            Text(
                text = "Lyric List", fontSize = fontSize, color = getUIStyle.themedOnContainerColor(),
                style = MaterialTheme.typography.bodyLarge
            )
            ci.ContentIcon(
                painterResource(R.drawable.rounded_list), "Lyric list", mdModifier,
                tint = getUIStyle.getColorScheme().onPrimaryContainer
            )
        }
    }

    @Composable
    fun Settings() {
        val fontSize = returnFontSizeBasedOnDp()
        CustomRow(onClick = { onNavigate(SettingsDestination.route) })
        {
            Text(
                text = "Settings", fontSize = fontSize, color = getUIStyle.themedOnContainerColor(),
                style = MaterialTheme.typography.bodyLarge
            )
            ci.ContentIcon(
                Icons.Default.Settings, "Main Settings", mdModifier,
                tint = getUIStyle.getColorScheme().onPrimaryContainer
            )
        }
    }

    @Composable
    fun AutoUpdateEnabled(autoUpdate: Boolean) {
        val fontSize = returnFontSizeBasedOnDp()
        val text = if (autoUpdate) "Auto-Update Enabled" else "Auto-Update Disabled"
        val icon = if (autoUpdate) R.drawable.toggle_on else R.drawable.toggle_off
        CustomRow(onClick = { navVM.toggleAutoUpdate(!autoUpdate) }) {
            Text(
                text = text, fontSize = fontSize, color = getUIStyle.themedOnContainerColor(),
                style = MaterialTheme.typography.bodyLarge
            )
            ci.ContentIcon(
                painterResource(icon), "Main Settings", mdModifier,
                tint = getUIStyle.getColorScheme().onPrimaryContainer
            )
        }

    }

    @Composable
    fun IdWritingUpdateEnabled(writingEnabled: Boolean) {
        val fontSize = returnFontSizeBasedOnDp()
        val text = if (writingEnabled) "ID Writing Enabled" else "ID Writing Disabled"
        val icon = if (writingEnabled) R.drawable.toggle_on else R.drawable.toggle_off
        CustomRow(onClick = { navVM.toggleWritingEnabled(!writingEnabled) }) {
            Text(
                text = text, fontSize = fontSize, color = getUIStyle.themedOnContainerColor(),
                style = MaterialTheme.typography.bodyLarge
            )
            ci.ContentIcon(
                painterResource(icon), "Main Settings", mdModifier,
                tint = getUIStyle.getColorScheme().onPrimaryContainer
            )
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun returnFontSizeBasedOnDp(): TextUnit {
    val config = LocalConfiguration.current
    val widthDp = config.screenWidthDp
    return when (widthDp) {
        in 0..199 -> {
            10.sp
        }

        in 200..300 -> {
            12.sp
        }

        in 301..400 -> {
            14.sp
        }

        else -> {
            TextUnit.Unspecified
        }
    }
}


@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun Modifier.paddingForModal(): Modifier {
    val config = LocalConfiguration.current
    val widthDp = config.screenWidthDp
    return when (widthDp) {
        in 0..199 -> {
            this.padding(top = 12.dp, bottom = 6.dp, start = 4.dp, end = 4.dp)
        }

        in 200..300 -> {
            this.padding(top = 15.dp, bottom = 6.dp, start = 6.dp, end = 6.dp)
        }

        in 301..400 -> {
            this.padding(top = 15.dp, bottom = 6.dp, start = 8.dp, end = 8.dp)
        }

        else -> {
            this.padding(top = 15.dp, bottom = 6.dp, start = 15.dp, end = 15.dp)
        }
    }
}


@Composable
private fun CustomRow(
    onClick: () -> Unit, content: @Composable () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .paddingForModal()
    )
    { content() }
}