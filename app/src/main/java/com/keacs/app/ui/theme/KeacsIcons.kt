package com.keacs.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.keacs.app.ui.navigation.KeacsDestination

fun KeacsDestination.icon(): ImageVector =
    when (this) {
        KeacsDestination.Home -> Icons.Rounded.Home
        KeacsDestination.Stats -> Icons.Rounded.BarChart
        KeacsDestination.Add -> Icons.Rounded.Add
        KeacsDestination.Discover -> Icons.Rounded.Explore
        KeacsDestination.Mine -> Icons.Rounded.Person
    }

val AccountIcon: ImageVector = Icons.Rounded.AccountBalanceWallet
