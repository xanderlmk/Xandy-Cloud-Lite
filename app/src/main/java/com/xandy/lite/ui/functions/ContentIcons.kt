package com.xandy.lite.ui.functions

import android.annotation.SuppressLint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.xandy.lite.R
import com.xandy.lite.ui.GetUIStyle
import kotlinx.coroutines.delay

@SuppressLint("ModifierParameter")
class ContentIcons(private val getUIStyle: GetUIStyle) {
    @Composable
    fun ContentIcon(
        icon: ImageVector, contentDescription: String? = null, modifier: Modifier = Modifier
    ) {
        Icon(
            imageVector = icon, modifier = modifier, contentDescription = contentDescription,
            tint = getUIStyle.themedOnContainerColor()
        )
    }

    @Composable
    fun ContentIcon(
        icon: Painter, contentDescription: String? = null, modifier: Modifier = Modifier
    ) {
        Icon(
            painter = icon, modifier = modifier, contentDescription = contentDescription,
            tint = getUIStyle.themedOnContainerColor()
        )
    }

    @Composable
    fun ContentIcon(icon: Painter, cd: String? = null, modifier: Modifier = Modifier, tint: Color) {
        Icon(
            painter = icon, modifier = modifier, contentDescription = cd, tint = tint
        )
    }

    @Composable
    fun ContentIcon(
        icon: ImageVector, cd: String? = null, modifier: Modifier = Modifier, tint: Color
    ) {
        Icon(
            imageVector = icon, modifier = modifier, contentDescription = cd, tint = tint
        )
    }

    @Composable
    fun ContentIcon(
        icon: ImageVector, cd: String? = null, modifier: Modifier = Modifier, enabled: Boolean
    ) {
        Icon(
            imageVector = icon, modifier = modifier, contentDescription = cd,
            tint = if (enabled) getUIStyle.themedOnContainerColor() else getUIStyle.disabledThemedColor()
        )
    }

    @Composable
    fun ContentIcon(
        icon: Painter, cd: String? = null, modifier: Modifier = Modifier, enabled: Boolean
    ) {
        Icon(
            painter = icon, modifier = modifier, contentDescription = cd,
            tint = if (enabled) getUIStyle.themedOnContainerColor() else getUIStyle.disabledThemedColor()
        )
    }


    @Composable
    fun RefreshButton(
        isLoading: Boolean,isGettingPics: Boolean, onClick: () -> Unit, color: Color
    ) {
        if (!isLoading) IconButton(onClick = onClick, modifier = Modifier) {
            ContentIcon(Icons.Default.Refresh, cd = "Refresh", tint = color)
        }
        else {
            if (isGettingPics) Box(
                modifier = Modifier
                    .width(50.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                var dots by remember { mutableIntStateOf(1) }

                // drive the dot cycle while this composable is active
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(350L)
                        dots = (dots % 3) + 1
                    }
                }
                ContentIcon(
                    painterResource(R.drawable.outline_animated_images),
                    modifier = Modifier.zIndex(1f)
                )
                val infinite = rememberInfiniteTransition()
                val scale by infinite.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                Text(
                    text = ".".repeat(dots),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = getUIStyle.pickedSongColor(),
                    modifier = Modifier
                        .scale(scale)
                        .zIndex(2f),
                )
            }
            else {

                var dots by remember { mutableIntStateOf(1) }
                val infinite = rememberInfiniteTransition()
                val scale by infinite.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                // drive the dot cycle while this composable is active
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(350L)
                        dots = (dots % 3) + 1
                    }
                }
                Text(
                    text = ".".repeat(dots),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = getUIStyle.pickedSongColor(),
                    modifier = Modifier
                        .width(50.dp)
                        .fillMaxHeight()
                        .scale(scale)
                        .zIndex(2f),
                )
            }
        }
    }

    @Composable
    fun ToggleIconButton(onClick: () -> Unit, isOn: Boolean, modifier: Modifier = Modifier) {
        IconButton(onClick = onClick, modifier = modifier) {
            ContentIcon(
                icon = if (!isOn) painterResource(R.drawable.toggle_off)
                else painterResource(R.drawable.toggle_on),
                contentDescription = "ToggleIcon"
            )
        }
    }

}