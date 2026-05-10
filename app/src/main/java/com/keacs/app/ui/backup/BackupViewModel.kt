package com.keacs.app.ui.backup

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.keacs.app.data.importer.ExcelRecordImportService
import com.keacs.app.domain.usecase.ExportBackupUseCase
import com.keacs.app.domain.usecase.ImportBackupUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.util.zip.ZipException

class BackupViewModel(
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val excelRecordImportService: ExcelRecordImportService,
) : ViewModel() {

    var message by mutableStateOf<String?>(null)
        private set

    var isError by mutableStateOf(false)
        private set

    fun clearMessage() {
        message = null
        isError = false
    }

    suspend fun exportBackup(context: Context, uri: Uri) {
        try {
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    exportBackupUseCase(outputStream)
                } ?: throw IllegalStateException("无法打开文件进行写入")
            }
            message = "备份导出成功"
            isError = false
        } catch (e: Exception) {
            message = "导出失败: ${e.message}"
            isError = true
        }
    }

    suspend fun importBackup(context: Context, uri: Uri) {
        try {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    importBackupUseCase(inputStream)
                } ?: throw IllegalStateException("无法读取选中的文件")
            }
            message = "备份导入成功"
            isError = false
        } catch (e: Exception) {
            message = "导入失败：${backupErrorMessage(e)}"
            isError = true
        }
    }

    suspend fun importExcelRecords(context: Context, uri: Uri) {
        try {
            val result = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    excelRecordImportService.import(inputStream)
                } ?: throw IllegalStateException("无法读取选中的文件")
            }
            message = if (result.createdRows == 0) {
                when {
                    result.totalRows == 0 -> "没有可添加的账目，请检查 Excel 内容"
                    else -> "没有添加成功，请检查日期、类型、分类、账户和金额格式"
                }
            } else {
                buildString {
                    append("已添加${result.createdRows}条")
                    if (result.skippedRows > 0) append("，${result.skippedRows}条未处理")
                    if (result.fallbackCategoryRows > 0) append("，${result.fallbackCategoryRows}条已归到其他")
                }
            }
            isError = result.createdRows == 0
        } catch (e: Exception) {
            message = "添加失败：${excelErrorMessage(e)}"
            isError = true
        }
    }

    private fun backupErrorMessage(error: Exception): String =
        when (error) {
            is JSONException -> "备份文件格式不正确，请选择 Keacs 导出的备份文件"
            is IllegalArgumentException -> error.message ?: "备份文件格式不正确"
            else -> error.message ?: "请稍后重试"
        }

    private fun excelErrorMessage(error: Exception): String =
        when (error) {
            is ZipException -> "文件格式不正确，请选择 .xlsx 文件"
            is IllegalArgumentException, is IllegalStateException -> {
                error.message?.takeIf { it.startsWith("缺少") || it.contains("格式") }
                    ?: "Excel 格式不正确，请按示例准备后再导入"
            }
            else -> "文件格式不正确，请按示例准备 Excel 后再导入"
        }
}
