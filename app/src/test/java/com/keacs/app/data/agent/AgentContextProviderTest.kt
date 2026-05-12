package com.keacs.app.data.agent

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.keacs.app.data.local.database.KeacsDatabase
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.data.repository.ScheduledRecordRepository
import com.keacs.app.domain.model.RecordType
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
class AgentContextProviderTest {
    private lateinit var database: KeacsDatabase
    private lateinit var repository: LocalDataRepository
    private lateinit var scheduledRepository: ScheduledRecordRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeacsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LocalDataRepository(database) { dateMillis(2026, 5, 11) }
        scheduledRepository = ScheduledRecordRepository(database, repository) { dateMillis(2026, 5, 11) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun monthQuestionBuildsStatsWithoutWritingRecords() = runTest {
        repository.initializePresets()
        val salary = repository.getCategories().first { it.name == "工资" }
        val food = repository.getCategories().first { it.name == "餐饮" }
        val cash = repository.getAccounts().first { it.name == "现金" }

        repository.saveRecord(null, RecordType.INCOME, 50_000, dateMillis(2026, 5, 1), salary.id, null, cash.id, "")
        repository.saveRecord(null, RecordType.EXPENSE, 1_800, dateMillis(2026, 5, 10), food.id, cash.id, null, "午饭")
        repository.saveRecord(null, RecordType.EXPENSE, 900, dateMillis(2026, 4, 20), food.id, cash.id, null, "四月")
        val beforeCount = repository.getRecords().size

        val context = AgentContextProvider(
            repository = repository,
            scheduledRepository = scheduledRepository,
            clock = { dateMillis(2026, 5, 11) },
        ).buildForMessage("这个月餐饮花了多少")

        assertEquals("本月", context.stats["rangeLabel"])
        assertEquals(50_000L, context.stats["incomeCent"])
        assertEquals(1_800L, context.stats["expenseCent"])
        assertEquals(48_200L, context.stats["balanceCent"])
        assertTrue(context.records.isEmpty())
        assertEquals(beforeCount, repository.getRecords().size)
    }

    @Test
    fun deleteQuestionOnlyIncludesYesterdayCandidates() = runTest {
        repository.initializePresets()
        val food = repository.getCategories().first { it.name == "餐饮" }
        val cash = repository.getAccounts().first { it.name == "现金" }

        repository.saveRecord(null, RecordType.EXPENSE, 1_800, dateMillis(2026, 5, 10), food.id, cash.id, null, "午饭")
        repository.saveRecord(null, RecordType.EXPENSE, 2_000, dateMillis(2026, 5, 11), food.id, cash.id, null, "今天")

        val context = AgentContextProvider(
            repository = repository,
            scheduledRepository = scheduledRepository,
            clock = { dateMillis(2026, 5, 11) },
        ).buildForMessage("删除昨天午饭")

        assertEquals(1, context.records.size)
        assertEquals("午饭", context.records.single()["note"])
        assertEquals(2, repository.getRecords().size)
    }

    @Test
    fun yearAnalysisIncludesCurrentYearRecords() = runTest {
        repository.initializePresets()
        val food = repository.getCategories().first { it.name == "餐饮" }
        val cash = repository.getAccounts().first { it.name == "现金" }

        repository.saveRecord(null, RecordType.EXPENSE, 1_800, dateMillis(2026, 5, 10), food.id, cash.id, null, "今年午饭")
        repository.saveRecord(null, RecordType.EXPENSE, 900, dateMillis(2025, 12, 20), food.id, cash.id, null, "去年午饭")

        val context = AgentContextProvider(
            repository = repository,
            scheduledRepository = scheduledRepository,
            clock = { dateMillis(2026, 5, 11) },
        ).buildForMessage("今年帮我分析消费习惯")

        assertEquals("今年", context.stats["rangeLabel"])
        assertEquals(1, context.records.size)
        assertEquals("今年午饭", context.records.single()["note"])
    }

    @Test
    fun fullHistoryQuestionIncludesAllRecords() = runTest {
        repository.initializePresets()
        val food = repository.getCategories().first { it.name == "餐饮" }
        val cash = repository.getAccounts().first { it.name == "现金" }

        repository.saveRecord(null, RecordType.EXPENSE, 1_800, dateMillis(2026, 5, 10), food.id, cash.id, null, "今年午饭")
        repository.saveRecord(null, RecordType.EXPENSE, 900, dateMillis(2025, 12, 20), food.id, cash.id, null, "去年午饭")

        val context = AgentContextProvider(
            repository = repository,
            scheduledRepository = scheduledRepository,
            clock = { dateMillis(2026, 5, 11) },
        ).buildForMessage("全部账单帮我分析消费习惯")

        assertEquals("全部账单", context.stats["rangeLabel"])
        assertEquals(2, context.records.size)
        assertTrue(context.records.map { it["note"] }.contains("今年午饭"))
        assertTrue(context.records.map { it["note"] }.contains("去年午饭"))
    }

    private fun dateMillis(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(Locale.getDefault()).apply {
            set(year, month - 1, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
