package com.keacs.app.data.agent

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.keacs.app.data.local.database.KeacsDatabase
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.data.repository.ScheduledFrequency
import com.keacs.app.data.repository.ScheduledRecordRepository
import com.keacs.app.domain.model.RecordType
import com.keacs.app.domain.rule.balanceFor
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class AgentActionExecutorTest {
    private lateinit var database: KeacsDatabase
    private lateinit var repository: LocalDataRepository
    private lateinit var scheduledRepository: ScheduledRecordRepository
    private lateinit var executor: AgentActionExecutor

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeacsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LocalDataRepository(database) { dateMillis(2026, 5, 11) }
        scheduledRepository = ScheduledRecordRepository(database, repository) { dateMillis(2026, 5, 11) }
        executor = AgentActionExecutor(repository, scheduledRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createUpdateAndDeleteRecordThroughUseCases() = runTest {
        repository.initializePresets()
        val food = repository.getCategories().first { it.name == "餐饮" }
        val traffic = repository.getCategories().first { it.name == "交通" }
        val cash = repository.getAccounts().first { it.name == "现金" }

        val createResult = executor.execute(
            AgentActionPreview(
                type = "create_record",
                title = "新增账目",
                records = listOf(
                    mapOf(
                        "type" to RecordType.EXPENSE,
                        "amountCent" to 1_800L,
                        "occurredAt" to dateMillis(2026, 5, 10),
                        "categoryId" to food.id,
                        "fromAccountId" to cash.id,
                        "note" to "午饭",
                    ),
                ),
            ),
        )
        assertTrue(createResult is AgentExecutionResult.Success)
        val created = repository.getRecords().single()
        assertEquals(-1_800L, balanceFor(repository.getAccounts().first { it.id == cash.id }, repository.getRecords()))

        val updateResult = executor.execute(
            AgentActionPreview(
                type = "update_record",
                title = "修改账目",
                records = listOf(
                    mapOf(
                        "id" to created.id,
                        "amountCent" to 2_000L,
                        "categoryId" to traffic.id,
                    ),
                ),
            ),
        )
        assertTrue(updateResult is AgentExecutionResult.Success)
        val updated = repository.getRecords().single()
        assertEquals(2_000L, updated.amountCent)
        assertEquals(traffic.id, updated.categoryId)

        val deleteResult = executor.execute(
            AgentActionPreview(
                type = "delete_record",
                title = "删除账目",
                records = listOf(mapOf("id" to created.id)),
            ),
        )
        assertTrue(deleteResult is AgentExecutionResult.Success)
        assertTrue(repository.getRecords().isEmpty())
        assertEquals(0L, balanceFor(repository.getAccounts().first { it.id == cash.id }, repository.getRecords()))
    }

    @Test
    fun batchUpdateAndScheduledRecordMainPathWorks() = runTest {
        repository.initializePresets()
        val food = repository.getCategories().first { it.name == "餐饮" }
        val traffic = repository.getCategories().first { it.name == "交通" }
        val cash = repository.getAccounts().first { it.name == "现金" }

        repository.saveRecord(null, RecordType.EXPENSE, 1_000L, dateMillis(2026, 5, 8), food.id, cash.id, null, "")
        repository.saveRecord(null, RecordType.EXPENSE, 2_000L, dateMillis(2026, 5, 9), food.id, cash.id, null, "")
        val records = repository.getRecords()

        val batchResult = executor.execute(
            AgentActionPreview(
                type = "batch_update_records",
                title = "批量修改分类",
                records = records.map { mapOf("id" to it.id, "categoryId" to traffic.id) },
            ),
        )
        assertTrue(batchResult is AgentExecutionResult.Success)
        assertTrue(repository.getRecords().all { it.categoryId == traffic.id })

        val createScheduleResult = executor.execute(
            AgentActionPreview(
                type = "create_scheduled_record",
                title = "新增定时记账",
                scheduledRecords = listOf(
                    mapOf(
                        "type" to RecordType.EXPENSE,
                        "amountCent" to 5_000L,
                        "categoryId" to food.id,
                        "fromAccountId" to cash.id,
                        "frequency" to ScheduledFrequency.MONTHLY,
                        "nextRunAt" to dateMillis(2026, 6, 1),
                        "note" to "房租",
                        "isEnabled" to true,
                    ),
                ),
            ),
        )
        assertTrue(createScheduleResult is AgentExecutionResult.Success)
        val schedule = scheduledRepository.getSchedules().single()

        val disableResult = executor.execute(
            AgentActionPreview(
                type = "disable_scheduled_record",
                title = "停用定时记账",
                scheduledRecords = listOf(mapOf("id" to schedule.id)),
            ),
        )
        assertTrue(disableResult is AgentExecutionResult.Success)
        assertEquals(false, scheduledRepository.getSchedules().single().isEnabled)
    }

    private fun dateMillis(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(Locale.getDefault()).apply {
            set(year, month - 1, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
