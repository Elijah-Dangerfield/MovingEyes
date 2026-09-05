@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs
import kotlin.time.TimeSource

/**
 * The canvas gesture set.
 *
 * Nothing here is invented. Drag to move, pinch to scale, twist to rotate, tap
 * to select, two-finger tap to undo and three-finger tap to redo are what
 * Procreate, Figma, Keynote and Photos already do — so a first-time user has
 * already learned this app. Inventing a gesture would mean teaching it, and
 * there is no onboarding left to teach it in.
 *
 * **There is no pinch-to-zoom on the canvas**, and that absence is the most
 * important thing in this file. The canvas is the screen at 1:1 because a
 * pixel here is a physical millimetre on a device about to be taped to
 * cardboard; a zoom would make every measurement on screen a lie. Pinch is
 * spent scaling the selection instead.
 *
 * Undo on a two-finger tap earns its place for a physical reason: when you've
 * just fumbled a placement, your hand is on the canvas, and the toolbar button
 * is somewhere else.
 */
@Composable
fun Modifier.editorGestures(
    enabled: Boolean,
    /** Which selection handle a touch landed on, if any. Checked before
     *  anything else: a corner is a resize, not a move of the eye beneath it. */
    handleAt: (Offset) -> SelectionHandle?,
    onGestureStart: () -> Unit,
    onTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit,
    onDrag: (pan: Offset) -> Unit,
    onHandleDrag: (handle: SelectionHandle, from: Offset, to: Offset) -> Unit,
    onTransform: (zoom: Float, rotation: Float, centroid: Offset) -> Unit,
    onGestureEnd: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
): Modifier {
    // Every callback is read through rememberUpdatedState, because the block
    // below is a long-lived coroutine keyed only on `enabled` — it is created
    // once and then runs for the life of the screen.
    //
    // Without this it captures whatever these lambdas closed over on the frame
    // it started, and the editor is rebuilt whenever the open scene changes.
    // The result was a canvas that could not be touched at all: gestures still
    // arrived and still drew their guides, but every drag and tap was mutating
    // an EditorState nothing was rendering any more. Entering and leaving
    // display mode fixed it, because that flips `enabled` and rebuilds the
    // block, which is exactly the tell.
    val currentHandleAt by rememberUpdatedState(handleAt)
    val currentGestureStart by rememberUpdatedState(onGestureStart)
    val currentTap by rememberUpdatedState(onTap)
    val currentDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentDrag by rememberUpdatedState(onDrag)
    val currentHandleDrag by rememberUpdatedState(onHandleDrag)
    val currentTransform by rememberUpdatedState(onTransform)
    val currentGestureEnd by rememberUpdatedState(onGestureEnd)
    val currentUndo by rememberUpdatedState(onUndo)
    val currentRedo by rememberUpdatedState(onRedo)

    return pointerInput(enabled) {
            if (!enabled) return@pointerInput

        val touchSlop = viewConfiguration.touchSlop
        val doubleTapWindowMillis = viewConfiguration.doubleTapTimeoutMillis
        val timeSource = TimeSource.Monotonic
        var lastTapAt = timeSource.markNow()
        var sawATap = false

        awaitEachGesture {
            val first = awaitFirstDown(requireUnconsumed = false)

            val grabbed = currentHandleAt(first.position)
            var pastSlop = false
            var startedGesture = false
            var maxPointers = 1
            var handleFrom = first.position

            // Movement accumulates across events. Each pointer event carries only
            // the delta since the last one, so comparing a single event against
            // the slop threshold means a smooth drag never crosses it — every
            // frame's delta is a few pixels and the gesture reads as a tap
            // forever.
            var travelled = 0f

            // Movement made *before* the slop threshold is crossed is kept and
            // handed to the first drag callback rather than thrown away. Dropping
            // it would leave the eye permanently trailing the finger by up to a
            // slop's width — on a tool whose entire job is lining things up with
            // holes in cardboard, a constant offset between what you touch and
            // what moves is the worst kind of bug: invisible and systematic.
            var pendingPan = Offset.Zero

            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.count { it.pressed }
                if (pressed == 0) break

                // A three-finger tap is three fingers *at some point*, not three
                // fingers at lift-off — by then two are already gone.
                maxPointers = maxOf(maxPointers, pressed)

                val zoom = event.calculateZoom()
                val rotation = event.calculateRotation()
                val pan = event.calculatePan()

                if (!pastSlop) {
                    // Rotation and zoom have to be commensurable with a distance
                    // before they can share one slop budget, hence the weights.
                    travelled += pan.getDistance() +
                        abs(rotation) * SlopDegreesToPx +
                        abs(1f - zoom) * SlopZoomToPx
                    pendingPan += pan
                    if (travelled > touchSlop) pastSlop = true
                }

                if (pastSlop) {
                    val firstMove = !startedGesture
                    if (firstMove) {
                        currentGestureStart()
                        startedGesture = true
                    }

                    val position = event.changes.first().position
                    when {
                        grabbed != null -> {
                            currentHandleDrag(grabbed, handleFrom, position)
                            handleFrom = position
                        }

                        pressed >= 2 ->
                            currentTransform(zoom, rotation, event.calculateCentroid(useCurrent = true))

                        else -> currentDrag(if (firstMove) pendingPan else pan)
                    }
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            }

            when {
                startedGesture -> {
                    currentGestureEnd()
                    sawATap = false
                }

                maxPointers == 2 -> currentUndo()
                maxPointers >= 3 -> currentRedo()

                else -> {
                    // Double tap is tracked across gestures rather than by waiting
                    // inside one, so a single tap selects immediately instead of
                    // paying the double-tap timeout every time. The cost is that a
                    // double tap fires onTap first, which is harmless: on empty
                    // canvas that's "deselect, then select all".
                    val withinWindow = sawATap &&
                        lastTapAt.elapsedNow().inWholeMilliseconds < doubleTapWindowMillis
                    if (withinWindow) {
                        currentDoubleTap(first.position)
                        sawATap = false
                    } else {
                        currentTap(first.position)
                        lastTapAt = timeSource.markNow()
                        sawATap = true
                    }
                }
            }
        }
    }
}

/** How much a degree of twist counts toward the slop budget. */
private const val SlopDegreesToPx = 2f

/** How much a unit of zoom counts toward the slop budget. */
private const val SlopZoomToPx = 100f
