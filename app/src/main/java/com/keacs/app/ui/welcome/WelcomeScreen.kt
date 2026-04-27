package com.keacs.app.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun SplashScreen() {
    WelcomePage(
        showStartButton = false,
        onStartClick = {},
    )
}

@Composable
fun WelcomeScreen(onStartClick: () -> Unit) {
    WelcomePage(
        showStartButton = true,
        onStartClick = onStartClick,
    )
}

@Composable
private fun WelcomePage(
    showStartButton: Boolean,
    onStartClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KeacsColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 28.dp)
            .testTag("screen-welcome"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "轻记账",
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "轻量 · 简单 · 离线记账",
            color = KeacsColors.TextSecondary,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(28.dp))
        WelcomeIllustration(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 340.dp)
                .height(230.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        FeatureCard(
            modifier = Modifier.widthIn(max = 430.dp),
        )
        Spacer(modifier = Modifier.height(30.dp))
        ActionArea(
            showStartButton = showStartButton,
            onStartClick = onStartClick,
            modifier = Modifier.widthIn(max = 430.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))
        PrivacyNote(modifier = Modifier.widthIn(max = 430.dp))
    }
}

@Composable
private fun FeatureCard(modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = KeacsColors.Surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            FeatureRow(
                icon = Icons.Rounded.VerifiedUser,
                title = "无需登录",
                subtitle = "打开即用，不用注册登录",
            )
            FeatureDivider()
            FeatureRow(
                icon = Icons.Rounded.Lock,
                title = "本地保存",
                subtitle = "数据仅保存在本机",
            )
            FeatureDivider()
            FeatureRow(
                icon = Icons.Rounded.WifiOff,
                title = "离线可用",
                subtitle = "无需网络，随时随地记账",
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(KeacsColors.SurfaceSubtle, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = KeacsColors.TextPrimary,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = KeacsColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun FeatureDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 64.dp),
        color = KeacsColors.Border,
        thickness = 0.5.dp,
    )
}

@Composable
private fun ActionArea(
    showStartButton: Boolean,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (showStartButton) {
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("welcome-start"),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KeacsColors.Primary,
                    contentColor = KeacsColors.Surface,
                ),
                contentPadding = PaddingValues(horizontal = KeacsSpacing.CardPadding),
            ) {
                Text(
                    text = "开始记账",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(KeacsColors.Surface, MaterialTheme.shapes.medium),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = KeacsColors.Primary,
                    trackColor = KeacsColors.PrimaryLight,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "正在进入",
                    color = KeacsColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun PrivacyNote(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Security,
            contentDescription = null,
            tint = KeacsColors.TextSecondary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "数据仅保存在本设备，不会上传或同步任何云端",
            color = KeacsColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}
