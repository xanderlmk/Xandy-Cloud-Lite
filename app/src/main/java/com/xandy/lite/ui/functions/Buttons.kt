package com.xandy.lite.ui.functions

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xandy.lite.models.XCToast
import com.xandy.lite.ui.theme.GetUIStyle

@Composable
fun SearchIconButton(ci: ContentIcons, onSearch: () -> Unit) {
    IconButton(onClick = onSearch) {
        ci.ContentIcon(Icons.Default.Search)
    }
}

@Composable
fun AddIconButton(ci: ContentIcons, onAdd: () -> Unit) {
    IconButton(
        onClick = onAdd
    ) {
        ci.ContentIcon(Icons.Default.Add)
    }
}


@Composable
fun SmallAddButton(
    onClick: () -> Unit, iconSize: Int = 45,
    getUIStyle: GetUIStyle, modifier: Modifier, enabled: Boolean
) {
    val ci = ContentIcons(getUIStyle)
    FloatingActionButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier.padding(16.dp),
        containerColor = getUIStyle.floatingButtonColor(),
    ) {
        ci.ContentIcon(
            icon = Icons.Outlined.Add, cd = "Import file",
            modifier = Modifier.size(iconSize.dp), tint = getUIStyle.themedOnContainerColor(),
        )
    }
}