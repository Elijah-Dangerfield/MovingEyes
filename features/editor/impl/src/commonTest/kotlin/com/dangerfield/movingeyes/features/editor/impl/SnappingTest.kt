package com.dangerfield.movingeyes.features.editor.impl

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Snapping is where alignment is either trustworthy or not, and it's pure
 * geometry, so it gets tested properly rather than eyeballed.
 */
class SnappingTest {

    private val canvasWidth = 1000f
    private val canvasHeight = 800f
    private val threshold = 12f

    @Test
    fun `a near miss on the canvas centre lands exactly on it`() {
        val result = resolveSnap(
            dragged = SnapCandidate(1, CanvasPoint(504f, 396f)),
            others = emptyList(),
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            thresholdPx = threshold,
        )

        assertEquals(500f, result.position.x)
        assertEquals(400f, result.position.y)
        assertEquals(2, result.guides.size, "both axes should report a guide")
    }

    @Test
    fun `a drag outside the threshold is left exactly where it was`() {
        val position = CanvasPoint(560f, 300f)
        val result = resolveSnap(
            dragged = SnapCandidate(1, position),
            others = emptyList(),
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            thresholdPx = threshold,
        )

        // Nothing is more annoying than a snap that grabs from too far away.
        assertEquals(position, result.position)
        assertTrue(result.guides.isEmpty())
    }

    @Test
    fun `axes snap independently`() {
        val result = resolveSnap(
            // On the centre line horizontally, nowhere near it vertically.
            dragged = SnapCandidate(1, CanvasPoint(503f, 120f)),
            others = emptyList(),
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            thresholdPx = threshold,
        )

        // A snap that dragged the other axis along with it would feel like the
        // app taking the eye somewhere the user didn't ask for.
        assertEquals(500f, result.position.x)
        assertEquals(120f, result.position.y)
        assertEquals(1, result.guides.size)
        assertTrue(result.guides.single() is SnapGuide.Vertical)
    }

    @Test
    fun `an eye levels with another eye`() {
        val other = SnapCandidate(2, CanvasPoint(300f, 250f))
        val result = resolveSnap(
            dragged = SnapCandidate(1, CanvasPoint(700f, 255f)),
            others = listOf(other),
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            thresholdPx = threshold,
        )

        // Level is what makes a pair read as a face.
        assertEquals(250f, result.position.y)
        assertEquals(700f, result.position.x)
        val guide = result.guides.filterIsInstance<SnapGuide.Horizontal>().single()
        assertEquals(SnapKind.OtherEye, guide.kind)
    }

    @Test
    fun `the canvas centre wins over another eye at the same distance`() {
        // An eye sitting 6px the other side of the centre line.
        val other = SnapCandidate(2, CanvasPoint(494f, 100f))
        val result = resolveSnap(
            dragged = SnapCandidate(1, CanvasPoint(497f, 300f)),
            others = listOf(other),
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            thresholdPx = threshold,
        )

        // Both are in range and the eye is marginally closer, but someone
        // reaching for the middle of the screen means the middle of the screen.
        assertEquals(500f, result.position.x)
        assertEquals(
            SnapKind.CanvasCenter,
            result.guides.filterIsInstance<SnapGuide.Vertical>().single().kind,
        )
    }

    @Test
    fun `a second pair levels itself and matches the first pair's spacing`() {
        // The flagship case: a portrait with two pairs of holes. One pair is
        // already placed 200px apart, high on the canvas.
        val pairA = SnapCandidate(2, CanvasPoint(600f, 100f))
        val pairB = SnapCandidate(3, CanvasPoint(800f, 100f))
        val partner = SnapCandidate(4, CanvasPoint(100f, 600f))

        val result = resolveSnap(
            // Roughly level with its partner, and roughly 200 away from it.
            dragged = SnapCandidate(1, CanvasPoint(306f, 603f)),
            others = listOf(pairA, pairB, partner),
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            thresholdPx = threshold,
        )

        // Both things happen at once, which is the entire point: the Y snap
        // levels the new pair, the spacing snap sets its width.
        assertEquals(600f, result.position.y, "should have levelled with its partner")

        val distance = kotlin.math.hypot(
            result.position.x - partner.center.x,
            result.position.y - partner.center.y,
        )
        assertTrue(abs(distance - 200f) < 0.5f, "expected a 200px gap, got $distance")

        assertTrue(result.guides.any { it is SnapGuide.Horizontal })
        val guide = result.guides.filterIsInstance<SnapGuide.MatchedSpacing>().single()
        assertEquals(200f, guide.distancePx, absoluteTolerance = 0.5f)
    }

