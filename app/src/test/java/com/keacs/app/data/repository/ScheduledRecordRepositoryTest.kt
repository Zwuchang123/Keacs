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
        assertEquals(dateMillis(2026, 6, 1), schedule.nextRunAt)
        assertEquals(10_000L, repository.getAccounts().first { it.id == account.id }.initialBalanceCent)
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
        assertEquals(dateMillis(2026, 5, 11), schedule.nextRunAt)
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

    private fun dateMillis(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(Locale.CHINA).apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
