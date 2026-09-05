package com.dangerfield.movingeyes.libraries.render

import com.dangerfield.movingeyes.libraries.eyes.EyeStyles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Which eyes count as a pair. Getting this wrong is visible immediately — a
 * mis-paired eye blinks with a stranger across the room — so the rule is
 * deliberately strict and these are the cases that pin it down.
 */
class EyePairingTest {

    @Test
    fun `two eyes side by side are a pair`() {
        val groups = blinkGroupsFor(listOf(eye(0.45f), eye(0.55f)))

        assertEquals(groups[0], groups[1])
    }

    @Test
    fun `a wall pairs each couple and keeps the couples apart`() {
        val groups = blinkGroupsFor(
            listOf(
                eye(0.10f, y = 0.2f), eye(0.18f, y = 0.2f),
                eye(0.60f, y = 0.7f), eye(0.68f, y = 0.7f),
            ),
        )

        assertEquals(groups[0], groups[1], "the near pair split")
        assertEquals(groups[2], groups[3], "the far pair split")
        assertNotEquals(groups[0], groups[2], "two separate pairs share a timeline")
    }

    /**
     * The case the mutual rule exists for. Three eyes in an even row: the
     * middle one is nearest to both ends, but neither end is nearest to it in
     * return except by accident. Pairing greedily would make a face and a half.
     */
    @Test
    fun `an even row does not invent a third eye into a pair`() {
        val groups = blinkGroupsFor(listOf(eye(0.30f), eye(0.50f), eye(0.70f)))

        assertTrue(groups.toSet().size >= 2, "three evenly spaced eyes were lumped together")
    }

    /**
     * Distance is measured in eye-widths, not pixels. A far pair's separation
     * is smaller in pixels than a near eye's own width, so raw distance would
     * pair a distant eye with the big one beside it.
     */
    @Test
    fun `a distant pair pairs with itself rather than with a nearby large eye`() {
        val groups = blinkGroupsFor(
            listOf(
                eye(0.40f, y = 0.8f, size = 200f),
                eye(0.52f, y = 0.8f, size = 200f),
                eye(0.60f, y = 0.3f, size = 20f),
                eye(0.63f, y = 0.3f, size = 20f),
            ),
        )

        assertEquals(groups[0], groups[1], "the near pair split")
        assertEquals(groups[2], groups[3], "the far pair split")
        assertNotEquals(groups[1], groups[2], "a far eye paired with a near one")
    }

    @Test
    fun `a lone eye still gets a group of its own`() {
        assertEquals(1, blinkGroupsFor(listOf(eye(0.5f))).size)
    }

    private fun eye(x: Float, y: Float = 0.5f, size: Float = 100f) = RenderedEye(
        style = EyeStyles.HumanBasic,
        centerX = x,
        centerY = y,
        sizePx = size,
    )
}
