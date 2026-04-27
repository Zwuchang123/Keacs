package com.keacs.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.components.CategoryIcon
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-about")
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        KeacsCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(it),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CategoryIcon(
                    icon = Icons.Rounded.Info,
                    backgroundColor = KeacsColors.Primary,
                )
                Text(
                    text = "Keacs",
                    color = KeacsColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "版本 1.0",
                    color = KeacsColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        KeacsCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(it),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("本地记账", color = KeacsColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "数据只保存在本机，不需要账号，也不依赖网络。",
                    color = KeacsColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "适合个人快速记录收入、支出和转账，页面保持简单轻量。",
                    color = KeacsColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
