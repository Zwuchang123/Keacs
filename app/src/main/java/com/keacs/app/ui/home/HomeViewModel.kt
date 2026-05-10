package com.keacs.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.local.entity.RecordEntity
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.domain.model.RecordType
import com.keacs.app.domain.rule.balanceFor
import com.keacs.app.domain.rule.totalExpense
import com.keacs.app.domain.rule.totalIncome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.DecimalFormat
import java.util.Calendar
import java.util.Locale

data class HomeDailyBillGroup(
    val dayStart: Long,
    val monthDay: String,
    val weekDay: String,
    val expense: Long,
    val records: List<RecordEntity>,
)

data class HomeUiState(
    val selectedMonth: Long = System.currentTimeMillis(),
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val totalAsset: Long = 0L,
    val totalLiability: Long = 0L,
    val dailyGroups: List<HomeDailyBillGroup> = emptyList(),
    val categories: Map<Long, CategoryEntity> = emptyMap(),
    val accounts: Map<Long, AccountEntity> = emptyMap(),
    val isLoading: Boolean = true,
)

class HomeViewModel(
    private val repository: LocalDataRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(startOfMonth(clock()))

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeRecords(),
        repository.observeCategories(),
        repository.observeAccounts(),
        selectedMonth,
    ) { records, categories, accounts, month ->
        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }

        val monthStart = startOfMonth(month.coerceAtMost(clock()))
        val monthEnd = endOfMonth(monthStart)
        val monthRecords = records
            .filter { it.occurredAt in monthStart until monthEnd }
            .sortedByDescending { it.occurredAt }

        val assetAccounts = accounts.filter { it.nature == PresetSeedData.ACCOUNT_ASSET && it.isEnabled }
        val liabilityAccounts = accounts.filter { it.nature == PresetSeedData.ACCOUNT_LIABILITY && it.isEnabled }

        val totalAsset = assetAccounts.sumOf { balanceFor(it, records) }
        val totalLiability = liabilityAccounts.sumOf { balanceFor(it, records) }

        HomeUiState(
            selectedMonth = monthStart,
            totalIncome = totalIncome(monthRecords),
            totalExpense = totalExpense(monthRecords),
            totalAsset = totalAsset,
            totalLiability = totalLiability,
            dailyGroups = buildDailyGroups(monthRecords),
            categories = categoryMap,
            accounts = accountMap,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(),
    )

    fun selectMonth(month: Long) {
        selectedMonth.value = startOfMonth(month.coerceAtMost(clock()))
    }

    companion object {
        private val currencyFormat = DecimalFormat("#0.##")

        fun formatCent(value: Long): String =
            currencyFormat.format(value / 100.0)
    }
}

private fun buildDailyGroups(records: List<RecordEntity>): List<HomeDailyBillGroup> =
    records
        .groupBy { startOfDay(it.occurredAt) }
        .toSortedMap(compareByDescending { it })
        .map { (dayStart, dayRecords) ->
            val sortedRecords = dayRecords.sortedByDescending { it.occurredAt }
            HomeDailyBillGroup(
                dayStart = dayStart,
                monthDay = formatMonthDay(dayStart),
                weekDay = formatWeekDay(dayStart),
                expense = totalExpense(sortedRecords),
                records = sortedRecords,
            )
        }

private fun startOfMonth(timestamp: Long): Long {
    val calendar = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = timestamp }
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun endOfMonth(monthStart: Long): Long {
    val calendar = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = monthStart }
    calendar.add(Calendar.MONTH, 1)
    return calendar.timeInMillis
}

private fun startOfDay(timestamp: Long): Long {
    val calendar = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = timestamp }
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun formatMonthDay(timestamp: Long): String {
    val calendar = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = timestamp }
    return "${calendar.get(Calendar.MONTH) + 1}月${calendar.get(Calendar.DAY_OF_MONTH)}日"
}

private fun formatWeekDay(timestamp: Long): String {
    val calendar = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = timestamp }
    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "周一"
        Calendar.TUESDAY -> "周二"
        Calendar.WEDNESDAY -> "周三"
        Calendar.THURSDAY -> "周四"
        Calendar.FRIDAY -> "周五"
        Calendar.SATURDAY -> "周六"
        else -> "周日"
    }
}
