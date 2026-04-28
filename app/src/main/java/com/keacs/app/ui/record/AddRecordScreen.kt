package com.keacs.app.ui.record

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.PreferencesManager
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.domain.model.RecordType
import com.keacs.app.ui.components.KeacsSnackbar
import com.keacs.app.ui.components.SegmentedTabs
import com.keacs.app.ui.theme.KeacsSpacing
import kotlinx.coroutines.launch

@Composable
fun AddRecordScreen(
    repository: LocalDataRepository,
    preferencesManager: PreferencesManager,
    recordId: Long? = null,
    entryKey: Int = 0,
    onDone: () -> Unit = {},
) {
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val accounts by repository.observeAccounts().collectAsState(initial = emptyList())
    val records by repository.observeRecords().collectAsState(initial = emptyList())
    val defaultAccountId by preferencesManager.defaultRecordAccountId.collectAsState(initial = null)
    val defaultRecordType by preferencesManager.defaultRecordType.collectAsState(initial = null)
    val editing = records.firstOrNull { it.id == recordId }
    val scope = rememberCoroutineScope()

    var type by rememberSaveable(recordId, entryKey) { mutableStateOf(RecordType.EXPENSE) }
    var amount by rememberSaveable(recordId, entryKey) { mutableStateOf("") }
    var categoryId by rememberSaveable(recordId, entryKey) { mutableStateOf<Long?>(null) }
    var accountId by rememberSaveable(recordId, entryKey) { mutableStateOf<Long?>(null) }
    var fromAccountId by rememberSaveable(recordId, entryKey) { mutableStateOf<Long?>(null) }
    var toAccountId by rememberSaveable(recordId, entryKey) { mutableStateOf<Long?>(null) }
    var note by rememberSaveable(recordId, entryKey) { mutableStateOf("") }
    var occurredAt by rememberSaveable(recordId, entryKey) { mutableLongStateOf(System.currentTimeMillis()) }
    var error by rememberSaveable(recordId, entryKey) { mutableStateOf<String?>(null) }
    var isSaving by rememberSaveable(recordId, entryKey) { mutableStateOf(false) }
    var initializedFromPreference by rememberSaveable(recordId, entryKey) { mutableStateOf(false) }
    var amountLimitMessage by rememberSaveable(recordId, entryKey) { mutableStateOf<String?>(null) }

    val enabledAccounts = accounts.filter { it.isEnabled }
    val direction = if (type == RecordType.INCOME) PresetSeedData.CATEGORY_INCOME else PresetSeedData.CATEGORY_EXPENSE
    val availableCategories = categories.filter { it.isEnabled && it.direction == direction }
    val parsedAmount = amountToCent(amount)
    val canSave = parsedAmount != null && if (type == RecordType.TRANSFER) {
        fromAccountId != null && toAccountId != null && fromAccountId != toAccountId
    } else {
        categoryId != null
    }

    LaunchedEffect(editing?.id, defaultRecordType, defaultAccountId, enabledAccounts) {
        if (editing != null) {
            type = editing.type
            amount = centToInput(editing.amountCent)
            categoryId = editing.categoryId
            accountId = editing.fromAccountId ?: editing.toAccountId
            fromAccountId = editing.fromAccountId
            toAccountId = editing.toAccountId
            note = editing.note.orEmpty()
            occurredAt = editing.occurredAt
            initializedFromPreference = true
        } else if (!initializedFromPreference) {
            val preferredType = defaultRecordType?.toRecordType() ?: return@LaunchedEffect
            type = preferredType
            accountId = enabledAccounts.firstOrNull { it.id == defaultAccountId }?.id
            initializedFromPreference = true
        } else if (accountId == null) {
            accountId = enabledAccounts.firstOrNull { it.id == defaultAccountId }?.id
        }
    }

    LaunchedEffect(type, availableCategories, enabledAccounts) {
        if (type != RecordType.TRANSFER && availableCategories.none { it.id == categoryId }) {
            categoryId = availableCategories.firstOrNull()?.id
        }
        if (type != RecordType.TRANSFER && enabledAccounts.none { it.id == accountId }) {
            accountId = null
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
            .pointerInput(type) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        totalDrag += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        val nextType = when {
                            totalDrag <= -60f -> listOf(RecordType.EXPENSE, RecordType.INCOME, RecordType.TRANSFER)
                                .getOrNull(typeIndex(type) + 1)
                            totalDrag >= 60f -> listOf(RecordType.EXPENSE, RecordType.INCOME, RecordType.TRANSFER)
                                .getOrNull(typeIndex(type) - 1)
                            else -> null
                        }
                        if (nextType != null) {
                            type = nextType
                            error = null
                        }
                        totalDrag = 0f
                    },
                )
            }
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
    ) {
        SegmentedTabs(
            items = listOf("支出", "收入", "转账"),
            selectedIndex = typeIndex(type),
            onSelected = {
                type = listOf(RecordType.EXPENSE, RecordType.INCOME, RecordType.TRANSFER)[it]
                error = null
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (type == RecordType.TRANSFER) {
                TransferAccounts(
                    accounts = enabledAccounts,
                    accountCategories = categories,
                    fromId = fromAccountId,
                    toId = toAccountId,
                    onFrom = { fromAccountId = it },
                    onTo = { toAccountId = it },
                    modifier = Modifier.weight(1f),
                )
            } else {
                CategoryGrid(
                    categories = availableCategories,
                    selectedId = categoryId,
                    modifier = Modifier.weight(1f),
                    onSelected = { categoryId = it },
                )
            }
            FormArea(
                accounts = enabledAccounts,
                accountCategories = categories,
                accountId = accountId,
                showAccount = type != RecordType.TRANSFER,
                occurredAt = occurredAt,
                note = note,
                onAccountSelected = { accountId = it },
                onDateSelected = { occurredAt = it },
                onNoteChange = { note = it },
            )
        }
        AmountKeyboardPanel(
            modifier = Modifier.padding(top = 8.dp),
            amount = amount,
            parsedAmount = parsedAmount,
            message = validationText(type, parsedAmount, categoryId, fromAccountId, toAccountId, error),
            saveEnabled = canSave && !isSaving,
            onKeyClick = { key ->
                val next = nextAmount(amount, key)
                if (next != amount && amountInputWouldOverflow(next)) {
                    amountLimitMessage = "不能再加啦~"
                } else {
                    amount = next
                    error = null
                }
            },
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

    amountLimitMessage?.let { message ->
        KeacsSnackbar(
            message = message,
            atTop = true,
            onDismiss = { amountLimitMessage = null },
        )
    }
}

private fun String.toRecordType(): String = when (this) {
    RecordType.INCOME, RecordType.TRANSFER -> this
    else -> RecordType.EXPENSE
}
