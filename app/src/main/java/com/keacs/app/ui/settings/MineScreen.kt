package com.keacs.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.ui.backup.BackupViewModel
import com.keacs.app.ui.components.ConfirmDialog
import com.keacs.app.ui.components.DividedMenuCard
import com.keacs.app.ui.components.KeacsSnackbar
import com.keacs.app.ui.components.MenuDivider
import com.keacs.app.ui.components.MenuRow
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import kotlinx.coroutines.launch

@Composable
fun MineScreen(
    repository: LocalDataRepository,
    backupViewModel: BackupViewModel,
    onCategoryClick: () -> Unit,
    onAccountClick: () -> Unit,
    onScheduledClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onSwipeRight: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showImportConfirm by remember { mutableStateOf<Uri?>(null) }
    var showExcelGuide by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                backupViewModel.exportBackup(context, uri)
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

    val excelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                backupViewModel.importExcelRecords(context, uri)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-mine")
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        totalDrag += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        if (totalDrag >= 60f) {
                            onSwipeRight()
                        }
                        totalDrag = 0f
                    }
                )
            }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        MineOverviewSection(repository = repository)
        DividedMenuCard {
            MenuRow(Icons.Rounded.Category, "分类管理", KeacsColors.Primary, onClick = onCategoryClick)
            MenuDivider()
            MenuRow(Icons.Rounded.AccountBalanceWallet, "账户管理", KeacsColors.Income, onClick = onAccountClick)
            MenuDivider()
            MenuRow(Icons.Rounded.Schedule, "定时记账", KeacsColors.Warning, onClick = onScheduledClick)
        }
        DividedMenuCard {
            MenuRow(
                Icons.Rounded.FileUpload,
                "导出备份",
                KeacsColors.Warning,
                onClick = {
                    val fileName = "keacs_backup_${System.currentTimeMillis()}.json"
                    exportLauncher.launch(fileName)
                }
            )
            MenuDivider()
            MenuRow(
                Icons.Rounded.FileDownload,
                "导入备份",
                KeacsColors.Primary,
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                }
            )
            MenuDivider()
            MenuRow(
                Icons.Rounded.TableChart,
                "Excel 添加账目",
                KeacsColors.Income,
                onClick = {
                    showExcelGuide = true
                }
            )
        }
        DividedMenuCard {
            MenuRow(Icons.Rounded.Settings, "设置", KeacsColors.TextSecondary, onClick = onSettingsClick)
            MenuDivider()
            MenuRow(Icons.Rounded.Info, "关于", KeacsColors.Primary, onClick = onAboutClick)
        }
    }

    showImportConfirm?.let { uri ->
        ConfirmDialog(
            title = "合并导入备份",
            text = "导入后会和现有数据合并。",
            confirmText = "继续导入",
            onConfirm = {
                showImportConfirm = null
                coroutineScope.launch {
                    backupViewModel.importBackup(context, uri)
                }
            },
            onDismiss = { showImportConfirm = null },
            isDestructive = true
        )
    }

    if (showExcelGuide) {
        ConfirmDialog(
            title = "Excel 添加账目",
            text = "请准备 .xlsx 文件。\n\n表头：日期｜收支类型｜分类｜账户｜金额｜备注\n示例：2026-05-10｜支出｜餐饮｜微信｜12.50｜早餐\n\n收支类型填写“支出”或“收入”。",
            confirmText = "选择文件",
            onConfirm = {
                showExcelGuide = false
                excelLauncher.launch(
                    arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "*/*",
                    ),
                )
            },
            onDismiss = { showExcelGuide = false },
        )
    }

    backupViewModel.message?.let { message ->
        KeacsSnackbar(
            message = message,
            isError = backupViewModel.isError,
            atTop = backupViewModel.isError,
            onDismiss = { backupViewModel.clearMessage() }
        )
    }
}
