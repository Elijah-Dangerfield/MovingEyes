package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Handles are drawn, so they promise they can be dragged. These are the sums
 * behind that promise.
 */
class SelectionHandlesTest {

    private val bounds = Rect(left = 100f, top = 200f, right = 300f, bottom = 400f)
    private val radius = 30f
    private val gap = 40f

    @Test
    fun `a touch in open space grabs nothing`() {
        assertNull(handleAt(Offset(200f, 300f), bounds, radius, gap))
    }

    @Test
    fun `each corner is grabbable`() {
        listOf(
            bounds.topLeft,
            Offset(bounds.right, bounds.top),
            Offset(bounds.left, bounds.bottom),
            Offset(bounds.right, bounds.bottom),
        ).forEach { corner ->
            assertIs<SelectionHandle.Corner>(handleAt(corner, bounds, radius, gap), "at $corner")
        }
    }

    /** The anchor is what stays put, so it has to be the opposite corner. */
    @Test
    fun `a corner anchors to the one diagonally opposite`() {
        val handle = handleAt(bounds.topLeft, bounds, radius, gap)

        assertIs<SelectionHandle.Corner>(handle)
        assertEquals(Offset(bounds.right, bounds.bottom), handle.anchor)
    }

    @Test
    fun `the rotate handle is grabbable above the box`() {
        val handle = handleAt(rotateHandleCenter(bounds, gap), bounds, radius, gap)

        assertIs<SelectionHandle.Rotate>(handle)
        assertEquals(bounds.center, handle.pivot)
    }

    /** The drawn handle is 14dp but the finger is nearer 50, so a near miss
     *  still counts — dragging the whole selection by accident costs an
     *  alignment. */
    @Test
    fun `a near miss still counts`() {
        val nearlyCorner = bounds.topLeft + Offset(radius * 0.6f, radius * 0.6f)

        assertIs<SelectionHandle.Corner>(handleAt(nearlyCorner, bounds, radius, gap))
    }

    @Test
    fun `dragging a corner away from the anchor grows the selection`() {
        val anchor = Offset(0f, 0f)

        val factor = cornerScale(anchor, from = Offset(100f, 0f), to = Offset(150f, 0f))

        assertTrue(factor > 1f)
        assertNear(1.5f, factor)
    }

    @Test
    fun `dragging a corner toward the anchor shrinks it`() {
        val factor = cornerScale(Offset(0f, 0f), from = Offset(100f, 0f), to = Offset(50f, 0f))

        assertNear(0.5f, factor)
    }

    /** Distance from the anchor, not one axis, so an eye never stretches. */
    @Test
    fun `scale is proportional in both directions`() {
        val diagonal = cornerScale(Offset(0f, 0f), Offset(30f, 40f), Offset(60f, 80f))

        assertNear(2f, diagonal)
    }

    @Test
    fun `a degenerate drag does nothing rather than exploding`() {
        assertEquals(1f, cornerScale(Offset(0f, 0f), Offset(0f, 0f), Offset(100f, 100f)))
    }

    @Test
    fun `one jumpy frame cannot collapse or explode the selection`() {
        assertTrue(cornerScale(Offset(0f, 0f), Offset(100f, 0f), Offset(9999f, 0f)) <= 2f)
        assertTrue(cornerScale(Offset(0f, 0f), Offset(100f, 0f), Offset(0.01f, 0f)) >= 0.5f)
    }

    @Test
    fun `rotation reports the angle swept about the pivot`() {
        val pivot = Offset(0f, 0f)

        assertNear(90f, rotationBetween(pivot, Offset(100f, 0f), Offset(0f, 100f)))
        assertNear(-90f, rotationBetween(pivot, Offset(0f, 100f), Offset(100f, 0f)))
    }

    /** Crossing the seam must not spin the selection most of the way back. */
    @Test
    fun `rotation takes the short way round the seam`() {
        val pivot = Offset(0f, 0f)
        val justBelow = Offset(-100f, -1f)
        val justAbove = Offset(-100f, 1f)

        val degrees = rotationBetween(pivot, justBelow, justAbove)

        assertTrue(abs(degrees) < 5f, "swept $degrees° across the seam")
    }

    @Test
    fun `no movement is no rotation`() {
        assertEquals(0f, rotationBetween(Offset(0f, 0f), Offset(50f, 50f), Offset(50f, 50f)))
    }

    private fun assertNear(expected: Float, actual: Float) {
        assertTrue(abs(expected - actual) < 0.01f, "expected $expected but was $actual")
    }
}
