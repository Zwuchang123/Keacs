package com.keacs.app.ui.scheduled

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
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.data.repository.ScheduledFrequency
import com.keacs.app.data.repository.ScheduledRecordRepository
import com.keacs.app.domain.model.RecordType
import com.keacs.app.ui.components.ConfirmDialog
import com.keacs.app.ui.components.SegmentedTabs
import com.keacs.app.ui.record.AccountSelectorBottomSheet
import com.keacs.app.ui.record.AmountKeyboardPanel
import com.keacs.app.ui.record.CategoryGrid
import com.keacs.app.ui.record.TransferAccounts
import com.keacs.app.ui.record.amountInputWouldOverflow
import com.keacs.app.ui.record.amountToCent
import com.keacs.app.ui.record.centToInput
import com.keacs.app.ui.record.nextAmount
import com.keacs.app.ui.record.typeIndex
import com.keacs.app.ui.theme.KeacsSpacing
import kotlinx.coroutines.launch

@Composable
fun ScheduledRecordEditScreen(
    repository: LocalDataRepository,
    scheduledRepository: ScheduledRecordRepository,
    scheduleId: Long?,
    deleteRequest: Int = 0,
    onDone: () -> Unit,
) {
    val schedules by scheduledRepository.observeSchedules().collectAsState(initial = emptyList())
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val accounts by repository.observeAccounts().collectAsState(initial = emptyList())
    val editing = schedules.firstOrNull { it.id == scheduleId }
    val scope = rememberCoroutineScope()

    var type by rememberSaveable(scheduleId) { mutableStateOf(RecordType.EXPENSE) }
    var frequency by rememberSaveable(scheduleId) { mutableStateOf(ScheduledFrequency.MONTHLY) }
    var amount by rememberSaveable(scheduleId) { mutableStateOf("") }
    var categoryId by rememberSaveable(scheduleId) { mutableStateOf<Long?>(null) }
    var fromAccountId by rememberSaveable(scheduleId) { mutableStateOf<Long?>(null) }
    var toAccountId by rememberSaveable(scheduleId) { mutableStateOf<Long?>(null) }
    var nextRunAt by rememberSaveable(scheduleId) { mutableLongStateOf(defaultNextRunAt()) }
    var recurrenceValues by rememberSaveable(scheduleId) {
        mutableStateOf("")
    }
    var note by rememberSaveable(scheduleId) { mutableStateOf("") }
    var isEnabled by rememberSaveable(scheduleId) { mutableStateOf(true) }
    var error by rememberSaveable(scheduleId) { mutableStateOf<String?>(null) }
    var isSaving by rememberSaveable(scheduleId) { mutableStateOf(false) }
    var showAccountSelector by rememberSaveable { mutableStateOf(false) }
    var showDateSelector by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable(scheduleId) { mutableStateOf(false) }
    var handledDeleteRequest by rememberSaveable(scheduleId) { mutableStateOf(deleteRequest) }

    val direction = if (type == RecordType.INCOME) PresetSeedData.CATEGORY_INCOME else PresetSeedData.CATEGORY_EXPENSE
    val availableCategories = categories.filter { it.direction == direction && it.isEnabled }
    val availableAccounts = accounts.filter { it.isEnabled }
    val parsedAmount = amountToCent(amount)
    val canSave = parsedAmount != null && recurrenceValues.isNotBlank() && if (type == RecordType.TRANSFER) {
        fromAccountId != null && toAccountId != null && fromAccountId != toAccountId
    } else {
        categoryId != null
    }

    LaunchedEffect(editing?.id) {
        editing?.let {
            type = it.type
            frequency = ScheduledRecordRepository.normalizeFrequency(it.frequency)
            amount = centToInput(it.amountCent)
            categoryId = it.categoryId
            fromAccountId = it.fromAccountId
            toAccountId = it.toAccountId
            nextRunAt = it.nextRunAt
            recurrenceValues = it.recurrenceValues
                ?: ScheduledRecordRepository.recurrenceValuesFromParts(
                    frequency = it.frequency,
                    month = it.recurrenceMonth,
                    day = it.recurrenceDay,
                    weekday = it.recurrenceWeekday,
                    fallbackMillis = it.nextRunAt,
                )
            note = it.note.orEmpty()
            isEnabled = it.isEnabled
        }
    }

    LaunchedEffect(type, availableCategories) {
        if (type != RecordType.TRANSFER && availableCategories.none { it.id == categoryId }) {
            categoryId = availableCategories.firstOrNull()?.id
        }
    }

    LaunchedEffect(type, availableAccounts) {
        if (type == RecordType.TRANSFER && availableAccounts.isNotEmpty()) {
            if (availableAccounts.none { it.id == fromAccountId }) fromAccountId = availableAccounts.first().id
            if (availableAccounts.none { it.id == toAccountId }) toAccountId = availableAccounts.drop(1).firstOrNull()?.id
        }
    }

    LaunchedEffect(deleteRequest) {
        if (deleteRequest > handledDeleteRequest && scheduleId != null) {
            handledDeleteRequest = deleteRequest
            showDeleteDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-scheduled-edit")
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                TransferAccounts(
                    accounts = availableAccounts,
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
            if (scheduleId != null) {
                EnabledField(isEnabled = isEnabled, onEnabledChange = { isEnabled = it })
            }
        }
        AmountKeyboardPanel(
            modifier = Modifier.padding(top = 8.dp),
            amount = amount,
            parsedAmount = parsedAmount,
            message = error ?: when {
                parsedAmount != null && recurrenceValues.isBlank() -> "请选择记账时间"
                else -> scheduledValidationText(type, parsedAmount, categoryId, fromAccountId, toAccountId, null)
            },
            saveEnabled = canSave && !isSaving,
            onKeyClick = { key ->
                val next = nextAmount(amount, key)
                if (!amountInputWouldOverflow(next)) {
                    amount = next
                    error = null
                }
            },
            onSaveClick = {
                scope.launch {
                    val cents = parsedAmount
                    if (cents == null) {
                        error = "金额大于0才可保存"
                        return@launch
                    }
                    isSaving = true
                    runCatching {
                        scheduledRepository.saveSchedule(
                            id = scheduleId,
                            type = type,
                            amountCent = cents,
                            categoryId = categoryId,
                            fromAccountId = fromAccountId,
                            toAccountId = toAccountId,
                            frequency = frequency,
                            recurrenceValues = recurrenceValues,
                            nextRunAt = nextRunAt,
                            note = note,
                            isEnabled = isEnabled,
                        )
                    }.onSuccess { onDone() }
                        .onFailure { error = it.message ?: "保存失败，请稍后重试" }
                    isSaving = false
                }
            },
            supplementaryContent = {
                com.keacs.app.ui.record.RecordSupplementaryRow(
                    accounts = availableAccounts,
                    accountCategories = categories,
                    accountId = if (type == RecordType.INCOME) toAccountId else fromAccountId,
                    showAccount = type != RecordType.TRANSFER,
                    dateText = recurrenceLabel(frequency, recurrenceValues, nextRunAt).ifBlank { "记账时间" },
                    note = note,
                    onAccountClick = { showAccountSelector = true },
                    onDateClick = { showDateSelector = true },
                    onNoteChange = { note = it }
                )
            }
        )
    }

    if (showAccountSelector && type != RecordType.TRANSFER) {
        AccountSelectorBottomSheet(
            accounts = availableAccounts,
            accountCategories = categories,
            selectedId = if (type == RecordType.INCOME) toAccountId else fromAccountId,
            title = "选择账户",
            includeNone = true,
            onSelected = {
                if (type == RecordType.INCOME) {
                    toAccountId = it
                } else {
                    fromAccountId = it
                }
            },
            onDismiss = { showAccountSelector = false },
        )
    }
    if (showDateSelector) {
        RecurrencePickerBottomSheet(
            frequency = frequency,
            recurrenceValues = recurrenceValues,
            nextRunAt = nextRunAt,
            onSelected = { nextFrequency, nextValues, nextTime ->
                frequency = nextFrequency
                recurrenceValues = nextValues
                nextRunAt = nextTime
                showDateSelector = false
            },
            onDismiss = { showDateSelector = false },
        )
    }
    if (showDeleteDialog) {
        ConfirmDialog(
            title = "删除这个定时记账？",
            text = "删除后无法恢复。",
            confirmText = "删除",
            onConfirm = {
                showDeleteDialog = false
                scheduleId?.let { id ->
                    scope.launch {
                        scheduledRepository.deleteSchedule(id)
                        onDone()
                    }
                }
            },
            onDismiss = { showDeleteDialog = false },
            isDestructive = true,
        )
    }
}
private fun defaultNextRunAt(): Long {
    val now = System.currentTimeMillis()
    val calendar = java.util.Calendar.getInstance(java.util.Locale.getDefault()).apply { timeInMillis = now }
    return ScheduledRecordRepository.nextOccurrence(
        frequency = ScheduledFrequency.MONTHLY,
        month = null,
        day = calendar.get(java.util.Calendar.DAY_OF_MONTH),
        weekday = null,
        recurrenceValues = null,
        hour = ScheduledRecordRepository.DEFAULT_RECURRENCE_HOUR,
        afterMillis = now,
    )
}
