package com.keacs.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.PreferencesManager
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.domain.model.RecordType
import com.keacs.app.ui.components.ConfirmDialog
import com.keacs.app.ui.components.DividedMenuCard
import com.keacs.app.ui.components.KeacsSnackbar
import com.keacs.app.ui.components.MenuDivider
import com.keacs.app.ui.components.MenuRow
import com.keacs.app.ui.record.AccountSelectorBottomSheet
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: LocalDataRepository,
    preferencesManager: PreferencesManager,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accounts by repository.observeAccounts().collectAsState(initial = emptyList())
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val defaultAccountId by preferencesManager.defaultRecordAccountId.collectAsState(initial = null)
    val defaultRecordType by preferencesManager.defaultRecordType.collectAsState(initial = RecordType.EXPENSE)
    val enabledAccounts = accounts.filter { it.isEnabled }

    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showAccountSelector by remember { mutableStateOf(false) }
    var showTypeSelector by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val defaultAccountLabel = enabledAccounts.firstOrNull { it.id == defaultAccountId }?.name ?: "不默认带出"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-settings")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        DividedMenuCard {
            MenuRow(
                icon = Icons.Rounded.AccountBalanceWallet,
                title = "默认记账账户",
                iconColor = KeacsColors.Primary,
                value = defaultAccountLabel,
                onClick = { showAccountSelector = true },
            )
            MenuDivider()
            MenuRow(
                icon = Icons.Rounded.Category,
                title = "默认分类",
                iconColor = KeacsColors.Primary,
                value = defaultRecordType.label(),
                onClick = { showTypeSelector = true },
            )
            MenuDivider()
            MenuRow(
                icon = Icons.Rounded.AccessTime,
                title = "记账时间默认值",
                iconColor = KeacsColors.Primary,
                value = "当前时间",
                onClick = { snackbarMessage = "暂不支持配置" },
            )
            MenuDivider()
            MenuRow(
                icon = Icons.Rounded.Numbers,
                title = "金额小数位",
                iconColor = KeacsColors.Primary,
                value = "2位",
                onClick = { snackbarMessage = "暂不支持配置" },
            )
        }
        DividedMenuCard {
            MenuRow(
                icon = Icons.Rounded.DeleteOutline,
                title = "清除缓存",
                iconColor = KeacsColors.Error,
                onClick = { showClearCacheConfirm = true },
            )
        }
    }

    if (showAccountSelector) {
        AccountSelectorBottomSheet(
            accounts = enabledAccounts,
            accountCategories = categories,
            selectedId = defaultAccountId,
            title = "默认记账账户",
            includeNone = true,
            onSelected = { selectedId ->
                scope.launch {
                    preferencesManager.setDefaultRecordAccountId(selectedId)
                }
            },
            onDismiss = { showAccountSelector = false },
        )
    }

    if (showTypeSelector) {
        RecordTypeBottomSheet(
            selectedType = defaultRecordType,
            onSelected = { selectedType ->
                scope.launch {
                    preferencesManager.setDefaultRecordType(selectedType)
                }
            },
            onDismiss = { showTypeSelector = false },
        )
    }

    if (showClearCacheConfirm) {
        ConfirmDialog(
            title = "清除缓存",
            text = "清除缓存不会删除账单数据，但可能需要重新加载部分内容。确定清除吗？",
            confirmText = "清除",
            onConfirm = {
                showClearCacheConfirm = false
                context.cacheDir.deleteRecursively()
                snackbarMessage = "缓存已清除"
            },
            onDismiss = { showClearCacheConfirm = false },
            isDestructive = true,
        )
    }

    snackbarMessage?.let { message ->
        KeacsSnackbar(
            message = message,
            atTop = message == "暂不支持配置",
            onDismiss = { snackbarMessage = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordTypeBottomSheet(
    selectedType: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(RecordType.EXPENSE, RecordType.INCOME, RecordType.TRANSFER)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = KeacsColors.Surface,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = KeacsSpacing.PageHorizontal)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(48.dp))
                Text(
                    text = "选择默认类型",
                    color = KeacsColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "取消",
                        tint = KeacsColors.TextSecondary,
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(options) { option ->
                    RecordTypeOptionRow(
                        label = option.label(),
                        selected = option == selectedType,
                        onClick = {
                            onSelected(option)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordTypeOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) KeacsColors.PrimaryLight else KeacsColors.SurfaceSubtle)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = if (selected) KeacsColors.Primary else KeacsColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (selected) {
            Text(
                text = "已选",
                color = KeacsColors.Primary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

private fun String.label(): String = when (this) {
    RecordType.INCOME -> "收入"
    RecordType.TRANSFER -> "转账"
    else -> "支出"
}
