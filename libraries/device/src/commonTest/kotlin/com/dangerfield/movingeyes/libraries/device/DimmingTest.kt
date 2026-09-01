package com.dangerfield.movingeyes.libraries.device

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The dim split is what lets a scene go darker than the OS allows, and it is
 * the one piece of the appliance that has no platform code to blame when it
 * looks wrong.
 */
class DimmingTest {

    @Test
    fun `full brightness uses the backlight alone`() {
        val levels = dimLevelsFor(1f)

        assertEquals(1f, levels.osBrightness)
        assertEquals(0f, levels.overlayAlpha)
    }

    @Test
    fun `above the threshold the overlay stays out of the way`() {
        listOf(0.3f, 0.5f, 0.9f).forEach { level ->
            val levels = dimLevelsFor(level)

            assertEquals(level, levels.osBrightness, "at $level")
            assertEquals(0f, levels.overlayAlpha, "at $level")
        }
    }

    @Test
    fun `below the threshold the backlight holds and the overlay takes over`() {
        val levels = dimLevelsFor(SoftwareDimBelow / 2f)

        assertEquals(SoftwareDimBelow, levels.osBrightness)
        assertNear(0.5f, levels.overlayAlpha)
    }

    /**
     * The property that matters to the eye: dragging the slider through the
     * handover point must not produce a visible step. Perceived output is
     * backlight times what the overlay lets through, and it has to stay
     * proportional to the requested level on both sides of the join.
     */
    @Test
    fun `perceived brightness stays proportional across the handover`() {
        generateSequence(0.02f) { it + 0.02f }
            .takeWhile { it <= 1f }
            .forEach { level ->
                val levels = dimLevelsFor(level)
                val perceived = levels.osBrightness * (1f - levels.overlayAlpha)

                assertNear(level, perceived, "at $level")
            }
    }

    /**
     * Continuity is a property of what the eye sees, not of either term on its
     * own: the backlight deliberately jumps from tracking the level to holding
     * at the threshold, and the overlay picks up exactly that difference.
     */
    @Test
    fun `the handover point itself is continuous`() {
        val below = dimLevelsFor(SoftwareDimBelow - 0.001f)
        val above = dimLevelsFor(SoftwareDimBelow + 0.001f)

        val perceivedBelow = below.osBrightness * (1f - below.overlayAlpha)
        val perceivedAbove = above.osBrightness * (1f - above.overlayAlpha)

        // The two probes are 0.002 apart in input, so perceived output should
        // differ by about that and not by a jump.
        assertTrue(
            abs(perceivedAbove - perceivedBelow) < 0.01f,
            "a visible step at the join: $perceivedBelow then $perceivedAbove",
        )
        assertTrue(below.overlayAlpha < 0.01f, "the overlay jumps in too hard")
    }

    @Test
    fun `zero is fully covered rather than a black backlight`() {
        val levels = dimLevelsFor(0f)

        assertEquals(SoftwareDimBelow, levels.osBrightness)
        assertEquals(1f, levels.overlayAlpha)
    }

    @Test
    fun `out of range input is clamped rather than trusted`() {
        assertEquals(1f, dimLevelsFor(4f).osBrightness)
        assertEquals(0f, dimLevelsFor(4f).overlayAlpha)
        assertEquals(1f, dimLevelsFor(-2f).overlayAlpha)
    }

    private fun assertNear(expected: Float, actual: Float, hint: String = "") {
        assertTrue(
            abs(expected - actual) < 0.001f,
            "expected $expected but was $actual${if (hint.isEmpty()) "" else " ($hint)"}",
        )
    }
}
