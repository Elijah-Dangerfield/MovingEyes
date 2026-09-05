@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.features.editor.impl

import kotlin.math.abs
import kotlin.math.hypot

/** A point on the canvas, in pixels. Editor-local so this file stays testable
 *  without Compose. */
data class CanvasPoint(val x: Float, val y: Float)

/**
 * One eye's position and extent, as far as snapping cares.
 *
 * The half-extents default to zero so a caller that only cares about centres —
 * spacing matching, most tests — needn't invent a size. An eye with no extent
 * simply has its three anchors on top of each other, which degrades to
 * centre-only snapping rather than misbehaving.
 */
data class SnapCandidate(
    val id: Int,
    val center: CanvasPoint,
    val halfWidth: Float = 0f,
    val halfHeight: Float = 0f,
)

/**
 * The three places on a box an alignment can happen, on either axis.
 *
 * Following Figma and every design tool since: things line up by their edges as
 * readily as by their middles, and which one you meant is usually obvious from
 * which one you dragged toward.
 */
enum class SnapAnchor(val scale: Float) {
    Start(-1f),
    Middle(0f),
    End(1f),
}

/**
 * What the drag landed on, and what to draw because of it.
 *
 * Guides exist so a snap is *legible* — a position that quietly changes under
 * your finger with no explanation reads as the app fighting you.
 */
sealed interface SnapGuide {
    /**
     * Aligned to something's X. Drawn as a vertical line at [x], spanning
     * [from]..[to] — the extent of the things that actually line up, not the
     * whole canvas. In a scene with fourteen eyes a full-height rule says
     * "something here is aligned" and leaves you to work out what.
     */
    data class Vertical(
        val x: Float,
        val kind: SnapKind,
        val from: Float = Float.NaN,
        val to: Float = Float.NaN,
    ) : SnapGuide

    /** Aligned to something's Y. Drawn as a horizontal line at [y], spanning
     *  [from]..[to]. */
    data class Horizontal(
        val y: Float,
        val kind: SnapKind,
        val from: Float = Float.NaN,
        val to: Float = Float.NaN,
    ) : SnapGuide

    /**
     * Matched the spacing of an existing pair. Drawn as **two** measure bars
     * at once — the pair you matched and the pair you're making — because
     * "these two gaps are now the same" is not something a single line can
     * say.
     */
    /**
     * Reached the same size as an existing eye. Drawn as **two** width bars,
     * the same way [MatchedSpacing] draws two measure bars: "these are now the
     * same size" is a statement about a pair, and a marker on only the one
     * being dragged would be a statement about one.
     */
    data class MatchedSize(
        val fromA: CanvasPoint,
        val toA: CanvasPoint,
        val fromB: CanvasPoint,
        val toB: CanvasPoint,
    ) : SnapGuide

    data class MatchedSpacing(
        val distancePx: Float,
        val fromA: CanvasPoint,
        val toA: CanvasPoint,
        val fromB: CanvasPoint,
        val toB: CanvasPoint,
    ) : SnapGuide
}

enum class SnapKind {
    /** The middle of the screen. */
    CanvasCenter,

    /** Another eye's centre line. */
    OtherEye,
}

data class SnapResult(
    val position: CanvasPoint,
    val guides: List<SnapGuide>,
) {
    val snapped: Boolean get() = guides.isNotEmpty()
}

/**
 * Where a dragged eye should actually land.
 *
 * Snapping does most of the alignment work in this app, which is why it earns
 * three targets rather than one:
 *
 *  1. **The canvas centre lines.** Centred is the single most common thing
 *     anyone wants and the hardest to hit by hand.
 *  2. **Other eyes' edges and centre lines.** Two eyes level with each other is
 *     what makes a pair read as a face rather than two things — and level can
 *     mean tops, middles or bottoms, so all three are candidates, matched
 *     against all three of the dragged eye's. Following Figma, which everyone
 *     has already learned.
 *  3. **A spacing that matches an existing pair.** The flagship case is a
 *     portrait with two holes cut in it; the second pair of eyes has to match
 *     the first, and matching a distance by eye at arm's length is genuinely
 *     hard.
 *
 * Axes are resolved independently, so a drag can snap horizontally while
 * staying free vertically — which is what makes it feel like assistance rather
 * than a magnet.
 *
 * Everything is pure: same inputs, same answer, no Compose, no state.
 */
