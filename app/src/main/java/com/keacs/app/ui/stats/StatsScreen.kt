package com.keacs.app.ui.stats

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.components.EmptyState
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.SegmentedTabs
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun StatsScreen(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-stats")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        SegmentedTabs(items = listOf("支出", "收入", "资产"), selectedIndex = 0)
        MonthSelector()
        TrendCard()
        RankCard(onAddClick = onAddClick)
    }
}

@Composable
private fun MonthSelector() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "本月",
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
        )
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = "选择时间",
            tint = KeacsColors.TextSecondary,
        )
    }
}

@Composable
private fun TrendCard() {
    KeacsCard {
        Column(modifier = Modifier.padding(it)) {
            Text(
                text = "支出趋势",
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "¥0.00",
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.height(12.dp))
            ChartPlaceholder()
            Text(
                text = "暂无数据",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = KeacsColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ChartPlaceholder() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(136.dp),
    ) {
        val gridColor = KeacsColors.Border.copy(alpha = 0.75f)
        repeat(4) { index ->
            val y = size.height * (index + 1) / 5f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
        }
        val points = listOf(
            Offset(0f, size.height * 0.74f),
            Offset(size.width * 0.18f, size.height * 0.62f),
            Offset(size.width * 0.38f, size.height * 0.68f),
            Offset(size.width * 0.58f, size.height * 0.44f),
            Offset(size.width * 0.78f, size.height * 0.52f),
            Offset(size.width, size.height * 0.36f),
        )
        points.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = KeacsColors.Primary,
                start = start,
                end = end,
                strokeWidth = 3f,
            )
        }
    }
}

@Composable
private fun RankCard(onAddClick: () -> Unit) {
    KeacsCard {
        Column(modifier = Modifier.padding(it)) {
            Text(
                text = "支出排行",
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
            )
            EmptyState(
                title = "暂无数据",
                description = "快去记账吧，查看分类排行",
                actionText = "记一笔",
                icon = Icons.Rounded.WorkspacePremium,
                onActionClick = onAddClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )
        }
    }
}
