package com.keacs.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.keacs.app.ui.components.DividedMenuCard
import com.keacs.app.ui.components.MenuDivider
import com.keacs.app.ui.components.MenuRow
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun MineScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-mine")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        DividedMenuCard {
            MenuRow(Icons.Rounded.Category, "分类管理", KeacsColors.Primary)
            MenuDivider()
            MenuRow(Icons.Rounded.AccountBalanceWallet, "账户管理", KeacsColors.Income)
        }
        DividedMenuCard {
            MenuRow(Icons.Rounded.FileUpload, "导出备份", KeacsColors.Warning)
            MenuDivider()
            MenuRow(Icons.Rounded.FileDownload, "导入备份", KeacsColors.Primary)
        }
        DividedMenuCard {
            MenuRow(Icons.Rounded.Settings, "设置", KeacsColors.TextSecondary)
            MenuDivider()
            MenuRow(Icons.Rounded.Info, "关于", KeacsColors.Primary)
        }
    }
}
