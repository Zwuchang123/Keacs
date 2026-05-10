package com.keacs.app.data.importer

import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.data.repository.ScheduledRecordRepository
import com.keacs.app.domain.model.RecordType
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ExcelImportResult(
    val totalRows: Int,
    val createdRows: Int,
    val skippedRows: Int,
    val fallbackCategoryRows: Int,
)

class ExcelRecordImportService(
    private val repository: LocalDataRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun import(inputStream: InputStream): ExcelImportResult {
        val rows = XlsxReader.readRows(inputStream)
        if (rows.isEmpty()) return ExcelImportResult(0, 0, 0, 0)

        val headerIndex = rows.indexOfFirst { row -> row.values.any { it.isNotBlank() } }
        require(headerIndex >= 0) { "表格没有可读取的内容" }
        val headers = rows[headerIndex].mapValues { normalizeHeader(it.value) }
        val columns = resolveColumns(headers)
        val dataRows = rows.drop(headerIndex + 1)

        val categories = repository.getCategories()
        val accounts = repository.getAccounts()
        var created = 0
        var skipped = 0
        var fallbackCategoryRows = 0

        dataRows.forEach { row ->
            val parsed = parseRow(row, columns)
            if (parsed == null) {
                skipped += 1
                return@forEach
            }
            val categoryMatch = matchCategory(categories, parsed.type, parsed.categoryName)
            val category = categoryMatch.category
            if (categoryMatch.usedFallback) fallbackCategoryRows += 1
            if (category == null) {
                skipped += 1
                return@forEach
            }
            val account = matchAccount(accounts, parsed.accountName)
            val note = buildNote(parsed.note, parsed.categoryName, categoryMatch.usedFallback)
            runCatching {
                if (parsed.type == RecordType.INCOME) {
                    repository.saveRecord(null, RecordType.INCOME, parsed.amountCent, parsed.date, category.id, null, account?.id, note)
                } else {
                    repository.saveRecord(null, RecordType.EXPENSE, parsed.amountCent, parsed.date, category.id, account?.id, null, note)
                }
            }.onSuccess {
                created += 1
            }.onFailure {
                skipped += 1
            }
        }

        return ExcelImportResult(
            totalRows = dataRows.count { it.values.any(String::isNotBlank) },
            createdRows = created,
            skippedRows = skipped,
            fallbackCategoryRows = fallbackCategoryRows,
        )
    }

    private fun parseRow(row: Map<Int, String>, columns: ImportColumns): ParsedExcelRecord? {
        val type = parseType(row[columns.type].orEmpty()) ?: return null
        val amount = parseAmount(row[columns.amount].orEmpty()) ?: return null
        val date = parseDate(row[columns.date].orEmpty()) ?: return null
        if (date > ScheduledRecordRepository.startOfDay(clock())) return null
        return ParsedExcelRecord(
            date = date,
            type = type,
            categoryName = row[columns.category].orEmpty().trim(),
            accountName = row[columns.account].orEmpty().trim(),
            amountCent = amount,
            note = row[columns.note].orEmpty().trim(),
        )
    }

    private fun parseType(text: String): String? =
        when (normalizeText(text).lowercase(Locale.ROOT)) {
            "收入", "入账", "收", "in", "income", "+" -> RecordType.INCOME
            "支出", "支", "出账", "out", "expense", "-" -> RecordType.EXPENSE
            else -> null
        }

    private fun parseAmount(text: String): Long? {
        val normalized = text
            .replace("￥", "")
            .replace("¥", "")
            .replace(",", "")
            .trim()
        val value = normalized.toBigDecimalOrNull()?.abs() ?: return null
        if (value <= BigDecimal.ZERO) return null
        return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()
    }

    private fun parseDate(text: String): Long? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null
        trimmed.toDoubleOrNull()?.let { serial ->
            return excelSerialToMillis(serial)
        }
        dateFormats.forEach { pattern ->
            val parser = SimpleDateFormat(pattern, Locale.CHINA).apply { isLenient = false }
            runCatching { parser.parse(trimmed)?.time }.getOrNull()?.let {
                return ScheduledRecordRepository.startOfDay(it)
            }
        }
        val currentYearText = "${Calendar.getInstance(Locale.getDefault()).get(Calendar.YEAR)}年$trimmed"
        return runCatching {
            SimpleDateFormat("yyyy年M月d日", Locale.CHINA).apply { isLenient = false }
                .parse(currentYearText)
                ?.time
        }.getOrNull()?.let { ScheduledRecordRepository.startOfDay(it) }
    }

    private fun excelSerialToMillis(serial: Double): Long {
        val millis = ((serial - EXCEL_EPOCH_DAYS) * ONE_DAY_MILLIS).toLong()
        return ScheduledRecordRepository.startOfDay(millis)
    }

    private fun matchCategory(categories: List<CategoryEntity>, type: String, name: String): CategoryMatch {
        val direction = if (type == RecordType.INCOME) PresetSeedData.CATEGORY_INCOME else PresetSeedData.CATEGORY_EXPENSE
        val available = categories.filter { it.direction == direction && it.isEnabled }
        val normalizedName = normalizeText(name)
        val exact = available.firstOrNull { normalizeText(it.name) == normalizedName }
        if (exact != null) return CategoryMatch(exact, false)
        return CategoryMatch(available.firstOrNull { it.name == "其他" } ?: available.firstOrNull(), normalizedName.isNotBlank())
    }

    private fun matchAccount(accounts: List<AccountEntity>, name: String): AccountEntity? {
        val normalizedName = normalizeText(name)
        if (normalizedName.isBlank()) return null
        return accounts.firstOrNull { it.isEnabled && normalizeText(it.name) == normalizedName }
    }

    private fun buildNote(note: String, originalCategory: String, usedFallback: Boolean): String {
        val categoryNote = if (usedFallback && originalCategory.isNotBlank()) "原分类：$originalCategory" else ""
        return listOf(note, categoryNote).filter { it.isNotBlank() }.joinToString("；")
    }

    private fun resolveColumns(headers: Map<Int, String>): ImportColumns {
        fun find(vararg names: String): Int? {
            val keys = names.map(::normalizeHeader).toSet()
            return headers.entries.firstOrNull { it.value in keys }?.key
        }
        return ImportColumns(
            date = find("日期", "时间", "账目日期") ?: error("缺少“日期”列"),
            type = find("收支类型", "类型", "收支") ?: error("缺少“收支类型”列"),
            category = find("分类", "账目分类") ?: error("缺少“分类”列"),
            account = find("账户", "账号", "付款账户", "收款账户") ?: error("缺少“账户”列"),
            amount = find("金额", "钱数") ?: error("缺少“金额”列"),
            note = find("备注", "说明") ?: -1,
        )
    }

    private data class ParsedExcelRecord(
        val date: Long,
        val type: String,
        val categoryName: String,
        val accountName: String,
        val amountCent: Long,
        val note: String,
    )

    private data class CategoryMatch(
        val category: CategoryEntity?,
        val usedFallback: Boolean,
    )

    private data class ImportColumns(
        val date: Int,
        val type: Int,
        val category: Int,
        val account: Int,
        val amount: Int,
        val note: Int,
    )

    private companion object {
        const val EXCEL_EPOCH_DAYS = 25569.0
        const val ONE_DAY_MILLIS = 86_400_000.0
        val dateFormats = listOf(
            "yyyy-MM-dd",
            "yyyy/M/d",
            "yyyy.MM.dd",
            "yyyy年M月d日",
        )
    }
}

private fun normalizeHeader(text: String): String =
    normalizeText(text).replace(" ", "")

private fun normalizeText(text: String): String =
    text.trim().replace("　", "").replace("\n", "")
