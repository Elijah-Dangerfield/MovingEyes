package com.dangerfield.movingeyes.libraries.ui.components

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ColorFieldHexTest {

    @Test
    fun `round trips every channel independently`() {
        listOf("000000", "FFFFFF", "F2A24B", "C8D24A", "0C0A0A").forEach { hex ->
            assertEquals(hex, hex.parseHexOrNull()!!.toHexDigits())
        }
    }

    @Test
    fun `parses a known accent`() {
        val parsed = requireNamed("F2A24B")
        assertEquals(Color(0xFFF2A24B), parsed)
    }

    @Test
    fun `rejects a partial entry so a half-typed value cannot repaint the canvas`() {
        assertNull("F2A2".parseHexOrNull())
        assertNull("F2A24".parseHexOrNull())
        assertNull("".parseHexOrNull())
    }

    @Test
    fun `rejects non-hex characters and over-long input`() {
        assertNull("GGGGGG".parseHexOrNull())
        assertNull("F2A24BB".parseHexOrNull())
        assertNull("#F2A24".parseHexOrNull())
    }

    @Test
    fun `accepts lowercase and normalizes on the way out`() {
        assertEquals("F2A24B", "f2a24b".parseHexOrNull()!!.toHexDigits())
    }

    private fun requireNamed(hex: String): Color = hex.parseHexOrNull()!!
}
