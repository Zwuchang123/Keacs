package com.keacs.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun MineScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-mine")
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.Section),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        KeacsCard {
            Column(modifier = Modifier.padding(it)) {
                MineRow("账户")
                MineRow("分类")
                MineRow("备份")
            }
        }
    }
}

@Composable
private fun MineRow(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = KeacsColors.TextTertiary,
        )
    }
}
