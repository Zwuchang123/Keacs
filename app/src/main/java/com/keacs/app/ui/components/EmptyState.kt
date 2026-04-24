package com.keacs.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.theme.KeacsColors

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionText: String? = null,
    icon: ImageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
    onActionClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(KeacsColors.PrimaryLight, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = KeacsColors.Primary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
        )
        if (description != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                color = KeacsColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(onClick = onActionClick) {
                Text(text = actionText)
            }
        }
    }
}
