package com.dangerfield.movingeyes.libraries.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Where a released sheet lands. Getting this wrong is the difference between a
 * gesture that feels answered and one that feels ignored.
 *
 * Measured in pixels of exposure, the same unit the canvas scales against: 0 is
 * gone, [Header] is the tab strip alone, [Extent] is fully open.
 */
class PanelSettleTest {

    private fun settle(exposurePx: Float, velocityPx: Float) =
        settleTarget(exposurePx, velocityPx, headerPx = Header, extentPx = Extent)

    @Test
    fun `a slow release goes wherever it is closest to`() {
        assertEquals(PanelPosition.Hidden, settle(exposurePx = 20f, velocityPx = 0f))
        assertEquals(PanelPosition.Collapsed, settle(exposurePx = 150f, velocityPx = 0f))
        assertEquals(PanelPosition.Expanded, settle(exposurePx = 700f, velocityPx = 0f))
    }

    /** The property that makes a flick feel answered: a fast short swipe must
     *  not leave the sheet stuck where it started. */
    @Test
    fun `a flick decides regardless of how far it travelled`() {
        assertEquals(PanelPosition.Hidden, settle(exposurePx = Extent - 5f, velocityPx = 3000f))
        assertEquals(PanelPosition.Expanded, settle(exposurePx = 5f, velocityPx = -3000f))
    }

    /**
     * A thrown-away panel goes away rather than resting on its header. Stopping
     * at collapsed would mean a hard downward swipe leaves a strip of chrome
     * behind, which is not what throwing something away looks like.
     */
    @Test
    fun `a downward flick dismisses rather than collapsing`() {
        assertEquals(PanelPosition.Hidden, settle(exposurePx = Extent, velocityPx = 3000f))
    }

    @Test
    fun `a gentle nudge is not a flick`() {
        assertEquals(PanelPosition.Collapsed, settle(exposurePx = Header, velocityPx = 100f))
        assertEquals(PanelPosition.Expanded, settle(exposurePx = Extent, velocityPx = -100f))
    }

    @Test
    fun `the midpoint between two rests tips to the nearer one`() {
        assertEquals(PanelPosition.Hidden, settle(exposurePx = Header / 2f - 1f, velocityPx = 0f))
        assertEquals(PanelPosition.Collapsed, settle(exposurePx = Header / 2f + 1f, velocityPx = 0f))
    }

    private companion object {
        const val Header = 120f
        const val Extent = 900f
    }
}
