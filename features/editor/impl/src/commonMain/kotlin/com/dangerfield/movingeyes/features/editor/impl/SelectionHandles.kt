@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * The grab targets on the selection box.
 *
 * Drawing a handle is a promise that it can be dragged, so these are hit-tested
 * before anything else in the gesture chain — a touch on a corner is a resize,
 * not a move of whatever eye happens to be underneath.
 */
sealed interface SelectionHandle {
    /** [anchor] is the opposite corner, which stays put while dragging. */
    data class Corner(val position: Offset, val anchor: Offset) : SelectionHandle

    data class Rotate(val pivot: Offset) : SelectionHandle
}

/**
 * Which handle [touch] landed on, if any.
 *
 * [touchRadius] is generous on purpose: the drawn handle is 14dp but the finger
 * covering it is nearer 50, and missing a corner and dragging the whole
 * selection instead is the kind of mistake that costs an alignment.
 */
fun handleAt(
    touch: Offset,
    bounds: Rect,
    touchRadius: Float,
    rotateGapPx: Float,
): SelectionHandle? {
    val rotate = rotateHandleCenter(bounds, rotateGapPx)
    if ((touch - rotate).getDistance() <= touchRadius) return SelectionHandle.Rotate(bounds.center)

    val corners = listOf(
        bounds.topLeft to Offset(bounds.right, bounds.bottom),
        Offset(bounds.right, bounds.top) to Offset(bounds.left, bounds.bottom),
        Offset(bounds.left, bounds.bottom) to Offset(bounds.right, bounds.top),
        Offset(bounds.right, bounds.bottom) to bounds.topLeft,
    )
    return corners
        .firstOrNull { (corner, _) -> (touch - corner).getDistance() <= touchRadius }
        ?.let { (corner, anchor) -> SelectionHandle.Corner(corner, anchor) }
}

fun rotateHandleCenter(bounds: Rect, gapPx: Float): Offset =
    Offset(bounds.center.x, bounds.top - gapPx)

/**
 * Scale factor for a corner dragged from [from] to [to] about [anchor].
 *
 * Distance from the anchor rather than either axis alone, so the selection
 * keeps its proportions — an eye stretched on one axis stops being an eye.
 */
fun cornerScale(anchor: Offset, from: Offset, to: Offset): Float {
    val before = hypot(from.x - anchor.x, from.y - anchor.y)
    if (before < 1f) return 1f
    val after = hypot(to.x - anchor.x, to.y - anchor.y)
    return (after / before).coerceIn(MinStep, MaxStep)
}

/** Degrees swept around [pivot] going from [from] to [to]. */
fun rotationBetween(pivot: Offset, from: Offset, to: Offset): Float {
    val before = atan2(from.y - pivot.y, from.x - pivot.x)
    val after = atan2(to.y - pivot.y, to.x - pivot.x)
    var degrees = (after - before) * 180f / PI.toFloat()
    // Shortest way round, so crossing the -180/180 seam doesn't spin the
    // selection most of the way back the other way.
    while (degrees > 180f) degrees -= 360f
    while (degrees < -180f) degrees += 360f
    return degrees
}

/** How far the rotate handle floats above the box. Shared so the drawn
 *  position and the grabbable one can't drift apart. */
val RotateHandleGap: Dp = 40.dp

/** Per-event limits, so one jumpy frame can't collapse or explode a selection. */
private const val MinStep = 0.5f
private const val MaxStep = 2f
