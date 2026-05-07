package com.keacs.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.theme.KeacsColors

@Composable
fun RecordListItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    amount: String,
    amountColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    compact: Boolean = false,
) {
    val minHeight = if (compact) 48.dp else 64.dp
    val verticalPadding = if (compact) 4.dp else 10.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .padding(vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(icon = icon, backgroundColor = iconColor)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = KeacsColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Text(
            text = amount,
            color = amountColor,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
        )
    }
}
