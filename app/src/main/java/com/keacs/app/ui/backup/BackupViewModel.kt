package com.keacs.app.ui.backup

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.keacs.app.domain.usecase.ExportBackupUseCase
import com.keacs.app.domain.usecase.ImportBackupUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupViewModel(
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase
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
            message = "导入失败: ${e.message}"
            isError = true
        }
    }
}
