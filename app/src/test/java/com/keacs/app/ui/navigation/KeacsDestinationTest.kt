package com.keacs.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class KeacsDestinationTest {
    @Test
    fun bottomNavigationKeepsFixedOrder() {
        assertEquals(
            listOf(
                KeacsDestination.Home,
                KeacsDestination.Records,
                KeacsDestination.Add,
                KeacsDestination.Stats,
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
