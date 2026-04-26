package com.keacs.app.ui.record

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.domain.model.RecordType
import com.keacs.app.domain.usecase.DeleteRecordUseCase
import com.keacs.app.ui.components.SegmentedTabs
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import kotlinx.coroutines.launch

@Composable
fun AddRecordScreen(
    repository: LocalDataRepository,
    recordId: Long? = null,
    onDone: () -> Unit = {},
) {
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val accounts by repository.observeAccounts().collectAsState(initial = emptyList())
    val records by repository.observeRecords().collectAsState(initial = emptyList())
    val editing = records.firstOrNull { it.id == recordId }
    val scope = rememberCoroutineScope()

    var type by rememberSaveable(recordId) { mutableStateOf(RecordType.EXPENSE) }
    var amount by rememberSaveable(recordId) { mutableStateOf("") }
    var categoryId by rememberSaveable(recordId) { mutableStateOf<Long?>(null) }
    var accountId by rememberSaveable(recordId) { mutableStateOf<Long?>(null) }
    var fromAccountId by rememberSaveable(recordId) { mutableStateOf<Long?>(null) }
    var toAccountId by rememberSaveable(recordId) { mutableStateOf<Long?>(null) }
    var note by rememberSaveable(recordId) { mutableStateOf("") }
    var occurredAt by rememberSaveable(recordId) { mutableLongStateOf(System.currentTimeMillis()) }
    var error by rememberSaveable(recordId) { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable(recordId) { mutableStateOf(false) }
    var isSaving by rememberSaveable(recordId) { mutableStateOf(false) }

    val enabledAccounts = accounts.filter { it.isEnabled }
    val direction = if (type == RecordType.INCOME) PresetSeedData.CATEGORY_INCOME else PresetSeedData.CATEGORY_EXPENSE
    val availableCategories = categories.filter { it.isEnabled && it.direction == direction }
    val parsedAmount = amountToCent(amount)
    val canSave = parsedAmount != null && if (type == RecordType.TRANSFER) {
        fromAccountId != null && toAccountId != null && fromAccountId != toAccountId
    } else {
        categoryId != null
    }

    LaunchedEffect(editing?.id) {
        if (editing != null) {
            type = editing.type
            amount = centToInput(editing.amountCent)
            categoryId = editing.categoryId
            accountId = editing.fromAccountId ?: editing.toAccountId
            fromAccountId = editing.fromAccountId
            toAccountId = editing.toAccountId
            note = editing.note.orEmpty()
            occurredAt = editing.occurredAt
        }
    }

    LaunchedEffect(type, availableCategories, enabledAccounts) {
        if (type != RecordType.TRANSFER && availableCategories.none { it.id == categoryId }) {
            categoryId = availableCategories.firstOrNull()?.id
        }
        if (type == RecordType.TRANSFER && enabledAccounts.isNotEmpty()) {
            if (enabledAccounts.none { it.id == fromAccountId }) fromAccountId = enabledAccounts.first().id
            if (enabledAccounts.none { it.id == toAccountId }) toAccountId = enabledAccounts.drop(1).firstOrNull()?.id
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-add")
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
        ) {
            SegmentedTabs(
                items = listOf("支出", "收入", "转账"),
                selectedIndex = typeIndex(type),
                onSelected = {
                    type = listOf(RecordType.EXPENSE, RecordType.INCOME, RecordType.TRANSFER)[it]
                    error = null
                },
            )
            if (type == RecordType.TRANSFER) {
                TransferAccounts(enabledAccounts, fromAccountId, toAccountId, { fromAccountId = it }, { toAccountId = it })
            } else {
                CategoryGrid(availableCategories, categoryId) { categoryId = it }
            }
            FormArea(
                accounts = enabledAccounts,
                accountId = accountId,
                showAccount = type != RecordType.TRANSFER,
                occurredAt = occurredAt,
                note = note,
                onAccountSelected = { accountId = it },
                onToday = { occurredAt = System.currentTimeMillis() },
                onYesterday = { occurredAt = System.currentTimeMillis() - ONE_DAY },
                onNoteChange = { note = it },
            )
            if (recordId != null) {
                Text(
                    text = "删除账目",
                    color = KeacsColors.Error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { confirmDelete = true }
                        .padding(12.dp),
                )
            }
        }
        AmountKeyboardPanel(
            modifier = Modifier.padding(top = KeacsSpacing.ItemGap),
            amount = amount,
            parsedAmount = parsedAmount,
            message = validationText(type, parsedAmount, categoryId, fromAccountId, toAccountId, error),
            saveEnabled = canSave && !isSaving,
            onKeyClick = { key -> amount = nextAmount(amount, key); error = null },
            onSaveClick = {
                val cents = parsedAmount ?: return@AmountKeyboardPanel
                scope.launch {
                    isSaving = true
                    runCatching {
                        saveRecord(repository, recordId, type, cents, occurredAt, categoryId, accountId, fromAccountId, toAccountId, note)
                    }.onSuccess { onDone() }
                        .onFailure { error = it.message ?: "保存失败，请稍后重试" }
                    isSaving = false
                }
            },
        )
    }

    if (confirmDelete) {
        DeleteDialog(
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    recordId?.let { DeleteRecordUseCase(repository)(it) }
                    onDone()
                }
            },
        )
    }
}
