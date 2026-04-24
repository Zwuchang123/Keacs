package com.keacs.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.theme.KeacsColors

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.AddCircleOutline,
            contentDescription = null,
            tint = KeacsColors.TextTertiary,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            color = KeacsColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(onClick = onActionClick) {
                Text(text = actionText)
            }
        }
    }
}
