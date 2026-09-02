package com.dangerfield.movingeyes.libraries.ui.components

import androidx.compose.ui.graphics.Color
import com.dangerfield.movingeyes.libraries.ui.components.color.Hsv
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HsvTest {

    @Test
    fun `the primaries land on their hues`() {
        assertNear(0f, Hsv.from(Color.Red).hue)
        assertNear(120f, Hsv.from(Color.Green).hue)
        assertNear(240f, Hsv.from(Color.Blue).hue)
    }

    @Test
    fun `greys have no saturation`() {
        assertNear(0f, Hsv.from(Color.White).saturation)
        assertNear(0f, Hsv.from(Color.Gray).saturation)
        assertNear(1f, Hsv.from(Color.White).value)
        assertNear(0f, Hsv.from(Color.Black).value)
    }

    @Test
    fun `a colour survives a round trip`() {
        listOf(
            Color(0xFFF07A1E),
            Color(0xFF4FA3C7),
            Color(0xFF7A5A34),
            Color(0xFFC8D24A),
        ).forEach { original ->
            val round = Hsv.from(original).toColor()

            assertNear(original.red, round.red)
            assertNear(original.green, round.green)
            assertNear(original.blue, round.blue)
        }
    }

    /**
     * The reason the picker holds [Hsv] rather than deriving it from the
     * colour: black and grey have no hue to recover, so a thumb dragged to the
     * bottom of the panel would snap the slider to red and never come back.
     */
    @Test
    fun `black and grey report no hue to recover`() {
        assertNear(0f, Hsv.from(Color.Black).hue)
        assertNear(0f, Hsv.from(Color.Gray).hue)
    }

    @Test
    fun `hue wraps rather than clamping`() {
        val wrapped = Hsv(hue = 400f, saturation = 1f, value = 1f).toColor()
        val equivalent = Hsv(hue = 40f, saturation = 1f, value = 1f).toColor()

        assertEquals(equivalent, wrapped)
    }




    private fun assertNear(expected: Float, actual: Float) {
        assertTrue(abs(expected - actual) < 0.01f, "expected $expected but was $actual")
    }
}
