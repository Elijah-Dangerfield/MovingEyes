package com.dangerfield.movingeyes.features.editor.impl

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Geometry for operations on a whole selection, in canvas pixels.
 *
 * Kept pure and away from [EditorState] because this is where an off-by-one in
 * a pivot or a mishandled aspect ratio silently ruins an alignment, and that
 * class of bug is invisible on screen until someone has already cut the
 * cardboard.
 */

/**
 * Which of the two rotations the user means, and the distinction v2 spends a
 * paragraph on because it is genuinely two different intentions.
 */
enum class RotationMode {
    /**
     * The selection turns as one rigid body: eyes orbit the shared centre and
     * each also turns. Turning a pair to match a portrait hung at 7° is this.
     */
    Group,

    /**
     * Each eye spins in place and nothing moves. Making two eyes squint at each
     * other is this, and doing it as a Group instead would swing them apart.
     */
    Each,
}

/** Centroid of a set of points. The pivot for a [RotationMode.Group] turn. */
fun centroidOf(points: List<CanvasPoint>): CanvasPoint {
    if (points.isEmpty()) return CanvasPoint(0f, 0f)
    return CanvasPoint(
        x = points.sumOf { it.x.toDouble() }.toFloat() / points.size,
        y = points.sumOf { it.y.toDouble() }.toFloat() / points.size,
    )
}

/**
 * Rotate [points] about [pivot].
 *
 * Takes canvas *pixels*, not normalised coordinates, and that is the whole
 * reason this function exists rather than being three lines inline. Normalised
 * coordinates are anisotropic — a 90° turn in 0..1 space on a 16:10 canvas
 * distorts the shape it turns — so positions have to come into pixel space,
 * rotate, and go back.
 */
fun rotateAbout(points: List<CanvasPoint>, pivot: CanvasPoint, degrees: Float): List<CanvasPoint> {
    val radians = degrees * PI.toFloat() / 180f
    val cosine = cos(radians)
    val sine = sin(radians)
    return points.map { point ->
        val dx = point.x - pivot.x
        val dy = point.y - pivot.y
        CanvasPoint(
            x = pivot.x + dx * cosine - dy * sine,
            y = pivot.y + dx * sine + dy * cosine,
        )
    }
}

/**
 * Respace points evenly between the two extremes along their dominant axis,
 * leaving the endpoints where they are.
 *
 * The endpoints stay put deliberately: they're the ones the user most likely
 * placed against something real, and an "align" that moves all six eyes is a
 * destructive surprise rather than a tidy-up.
 *
 * Returns the input unchanged for fewer than three points, where there is
 * nothing to distribute.
 */
fun distributeEvenly(points: List<CanvasPoint>): List<CanvasPoint> {
    if (points.size < 3) return points

    val spanX = points.maxOf { it.x } - points.minOf { it.x }
    val spanY = points.maxOf { it.y } - points.minOf { it.y }
    val alongX = spanX >= spanY

    // Sorted for spacing, then restored: the caller's list is indexed against
    // the selection and must not be reshuffled.
    val order = points.indices.sortedBy { if (alongX) points[it].x else points[it].y }
    val first = points[order.first()]
    val last = points[order.last()]
    val steps = order.size - 1

    val result = points.toMutableList()
    order.forEachIndexed { step, index ->
        val t = step.toFloat() / steps
        result[index] = CanvasPoint(
            x = first.x + (last.x - first.x) * t,
            y = first.y + (last.y - first.y) * t,
        )
    }
    return result
}

/**
 * Mirror points left-to-right about the selection's own vertical centre line.
 *
 * About the selection rather than the canvas, because the gesture people
 * actually want is "make this pair symmetric with itself", not "throw my eyes
 * to the other side of the screen".
 */
fun mirrorHorizontally(points: List<CanvasPoint>): List<CanvasPoint> {
    if (points.isEmpty()) return points
    val axis = (points.maxOf { it.x } + points.minOf { it.x }) / 2f
    return points.map { CanvasPoint(x = axis * 2f - it.x, y = it.y) }
}
