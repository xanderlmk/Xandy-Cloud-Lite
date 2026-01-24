@file:OptIn(ExperimentalMaterial3Api::class)

package com.xandy.lite.ui.functions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xandy.lite.R


@Composable
fun DeleteModal(onDismissRequest: () -> Unit, onDelete: () -> Unit, string: String?) {
    val item = string ?: "item"
    ModalBottomSheet(
        onDismissRequest = onDismissRequest
    ) {
        Text(
            text = "Delete $item?",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onDismissRequest) { Text(stringResource(R.string.Cancel)) }
            Button(onClick = onDelete) { Text(stringResource(R.string.Delete)) }
        }
    }
}

@Composable
fun SelectImageModal(
    onDismissRequest: () -> Unit, onSelectLocal: () -> Unit, onSelectGallery: () -> Unit,
    show: Boolean, localEnabled: Boolean
) {
    if (show) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest
        ) {
            Text(
                text = "Select image from:",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onSelectGallery,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.Gallery),
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = onSelectLocal,
                        enabled = localEnabled,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.local_artwork),
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                        )
                    }
                }
                Button(onClick = onDismissRequest) { Text(stringResource(R.string.Cancel)) }
            }
        }
    }
}