package com.keacs.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.keacs.app.data.local.database.KeacsDatabase
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.domain.model.RecordType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class ScheduledRecordRepositoryTest {
    private lateinit var database: KeacsDatabase
    private lateinit var repository: LocalDataRepository
    private lateinit var scheduledRepository: ScheduledRecordRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeacsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LocalDataRepository(database) { dateMillis(2026, 5, 10) }
        scheduledRepository = ScheduledRecordRepository(database, repository) { dateMillis(2026, 5, 10) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun dueScheduleCreatesRecordAndMovesNextDate() = runTest {
        repository.initializePresets()
        val category = repository.getCategories().first { it.name == "工资" }
        val account = repository.getAccounts().first { it.name == "现金" }

        scheduledRepository.saveSchedule(
            id = null,
            type = RecordType.INCOME,
            amountCent = 10_000,
            categoryId = category.id,
            fromAccountId = null,
            toAccountId = account.id,
            frequency = ScheduledFrequency.MONTHLY,
            nextRunAt = dateMillis(2026, 5, 1),
            note = "固定收入",
            isEnabled = true,
        )

        val createdCount = scheduledRepository.createDueRecords()

        val record = repository.getRecords().single()
        val schedule = scheduledRepository.getSchedules().single()
        assertEquals(1, createdCount)
        assertEquals(RecordType.INCOME, record.type)
        assertEquals(10_000L, record.amountCent)
        assertEquals("固定收入", record.note)
        assertEquals(dateMillis(2026, 6, 1, 9), schedule.nextRunAt)
        assertEquals(10_000L, repository.getAccounts().first { it.id == account.id }.initialBalanceCent)
    }

    @Test
    fun dueExpenseScheduleCreatesRecordAndMovesBalance() = runTest {
        repository.initializePresets()
        val category = repository.getCategories().first { it.name == "餐饮" }
        val account = repository.getAccounts().first { it.name == "现金" }

        scheduledRepository.saveSchedule(
            id = null,
            type = RecordType.EXPENSE,
            amountCent = 2_500,
            categoryId = category.id,
            fromAccountId = account.id,
            toAccountId = null,
            frequency = ScheduledFrequency.MONTHLY,
            nextRunAt = dateMillis(2026, 5, 1),
            note = "固定支出",
            isEnabled = true,
        )

        val createdCount = scheduledRepository.createDueRecords()

        val record = repository.getRecords().single()
        assertEquals(1, createdCount)
        assertEquals(RecordType.EXPENSE, record.type)
        assertEquals(account.id, record.fromAccountId)
        assertEquals(-2_500L, repository.getAccounts().first { it.id == account.id }.initialBalanceCent)
    }

    @Test
    fun incomeAndExpenseSchedulesRequireAccounts() = runTest {
        repository.initializePresets()
        val incomeCategory = repository.getCategories().first { it.name == "工资" }
        val expenseCategory = repository.getCategories().first { it.name == "餐饮" }

        assertFailsWithMessage("请选择转入账户") {
            scheduledRepository.saveSchedule(
                id = null,
                type = RecordType.INCOME,
                amountCent = 10_000,
                categoryId = incomeCategory.id,
                fromAccountId = null,
                toAccountId = null,
                frequency = ScheduledFrequency.MONTHLY,
                nextRunAt = dateMillis(2026, 5, 1),
                note = "",
                isEnabled = true,
            )
        }

        assertFailsWithMessage("请选择转出账户") {
            scheduledRepository.saveSchedule(
                id = null,
                type = RecordType.EXPENSE,
                amountCent = 2_500,
                categoryId = expenseCategory.id,
                fromAccountId = null,
                toAccountId = null,
                frequency = ScheduledFrequency.MONTHLY,
                nextRunAt = dateMillis(2026, 5, 1),
                note = "",
                isEnabled = true,
            )
        }
    }

    @Test
    fun dueTransferScheduleCreatesTransferRecordAndMovesBalances() = runTest {
        repository.initializePresets()
        val cash = repository.getAccounts().first { it.name == "现金" }
        val bank = repository.getAccounts().first { it.name == "银行卡" }

        scheduledRepository.saveSchedule(
            id = null,
            type = RecordType.TRANSFER,
            amountCent = 3_000,
            categoryId = null,
            fromAccountId = cash.id,
            toAccountId = bank.id,
            frequency = ScheduledFrequency.WEEKLY,
            nextRunAt = dateMillis(2026, 5, 4),
            note = "固定转账",
            isEnabled = true,
        )

        val createdCount = scheduledRepository.createDueRecords()

        val record = repository.getRecords().single()
        val schedule = scheduledRepository.getSchedules().single()
        assertEquals(1, createdCount)
        assertEquals(RecordType.TRANSFER, record.type)
        assertEquals(cash.id, record.fromAccountId)
        assertEquals(bank.id, record.toAccountId)
        assertEquals(-3_000L, repository.getAccounts().first { it.id == cash.id }.initialBalanceCent)
        assertEquals(3_000L, repository.getAccounts().first { it.id == bank.id }.initialBalanceCent)
        assertEquals(dateMillis(2026, 5, 11, 9), schedule.nextRunAt)
    }

    @Test
    fun weeklyScheduleSupportsMultipleWeekdays() = runTest {
        repository.initializePresets()
        val category = repository.getCategories().first { it.name == "餐饮" }
        val account = repository.getAccounts().first { it.name == "现金" }

        scheduledRepository.saveSchedule(
            id = null,
            type = RecordType.EXPENSE,
            amountCent = 2_000,
            categoryId = category.id,
            fromAccountId = account.id,
            toAccountId = null,
            frequency = ScheduledFrequency.WEEKLY,
            recurrenceValues = "${Calendar.MONDAY},${Calendar.TUESDAY}",
            nextRunAt = dateMillis(2026, 5, 4),
            note = "",
            isEnabled = true,
        )

        val createdCount = scheduledRepository.createDueRecords()

        val records = repository.getRecords()
        val schedule = scheduledRepository.getSchedules().single()
        assertEquals(2, createdCount)
        assertEquals(2, records.size)
        assertEquals(dateMillis(2026, 5, 11, 9), schedule.nextRunAt)
    }

    @Test
    fun nextOccurrenceUsesMonthlyAndYearlySelections() {
        val monthly = ScheduledRecordRepository.nextOccurrence(
            frequency = ScheduledFrequency.MONTHLY,
            month = null,
            day = null,
            weekday = null,
            recurrenceValues = "15,20",
            hour = ScheduledRecordRepository.DEFAULT_RECURRENCE_HOUR,
            afterMillis = dateMillis(2026, 5, 10),
        )
        val yearly = ScheduledRecordRepository.nextOccurrence(
            frequency = ScheduledFrequency.YEARLY,
            month = null,
            day = null,
            weekday = null,
            recurrenceValues = "6-1,12-1",
            hour = ScheduledRecordRepository.DEFAULT_RECURRENCE_HOUR,
            afterMillis = dateMillis(2026, 5, 10),
        )

        assertEquals(dateMillis(2026, 5, 15, 9), monthly)
        assertEquals(dateMillis(2026, 6, 1, 9), yearly)
    }

    @Test
    fun categoryOrderCanBeChanged() = runTest {
        repository.initializePresets()
        val expenseIds = repository.getCategories()
            .filter { it.direction == PresetSeedData.CATEGORY_EXPENSE }
            .map { it.id }

        repository.reorderCategories(
            PresetSeedData.CATEGORY_EXPENSE,
            listOf(expenseIds[1], expenseIds[0]) + expenseIds.drop(2),
        )

        val updatedIds = repository.getCategories()
            .filter { it.direction == PresetSeedData.CATEGORY_EXPENSE }
            .map { it.id }
        assertEquals(expenseIds[1], updatedIds[0])
        assertEquals(expenseIds[0], updatedIds[1])
    }

    private fun dateMillis(year: Int, month: Int, day: Int, hour: Int = 0): Long =
        Calendar.getInstance(Locale.CHINA).apply {
            set(year, month - 1, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private inline fun assertFailsWithMessage(message: String, block: () -> Unit) {
        val error = runCatching(block).exceptionOrNull()
        if (error == null) {
            fail("应该提示：$message")
            return
        }
        assertEquals(message, error.message)
    }
}