fun resolveSnap(
    dragged: SnapCandidate,
    others: List<SnapCandidate>,
    canvasWidth: Float,
    canvasHeight: Float,
    thresholdPx: Float,
): SnapResult {
    val guides = mutableListOf<SnapGuide>()

    val canvasCenterX = canvasWidth / 2f
    val canvasCenterY = canvasHeight / 2f

    // Canvas centre wins over another eye when both are in range: someone
    // reaching for the middle of the screen means the middle of the screen.
    // Each candidate is the *centre* the dragged eye would need to sit at for
    // some anchor of it to meet some anchor of a target. Resolving in centre
    // coordinates keeps the two axes independent and the arithmetic in one
    // place.
    val xTargets = buildList {
        add(canvasCenterX to SnapKind.CanvasCenter)
        others.forEach { other ->
            SnapAnchor.entries.forEach { theirs ->
                val at = other.center.x + theirs.scale * other.halfWidth
                SnapAnchor.entries.forEach { ours ->
                    add(at - ours.scale * dragged.halfWidth to SnapKind.OtherEye)
                }
            }
        }
    }
    val yTargets = buildList {
        add(canvasCenterY to SnapKind.CanvasCenter)
        others.forEach { other ->
            SnapAnchor.entries.forEach { theirs ->
                val at = other.center.y + theirs.scale * other.halfHeight
                SnapAnchor.entries.forEach { ours ->
                    add(at - ours.scale * dragged.halfHeight to SnapKind.OtherEye)
                }
            }
        }
    }

    val snappedX = nearest(dragged.center.x, xTargets, thresholdPx)
    val snappedY = nearest(dragged.center.y, yTargets, thresholdPx)

    var position = CanvasPoint(
        x = snappedX?.first ?: dragged.center.x,
        y = snappedY?.first ?: dragged.center.y,
    )

    // Guides describe the position that was *reached*, not the one candidate
    // that got it there. Two eyes of equal height whose middles meet also have
    // their tops and bottoms meeting, and drawing all three is how a design
    // tool says "these are the same size and lined up" without a badge for it.
    guides += alignmentGuides(dragged, position, others, canvasCenterX, canvasCenterY)

    // Spacing runs *after* the axis snaps and on whatever freedom they left,
    // because the two are usually orthogonal rather than competing. The
    // flagship case makes this obvious: a second pair of eyes wants to be
    // level with itself (a Y snap onto its partner) **and** spaced to match
    // the first pair (an X adjustment). Treating spacing as an alternative to
    // axis snapping would mean the common case never gets both.
    val freeAxis = when {
        snappedX != null && snappedY != null -> null
        snappedX != null -> Axis.Y
        snappedY != null -> Axis.X
        else -> Axis.Both
    }

    if (freeAxis != null) {
        matchSpacing(position, others, thresholdPx, freeAxis)?.let { match ->
            position = match.first
            guides += match.second
        }
    }

    return SnapResult(position, guides)
}

/**
 * Every alignment true at [position], as something to draw.
 *
 * Tolerance is a hair rather than the snap threshold: this answers "what is
 * actually lined up", and reporting near-misses as lines would mean guides that
 * appear before anything has snapped.
 */
private fun alignmentGuides(
    dragged: SnapCandidate,
    position: CanvasPoint,
    others: List<SnapCandidate>,
    canvasCenterX: Float,
    canvasCenterY: Float,
): List<SnapGuide> = buildList {
    if (abs(position.x - canvasCenterX) < GuideTolerancePx) {
        add(SnapGuide.Vertical(canvasCenterX, SnapKind.CanvasCenter))
    }
    if (abs(position.y - canvasCenterY) < GuideTolerancePx) {
        add(SnapGuide.Horizontal(canvasCenterY, SnapKind.CanvasCenter))
    }

    others.forEach { other ->
        SnapAnchor.entries.forEach { ours ->
            val ourX = position.x + ours.scale * dragged.halfWidth
            val ourY = position.y + ours.scale * dragged.halfHeight

            SnapAnchor.entries.forEach { theirs ->
                if (abs(ourX - (other.center.x + theirs.scale * other.halfWidth)) < GuideTolerancePx) {
                    add(
                        SnapGuide.Vertical(
                            x = ourX,
                            kind = SnapKind.OtherEye,
                            from = minOf(position.y - dragged.halfHeight, other.center.y - other.halfHeight),
                            to = maxOf(position.y + dragged.halfHeight, other.center.y + other.halfHeight),
                        ),
                    )
                }
                if (abs(ourY - (other.center.y + theirs.scale * other.halfHeight)) < GuideTolerancePx) {
                    add(
                        SnapGuide.Horizontal(
                            y = ourY,
                            kind = SnapKind.OtherEye,
                            from = minOf(position.x - dragged.halfWidth, other.center.x - other.halfWidth),
                            to = maxOf(position.x + dragged.halfWidth, other.center.x + other.halfWidth),
                        ),
                    )
                }
            }
        }
    }
}.distinct()

/** Close enough to call aligned. Sub-pixel, so only a real snap draws a line. */
private const val GuideTolerancePx = 0.5f

private fun nearest(
    value: Float,
    targets: List<Pair<Float, SnapKind>>,
    thresholdPx: Float,
): Pair<Float, SnapKind>? = targets
    .filter { abs(it.first - value) <= thresholdPx }
    // Canvas centre first on a tie, then whichever is closest.
    .minWithOrNull(compareBy({ abs(it.first - value) }, { it.second.ordinal }))

/** Which directions the spacing snap is still allowed to move in. */
private enum class Axis { X, Y, Both }

/**
 * If the dragged eye's distance to its nearest neighbour is close to the
 * distance between two other eyes, pull it onto that exact distance.
 *
 * Needs at least three other eyes: one to pair with, and two to measure
 * against. [free] limits which way it may move, so a snap that has already
 * levelled the pair can't un-level it to fix the spacing.
 */
