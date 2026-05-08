package com.keacs.app.ui.navigation

import androidx.annotation.StringRes
import com.keacs.app.R

enum class KeacsDestination(
    val route: String,
    @StringRes val titleRes: Int,
    @StringRes val contentDescriptionRes: Int,
) {
    Home(
        route = "home",
        titleRes = R.string.nav_home,
        contentDescriptionRes = R.string.nav_to_home,
    ),
    Stats(
        route = "stats",
        titleRes = R.string.nav_stats,
        contentDescriptionRes = R.string.nav_to_stats,
    ),
    Add(
        route = "add",
        titleRes = R.string.nav_add,
        contentDescriptionRes = R.string.nav_to_add,
    ),
    Discover(
        route = "discover",
        titleRes = R.string.nav_discover,
        contentDescriptionRes = R.string.nav_to_discover,
    ),
    Mine(
        route = "mine",
        titleRes = R.string.nav_mine,
        contentDescriptionRes = R.string.nav_to_mine,
    );
}

val bottomDestinations = listOf(
    KeacsDestination.Home,
    KeacsDestination.Stats,
    KeacsDestination.Add,
    KeacsDestination.Discover,
    KeacsDestination.Mine,
)

fun destinationForRoute(route: String): KeacsDestination =
    bottomDestinations.firstOrNull { it.route == route } ?: KeacsDestination.Home
