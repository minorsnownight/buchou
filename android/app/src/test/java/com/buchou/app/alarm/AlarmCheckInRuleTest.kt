package com.buchou.app.alarm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmCheckInRuleTest {
    @Test
    fun `alarm is suppressed when today has a smoke-free check-in`() {
        assertTrue(
            hasRecordForDate(
                checkInDates = listOf("2026-08-03", "2026-08-04"),
                smokingDates = emptyList(),
                date = "2026-08-04",
            ),
        )
    }

    @Test
    fun `alarm is suppressed when today has a smoking event`() {
        assertTrue(
            hasRecordForDate(
                checkInDates = emptyList(),
                smokingDates = listOf("2026-08-04"),
                date = "2026-08-04",
            ),
        )
    }

    @Test
    fun `alarm may run when today has no check-in`() {
        assertFalse(
            hasRecordForDate(
                checkInDates = listOf("2026-08-03"),
                smokingDates = emptyList(),
                date = "2026-08-04",
            ),
        )
    }
}
