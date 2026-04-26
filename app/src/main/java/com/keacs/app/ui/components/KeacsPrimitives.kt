package com.keacs.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSize

@Composable
fun SegmentedTabs(
    items: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(KeacsColors.SurfaceSubtle)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            Text(
                text = item,
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(if (selected) KeacsColors.Primary else Color.Transparent)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { onSelected(index) },
                    )
                    .padding(vertical = 8.dp),
                color = if (selected) KeacsColors.Surface else KeacsColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

@Composable
fun CategoryIcon(
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    tint: Color = KeacsColors.Surface,
) {
    Box(
        modifier = modifier
            .size(KeacsSize.CategoryIcon)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
fun SearchBox(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(KeacsColors.SurfaceSubtle)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = KeacsColors.TextTertiary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = KeacsColors.TextTertiary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun FormFieldRow(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(KeacsColors.Surface)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = KeacsColors.TextSecondary,
            modifier = Modifier.size(21.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = KeacsColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = KeacsColors.TextTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun MenuRow(
    icon: ImageVector,
    title: String,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(
            icon = icon,
            backgroundColor = iconColor.copy(alpha = 0.14f),
            tint = iconColor,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = KeacsColors.TextTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun AmountText(
    amount: String,
    modifier: Modifier = Modifier,
    color: Color = KeacsColors.TextPrimary,
) {
    Text(
        text = amount,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.displaySmall,
        fontFamily = FontFamily.Monospace,
    )
}

@Composable
fun DividedMenuCard(
    rows: @Composable ColumnScope.() -> Unit,
) {
    KeacsCard(contentPadding = PaddingValues(0.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            rows()
        }
    }
}

@Composable
fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 66.dp),
        thickness = 0.5.dp,
        color = KeacsColors.Border,
    )
}
