package com.xandy.lite.views.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xandy.lite.models.XCToast
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.theme.GetUIStyle
import kotlinx.coroutines.launch

@Composable
fun SettingsView(getUIStyle: GetUIStyle) {
    val scope = rememberCoroutineScope()
    val ci = ContentIcons(getUIStyle)
    val offloadingEnabled by getUIStyle.getOffloadingEnabled().collectAsStateWithLifecycle()
    val toast = XCToast(LocalContext.current)
    val offloadingText = if (offloadingEnabled) "Offloading enabled" else "Offloading disabled"
    var showOffloadingHint by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize(), verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ThemeSettings(getUIStyle) { scope.launch { getUIStyle.changeTheme(it) } }
        ColumnContentWithBorder(getUIStyle, "Player options") {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = offloadingText, style = MaterialTheme.typography.titleMedium)
                ci.ToggleIconButton(
                    onClick = {
                        scope.launch {
                            getUIStyle.toggleOffloadingEnabled(!offloadingEnabled)
                            toast.makeMessage("Restart app to apply changes")
                        }
                    },
                    isOn = offloadingEnabled
                )
            }
            Text(
                text =
                    if (showOffloadingHint) "If you want to save battery, enable offloading, but if it causes issues, disable it."
                    else "Why enable/disable offloading?",
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clickable { showOffloadingHint = !showOffloadingHint }
            )
            HorizontalDivider(modifier = Modifier.fillMaxWidth(.90f), thickness = 1.5.dp)
        }
    }
}

@Composable
private fun ColumnContentWithBorder(
    getUIStyle: GetUIStyle, text: String, modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .border(
                width = 2.dp, shape = RoundedCornerShape(12.dp),
                color = if (getUIStyle.getIsDarkTheme()) Color.Gray else Color.Black
            )
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = getUIStyle.themedOnContainerColor(),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth()
                .clickable { expanded = !expanded }
        )
        if (expanded) {
            HorizontalDivider(modifier = Modifier.fillMaxWidth(0.95f), thickness = 2.dp)
            content()
        }
    }
}