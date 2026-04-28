package com.keacs.app.ui.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddRecordLogicTest {
    @Test
    fun amountSupportsPlusAndMinusCalculation() {
        val amount = listOf("1", "0", "+", "2", ".", "5", "0")
            .fold("") { current, key -> nextAmount(current, key) }

        assertEquals("10+2.50", amount)
        assertEquals(1_250L, amountToCent(amount))
        assertEquals(750L, amountToCent("10-2.50"))
    }

    @Test
    fun amountRejectsIncompleteCalculation() {
        assertNull(amountToCent("10+"))
        assertNull(amountToCent("10-10"))
    }

    @Test
    fun amountRejectsTextThatWouldWrap() {
        assertFalse(amountInputWouldOverflow("12345678901"))
        assertTrue(amountInputWouldOverflow("123456789012"))
    }
}