private fun matchSpacing(
    position: CanvasPoint,
    others: List<SnapCandidate>,
    thresholdPx: Float,
    free: Axis,
): Pair<CanvasPoint, SnapGuide.MatchedSpacing>? {
    if (others.size < 3) return null

    val partner = others.minByOrNull { distance(position, it.center) } ?: return null
    val currentDistance = distance(position, partner.center)
    if (currentDistance < MinimumPairDistancePx) return null

    val rest = others.filter { it.id != partner.id }
    var best: Triple<Float, SnapCandidate, SnapCandidate>? = null

    for (i in rest.indices) {
        for (j in i + 1 until rest.size) {
            val d = distance(rest[i].center, rest[j].center)
            if (abs(d - currentDistance) > thresholdPx) continue
            if (best == null || abs(d - currentDistance) < abs(best.first - currentDistance)) {
                best = Triple(d, rest[i], rest[j])
            }
        }
    }

    val (targetDistance, a, b) = best ?: return null

    val snapped = when (free) {
        // Slide along the line from the partner, so the match changes the
        // spacing and nothing else — the angle the user chose is preserved.
        Axis.Both -> {
            val scale = targetDistance / currentDistance
            CanvasPoint(
                x = partner.center.x + (position.x - partner.center.x) * scale,
                y = partner.center.y + (position.y - partner.center.y) * scale,
            )
        }

        // One axis is already pinned by a guide. Solve for the other so the
        // distance comes out right without disturbing the alignment: the
        // pinned offset is one leg of the triangle, the target distance the
        // hypotenuse.
        Axis.X -> solveAlongAxis(
            pinned = position.y - partner.center.y,
            free = position.x - partner.center.x,
            target = targetDistance,
        )?.let { CanvasPoint(partner.center.x + it, position.y) }

        Axis.Y -> solveAlongAxis(
            pinned = position.x - partner.center.x,
            free = position.y - partner.center.y,
            target = targetDistance,
        )?.let { CanvasPoint(position.x, partner.center.y + it) }
    } ?: return null

    return snapped to SnapGuide.MatchedSpacing(
        distancePx = targetDistance,
        fromA = a.center,
        toA = b.center,
        fromB = partner.center,
        toB = snapped,
    )
}

private fun distance(a: CanvasPoint, b: CanvasPoint): Float = hypot(b.x - a.x, b.y - a.y)

/**
 * The remaining leg of a right triangle, keeping the sign of the current one
 * so the eye stays on the side of its partner the user put it on.
 *
 * Null when [target] is shorter than the pinned leg — the requested distance
 * simply isn't reachable without breaking the alignment, and breaking it is
 * not on offer.
 */
private fun solveAlongAxis(pinned: Float, free: Float, target: Float): Float? {
    val squared = target * target - pinned * pinned
    if (squared <= 0f) return null
    val magnitude = kotlin.math.sqrt(squared)
    return if (free < 0f) -magnitude else magnitude
}

/** Below this, two eyes are effectively on top of each other and "the spacing
 *  between them" isn't a meaningful thing to match. */
private const val MinimumPairDistancePx = 8f

/**
 * Rotation snapping: 15° detents, but any angle is reachable by continuing
 * past one.
 *
 * The detents exist because a portrait on a wall is almost never at an
 * arbitrary angle, and holding a two-finger twist steady at exactly 0° is
 * hard. Being able to leave them matters just as much: a picture rail is
 * sometimes at 7°.
 */
fun snapRotation(degrees: Float, thresholdDegrees: Float = 4f): Float {
    val normalized = ((degrees % 360f) + 360f) % 360f
    val detent = (normalized / DetentDegrees).toInt() * DetentDegrees
    val nearestDetent = if (normalized - detent > DetentDegrees / 2f) {
        detent + DetentDegrees
    } else {
        detent
    }
    return if (abs(normalized - nearestDetent) <= thresholdDegrees) {
        nearestDetent % 360f
    } else {
        normalized
    }
}

private const val DetentDegrees = 15f

/** The size a resize should actually land on, and what it matched. */
data class SizeSnap(val sizePx: Float, val matched: SnapCandidate?)

/**
 * Pulls a resize onto another eye's size when it gets close.
 *
 * Matching sizes by eye is the same problem as matching a distance by eye and
 * has the same answer: the app knows the number, so it should offer it. Without
 * this a resize is the one gesture in the editor with no assistance and no
 * feedback at all — you drag a corner, a figure changes, and nothing ever tells
 * you that you have arrived anywhere.
 *
 * Matches on width, since every eye's height follows from its style's aspect
 * ratio. Two eyes of the same width and different styles are *not* the same
 * size, and pretending otherwise would snap them to a lie.
 */
fun resolveSizeSnap(
    sizePx: Float,
    others: List<SnapCandidate>,
    thresholdPx: Float,
): SizeSnap {
    val match = others
        .filter { abs(it.halfWidth * 2f - sizePx) <= thresholdPx }
        .minByOrNull { abs(it.halfWidth * 2f - sizePx) }
        ?: return SizeSnap(sizePx, null)

    return SizeSnap(match.halfWidth * 2f, match)
}
