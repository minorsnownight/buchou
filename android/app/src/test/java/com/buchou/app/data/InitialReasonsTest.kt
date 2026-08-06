package com.buchou.app.data

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class InitialReasonsTest {
    @Test
    fun `system creates five localized reasons`() {
        val reasons = initialReasonContents(null, Locale.SIMPLIFIED_CHINESE)

        assertEquals(5, reasons.size)
        assertEquals("让呼吸更轻松", reasons.first())
    }

    @Test
    fun `custom reason is first and replaces the final default`() {
        val reasons = initialReasonContents("为了跑完马拉松", Locale.SIMPLIFIED_CHINESE)

        assertEquals(5, reasons.size)
        assertEquals("为了跑完马拉松", reasons.first())
        assertEquals(false, "重新掌控自己的生活" in reasons)
    }

    @Test
    fun `duplicate custom reason does not reduce the default set`() {
        val reasons = initialReasonContents("让呼吸更轻松", Locale.SIMPLIFIED_CHINESE)

        assertEquals(5, reasons.size)
        assertEquals(5, reasons.distinct().size)
    }

    @Test
    fun `existing reason is completed to five without being replaced`() {
        val reasons = completeReasonContents(
            existing = listOf("为了跑完马拉松"),
            locale = Locale.SIMPLIFIED_CHINESE,
        )

        assertEquals(5, reasons.size)
        assertEquals("为了跑完马拉松", reasons.first())
    }

    @Test
    fun `five custom reasons are preserved in order`() {
        val custom = listOf("理由一", "理由二", "理由三", "理由四", "理由五")

        assertEquals(custom, completeReasonContents(custom, Locale.SIMPLIFIED_CHINESE))
    }
}
