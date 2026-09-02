package com.dangerfield.movingeyes.libraries.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Where a released sheet lands. Getting this wrong is the difference between a
 * gesture that feels answered and one that feels ignored.
 */
class PanelSettleTest {

    @Test
    fun `a slow release goes wherever it is closest to`() {
        assertEquals(0f, settleTarget(travel = 0.2f, velocityPx = 0f))
        assertEquals(1f, settleTarget(travel = 0.8f, velocityPx = 0f))
    }

    /** The property that makes a flick feel answered: a fast short swipe must
     *  not leave the sheet stuck where it started. */
    @Test
    fun `a flick decides regardless of how far it travelled`() {
        assertEquals(1f, settleTarget(travel = 0.05f, velocityPx = 3000f))
        assertEquals(0f, settleTarget(travel = 0.95f, velocityPx = -3000f))
    }

    @Test
    fun `a gentle nudge is not a flick`() {
        assertEquals(0f, settleTarget(travel = 0.1f, velocityPx = 100f))
        assertEquals(1f, settleTarget(travel = 0.9f, velocityPx = -100f))
    }

    @Test
    fun `the midpoint tips to collapsed`() {
        assertEquals(0f, settleTarget(travel = 0.5f, velocityPx = 0f))
        assertEquals(1f, settleTarget(travel = 0.51f, velocityPx = 0f))
    }
}
