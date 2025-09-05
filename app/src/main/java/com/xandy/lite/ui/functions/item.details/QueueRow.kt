package com.xandy.lite.ui.functions.item.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xandy.lite.models.ui.order.by.OrderQueueBy
import com.xandy.lite.models.ui.order.by.QueueOrder
import com.xandy.lite.ui.functions.ContentIcons

@Composable
fun QueueRow(
    ci: ContentIcons, onUpdateOrder: (QueueOrder) -> Unit, onReverseOrder: () -> Unit,
    currentOrder: OrderQueueBy, asc: Boolean, currentIdx: Int, queueSize: Int, modifier: Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val onClick: (QueueOrder) -> Unit = {
        expanded = false; onUpdateOrder(it)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopCenter)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                ci.ContentIcon(Icons.Default.Menu)
                Text(
                    text = when (currentOrder) {
                        OrderQueueBy.ArtistASC -> "Artist Asc"
                        OrderQueueBy.ArtistDESC -> "Artist Desc"
                        OrderQueueBy.CreatedOnASC -> "Date added Asc"
                        OrderQueueBy.CreatedOnDESC -> "Date added Desc"
                        OrderQueueBy.Default -> "Default"
                        OrderQueueBy.TitleASC -> "Title Asc"
                        OrderQueueBy.TitleDESC -> "Title Desc"
                    }
                )
            }
            Text("$currentIdx/$queueSize")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Default") },
                onClick = { onClick(QueueOrder.Default) }
            )
            if (currentOrder !is OrderQueueBy.Default) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(if (asc) "Ascending" else "Descending") },
                    onClick = onReverseOrder
                )
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text("Title") },
                onClick = { onClick(QueueOrder.Title) }
            )
            DropdownMenuItem(
                text = { Text("Artist") },
                onClick = { onClick(QueueOrder.Artist) }
            )
            DropdownMenuItem(
                text = { Text("Date added") },
                onClick = { onClick(QueueOrder.CreatedOn) }
            )
        }
    }
}