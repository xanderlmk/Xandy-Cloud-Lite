package com.xandy.lite.ui.functions

import android.annotation.SuppressLint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.xandy.lite.R
import com.xandy.lite.ui.theme.GetUIStyle

@SuppressLint("ModifierParameter")
class ContentIcons(private val getUIStyle: GetUIStyle) {
    @Composable
    fun ContentIcon(icon: ImageVector, cd: String? = null, modifier: Modifier = Modifier) {
        Icon(
            imageVector = icon, modifier = modifier, contentDescription = cd,
            tint = getUIStyle.themedOnContainerColor()
        )
    }

    @Composable
    fun ContentIcon(icon: Painter, cd: String? = null, modifier: Modifier = Modifier) {
        Icon(
            painter = icon, modifier = modifier, contentDescription = cd,
            tint = getUIStyle.themedOnContainerColor()
        )
    }

    @Composable
    fun ContentIcon(icon: Painter, cd: String? = null, modifier: Modifier, tint: Color) {
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
    fun PercentRefreshButon(
        isLoading: Boolean, percentage: Int, onClick: () -> Unit, color: Color
    ) {
        if (!isLoading) IconButton(onClick = onClick, modifier = Modifier) {
            ContentIcon(Icons.Default.Refresh, cd = "Refresh", tint = color)
        }
        else Text(text = "$percentage%")
    }

    @Composable
    fun ToggleIconButton(onClick: () -> Unit, isOn: Boolean, modifier: Modifier = Modifier) {
        IconButton(onClick = onClick, modifier = modifier) {
            ContentIcon(
                icon = if (!isOn) painterResource(R.drawable.toggle_off)
                else painterResource(R.drawable.toggle_on),
                cd = "ToggleIcon"
            )
        }
    }

}