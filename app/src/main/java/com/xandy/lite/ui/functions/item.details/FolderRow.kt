package com.xandy.lite.ui.functions.item.details

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xandy.lite.R
import com.xandy.lite.db.tables.Bucket
import com.xandy.lite.ui.functions.ContentIcons
import com.xandy.lite.ui.theme.GetUIStyle


@Composable
fun FolderRow(
    folder: Bucket, getUIStyle: GetUIStyle, onClick: () -> Unit, onLongPress: () -> Unit,
    isSelecting: Boolean, isSelected: Boolean, ci: ContentIcons
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() }, onLongPress = { onLongPress() }
                )
            }
            .border(2.dp, getUIStyle.themedOnContainerColor(), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isSelecting) {
            ci.ContentIcon(
                if (isSelected) painterResource(R.drawable.baseline_square)
                else painterResource(R.drawable.outline_square)
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "${folder.volumeName}://${folder.name}",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                fontSize = 17.sp,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = folder.relativePath, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}