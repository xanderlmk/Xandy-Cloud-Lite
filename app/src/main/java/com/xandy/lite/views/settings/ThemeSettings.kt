package com.xandy.lite.views.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xandy.lite.R
import com.xandy.lite.models.Theme
import com.xandy.lite.models.application.PreferencesManager
import com.xandy.lite.ui.GetUIStyle

@Composable
fun ThemeSettings(
    getUIStyle: GetUIStyle, pm: PreferencesManager, onChangeTheme: (Theme) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val theme by pm.theme.collectAsStateWithLifecycle()
    Box(
        Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .border(
                width = 2.dp,
                shape = RoundedCornerShape(12.dp),
                color = if (getUIStyle.getIsDarkTheme()) Color.Gray else Color.Black
            )
            .clickable { expanded = !expanded }
            .wrapContentSize(Alignment.TopCenter)
    ) {
        Text(
            text = "System Theme",
            fontSize = 20.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(vertical = 4.dp)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                onClick = { onChangeTheme(Theme.Dark) },
                text = { Text("Dark Theme") },
                leadingIcon = {
                    Icon(
                        painter = if (theme !is Theme.Dark) painterResource(R.drawable.toggle_off)
                        else painterResource(R.drawable.toggle_on),
                        contentDescription = "Toggle Dark Theme"
                    )
                }
            )
            DropdownMenuItem(
                onClick = { onChangeTheme(Theme.Light) },
                text = { Text("Light Theme") },
                leadingIcon = {
                    Icon(
                        painter = if (theme !is Theme.Light) painterResource(R.drawable.toggle_off)
                        else painterResource(R.drawable.toggle_on),
                        contentDescription = "Toggle Light Theme"
                    )
                }
            )
            DropdownMenuItem(
                onClick = { onChangeTheme(Theme.Default) },
                text = { Text("Default Theme") },
                leadingIcon = {
                    Icon(
                        painter = if (theme !is Theme.Default) painterResource(R.drawable.toggle_off)
                        else painterResource(R.drawable.toggle_on),
                        contentDescription = "Toggle Default Theme"
                    )
                }
            )
        }

    }
}