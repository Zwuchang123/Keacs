package com.keacs.app.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.components.EmptyState
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.SearchBox
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun RecordScreen(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-records")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        SearchBox(text = "搜索账单")
        MonthSummaryCard()
        EmptyRecordCard(onAddClick = onAddClick)
    }
}

@Composable
private fun MonthSummaryCard() {
    KeacsCard {
        Column(modifier = Modifier.padding(it)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "本月",
                    color = KeacsColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = KeacsColors.TextSecondary,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = "选择月份",
                    tint = KeacsColors.TextSecondary,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SummaryAmount("收入", "¥0.00", KeacsColors.Income)
                SummaryAmount("支出", "¥0.00", KeacsColors.Expense)
            }
        }
    }
}

@Composable
private fun SummaryAmount(
    label: String,
    amount: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = " $amount",
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun EmptyRecordCard(onAddClick: () -> Unit) {
    KeacsCard {
        EmptyState(
            title = "暂无记录",
            description = "当前没有账单，快去记一笔吧",
            actionText = "记一笔",
            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
            onActionClick = onAddClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .padding(it),
        )
    }
}
