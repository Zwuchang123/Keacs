package com.keacs.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.ui.components.KeacsBottomBar
import com.keacs.app.ui.components.KeacsScaffold
import com.keacs.app.ui.home.HomeScreen
import com.keacs.app.ui.home.HomeViewModel
import com.keacs.app.ui.management.AccountEditScreen
import com.keacs.app.ui.management.AccountListScreen
import com.keacs.app.ui.management.CategoryEditScreen
import com.keacs.app.ui.management.CategoryListScreen
import com.keacs.app.ui.navigation.KeacsDestination
import com.keacs.app.ui.navigation.bottomDestinations
import com.keacs.app.ui.navigation.destinationForRoute
import com.keacs.app.ui.record.AddRecordScreen
import com.keacs.app.ui.record.RecordDetailScreen
import com.keacs.app.ui.record.RecordScreen
import com.keacs.app.ui.settings.MineScreen
import com.keacs.app.ui.stats.StatsScreen
import com.keacs.app.ui.theme.KeacsColors

import com.keacs.app.data.backup.BackupService
import com.keacs.app.domain.usecase.ExportBackupUseCase
import com.keacs.app.domain.usecase.ImportBackupUseCase
import com.keacs.app.ui.backup.BackupScreen
import com.keacs.app.ui.backup.BackupViewModel
import com.keacs.app.ui.settings.SettingsScreen

