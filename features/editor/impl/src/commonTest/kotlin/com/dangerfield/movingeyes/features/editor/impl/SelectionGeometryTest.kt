package com.dangerfield.movingeyes.features.editor.impl

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Group rotation and alignment are where a wrong pivot silently ruins a
 * composition — the eyes still look fine, they're just no longer where the
 * holes are. Pure geometry, so it gets asserted rather than eyeballed.
 */
class SelectionGeometryTest {

    private val tolerance = 0.001f

    @Test
    fun `the centroid of a pair is the point between them`() {
        val centre = centroidOf(listOf(CanvasPoint(100f, 200f), CanvasPoint(300f, 400f)))

        assertNear(200f, centre.x)
        assertNear(300f, centre.y)
    }

    @Test
    fun `rotating by zero changes nothing`() {
        val points = listOf(CanvasPoint(10f, 20f), CanvasPoint(30f, 40f))

        val turned = rotateAbout(points, centroidOf(points), 0f)

        assertNear(points[0].x, turned[0].x)
        assertNear(points[1].y, turned[1].y)
    }

    /**
     * The load-bearing property of a Group turn: it's rigid. If the distance
     * between two eyes changes when you rotate them, an inter-pupil spacing the
     * user measured against a face has just been destroyed.
     */
    @Test
    fun `a group rotation preserves the spacing between eyes`() {
        val points = listOf(CanvasPoint(100f, 300f), CanvasPoint(340f, 300f))
        val before = distance(points[0], points[1])

        listOf(7f, 15f, 90f, 180f, -33f).forEach { degrees ->
            val turned = rotateAbout(points, centroidOf(points), degrees)
            assertNear(before, distance(turned[0], turned[1]), "at $degrees°")
        }
    }

    @Test
    fun `a group rotation keeps the centroid where it was`() {
        val points = listOf(
            CanvasPoint(100f, 100f),
            CanvasPoint(300f, 140f),
            CanvasPoint(200f, 400f),
        )
        val pivot = centroidOf(points)

        val turned = rotateAbout(points, pivot, 42f)

        val after = centroidOf(turned)
        assertNear(pivot.x, after.x)
        assertNear(pivot.y, after.y)
    }

    @Test
    fun `a quarter turn moves a point a quarter of the way round`() {
        val turned = rotateAbout(
            points = listOf(CanvasPoint(100f, 0f)),
            pivot = CanvasPoint(0f, 0f),
            degrees = 90f,
        )

        assertNear(0f, turned[0].x)
        assertNear(100f, turned[0].y)
    }

    @Test
    fun `four quarter turns return to the start`() {
        val points = listOf(CanvasPoint(37f, 91f))
        val pivot = CanvasPoint(120f, 80f)

        var turned = points
        repeat(4) { turned = rotateAbout(turned, pivot, 90f) }

        assertNear(points[0].x, turned[0].x)
        assertNear(points[0].y, turned[0].y)
    }

    /**
     * The endpoints stay put deliberately — they're the ones most likely placed
     * against something real, and an align that moves everything is a
     * destructive surprise rather than a tidy-up.
     */
    @Test
    fun `spacing evenly leaves the endpoints alone and evens out the middle`() {
        val points = listOf(
            CanvasPoint(0f, 100f),
            CanvasPoint(30f, 100f),
            CanvasPoint(90f, 100f),
            CanvasPoint(300f, 100f),
        )

        val spaced = distributeEvenly(points)

        assertNear(0f, spaced.first().x)
        assertNear(300f, spaced.last().x)
        assertNear(100f, spaced[1].x)
        assertNear(200f, spaced[2].x)
    }

    @Test
    fun `spacing evenly works down the vertical axis too`() {
        val points = listOf(
            CanvasPoint(50f, 0f),
            CanvasPoint(50f, 10f),
            CanvasPoint(50f, 200f),
        )

        val spaced = distributeEvenly(points)

        assertNear(100f, spaced[1].y)
    }

    /** The caller's list is indexed against the selection, so re-spacing must
     *  not reorder it even though it sorts internally. */
    @Test
    fun `spacing evenly preserves the caller's ordering`() {
        val points = listOf(
            CanvasPoint(300f, 0f),
            CanvasPoint(0f, 0f),
            CanvasPoint(40f, 0f),
        )

        val spaced = distributeEvenly(points)

        // Index 0 was the rightmost and must still be the rightmost.
        assertNear(300f, spaced[0].x)
        assertNear(0f, spaced[1].x)
        assertNear(150f, spaced[2].x)
    }

    @Test
    fun `spacing fewer than three points does nothing`() {
        val pair = listOf(CanvasPoint(0f, 0f), CanvasPoint(100f, 0f))

        assertEquals(pair, distributeEvenly(pair))
        assertEquals(emptyList(), distributeEvenly(emptyList()))
    }

    @Test
    fun `mirroring flips about the selection's own centre, not the canvas`() {
        val points = listOf(CanvasPoint(100f, 50f), CanvasPoint(200f, 50f), CanvasPoint(120f, 90f))

        val mirrored = mirrorHorizontally(points)

        assertNear(200f, mirrored[0].x)
        assertNear(100f, mirrored[1].x)
        assertNear(180f, mirrored[2].x)
        assertNear(90f, mirrored[2].y, "mirroring must not move anything vertically")
    }

    @Test
    fun `mirroring twice is the identity`() {
        val points = listOf(CanvasPoint(11f, 3f), CanvasPoint(97f, 3f), CanvasPoint(40f, 60f))

        val there = mirrorHorizontally(points)
        val back = mirrorHorizontally(there)

        points.forEachIndexed { index, point -> assertNear(point.x, back[index].x) }
    }

    private fun distance(a: CanvasPoint, b: CanvasPoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun assertNear(expected: Float, actual: Float, hint: String = "") {
        assertTrue(
            abs(expected - actual) < tolerance,
            "expected $expected but was $actual${if (hint.isEmpty()) "" else " ($hint)"}",
        )
    }
}
