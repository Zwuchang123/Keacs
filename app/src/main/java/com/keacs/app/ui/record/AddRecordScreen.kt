package com.keacs.app.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun AddRecordScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-add")
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.Section),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        RecordTypeTabs()
        AmountCard()
        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(text = "保存")
        }
    }
}

@Composable
private fun RecordTypeTabs() {
    val items = listOf("支出", "收入", "转账")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(KeacsColors.SurfaceSubtle)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == 0
            Text(
                text = item,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (selected) KeacsColors.Surface else KeacsColors.SurfaceSubtle)
                    .selectable(
                        selected = selected,
                        onClick = {},
                        role = Role.Tab,
                    )
                    .padding(vertical = 10.dp),
                color = if (selected) KeacsColors.Primary else KeacsColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AmountCard() {
    KeacsCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "金额",
                color = KeacsColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "¥0.00",
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.displaySmall,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "选择分类和账户后保存",
                color = KeacsColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