class KeacsViewModelFactory(
    private val repository: LocalDataRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(BackupViewModel::class.java)) {
            val backupService = BackupService(repository)
            return BackupViewModel(
                exportBackupUseCase = ExportBackupUseCase(backupService),
                importBackupUseCase = ImportBackupUseCase(backupService)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

@Composable
fun KeacsApp(repository: LocalDataRepository) {
    var currentRoute by rememberSaveable { mutableStateOf(KeacsDestination.Home.route) }
    var accountDeleteRequest by rememberSaveable { mutableStateOf(0) }
    val currentDestination = bottomDestinations.firstOrNull { it.route == currentRoute }
    val screenTitle = when {
        currentRoute == ROUTE_CATEGORY_LIST -> "分类管理"
        currentRoute.startsWith(ROUTE_CATEGORY_EDIT) -> "编辑分类"
        currentRoute == ROUTE_ACCOUNT_LIST -> "账户管理"
        currentRoute.startsWith(ROUTE_ACCOUNT_EDIT) -> "编辑账户"
        currentRoute.startsWith(ROUTE_RECORD_DETAIL) -> "账目详情"
        currentRoute.startsWith(ROUTE_RECORD_EDIT) -> "编辑账目"
        currentRoute == ROUTE_SETTINGS -> "设置"
        currentRoute == ROUTE_BACKUP -> "数据备份"
        currentDestination == KeacsDestination.Add -> "新增记录"
        currentDestination != null -> stringResource(currentDestination.titleRes)
        else -> stringResource(destinationForRoute(currentRoute).titleRes)
    }

    BackHandler(enabled = currentRoute != KeacsDestination.Home.route) {
        currentRoute = backRoute(currentRoute)
    }

    KeacsScaffold(
        title = screenTitle,
        showBack = currentDestination == null || currentDestination == KeacsDestination.Add,
        actionText = if (isExistingAccountEditRoute(currentRoute)) "删除" else null,
        onActionClick = { accountDeleteRequest += 1 },
        onBackClick = { currentRoute = backRoute(currentRoute) },
        actions = {
            currentDestination?.let { TopActions(destination = it) }
        },
        bottomBar = {
            if (currentDestination != null && currentDestination != KeacsDestination.Add) {
                KeacsBottomBar(
                    currentDestination = currentDestination,
                    onDestinationSelected = { currentRoute = it.route },
                )
            }
        },
    ) { contentPadding ->
        Crossfade(
            targetState = currentRoute,
            modifier = Modifier.padding(contentPadding),
            label = "main-navigation",
        ) { route ->
            when {
                route == KeacsDestination.Home.route -> {
                    val homeViewModel: HomeViewModel = viewModel(
                        factory = KeacsViewModelFactory(repository),
                    )
                    HomeScreen(
                        viewModel = homeViewModel,
                        onAddClick = { currentRoute = KeacsDestination.Add.route },
                        onRecordsClick = { currentRoute = KeacsDestination.Records.route },
                        onStatsClick = { currentRoute = KeacsDestination.Stats.route },
                        onMineClick = { currentRoute = KeacsDestination.Mine.route },
                        onRecordClick = { currentRoute = recordDetailRoute(it) },
                    )
                }

                route == KeacsDestination.Records.route -> RecordScreen(
                    repository = repository,
                    onEditRecord = { currentRoute = recordEditRoute(it) },
                )

                route == KeacsDestination.Add.route -> AddRecordScreen(
                    repository = repository,
                    onDone = { currentRoute = KeacsDestination.Records.route },
                )

                route == KeacsDestination.Stats.route -> StatsScreen(repository)

                route == KeacsDestination.Mine.route -> MineScreen(
                    onCategoryClick = { currentRoute = ROUTE_CATEGORY_LIST },
                    onAccountClick = { currentRoute = ROUTE_ACCOUNT_LIST },
                    onSettingsClick = { currentRoute = ROUTE_SETTINGS },
                    onBackupClick = { currentRoute = ROUTE_BACKUP }
                )

                route == ROUTE_SETTINGS -> SettingsScreen()

                route == ROUTE_BACKUP -> {
                    val backupViewModel: BackupViewModel = viewModel(
                        factory = KeacsViewModelFactory(repository)
                    )
                    BackupScreen(viewModel = backupViewModel)
                }

                route == ROUTE_CATEGORY_LIST -> CategoryListScreen(
                    repository = repository,
                    onEditCategory = { currentRoute = categoryEditRoute(it) },
                )

                route.startsWith(ROUTE_CATEGORY_EDIT) -> CategoryEditScreen(
                    repository = repository,
                    categoryId = routeId(route, ROUTE_CATEGORY_EDIT),
                    onDone = { currentRoute = ROUTE_CATEGORY_LIST },
                )

                route == ROUTE_ACCOUNT_LIST -> AccountListScreen(
                    repository = repository,
                    onEditAccount = { currentRoute = accountEditRoute(it) },
                )

                route.startsWith(ROUTE_ACCOUNT_EDIT) -> AccountEditScreen(
                    repository = repository,
                    accountId = routeId(route, ROUTE_ACCOUNT_EDIT),
                    deleteRequest = accountDeleteRequest,
                    onDone = { currentRoute = ROUTE_ACCOUNT_LIST },
                )

                route.startsWith(ROUTE_RECORD_DETAIL) -> RecordDetailScreen(
                    recordId = routeId(route, ROUTE_RECORD_DETAIL) ?: 0L,
                    repository = repository,
                    onBack = { currentRoute = KeacsDestination.Records.route },
                    onEdit = { currentRoute = recordEditRoute(it) },
                )

                route.startsWith(ROUTE_RECORD_EDIT) -> AddRecordScreen(
                    repository = repository,
                    recordId = routeId(route, ROUTE_RECORD_EDIT),
                    onDone = { currentRoute = KeacsDestination.Records.route },
                )
            }
        }
    }
}

private const val ROUTE_CATEGORY_LIST = "category-list"
private const val ROUTE_CATEGORY_EDIT = "category-edit/"
private const val ROUTE_ACCOUNT_LIST = "account-list"
private const val ROUTE_ACCOUNT_EDIT = "account-edit/"
private const val ROUTE_RECORD_DETAIL = "record-detail/"
private const val ROUTE_RECORD_EDIT = "record-edit/"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_BACKUP = "backup"

private fun categoryEditRoute(id: Long?): String = ROUTE_CATEGORY_EDIT + (id?.toString() ?: "new")
private fun accountEditRoute(id: Long?): String = ROUTE_ACCOUNT_EDIT + (id?.toString() ?: "new")
private fun recordDetailRoute(id: Long): String = ROUTE_RECORD_DETAIL + id
private fun recordEditRoute(id: Long): String = ROUTE_RECORD_EDIT + id

private fun routeId(route: String, prefix: String): Long? =
    route.removePrefix(prefix).takeIf { it != "new" }?.toLongOrNull()

private fun isExistingAccountEditRoute(route: String): Boolean =
    route.startsWith(ROUTE_ACCOUNT_EDIT) && routeId(route, ROUTE_ACCOUNT_EDIT) != null

private fun backRoute(route: String): String = when {
    route == KeacsDestination.Add.route -> KeacsDestination.Home.route
    route.startsWith(ROUTE_CATEGORY_EDIT) -> ROUTE_CATEGORY_LIST
    route.startsWith(ROUTE_ACCOUNT_EDIT) -> ROUTE_ACCOUNT_LIST
    route.startsWith(ROUTE_RECORD_DETAIL) -> KeacsDestination.Records.route
    route.startsWith(ROUTE_RECORD_EDIT) -> {
        val id = routeId(route, ROUTE_RECORD_EDIT)
        if (id != null) KeacsDestination.Records.route else KeacsDestination.Records.route
    }
    route == ROUTE_CATEGORY_LIST || route == ROUTE_ACCOUNT_LIST || route == ROUTE_SETTINGS || route == ROUTE_BACKUP -> KeacsDestination.Mine.route
    else -> KeacsDestination.Home.route
}

@Composable
private fun TopActions(destination: KeacsDestination) {
    when (destination) {
        KeacsDestination.Home -> {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = "打开日历",
                    tint = KeacsColors.TextPrimary,
                )
            }
        }

        KeacsDestination.Records -> {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "搜索账单",
                    tint = KeacsColors.TextPrimary,
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = "更多操作",
                    tint = KeacsColors.TextPrimary,
                )
            }
        }

        KeacsDestination.Add,
        KeacsDestination.Stats,
        KeacsDestination.Mine -> Unit
    }
}
