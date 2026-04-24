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
import com.keacs.app.ui.components.KeacsBottomBar
import com.keacs.app.ui.components.KeacsScaffold
import com.keacs.app.ui.home.HomeScreen
import com.keacs.app.ui.navigation.KeacsDestination
import com.keacs.app.ui.navigation.destinationForRoute
import com.keacs.app.ui.record.AddRecordScreen
import com.keacs.app.ui.record.RecordScreen
import com.keacs.app.ui.settings.MineScreen
import com.keacs.app.ui.stats.StatsScreen
import com.keacs.app.ui.theme.KeacsColors

@Composable
fun KeacsApp() {
    var currentRoute by rememberSaveable { mutableStateOf(KeacsDestination.Home.route) }
    val currentDestination = destinationForRoute(currentRoute)
    val screenTitle = if (currentDestination == KeacsDestination.Add) {
        "新增记录"
    } else {
        stringResource(currentDestination.titleRes)
    }

    KeacsScaffold(
        title = screenTitle,
        showBack = currentDestination == KeacsDestination.Add,
        actionText = if (currentDestination == KeacsDestination.Add) "保存" else null,
        actionEnabled = false,
        onBackClick = { currentRoute = KeacsDestination.Home.route },
        actions = {
            TopActions(destination = currentDestination)
        },
        bottomBar = {
            KeacsBottomBar(
                currentDestination = currentDestination,
                onDestinationSelected = { currentRoute = it.route },
            )
        },
    ) { contentPadding ->
        Crossfade(
            targetState = currentDestination,
            modifier = Modifier.padding(contentPadding),
            label = "main-navigation",
        ) { destination ->
            when (destination) {
                KeacsDestination.Home -> HomeScreen(
                    onAddClick = { currentRoute = KeacsDestination.Add.route },
                    onRecordsClick = { currentRoute = KeacsDestination.Records.route },
                    onStatsClick = { currentRoute = KeacsDestination.Stats.route },
                    onMineClick = { currentRoute = KeacsDestination.Mine.route },
                )

                KeacsDestination.Records -> RecordScreen(
                    onAddClick = { currentRoute = KeacsDestination.Add.route },
                )

                KeacsDestination.Add -> AddRecordScreen()
                KeacsDestination.Stats -> StatsScreen(onAddClick = { currentRoute = KeacsDestination.Add.route })
                KeacsDestination.Mine -> MineScreen()
            }
        }
    }
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
