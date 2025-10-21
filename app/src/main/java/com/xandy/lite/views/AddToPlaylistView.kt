package com.xandy.lite.views

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xandy.lite.ui.functions.item.details.PlaylistRow
import com.xandy.lite.controllers.view.models.AddToLocalPlVM
import com.xandy.lite.ui.functions.AddPlDialog
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.theme.GetUIStyle


@Composable
fun AddToPlaylistView(
    getUIStyle: GetUIStyle, modifier: Modifier, vm: AddToLocalPlVM,
    onAdd: (String) -> Unit, onAddNew: (String) -> Unit, enabled: Boolean,
    showDialog: MutableState<Boolean>
) {
    val pls by vm.pls.collectAsStateWithLifecycle()
    val ci = ContentIcons(getUIStyle)

    Box(modifier = modifier) {
        AddPlDialog(
            showDialog = showDialog.value, getUIStyle = getUIStyle, enabled = enabled,
            onDismiss = {  if (enabled) showDialog.value = false },
            onSubmit = { onAddNew(it) },
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clickable { showDialog.value = true }
                        .border(2.dp, getUIStyle.themedOnContainerColor(), RoundedCornerShape(2.dp))
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Add new Playlist")
                    ci.ContentIcon(Icons.Default.Add)
                }
            }
            items(pls) { pl ->
                PlaylistRow(pl, getUIStyle) { onAdd(pl.id) }
            }
        }
    }
}