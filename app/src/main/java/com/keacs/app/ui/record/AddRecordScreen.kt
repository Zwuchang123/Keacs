package com.keacs.app.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.ui.components.AmountText
import com.keacs.app.ui.components.CategoryIcon
import com.keacs.app.ui.components.FormFieldRow
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.NumberPad
import com.keacs.app.ui.components.SegmentedTabs
import com.keacs.app.ui.management.colorFor
import com.keacs.app.ui.management.iconFor
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun AddRecordScreen(repository: LocalDataRepository) {
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val accounts by repository.observeAccounts().collectAsState(initial = emptyList())
    val enabledExpenses = categories
        .filter { it.direction == PresetSeedData.CATEGORY_EXPENSE && it.isEnabled }
        .take(10)
    val accountName = accounts.firstOrNull { it.isEnabled }?.name ?: "未选择"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-add")
            .padding(
                start = KeacsSpacing.PageHorizontal,
                top = KeacsSpacing.PageVertical,
                end = KeacsSpacing.PageHorizontal,
            ),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SegmentedTabs(items = listOf("支出", "收入", "转账"), selectedIndex = 0)
                CategoryGrid(enabledExpenses)
                AmountArea()
            }
            SaveHint()
            FormArea(accountName)
            Spacer(modifier = Modifier.weight(1f))
        }
        NumberPad(modifier = Modifier.padding(top = KeacsSpacing.ItemGap))
    }
}

@Composable
private fun CategoryGrid(categories: List<CategoryEntity>) {
    val rows = categories.chunked(5)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { category ->
                    CategoryChoice(
                        category = category,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(5 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryChoice(
    category: CategoryEntity,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CategoryIcon(
            icon = iconFor(category.iconKey),
            backgroundColor = colorFor(category.colorKey),
        )
        Text(
            text = category.name,
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun AmountArea() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        AmountText(amount = "¥0.00")
    }
}

@Composable
private fun SaveHint() {
    Text(
        text = "金额大于0才可保存",
        color = KeacsColors.TextTertiary,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun FormArea(accountName: String) {
    KeacsCard(contentPadding = PaddingValues(10.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormFieldRow(
                icon = Icons.Rounded.AccountBalanceWallet,
                title = "账户",
                value = accountName,
            )
            FormFieldRow(
                icon = Icons.Rounded.CalendarToday,
                title = "日期",
                value = "今天 4月25日",
            )
            FormFieldRow(
                icon = Icons.AutoMirrored.Rounded.Notes,
                title = "备注",
                value = "添加备注",
            )
        }
    }
}
