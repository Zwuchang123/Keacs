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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.DecimalFormat
import java.util.Calendar
import java.util.Locale

data class HomeUiState(
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val totalAsset: Long = 0L,
    val totalLiability: Long = 0L,
    val recentRecords: List<RecordEntity> = emptyList(),
    val categories: Map<Long, CategoryEntity> = emptyMap(),
    val accounts: Map<Long, AccountEntity> = emptyMap(),
    val isLoading: Boolean = true,
)

class HomeViewModel(
    private val repository: LocalDataRepository,
) : ViewModel() {

    private val currentMonthRange: Pair<Long, Long>
        get() {
            val calendar = Calendar.getInstance(Locale.getDefault())
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            val endOfMonth = calendar.timeInMillis

            return startOfMonth to endOfMonth
        }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeRecords(),
        repository.observeCategories(),
        repository.observeAccounts(),
    ) { records, categories, accounts ->
        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }

        val (startOfMonth, endOfMonth) = currentMonthRange
        val monthRecords = records.filter { it.occurredAt in startOfMonth until endOfMonth }

        val assetAccounts = accounts.filter { it.nature == PresetSeedData.ACCOUNT_ASSET && it.isEnabled }
        val liabilityAccounts = accounts.filter { it.nature == PresetSeedData.ACCOUNT_LIABILITY && it.isEnabled }

        val totalAsset = assetAccounts.sumOf { balanceFor(it, records) }
        val totalLiability = liabilityAccounts.sumOf { balanceFor(it, records) }

        val recentRecords = records.sortedByDescending { it.occurredAt }.take(8)

        HomeUiState(
            totalIncome = totalIncome(monthRecords),
            totalExpense = totalExpense(monthRecords),
            totalAsset = totalAsset,
            totalLiability = totalLiability,
            recentRecords = recentRecords,
            categories = categoryMap,
            accounts = accountMap,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(),
    )

    companion object {
        private val currencyFormat = DecimalFormat("#,##0.00")

        fun formatCent(value: Long): String =
            currencyFormat.format(value / 100.0)
    }
}
