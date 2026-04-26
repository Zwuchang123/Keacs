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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.ui.components.KeacsBottomBar
import com.keacs.app.ui.components.KeacsScaffold
import com.keacs.app.ui.home.HomeScreen
import com.keacs.app.ui.management.AccountEditScreen
import com.keacs.app.ui.management.AccountListScreen
import com.keacs.app.ui.management.CategoryEditScreen
import com.keacs.app.ui.management.CategoryListScreen
import com.keacs.app.ui.navigation.KeacsDestination
import com.keacs.app.ui.navigation.bottomDestinations
import com.keacs.app.ui.navigation.destinationForRoute
import com.keacs.app.ui.record.AddRecordScreen
import com.keacs.app.ui.record.RecordScreen
import com.keacs.app.ui.settings.MineScreen
import com.keacs.app.ui.stats.StatsScreen
import com.keacs.app.ui.theme.KeacsColors

@Composable
fun KeacsApp(repository: LocalDataRepository) {
    var currentRoute by rememberSaveable { mutableStateOf(KeacsDestination.Home.route) }
    val currentDestination = bottomDestinations.firstOrNull { it.route == currentRoute }
    val screenTitle = when {
        currentRoute == ROUTE_CATEGORY_LIST -> "分类管理"
        currentRoute.startsWith(ROUTE_CATEGORY_EDIT) -> "编辑分类"
        currentRoute == ROUTE_ACCOUNT_LIST -> "账户管理"
        currentRoute.startsWith(ROUTE_ACCOUNT_EDIT) -> "编辑账户"
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
        actionText = if (currentDestination == KeacsDestination.Add) "保存" else null,
        actionEnabled = false,
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
                route == KeacsDestination.Home.route -> HomeScreen(
                    onAddClick = { currentRoute = KeacsDestination.Add.route },
                    onRecordsClick = { currentRoute = KeacsDestination.Records.route },
                    onStatsClick = { currentRoute = KeacsDestination.Stats.route },
                    onMineClick = { currentRoute = KeacsDestination.Mine.route },
                )

                route == KeacsDestination.Records.route -> RecordScreen()
                route == KeacsDestination.Add.route -> AddRecordScreen(repository)
                route == KeacsDestination.Stats.route -> StatsScreen()
                route == KeacsDestination.Mine.route -> MineScreen(
                    onCategoryClick = { currentRoute = ROUTE_CATEGORY_LIST },
                    onAccountClick = { currentRoute = ROUTE_ACCOUNT_LIST },
                )
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
                    onDone = { currentRoute = ROUTE_ACCOUNT_LIST },
                )
            }
        }
    }
}

private const val ROUTE_CATEGORY_LIST = "category-list"
private const val ROUTE_CATEGORY_EDIT = "category-edit/"
private const val ROUTE_ACCOUNT_LIST = "account-list"
private const val ROUTE_ACCOUNT_EDIT = "account-edit/"

private fun categoryEditRoute(id: Long?): String = ROUTE_CATEGORY_EDIT + (id?.toString() ?: "new")

private fun accountEditRoute(id: Long?): String = ROUTE_ACCOUNT_EDIT + (id?.toString() ?: "new")

private fun routeId(route: String, prefix: String): Long? =
    route.removePrefix(prefix).takeIf { it != "new" }?.toLongOrNull()

private fun backRoute(route: String): String = when {
    route == KeacsDestination.Add.route -> KeacsDestination.Home.route
    route.startsWith(ROUTE_CATEGORY_EDIT) -> ROUTE_CATEGORY_LIST
    route.startsWith(ROUTE_ACCOUNT_EDIT) -> ROUTE_ACCOUNT_LIST
    route == ROUTE_CATEGORY_LIST || route == ROUTE_ACCOUNT_LIST -> KeacsDestination.Mine.route
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
