package com.keacs.app.ui.management

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.domain.rule.balanceFor
import com.keacs.app.domain.usecase.AccountManagementUseCase
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.KeacsSnackbar
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@Composable
fun AccountListScreen(
    repository: LocalDataRepository,
    onEditAccount: (Long?) -> Unit,
) {
    val accounts by repository.observeAccounts().collectAsState(initial = emptyList())
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val records by repository.observeRecords().collectAsState(initial = emptyList())
    val assets = accounts.filter { it.nature == PresetSeedData.ACCOUNT_ASSET }
    val liabilities = accounts.filter { it.nature == PresetSeedData.ACCOUNT_LIABILITY }
    val totalAsset = assets.sumOf { balanceFor(it, records) }
    val totalLiability = liabilities.sumOf { balanceFor(it, records) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-account-list")
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        AccountSummary(totalAsset = totalAsset, totalLiability = totalLiability)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
        ) {
            item { AccountGroup("资产账户", assets, categories, records, onEditAccount) }
            item { AccountGroup("负债账户", liabilities, categories, records, onEditAccount) }
        }
        Button(onClick = { onEditAccount(null) }, modifier = Modifier.fillMaxWidth()) {
            Text("＋ 新增账户")
        }
    }
}

@Composable
fun AccountEditScreen(
    repository: LocalDataRepository,
    accountId: Long?,
    deleteRequest: Int = 0,
    onDone: () -> Unit,
) {
    val accounts by repository.observeAccounts().collectAsState(initial = emptyList())
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val editing = accounts.firstOrNull { it.id == accountId }
    val typeOptions = remember(categories) { accountTypeOptions(categories) }
    val useCase = remember(repository) { AccountManagementUseCase(repository) }
    val scope = rememberCoroutineScope()

    var name by rememberSaveable(accountId) { mutableStateOf("") }
    var nature by rememberSaveable(accountId) { mutableStateOf(PresetSeedData.ACCOUNT_ASSET) }
    var type by rememberSaveable(accountId) { mutableStateOf("现金") }
    var balance by rememberSaveable(accountId) { mutableStateOf("0.00") }
    var iconKey by rememberSaveable(accountId) { mutableStateOf("wallet") }
    var colorKey by rememberSaveable(accountId) { mutableStateOf("green") }
    var isEnabled by rememberSaveable(accountId) { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var amountLimitMessage by rememberSaveable(accountId) { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var handledDeleteRequest by rememberSaveable(accountId) { mutableStateOf(deleteRequest) }

    LaunchedEffect(editing?.id) {
        editing?.let {
            name = it.name
            nature = it.nature
            type = it.type
            balance = centsToInput(it.initialBalanceCent)
            iconKey = it.iconKey
            colorKey = it.colorKey
            isEnabled = it.isEnabled
        }
    }

    LaunchedEffect(deleteRequest) {
        if (deleteRequest > handledDeleteRequest && accountId != null) {
            handledDeleteRequest = deleteRequest
            confirmDelete = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-account-edit")
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
        ) {
            ManagementTextField("账户名称", name, { name = it; error = null }, error = error)
            NatureSelector(nature) { nature = it }
            AccountTypeSelector(typeOptions, type) { option ->
                type = option.label
                iconKey = option.key
                colorKey = option.colorKey
            }
            SwitchCard(isEnabled, onCheckedChange = { isEnabled = it })
            ErrorText(error)
        }
        AccountBalanceKeyboardPanel(
            balance = balance,
            error = balanceError(balance),
            saveEnabled = name.isNotBlank() && parseCent(balance) != null,
            onKeyClick = {
                val next = nextBalanceAmount(balance, it)
                if (next != balance && balanceInputWouldOverflow(next)) {
                    amountLimitMessage = "不能再加啦~"
                } else {
                    balance = next
                    error = null
                }
            },
            onSaveClick = {
                scope.launch {
                    val cents = parseCent(balance)
                    if (cents == null) {
                        error = "余额格式不正确"
                        return@launch
                    }
                    runCatching {
                        useCase.save(accountId, name, nature, type, iconKey, colorKey, cents, isEnabled)
                    }.onSuccess { onDone() }
                        .onFailure { error = it.message ?: "保存失败，请稍后重试" }
                }
            },
        )
    }

    amountLimitMessage?.let { message ->
        KeacsSnackbar(
            message = message,
            atTop = true,
            onDismiss = { amountLimitMessage = null },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这个账户？") },
            text = { Text("没有历史记录时可以删除；已经用过的账户不能删除，只能停用。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        scope.launch {
                            runCatching { accountId?.let { useCase.delete(it) } }
                                .onSuccess { onDone() }
                                .onFailure { error = it.message ?: "删除失败，请稍后重试" }
                        }
                    },
                ) { Text("删除", color = KeacsColors.Error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun AccountSummary(totalAsset: Long, totalLiability: Long) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.verticalGradient(
                    listOf(KeacsColors.Primary.copy(alpha = 0.92f), KeacsColors.Primary),
                ),
            )
            .padding(KeacsSpacing.CardPadding),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("净资产", color = KeacsColors.PrimaryLight, style = MaterialTheme.typography.bodySmall)
            Text(
                text = formatCent(totalAsset - totalLiability),
                color = KeacsColors.Surface,
                style = MaterialTheme.typography.displaySmall,
                fontFamily = FontFamily.Monospace,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("资产 ${formatCent(totalAsset)}", color = KeacsColors.PrimaryLight)
                Text("负债 ${formatCent(totalLiability)}", color = KeacsColors.PrimaryLight)
            }
        }
    }
}

@Composable
private fun AccountGroup(
    title: String,
    accounts: List<com.keacs.app.data.local.entity.AccountEntity>,
    categories: List<com.keacs.app.data.local.entity.CategoryEntity>,
    records: List<com.keacs.app.data.local.entity.RecordEntity>,
    onEditAccount: (Long?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = KeacsColors.TextPrimary)
        KeacsCard(contentPadding = PaddingValues(0.dp)) {
            Column(modifier = Modifier.padding(it)) {
                accounts.forEachIndexed { index, account ->
                    val iconOption = accountIconOptionFor(account, categories)
                    ManagementListItem(
                        title = account.name,
                        subtitle = if (account.isEnabled) "${natureText(account.nature)} · 当前余额" else "历史记录仍会显示",
                        icon = iconOption.icon,
                        color = colorFor(iconOption.colorKey),
                        enabled = account.isEnabled,
                        trailing = formatCent(balanceFor(account, records)),
                        onClick = { onEditAccount(account.id) },
                    )
                    if (index != accounts.lastIndex) ListDivider()
                }
            }
        }
    }
}

private fun natureText(nature: String): String =
    if (nature == PresetSeedData.ACCOUNT_LIABILITY) "负债" else "资产"

private fun formatCent(value: Long): String =
    DecimalFormat("#,##0.00").format(value / 100.0)
