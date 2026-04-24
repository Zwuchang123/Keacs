package com.keacs.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.padding
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

@Composable
fun KeacsApp() {
    var currentRoute by rememberSaveable { mutableStateOf(KeacsDestination.Home.route) }
    val currentDestination = destinationForRoute(currentRoute)

    KeacsScaffold(
        title = stringResource(currentDestination.titleRes),
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
                KeacsDestination.Stats -> StatsScreen()
                KeacsDestination.Mine -> MineScreen()
            }
        }
    }
}
