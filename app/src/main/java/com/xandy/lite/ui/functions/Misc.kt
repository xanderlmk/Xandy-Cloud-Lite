package com.xandy.lite.ui.functions

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.xandy.lite.R


@Composable
fun SearchTextField(
    query: String, querySet: Set<String>, onUpdateQuerySet: (String) -> Unit,
    onValueChange: (String) -> Unit, onTurnOff: () -> Unit, ci: ContentIcons
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val keyboard = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = query,
        onValueChange = onValueChange,
        placeholder = { Text(stringResource(R.string.Search)) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState -> if (!focusState.isFocused) onUpdateQuerySet(query) },
        textStyle = TextStyle(fontSize = 18.sp),
        shape = ShapeDefaults.Large,
        leadingIcon = {
            IconButton(onClick = onTurnOff) { ci.ContentIcon(Icons.Default.Close) }
        },
        trailingIcon = {
            RecentQueries(
                expanded, querySet, updateQuery = onValueChange,
                onToggle = { expanded = !expanded }, onDismiss = { expanded = false }, ci
            )
        },
        keyboardActions = KeyboardActions(
            onDone = { onUpdateQuerySet(query); keyboard?.hide() },
            onSend = { onUpdateQuerySet(query); keyboard?.hide() },
        )
    )
}