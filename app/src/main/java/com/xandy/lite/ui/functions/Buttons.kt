package com.xandy.lite.ui.functions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

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
