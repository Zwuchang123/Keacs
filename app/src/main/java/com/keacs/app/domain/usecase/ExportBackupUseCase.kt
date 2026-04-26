package com.keacs.app.domain.usecase

import com.keacs.app.data.backup.BackupService
import java.io.OutputStream

class ExportBackupUseCase(
    private val backupService: BackupService
) {
    suspend operator fun invoke(outputStream: OutputStream) {
        backupService.exportBackup(outputStream)
    }
}
