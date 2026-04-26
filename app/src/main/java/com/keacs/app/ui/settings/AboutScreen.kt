package com.keacs.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-about")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(KeacsColors.Primary.copy(alpha = 0.92f), KeacsColors.Primary),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = KeacsColors.Surface,
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Keacs",
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "版本 1.0.0",
            color = KeacsColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "简单记账，轻松生活",
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AboutFeatureItem(
                title = "完全离线",
                description = "数据存储在本地设备，无需联网，保护您的隐私安全"
            )
            AboutFeatureItem(
                title = "无需注册",
                description = "打开即可使用，无需注册账号，无需填写任何信息"
            )
            AboutFeatureItem(
                title = "简单易用",
                description = "简洁直观的界面设计，让记账变得轻松愉快"
            )
            AboutFeatureItem(
                title = "轻量快速",
                description = "占用空间小，运行流畅，不消耗手机电量"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "感谢您使用 Keacs 记账！\n希望这款应用能帮助您更好地管理个人财务。",
            color = KeacsColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AboutFeatureItem(
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KeacsColors.SurfaceSubtle, MaterialTheme.shapes.medium)
            .padding(16.dp),
    ) {
        Text(
            text = title,
            color = KeacsColors.Primary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            color = KeacsColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
