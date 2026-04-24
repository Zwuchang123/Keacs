package com.keacs.app.ui.record

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.components.EmptyState
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun RecordScreen(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-records")
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.Section),
    ) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            enabled = false,
            placeholder = { Text("搜索账单") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )
        KeacsCard(
            modifier = Modifier.padding(top = KeacsSpacing.Section),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(it),
            ) {
                Text(
                    text = "本月",
                    color = KeacsColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
                EmptyState(
                    title = "还没有账单",
                    actionText = "记一笔",
                    onActionClick = onAddClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                )
            }
        }
    }
}
