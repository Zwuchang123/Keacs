package com.keacs.app.domain.usecase

import com.keacs.app.data.backup.BackupService
import java.io.InputStream

class ImportBackupUseCase(
    private val backupService: BackupService
) {
    suspend operator fun invoke(inputStream: InputStream) {
        backupService.importBackup(inputStream)
    }
}
