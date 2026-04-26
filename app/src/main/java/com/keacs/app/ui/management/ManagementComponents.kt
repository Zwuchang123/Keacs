package com.keacs.app.ui.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.components.CategoryIcon
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSize
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun ManagementListItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    enabled: Boolean,
    trailing: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .alpha(if (enabled) 1f else 0.48f)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(icon = icon, backgroundColor = color)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = KeacsColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!enabled) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "停用",
                        color = KeacsColors.TextTertiary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(
                text = subtitle,
                color = KeacsColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Text(
                text = trailing,
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = KeacsColors.TextTertiary,
        )
    }
}

@Composable
fun ListDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp),
        thickness = 0.5.dp,
        color = KeacsColors.Border,
    )
}

@Composable
fun ManagementTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    KeacsCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.padding(it)) {
            Text(
                text = label,
                color = KeacsColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(color = KeacsColors.TextPrimary),
                cursorBrush = SolidColor(KeacsColors.Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(42.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(KeacsColors.SurfaceSubtle)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
    if (error != null) {
        Text(
            text = error,
            color = KeacsColors.Error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
        )
    }
}

@Composable
fun OptionChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val border = if (selected) {
        BorderStroke(0.5.dp, KeacsColors.Primary.copy(alpha = 0.34f))
    } else {
        BorderStroke(0.dp, Color.Transparent)
    }
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) KeacsColors.PrimaryLight else KeacsColors.SurfaceSubtle)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .then(if (selected) Modifier.border(border, RoundedCornerShape(10.dp)) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) KeacsColors.Primary else KeacsColors.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SwitchCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    KeacsCard(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("用于新建记录", style = MaterialTheme.typography.bodySmall, color = KeacsColors.TextSecondary)
                Text(
                    text = if (checked) "开启" else "关闭",
                    style = MaterialTheme.typography.titleMedium,
                    color = KeacsColors.TextPrimary,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun ActionButtons(
    deleteText: String,
    saveText: String,
    onDeleteClick: () -> Unit,
    onSaveClick: () -> Unit,
    saveEnabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onDeleteClick,
            modifier = Modifier
                .weight(1f)
                .height(KeacsSize.MinTouch),
            colors = ButtonDefaults.buttonColors(
                containerColor = KeacsColors.Error.copy(alpha = 0.08f),
                contentColor = KeacsColors.Error,
            ),
            border = BorderStroke(0.5.dp, KeacsColors.Error.copy(alpha = 0.34f)),
        ) {
            Text(deleteText, fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = onSaveClick,
            enabled = saveEnabled,
            modifier = Modifier
                .weight(1f)
                .height(KeacsSize.MinTouch),
            colors = ButtonDefaults.buttonColors(
                containerColor = KeacsColors.Primary,
                contentColor = KeacsColors.Surface,
            ),
        ) {
            Text(saveText, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ErrorText(message: String?) {
    if (message != null) {
        Text(
            text = message,
            color = KeacsColors.Error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = KeacsSpacing.ItemGap),
        )
    }
}
