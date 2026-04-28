package com.keacs.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
    val infiniteTransition = rememberInfiniteTransition(label = "emptyStateBreathing")
    val translationY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingTranslation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer { this.translationY = translationY }
                .size(60.dp)
                .background(KeacsColors.SurfaceSubtle, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = KeacsColors.TextTertiary.copy(alpha = 0.72f),
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            color = KeacsColors.TextSecondary,
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
            Button(
                onClick = onActionClick,
                modifier = Modifier
                    .width(104.dp)
                    .height(40.dp),
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KeacsColors.Primary,
                    contentColor = KeacsColors.Surface,
                ),
            ) {
                Text(text = actionText)
            }
        }
    }
}
