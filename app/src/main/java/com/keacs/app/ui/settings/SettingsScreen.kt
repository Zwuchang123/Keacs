package com.keacs.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.keacs.app.ui.components.ConfirmDialog
import com.keacs.app.ui.components.DividedMenuCard
import com.keacs.app.ui.components.KeacsSnackbar
import com.keacs.app.ui.components.MenuDivider
import com.keacs.app.ui.components.MenuRow
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun SettingsScreen() {
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showClearSuccess by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-settings")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        DividedMenuCard {
            MenuRow(Icons.Rounded.AccountBalanceWallet, "默认记账账户", KeacsColors.Primary, value = "无")
            MenuDivider()
            MenuRow(Icons.Rounded.Category, "默认分类", KeacsColors.Primary, value = "无")
            MenuDivider()
            MenuRow(Icons.Rounded.AccessTime, "记账时间默认值", KeacsColors.Primary, value = "当前时间")
            MenuDivider()
            MenuRow(Icons.Rounded.Numbers, "金额小数位", KeacsColors.Primary, value = "2位")
        }
        DividedMenuCard {
            MenuRow(Icons.Rounded.DeleteOutline, "清除缓存", KeacsColors.Error, onClick = { showClearCacheConfirm = true })
        }
    }

    if (showClearCacheConfirm) {
        ConfirmDialog(
            title = "清除缓存",
            text = "清除缓存不会删除账单数据，但可能需要重新加载部分内容。确定清除吗？",
            confirmText = "清除",
            onConfirm = {
                showClearCacheConfirm = false
                context.cacheDir.deleteRecursively()
                showClearSuccess = true
            },
            onDismiss = { showClearCacheConfirm = false },
            isDestructive = true
        )
    }

    if (showClearSuccess) {
        KeacsSnackbar(
            message = "缓存已清除",
            onDismiss = { showClearSuccess = false }
        )
    }
}
