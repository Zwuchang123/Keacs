package com.keacs.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keacs.app.domain.agent.AgentModelServiceMode
import com.keacs.app.domain.agent.AgentSettings
import com.keacs.app.domain.agent.validateForRequest
import com.keacs.app.ui.components.CategoryIcon
import com.keacs.app.ui.components.DividedMenuCard
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.MenuDivider
import com.keacs.app.ui.components.MenuRow
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun AgentSettingsSection(
    settings: AgentSettings,
    showModeSelector: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onModeClick: () -> Unit,
    onModeSelected: (AgentModelServiceMode) -> Unit,
    onModeDismiss: () -> Unit,
    onCustomBaseUrlChange: (String) -> Unit,
    onCustomApiKeyChange: (String) -> Unit,
    onCustomModelNameChange: (String) -> Unit,
) {
    val validation = settings.validateForRequest()

    DividedMenuCard {
        AgentSwitchRow(
            enabled = settings.enabled,
            message = if (settings.enabled) {
                validation.message ?: "已启用"
            } else {
                "未启用时不会联网"
            },
            onEnabledChange = onEnabledChange,
        )
        MenuDivider()
        MenuRow(
            icon = Icons.Rounded.Cloud,
            title = "模型服务",
            iconColor = KeacsColors.Primary,
            value = settings.serviceMode.label(),
            onClick = onModeClick,
        )
    }

    if (settings.serviceMode == AgentModelServiceMode.CUSTOM) {
        KeacsCard(contentPadding = PaddingValues(KeacsSpacing.CardPadding)) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AgentTextField(
                    label = "访问地址",
                    value = settings.customBaseUrl,
                    placeholder = "https://api.example.com/v1",
                    icon = Icons.Rounded.Link,
                    onValueChange = onCustomBaseUrlChange,
                )
                AgentTextField(
                    label = "API Key",
                    value = settings.customApiKey,
                    placeholder = "填写后仅保存在本机",
                    icon = Icons.Rounded.Key,
                    secret = true,
                    onValueChange = onCustomApiKeyChange,
                )
                AgentTextField(
                    label = "模型名称",
                    value = settings.customModelName,
                    placeholder = "例如 gpt-4o-mini",
                    icon = Icons.Rounded.Psychology,
                    onValueChange = onCustomModelNameChange,
                )
            }
        }
    }

    if (showModeSelector) {
        AgentModeBottomSheet(
            selectedMode = settings.serviceMode,
            onSelected = onModeSelected,
            onDismiss = onModeDismiss,
        )
    }
}

@Composable
private fun AgentSwitchRow(
    enabled: Boolean,
    message: String,
    onEnabledChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(
            icon = Icons.Rounded.AutoAwesome,
            backgroundColor = KeacsColors.Primary.copy(alpha = 0.14f),
            tint = KeacsColors.Primary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "在线助手",
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = message,
                color = if (enabled && message != "已启用") KeacsColors.Warning else KeacsColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Composable
private fun AgentTextField(
    label: String,
    value: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    secret: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(KeacsColors.SurfaceSubtle)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = KeacsColors.TextSecondary,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = KeacsColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = KeacsColors.TextPrimary),
                cursorBrush = SolidColor(KeacsColors.Primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (secret) KeyboardType.Password else KeyboardType.Uri,
                ),
                visualTransformation = if (secret) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
                decorationBox = { inner ->
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            color = KeacsColors.TextTertiary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                },
            )
        }
    }
}
