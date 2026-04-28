package com.keacs.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.keacs.app.data.local.PreferencesManager
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
import com.keacs.app.ui.backup.BackupViewModel
import com.keacs.app.ui.settings.AboutScreen
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
fun KeacsApp(
    repository: LocalDataRepository,
    preferencesManager: PreferencesManager,
) {
    var currentRoute by rememberSaveable { mutableStateOf(KeacsDestination.Home.route) }
    var addSourceRoute by rememberSaveable { mutableStateOf(KeacsDestination.Home.route) }
    var addEntryKey by rememberSaveable { mutableIntStateOf(0) }
    var recordDetailSourceRoute by rememberSaveable { mutableStateOf(KeacsDestination.Records.route) }
    var accountDeleteRequest by rememberSaveable { mutableStateOf(0) }
    var navigationDirection by rememberSaveable { mutableIntStateOf(1) }
    val currentDestination = bottomDestinations.firstOrNull { it.route == currentRoute }
    val screenTitle = when {
        currentRoute == ROUTE_CATEGORY_LIST -> "分类管理"
        currentRoute.startsWith(ROUTE_CATEGORY_EDIT) -> "编辑分类"
        currentRoute == ROUTE_ACCOUNT_LIST -> "账户管理"
        currentRoute.startsWith(ROUTE_ACCOUNT_EDIT) -> "编辑账户"
        currentRoute.startsWith(ROUTE_RECORD_DETAIL) -> "账目详情"
        currentRoute.startsWith(ROUTE_RECORD_EDIT) -> "编辑账目"
        currentRoute == ROUTE_SETTINGS -> "设置"
        currentRoute == ROUTE_ABOUT -> "关于"
        currentDestination == KeacsDestination.Add -> "新增记录"
        currentDestination != null -> stringResource(currentDestination.titleRes)
        else -> stringResource(destinationForRoute(currentRoute).titleRes)
    }

    fun navigateBack() {
        navigationDirection = -1
        currentRoute = when {
            currentRoute == KeacsDestination.Add.route -> addSourceRoute
            currentRoute.startsWith(ROUTE_RECORD_DETAIL) -> recordDetailSourceRoute
            else -> backRoute(currentRoute)
        }
    }

    fun navigateRecordDetail(id: Long, sourceRoute: String) {
        navigationDirection = 1
        recordDetailSourceRoute = sourceRoute
        currentRoute = recordDetailRoute(id)
    }

    fun navigateTo(route: String) {
        navigationDirection = if (navigationAnimationIndex(route) >= navigationAnimationIndex(currentRoute)) 1 else -1
        currentRoute = route
    }

    fun navigateForward(route: String) {
        navigationDirection = 1
        currentRoute = route
    }

    fun navigateBackTo(route: String) {
        navigationDirection = -1
        currentRoute = route
    }

    BackHandler(enabled = currentRoute != KeacsDestination.Home.route) {
        navigateBack()
    }

    KeacsScaffold(
        title = screenTitle,
        showBack = currentDestination == null || currentDestination == KeacsDestination.Add,
        actionText = if (isExistingAccountEditRoute(currentRoute)) "删除" else null,
        onActionClick = { accountDeleteRequest += 1 },
        onBackClick = {
            navigateBack()
        },
        actions = {
            currentDestination?.let { TopActions(destination = it) }
        },
        bottomBar = {
            if (currentDestination != null && currentDestination != KeacsDestination.Add) {
                KeacsBottomBar(
                    currentDestination = currentDestination,
                    onDestinationSelected = {
                        if (it == KeacsDestination.Add) {
                            addSourceRoute = currentRoute
                            addEntryKey += 1
                            navigationDirection = 1
                            currentRoute = it.route
                        } else {
                            navigateTo(it.route)
                        }
                    },
                )
            }
        },
    ) { contentPadding ->
        AnimatedContent(
            targetState = currentRoute,
            modifier = Modifier.padding(contentPadding),
            transitionSpec = {
                val duration = 220
                if (targetState == KeacsDestination.Add.route || initialState == KeacsDestination.Add.route) {
                    (fadeIn(animationSpec = tween(duration)) + slideInVertically(
                        animationSpec = tween(duration),
                        initialOffsetY = { fullHeight -> fullHeight / 10 }
                    )).togetherWith(
                        fadeOut(animationSpec = tween(duration)) + slideOutVertically(
                            animationSpec = tween(duration),
                            targetOffsetY = { fullHeight -> fullHeight / 10 }
                        )
                    )
                } else {
                    val direction = navigationDirection
                    (fadeIn(animationSpec = tween(duration)) + slideInHorizontally(
                        animationSpec = tween(duration),
                        initialOffsetX = { fullWidth -> direction * fullWidth / 10 }
                    )).togetherWith(
                        fadeOut(animationSpec = tween(duration)) + slideOutHorizontally(
                            animationSpec = tween(duration),
                            targetOffsetX = { fullWidth -> -direction * fullWidth / 10 }
                        )
                    )
                }
            },
            label = "main-navigation",
        ) { route ->
            when {
                route == KeacsDestination.Home.route -> {
                    val homeViewModel: HomeViewModel = viewModel(
                        factory = KeacsViewModelFactory(repository),
                    )
                    HomeScreen(
                        viewModel = homeViewModel,
                        onRecordsClick = { navigateTo(KeacsDestination.Records.route) },
                        onRecordClick = { navigateRecordDetail(it, KeacsDestination.Home.route) },
                        onSwipeLeft = { navigateTo(KeacsDestination.Records.route) },
                    )
                }

                route == KeacsDestination.Records.route -> RecordScreen(
                    repository = repository,
                    onViewRecord = { navigateRecordDetail(it, KeacsDestination.Records.route) },
                    onSwipeLeft = { navigateTo(KeacsDestination.Stats.route) },
                    onSwipeRight = { navigateTo(KeacsDestination.Home.route) },
                )

                route == KeacsDestination.Add.route -> AddRecordScreen(
                    repository = repository,
                    preferencesManager = preferencesManager,
                    entryKey = addEntryKey,
                    onDone = { navigateBackTo(addSourceRoute) },
                )

                route == KeacsDestination.Stats.route -> StatsScreen(
                    repository = repository,
                    onSwipeBeyondStart = { navigateTo(KeacsDestination.Records.route) },
                    onSwipeBeyondEnd = { navigateTo(KeacsDestination.Mine.route) },
                )

                route == KeacsDestination.Mine.route -> {
                    val backupViewModel: BackupViewModel = viewModel(
                        factory = KeacsViewModelFactory(repository)
                    )
                    MineScreen(
                        backupViewModel = backupViewModel,
                        onCategoryClick = { navigateForward(ROUTE_CATEGORY_LIST) },
                        onAccountClick = { navigateForward(ROUTE_ACCOUNT_LIST) },
                        onSettingsClick = { navigateForward(ROUTE_SETTINGS) },
                        onAboutClick = { navigateForward(ROUTE_ABOUT) },
                        onSwipeRight = { navigateTo(KeacsDestination.Stats.route) },
                    )
                }

                route == ROUTE_SETTINGS -> SettingsScreen(
                    repository = repository,
                    preferencesManager = preferencesManager,
                )

                route == ROUTE_ABOUT -> AboutScreen()

                route == ROUTE_CATEGORY_LIST -> CategoryListScreen(
                    repository = repository,
                    onEditCategory = { navigateForward(categoryEditRoute(it)) },
                )

                route.startsWith(ROUTE_CATEGORY_EDIT) -> CategoryEditScreen(
                    repository = repository,
                    categoryId = routeId(route, ROUTE_CATEGORY_EDIT),
                    onDone = { navigateBackTo(ROUTE_CATEGORY_LIST) },
                )

                route == ROUTE_ACCOUNT_LIST -> AccountListScreen(
                    repository = repository,
                    onEditAccount = { navigateForward(accountEditRoute(it)) },
                )

                route.startsWith(ROUTE_ACCOUNT_EDIT) -> AccountEditScreen(
                    repository = repository,
                    accountId = routeId(route, ROUTE_ACCOUNT_EDIT),
                    deleteRequest = accountDeleteRequest,
                    onDone = { navigateBackTo(ROUTE_ACCOUNT_LIST) },
                )

                route.startsWith(ROUTE_RECORD_DETAIL) -> RecordDetailScreen(
                    recordId = routeId(route, ROUTE_RECORD_DETAIL) ?: 0L,
                    repository = repository,
                    onBack = { navigateBackTo(recordDetailSourceRoute) },
                    onEdit = { navigateForward(recordEditRoute(it)) },
                )

                route.startsWith(ROUTE_RECORD_EDIT) -> AddRecordScreen(
                    repository = repository,
                    preferencesManager = preferencesManager,
                    recordId = routeId(route, ROUTE_RECORD_EDIT),
                    onDone = { routeId(route, ROUTE_RECORD_EDIT)?.let { navigateBackTo(recordDetailRoute(it)) } },
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
private const val ROUTE_ABOUT = "about"

private fun categoryEditRoute(id: Long?): String = ROUTE_CATEGORY_EDIT + (id?.toString() ?: "new")
private fun accountEditRoute(id: Long?): String = ROUTE_ACCOUNT_EDIT + (id?.toString() ?: "new")
private fun recordDetailRoute(id: Long): String = ROUTE_RECORD_DETAIL + id
private fun recordEditRoute(id: Long): String = ROUTE_RECORD_EDIT + id

private fun routeId(route: String, prefix: String): Long? =
    route.removePrefix(prefix).takeIf { it != "new" }?.toLongOrNull()

private fun isExistingAccountEditRoute(route: String): Boolean =
    route.startsWith(ROUTE_ACCOUNT_EDIT) && routeId(route, ROUTE_ACCOUNT_EDIT) != null

private fun navigationAnimationIndex(route: String): Int = when (route) {
    KeacsDestination.Home.route -> 0
    KeacsDestination.Records.route -> 1
    KeacsDestination.Add.route -> 2
    KeacsDestination.Stats.route -> 3
    KeacsDestination.Mine.route -> 4
    else -> 5
}

private fun backRoute(route: String): String = when {
    route == KeacsDestination.Add.route -> KeacsDestination.Home.route
    route.startsWith(ROUTE_CATEGORY_EDIT) -> ROUTE_CATEGORY_LIST
    route.startsWith(ROUTE_ACCOUNT_EDIT) -> ROUTE_ACCOUNT_LIST
    route.startsWith(ROUTE_RECORD_DETAIL) -> KeacsDestination.Records.route
    route.startsWith(ROUTE_RECORD_EDIT) -> {
        val id = routeId(route, ROUTE_RECORD_EDIT)
        if (id != null) KeacsDestination.Records.route else KeacsDestination.Records.route
    }
    route == ROUTE_CATEGORY_LIST || route == ROUTE_ACCOUNT_LIST || route == ROUTE_SETTINGS || route == ROUTE_ABOUT -> KeacsDestination.Mine.route
    else -> KeacsDestination.Home.route
}

@Composable
private fun TopActions(destination: KeacsDestination) = Unit