    @Test
    fun `a spacing match never breaks an alignment to fix a distance`() {
        val pairA = SnapCandidate(2, CanvasPoint(600f, 100f))
        val pairB = SnapCandidate(3, CanvasPoint(800f, 100f))
        val partner = SnapCandidate(4, CanvasPoint(100f, 600f))

        val result = resolveSnap(
            // Level with the partner, but 400 away — the 200 target is simply
            // unreachable without un-levelling the pair.
            dragged = SnapCandidate(1, CanvasPoint(500f, 603f)),
            others = listOf(pairA, pairB, partner),
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            thresholdPx = threshold,
        )

        // Levelling is kept; the distance is simply left alone. Yanking the
        // eye off the guide line it just landed on would be worse than not
        // matching a spacing the user may not even have been reaching for.
        assertEquals(600f, result.position.y)
        assertEquals(500f, result.position.x)
    }

    @Test
    fun `spacing match preserves the angle the user chose`() {
        val pairA = SnapCandidate(2, CanvasPoint(100f, 100f))
        val pairB = SnapCandidate(3, CanvasPoint(300f, 100f))
        val partner = SnapCandidate(4, CanvasPoint(500f, 500f))

        // Dragged out at roughly 45°, at a distance near the pair's 200px.
        val dragged = CanvasPoint(500f + 146f, 500f + 146f)
        val result = resolveSnap(
            dragged = SnapCandidate(1, dragged),
            others = listOf(pairA, pairB, partner),
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            thresholdPx = threshold,
        )

        // The snap changes the spacing and nothing else — a match that also
        // rotated the pair would be taking a decision away from the user.
        val angleBefore = kotlin.math.atan2(dragged.y - partner.center.y, dragged.x - partner.center.x)
        val angleAfter = kotlin.math.atan2(
            result.position.y - partner.center.y,
            result.position.x - partner.center.x,
        )
        assertTrue(abs(angleBefore - angleAfter) < 0.001f, "the pair rotated during a spacing snap")
    }

    @Test
    fun `spacing match needs a pair to measure against`() {
        val partner = SnapCandidate(2, CanvasPoint(100f, 600f))

        val result = resolveSnap(
            dragged = SnapCandidate(1, CanvasPoint(306f, 600f)),
            others = listOf(partner),
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            thresholdPx = threshold,
        )

        // Two eyes on a canvas have no reference spacing, so there is nothing
        // to match and the drag must be left alone.
        assertTrue(result.guides.none { it is SnapGuide.MatchedSpacing })
    }

    /**
     * Edges align, not just middles — the rule every design tool has taught
     * people to expect. A box dragged near another's top should land on it.
     */
    @Test
    fun `an eye snaps its top edge to another eye's top edge`() {
        val other = SnapCandidate(2, CanvasPoint(300f, 200f), halfWidth = 40f, halfHeight = 30f)

        val result = resolveSnap(
            // Top at 174, four short of the other's 170.
            dragged = SnapCandidate(1, CanvasPoint(600f, 224f), halfWidth = 25f, halfHeight = 50f),
            others = listOf(other),
            canvasWidth = 1000f,
            canvasHeight = 1000f,
            thresholdPx = 10f,
        )

        assertEquals(220f, result.position.y, "tops did not meet")
    }

    /**
     * What "the same size and lined up" looks like without a badge for it: two
     * equal boxes on a shared middle also share their tops and bottoms, so all
     * three guides light at once and say so.
     */
    @Test
    fun `two equal eyes on one middle draw all three guides`() {
        val other = SnapCandidate(2, CanvasPoint(300f, 400f), halfWidth = 40f, halfHeight = 30f)

        val result = resolveSnap(
            dragged = SnapCandidate(1, CanvasPoint(600f, 403f), halfWidth = 40f, halfHeight = 30f),
            others = listOf(other),
            canvasWidth = 1000f,
            canvasHeight = 1000f,
            thresholdPx = 10f,
        )

        val levels = result.guides.filterIsInstance<SnapGuide.Horizontal>().map { it.y }.toSet()
        assertEquals(setOf(370f, 400f, 430f), levels, "expected top, middle and bottom")
    }

