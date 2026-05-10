package com.keacs.app.data.importer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.keacs.app.data.local.database.KeacsDatabase
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.domain.model.RecordType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class ExcelRecordImportServiceTest {
    private lateinit var database: KeacsDatabase
    private lateinit var repository: LocalDataRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeacsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LocalDataRepository(database) { dateMillis(2026, 5, 10) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun excelRecordsAreAddedAndUnknownCategoryFallsBackToOther() = runTest {
        repository.initializePresets()
        val service = ExcelRecordImportService(repository) { dateMillis(2026, 5, 10) }
        val workbook = workbookBytes(
            listOf(
                listOf("日期", "收支类型", "分类", "账户", "金额", "备注"),
                listOf("2026-05-01", "支出", "咖啡", "现金", "12.50", "早餐"),
                listOf("2026/05/02", "收入", "工资", "现金", "100", ""),
                listOf("2026/05/03", "转账", "其他", "现金", "1", ""),
            ),
        )

        val result = service.import(ByteArrayInputStream(workbook))

        val records = repository.getRecords()
        val otherExpense = repository.getCategories().first {
            it.name == "其他" && it.direction == "EXPENSE"
        }
        assertEquals(3, result.totalRows)
        assertEquals(2, result.createdRows)
        assertEquals(1, result.skippedRows)
        assertEquals(1, result.fallbackCategoryRows)
        assertTrue(records.any {
            it.type == RecordType.EXPENSE &&
                it.categoryId == otherExpense.id &&
                it.amountCent == 1_250L &&
                it.note == "早餐；原分类：咖啡"
        })
        assertTrue(records.any { it.type == RecordType.INCOME && it.amountCent == 10_000L })
    }

    private fun workbookBytes(rows: List<List<String>>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putText("[Content_Types].xml", "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"/>")
            zip.putText(
                "xl/workbook.xml",
                """
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                    xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                    <sheets><sheet name="Sheet1" sheetId="1" r:id="rId1"/></sheets>
                </workbook>
                """.trimIndent(),
            )
            zip.putText(
                "xl/_rels/workbook.xml.rels",
                """
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Target="worksheets/sheet1.xml"/>
                </Relationships>
                """.trimIndent(),
            )
            zip.putText("xl/worksheets/sheet1.xml", sheetXml(rows))
        }
        return output.toByteArray()
    }

    private fun ZipOutputStream.putText(name: String, text: String) {
        putNextEntry(ZipEntry(name))
        write(text.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun sheetXml(rows: List<List<String>>): String =
        buildString {
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            rows.forEachIndexed { rowIndex, row ->
                append("""<row r="${rowIndex + 1}">""")
                row.forEachIndexed { columnIndex, value ->
                    val ref = "${('A'.code + columnIndex).toChar()}${rowIndex + 1}"
                    append("""<c r="$ref" t="inlineStr"><is><t>${escape(value)}</t></is></c>""")
                }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }

    private fun escape(value: String): String =
        value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun dateMillis(year: Int, month: Int, day: Int): Long =
        java.util.Calendar.getInstance(java.util.Locale.CHINA).apply {
            set(year, month - 1, day, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
}
