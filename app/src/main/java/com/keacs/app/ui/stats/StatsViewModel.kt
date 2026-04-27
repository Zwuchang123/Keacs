package com.keacs.app.ui.stats

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

enum class TimePeriod { DAY, MONTH, YEAR }
enum class StatsTab { EXPENSE, INCOME, ASSET }

data class CategoryStats(
    val categoryId: Long,
    val categoryName: String,
    val iconKey: String,
    val colorKey: String,
    val amount: Long,
    val percentage: Float,
)

data class DailyStats(
    val day: Int,
    val amount: Long,
)

data class AccountBalanceStats(
    val accountId: Long,
    val accountName: String,
    val iconKey: String,
    val colorKey: String,
    val balance: Long,
    val isAsset: Boolean,
)

data class StatsUiState(
    val selectedTab: StatsTab = StatsTab.EXPENSE,
    val selectedPeriod: TimePeriod = TimePeriod.MONTH,
    val selectedDate: Long = System.currentTimeMillis(),
    val income: Long = 0L,
    val expense: Long = 0L,
    val netBalance: Long = 0L,
    val totalAsset: Long = 0L,
    val totalLiability: Long = 0L,
    val netAsset: Long = 0L,
    val categoryStats: List<CategoryStats> = emptyList(),
    val dailyTrend: List<DailyStats> = emptyList(),
    val netAssetTrend: List<DailyStats> = emptyList(),
    val accountBalances: List<AccountBalanceStats> = emptyList(),
    val categories: Map<Long, CategoryEntity> = emptyMap(),
    val accounts: Map<Long, AccountEntity> = emptyMap(),
    val isLoading: Boolean = true,
)

