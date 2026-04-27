package com.keacs.app.ui.settings

import android.content.ActivityNotFoundException
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keacs.app.BuildConfig
import com.keacs.app.R
import com.keacs.app.ui.components.DividedMenuCard
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.KeacsSnackbar
import com.keacs.app.ui.components.MenuDivider
import com.keacs.app.ui.components.MenuRow
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun AboutScreen() {
    val uriHandler = LocalUriHandler.current
    val errorMessage = remember { mutableStateOf<String?>(null) }

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
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher),
                    contentDescription = "Keacs 图标",
                    modifier = Modifier.size(64.dp),
                )
                Text(
                    text = "Keacs",
                    color = KeacsColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "版本 ${BuildConfig.VERSION_NAME}",
                    color = KeacsColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        DividedMenuCard {
            MenuRow(
                Icons.Rounded.Info,
                "当前版本",
                KeacsColors.TextSecondary,
                value = BuildConfig.VERSION_NAME,
                enabled = false,
            )
            MenuDivider()
            MenuRow(
                Icons.Rounded.FileDownload,
                "获取更新",
                KeacsColors.Primary,
                onClick = {
                    try {
                        uriHandler.openUri(BuildConfig.UPDATE_URL)
                    } catch (_: ActivityNotFoundException) {
                        errorMessage.value = "无法打开更新页面"
                    } catch (_: IllegalArgumentException) {
                        errorMessage.value = "无法打开更新页面"
                    }
                },
            )
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

    errorMessage.value?.let { message ->
        KeacsSnackbar(
            message = message,
            isError = true,
            onDismiss = { errorMessage.value = null },
        )
    }
}
