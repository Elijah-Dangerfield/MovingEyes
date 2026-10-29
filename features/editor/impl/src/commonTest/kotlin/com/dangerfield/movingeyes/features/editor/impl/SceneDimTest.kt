package com.dangerfield.movingeyes.features.editor.impl

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SceneDimTest {

    /**
     * The one that is really about the bug this replaced. Brightness defaults
     * to 1, and at 1 the app must do nothing at all: no overlay, and nothing
     * anywhere else that touches the device's own backlight.
     */
    @Test
    fun `full brightness draws no overlay`() {
        assertEquals(0f, dimOverlayAlpha(1f))
    }

    @Test
    fun `zero is fully covered`() {
        assertEquals(1f, dimOverlayAlpha(0f))
    }

    /**
     * No gear change anywhere in the range. The old split handed over from
     * backlight to overlay at a threshold, and every bug in it showed up as a
     * visible step mid-drag.
     */
    @Test
    fun `perceived output stays proportional to the slider`() {
        generateSequence(0.02f) { it + 0.02f }
            .takeWhile { it <= 1f }
            .forEach { level ->
                val perceived = 1f - dimOverlayAlpha(level)
                assertTrue(
                    abs(level - perceived) < 0.001f,
                    "at $level the scene reads as $perceived",
                )
            }
    }

    @Test
    fun `out of range input is clamped rather than trusted`() {
        assertEquals(0f, dimOverlayAlpha(4f))
        assertEquals(1f, dimOverlayAlpha(-2f))
    }
}
