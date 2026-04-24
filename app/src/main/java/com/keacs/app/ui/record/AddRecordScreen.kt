package com.keacs.app.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.components.AmountText
import com.keacs.app.ui.components.CategoryIcon
import com.keacs.app.ui.components.FormFieldRow
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.NumberPad
import com.keacs.app.ui.components.SegmentedTabs
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun AddRecordScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-add")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        SegmentedTabs(items = listOf("支出", "收入", "转账"), selectedIndex = 0)
        CategoryGrid()
        AmountArea()
        FormArea()
        NumberPad()
    }
}

@Composable
private fun CategoryGrid() {
    val rows = listOf(
        listOf(
            CategoryOption("餐饮", Icons.Rounded.Restaurant, KeacsColors.CategoryOrange),
            CategoryOption("交通", Icons.Rounded.DirectionsBus, KeacsColors.CategoryGray, KeacsColors.TextSecondary),
            CategoryOption("购物", Icons.Rounded.ShoppingBag, KeacsColors.CategoryGray, KeacsColors.TextSecondary),
            CategoryOption("住房", Icons.Rounded.Home, KeacsColors.CategoryGray, KeacsColors.TextSecondary),
            CategoryOption("更多", Icons.Rounded.MoreHoriz, KeacsColors.CategoryGray, KeacsColors.TextSecondary),
        ),
        listOf(
            CategoryOption("通讯", Icons.Rounded.PhoneAndroid, KeacsColors.CategoryGray, KeacsColors.TextSecondary),
            CategoryOption("医疗", Icons.Rounded.LocalHospital, KeacsColors.CategoryGray, KeacsColors.TextSecondary),
            CategoryOption("教育", Icons.Rounded.School, KeacsColors.CategoryGray, KeacsColors.TextSecondary),
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { options ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                options.forEach { option ->
                    CategoryChoice(option = option)
                }
            }
        }
    }
}

@Composable
private fun CategoryChoice(option: CategoryOption) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CategoryIcon(
            icon = option.icon,
            backgroundColor = option.background,
            tint = option.tint,
        )
        Text(
            text = option.name,
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
        Text(
            text = "金额大于 0 后才可保存",
            color = KeacsColors.TextTertiary,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Default,
        )
    }
}

@Composable
private fun FormArea() {
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
                value = "现金",
            )
            FormFieldRow(
                icon = Icons.Rounded.CalendarToday,
                title = "日期",
                value = "今天",
            )
            FormFieldRow(
                icon = Icons.AutoMirrored.Rounded.Notes,
                title = "备注",
                value = "添加备注",
            )
        }
    }
}

private data class CategoryOption(
    val name: String,
    val icon: ImageVector,
    val background: Color,
    val tint: Color = KeacsColors.Surface,
)
