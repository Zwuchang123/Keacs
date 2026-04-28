package com.keacs.app.ui.record

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.ui.components.CategoryIcon
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.management.colorFor
import com.keacs.app.ui.management.iconFor
import com.keacs.app.ui.theme.KeacsColors

@Composable
fun CategoryGrid(categories: List<CategoryEntity>, selectedId: Long?, onSelected: (Long) -> Unit) {
    KeacsCard(contentPadding = PaddingValues(0.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(it)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            categories.take(15).chunked(5).forEach { rowCategories ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    rowCategories.forEach { category ->
                        CategoryChoice(
                            category = category,
                            selected = selectedId == category.id,
                            modifier = Modifier.weight(1f),
                        ) {
                            onSelected(category.id)
                        }
                    }
                    repeat(5 - rowCategories.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChoice(
    category: CategoryEntity,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .height(62.dp)
            .shadow(
                elevation = if (selected) 10.dp else 0.dp,
                shape = MaterialTheme.shapes.medium,
                ambientColor = KeacsColors.Primary.copy(alpha = 0.22f),
                spotColor = KeacsColors.Primary.copy(alpha = 0.22f),
            )
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) KeacsColors.PrimaryLight else Color.Transparent)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (selected) KeacsColors.PrimaryLight else KeacsColors.Surface)
                .border(BorderStroke(if (selected) 1.5.dp else 0.dp, KeacsColors.Primary), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            CategoryIcon(
                icon = iconFor(category.iconKey),
                backgroundColor = if (selected) KeacsColors.Primary else colorFor(category.colorKey),
                modifier = Modifier.size(30.dp),
            )
        }
        Text(
            text = category.name,
            color = if (selected) KeacsColors.Primary else KeacsColors.TextPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
