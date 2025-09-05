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
import com.xandy.lite.ui.theme.GetUIStyle

@SuppressLint("ModifierParameter")
class ContentIcons(private val getUIStyle: GetUIStyle) {
    @Composable
    fun ContentIcon(icon: ImageVector, cd: String? = null, modifier: Modifier = Modifier) {
        Icon(
            imageVector = icon,
            modifier = modifier,
            contentDescription = cd,
            tint = getUIStyle.themedColor()
        )
    }

    @Composable
    fun ContentIcon(icon: Painter, cd: String? = null, modifier: Modifier = Modifier) {
        Icon(
            painter = icon,
            modifier = modifier,
            contentDescription = cd,
            tint = getUIStyle.themedColor()
        )
    }

    @Composable
    fun ContentIcon(icon: Painter, cd: String? = null, modifier: Modifier, tint: Color) {
        Icon(
            painter = icon,
            modifier = modifier,
            contentDescription = cd,
            tint = tint
        )
    }

    @Composable
    fun ContentIcon(
        icon: ImageVector,
        cd: String? = null,
        modifier: Modifier = Modifier,
        tint: Color
    ) {
        Icon(
            imageVector = icon,
            modifier = modifier,
            contentDescription = cd,
            tint = tint
        )
    }


    @Composable
    fun PercentRefreshButon(
        isLoading: Boolean, percentage: Int, onClick: () -> Unit, color: Color
    ) {
        if (!isLoading) {
            IconButton(
                onClick = onClick,
                modifier = Modifier
            ) {
                ContentIcon(Icons.Default.Refresh, cd = "Refresh", tint = color)
            }
        } else {
            Text(text = "$percentage%")
        }
    }
}