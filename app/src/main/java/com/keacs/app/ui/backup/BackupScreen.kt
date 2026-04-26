package com.keacs.app.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.components.ConfirmDialog
import com.keacs.app.ui.components.DividedMenuCard
import com.keacs.app.ui.components.KeacsSnackbar
import com.keacs.app.ui.components.MenuDivider
import com.keacs.app.ui.components.MenuRow
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import kotlinx.coroutines.launch

@Composable
fun BackupScreen(
    viewModel: BackupViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showImportConfirm by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                viewModel.exportBackup(context, uri)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            showImportConfirm = uri
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-backup")
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        Text(
            text = "数据备份用于换机迁移。所有数据仅保存在本机，请妥善保管导出的备份文件。",
            style = MaterialTheme.typography.bodyMedium,
            color = KeacsColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        DividedMenuCard {
            MenuRow(
                icon = Icons.Rounded.FileUpload,
                title = "导出完整备份",
                iconColor = KeacsColors.Primary,
                onClick = {
                    val fileName = "keacs_backup_${System.currentTimeMillis()}.json"
                    exportLauncher.launch(fileName)
                }
            )
            MenuDivider()
            MenuRow(
                icon = Icons.Rounded.FileDownload,
                title = "合并导入备份",
                iconColor = KeacsColors.Warning,
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                }
            )
        }
    }

    showImportConfirm?.let { uri ->
        ConfirmDialog(
            title = "合并导入备份",
            text = "导入会把备份内容追加到当前数据中，且不会自动去重。确定要继续导入吗？",
            confirmText = "继续导入",
            onConfirm = {
                showImportConfirm = null
                coroutineScope.launch {
                    viewModel.importBackup(context, uri)
                }
            },
            onDismiss = { showImportConfirm = null },
            isDestructive = true
        )
    }

    viewModel.message?.let { message ->
        KeacsSnackbar(
            message = message,
            isError = viewModel.isError,
            onDismiss = { viewModel.clearMessage() }
        )
    }
}
