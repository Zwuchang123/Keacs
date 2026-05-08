package com.keacs.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class KeacsDestinationTest {
    @Test
    fun bottomNavigationKeepsFixedOrder() {
        assertEquals(
            listOf(
                KeacsDestination.Home,
                KeacsDestination.Stats,
                KeacsDestination.Add,
                KeacsDestination.Discover,
                KeacsDestination.Mine,
            ),
            bottomDestinations,
        )
    }

    @Test
    fun unknownRouteFallsBackToHome() {
        assertEquals(
            KeacsDestination.Home,
            destinationForRoute("unknown"),
        )
    }
}
