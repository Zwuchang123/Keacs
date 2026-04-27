package com.keacs.app.ui.record

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.ui.components.CategoryIcon
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.management.colorFor
import com.keacs.app.ui.management.iconFor
import com.keacs.app.ui.theme.KeacsColors

@Composable
fun CategoryGrid(categories: List<CategoryEntity>, selectedId: Long?, onSelected: (Long) -> Unit) {
    KeacsCard(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .padding(it),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(categories, key = { category -> category.id }) { category ->
                CategoryChoice(category, selectedId == category.id) { onSelected(category.id) }
            }
        }
    }
}

@Composable
private fun CategoryChoice(category: CategoryEntity, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (selected) KeacsColors.PrimaryLight else KeacsColors.Surface)
                .border(BorderStroke(if (selected) 1.5.dp else 0.dp, KeacsColors.Primary), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            CategoryIcon(
                icon = iconFor(category.iconKey),
                backgroundColor = if (selected) KeacsColors.Primary else colorFor(category.colorKey),
                modifier = Modifier.size(34.dp),
            )
        }
        Text(
            text = category.name,
            color = if (selected) KeacsColors.Primary else KeacsColors.TextPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