    /** Different heights on a shared middle line up on the middle alone —
     *  anything more would be claiming an alignment that isn't there. */
    @Test
    fun `eyes of different heights draw only the middle guide`() {
        val other = SnapCandidate(2, CanvasPoint(300f, 400f), halfWidth = 40f, halfHeight = 30f)

        val result = resolveSnap(
            dragged = SnapCandidate(1, CanvasPoint(600f, 400f), halfWidth = 40f, halfHeight = 80f),
            others = listOf(other),
            canvasWidth = 1000f,
            canvasHeight = 1000f,
            thresholdPx = 10f,
        )

        val levels = result.guides.filterIsInstance<SnapGuide.Horizontal>().map { it.y }.toSet()
        assertEquals(setOf(400f), levels, "an alignment was claimed that isn't there")
    }

    /** A guide spans the things it aligns rather than the whole canvas, so in a
     *  crowded scene it points at the two that matter. */
    @Test
    fun `a guide between two eyes spans only those eyes`() {
        val other = SnapCandidate(2, CanvasPoint(300f, 400f), halfWidth = 40f, halfHeight = 30f)

        val result = resolveSnap(
            dragged = SnapCandidate(1, CanvasPoint(600f, 400f), halfWidth = 40f, halfHeight = 30f),
            others = listOf(other),
            canvasWidth = 1000f,
            canvasHeight = 1000f,
            thresholdPx = 10f,
        )

        val guide = result.guides.filterIsInstance<SnapGuide.Horizontal>().first { it.y == 400f }
        assertEquals(260f, guide.from)
        assertEquals(640f, guide.to)
    }

    /** Resizing gets the same assistance as moving: near an existing size, it
     *  lands on it exactly. */
    @Test
    fun `a resize lands on a neighbour's size`() {
        val other = SnapCandidate(2, CanvasPoint(300f, 200f), halfWidth = 60f, halfHeight = 40f)

        val snap = resolveSizeSnap(sizePx = 124f, others = listOf(other), thresholdPx = 10f)

        assertEquals(120f, snap.sizePx)
        assertEquals(other, snap.matched)
    }

    /** And leaves a deliberate difference alone, or you could never make two
     *  eyes nearly-but-not-quite the same on purpose. */
    @Test
    fun `a resize well clear of any neighbour is untouched`() {
        val other = SnapCandidate(2, CanvasPoint(300f, 200f), halfWidth = 60f, halfHeight = 40f)

        val snap = resolveSizeSnap(sizePx = 200f, others = listOf(other), thresholdPx = 10f)

        assertEquals(200f, snap.sizePx)
        assertEquals(null, snap.matched)
    }

    /** With several to choose from it takes the nearest, not the first. */
    @Test
    fun `a resize takes the closest of several neighbours`() {
        val near = SnapCandidate(2, CanvasPoint(0f, 0f), halfWidth = 61f)
        val far = SnapCandidate(3, CanvasPoint(0f, 0f), halfWidth = 65f)

        val snap = resolveSizeSnap(sizePx = 124f, others = listOf(far, near), thresholdPx = 20f)

        assertEquals(122f, snap.sizePx)
    }

    @Test
    fun `rotation clicks to fifteen degree detents`() {
        assertEquals(0f, snapRotation(2f))
        assertEquals(15f, snapRotation(13f))
        assertEquals(90f, snapRotation(92f))
        assertEquals(180f, snapRotation(178f))
    }

    @Test
    fun `rotation holds any angle once you push past a detent`() {
        // A picture rail is sometimes at 7°, and the user has to be able to
        // say so.
        assertEquals(7f, snapRotation(7f))
        assertEquals(52f, snapRotation(52f))
    }

    @Test
    fun `rotation normalises past a full turn`() {
        assertEquals(0f, snapRotation(361f))
        assertEquals(345f, snapRotation(-15f))
    }
}