class StatsViewModel(
    private val repository: LocalDataRepository,
) : ViewModel() {

    private val selectedTab = MutableStateFlow(StatsTab.EXPENSE)
    private val selectedPeriod = MutableStateFlow(TimePeriod.MONTH)
    private val selectedDate = MutableStateFlow(System.currentTimeMillis())

    private val dataFlow = combine(
        repository.observeRecords(),
        repository.observeCategories(),
        repository.observeAccounts(),
    ) { records, categories, accounts ->
        Triple(records, categories, accounts)
    }

    private val selectionFlow = combine(
        selectedTab,
        selectedPeriod,
        selectedDate,
    ) { tab, period, date ->
        Triple(tab, period, date)
    }

    val uiState: StateFlow<StatsUiState> = combine(
        dataFlow,
        selectionFlow,
    ) { data, selection ->
        val (records, categories, accounts) = data
        val (tab, period, date) = selection

        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }

        val (periodStart, periodEnd) = getPeriodRange(period, date)
        val periodRecords = records.filter { it.occurredAt in periodStart until periodEnd }

        val income = totalIncome(periodRecords)
        val expense = totalExpense(periodRecords)

        val assetAccounts = accounts.filter { it.nature == PresetSeedData.ACCOUNT_ASSET && it.isEnabled }
        val liabilityAccounts = accounts.filter { it.nature == PresetSeedData.ACCOUNT_LIABILITY && it.isEnabled }
        val totalAsset = assetAccounts.sumOf { balance -> balanceFor(balance, records) }
        val totalLiability = liabilityAccounts.sumOf { balance -> balanceFor(balance, records) }

        val categoryStatsResult = buildCategoryStats(periodRecords, categoryMap, tab)
        val dailyTrend = buildDailyTrend(periodRecords, period, periodStart)
        val netAssetTrend = buildNetAssetTrend(accounts, records, date)
        val accountBalances = buildAccountBalances(accounts, records)

        StatsUiState(
            selectedTab = tab,
            selectedPeriod = period,
            selectedDate = date,
            income = income,
            expense = expense,
            netBalance = income - expense,
            totalAsset = totalAsset,
            totalLiability = totalLiability,
            netAsset = totalAsset - totalLiability,
            categoryStats = categoryStatsResult,
            dailyTrend = dailyTrend,
            netAssetTrend = netAssetTrend,
            accountBalances = accountBalances,
            categories = categoryMap,
            accounts = accountMap,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState(),
    )

    fun selectTab(tab: StatsTab) {
        selectedTab.value = tab
    }

    fun selectPeriod(period: TimePeriod) {
        selectedPeriod.value = period
    }

    fun selectDate(date: Long) {
        selectedDate.value = date
    }

    private fun getPeriodRange(period: TimePeriod, date: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance(Locale.getDefault())
        calendar.timeInMillis = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return when (period) {
            TimePeriod.DAY -> {
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val end = calendar.timeInMillis
                start to end
            }
            TimePeriod.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                val end = calendar.timeInMillis
                start to end
            }
            TimePeriod.YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                val start = calendar.timeInMillis
                calendar.add(Calendar.YEAR, 1)
                val end = calendar.timeInMillis
                start to end
            }
        }
    }

    private fun buildCategoryStats(
        records: List<RecordEntity>,
        categories: Map<Long, CategoryEntity>,
        tab: StatsTab,
    ): List<CategoryStats> {
        val targetType = when (tab) {
            StatsTab.EXPENSE -> RecordType.EXPENSE
            StatsTab.INCOME -> RecordType.INCOME
            StatsTab.ASSET -> return emptyList()
        }

        val filteredRecords = records.filter { it.type == targetType && it.categoryId != null }
        val totalAmount = filteredRecords.sumOf { it.amountCent }.coerceAtLeast(1L)

        return filteredRecords
            .groupBy { it.categoryId }
            .mapNotNull { (categoryId, categoryRecords) ->
                val safeCategoryId = categoryId ?: return@mapNotNull null
                val category = categories[safeCategoryId] ?: return@mapNotNull null
                val amount = categoryRecords.sumOf { it.amountCent }
                CategoryStats(
                    categoryId = safeCategoryId,
                    categoryName = category.name,
                    iconKey = category.iconKey,
                    colorKey = category.colorKey,
                    amount = amount,
                    percentage = amount.toFloat() / totalAmount.toFloat() * 100f,
                )
            }
            .sortedByDescending { it.amount }
            .take(5)
    }

    private fun buildDailyTrend(
        records: List<RecordEntity>,
        period: TimePeriod,
        periodStart: Long,
    ): List<DailyStats> {
        if (period == TimePeriod.YEAR) return emptyList()

        val calendar = Calendar.getInstance(Locale.getDefault())
        calendar.timeInMillis = periodStart

        val dayCount = when (period) {
            TimePeriod.DAY -> 1
            TimePeriod.MONTH -> calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            TimePeriod.YEAR -> 0
        }

        return (1..dayCount).map { day ->
            val dayCalendar = Calendar.getInstance(Locale.getDefault())
            dayCalendar.timeInMillis = periodStart
            dayCalendar.set(Calendar.DAY_OF_MONTH, day)
            dayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            dayCalendar.set(Calendar.MINUTE, 0)
            dayCalendar.set(Calendar.SECOND, 0)
            val dayStart = dayCalendar.timeInMillis
            dayCalendar.add(Calendar.DAY_OF_MONTH, 1)
            val dayEnd = dayCalendar.timeInMillis

            val dayRecords = records.filter { it.occurredAt in dayStart until dayEnd }
            val income = dayRecords.filter { it.type == RecordType.INCOME }.sumOf { it.amountCent }
            val expense = dayRecords.filter { it.type == RecordType.EXPENSE }.sumOf { it.amountCent }
            DailyStats(day = day, amount = income - expense)
        }
    }

    private fun buildAccountBalances(
        accounts: List<AccountEntity>,
        records: List<RecordEntity>,
    ): List<AccountBalanceStats> {
        return accounts.filter { it.isEnabled }.map { account ->
            AccountBalanceStats(
                accountId = account.id,
                accountName = account.name,
                iconKey = account.iconKey,
                colorKey = account.colorKey,
                balance = balanceFor(account, records),
                isAsset = account.nature == PresetSeedData.ACCOUNT_ASSET,
            )
        }.sortedByDescending { it.balance }
    }

    private fun buildNetAssetTrend(
        accounts: List<AccountEntity>,
        records: List<RecordEntity>,
        selectedDate: Long,
    ): List<DailyStats> {
        val base = Calendar.getInstance(Locale.getDefault()).apply {
            timeInMillis = selectedDate
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -5)
        }
        return (0 until 6).map {
            val monthStart = base.clone() as Calendar
            monthStart.add(Calendar.MONTH, it)
            val monthEnd = (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, 1) }.timeInMillis
            val recordsBeforeMonthEnd = records.filter { record -> record.occurredAt < monthEnd }
            val asset = accounts
                .filter { account -> account.nature == PresetSeedData.ACCOUNT_ASSET && account.isEnabled }
                .sumOf { account -> balanceFor(account, recordsBeforeMonthEnd) }
            val liability = accounts
                .filter { account -> account.nature == PresetSeedData.ACCOUNT_LIABILITY && account.isEnabled }
                .sumOf { account -> balanceFor(account, recordsBeforeMonthEnd) }
            DailyStats(day = monthStart.get(Calendar.MONTH) + 1, amount = asset - liability)
        }
    }

    companion object {
        private val currencyFormat = DecimalFormat("#,##0.00")

        fun formatCent(value: Long): String =
            "¥" + currencyFormat.format(value / 100.0)

        fun formatPercentage(value: Float): String =
            DecimalFormat("#0.0").format(value) + "%"
    }
}
