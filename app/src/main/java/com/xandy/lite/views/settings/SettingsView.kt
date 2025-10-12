package com.xandy.lite.views.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.xandy.lite.ui.theme.GetUIStyle
import kotlinx.coroutines.launch

@Composable
fun SettingsView(getUIStyle: GetUIStyle) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize(), verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ThemeSettings(getUIStyle) {
            scope.launch { getUIStyle.changeTheme(it) }
        }
    }
}