package com.dangerfield.movingeyes.libraries.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dangerfield.movingeyes.system.Motion
import kotlin.math.abs

/** Where a panel can come to rest. */
enum class PanelPosition { Hidden, Collapsed, Expanded }

/**
 * How much of the screen the panel is taking, in pixels, right now.
 *
 * **One number, measured in the same unit the canvas cares about.** The canvas
 * scales itself against [occupiedPx], so panel and canvas move together frame
 * for frame. Two independent animations — one for the sheet, one for the canvas
 * reacting to its measured height — is exactly how you get lag and stutter: the
 * canvas would always be chasing a number the sheet had already moved past.
 *
 * **Hidden is a real position, not a special case.** The editor's resting state
 * is a full-bleed canvas with nothing over it; the panel only exists once you
 * have said what you want to edit. Modelling that as "collapsed but with zero
 * height" would leave a grab edge on screen forever, which is a permanent strip
 * of chrome charging rent on the one thing the app is for.
 */
@Stable
class PanelState internal constructor(initial: PanelPosition) {

    private var pending: PanelPosition = initial

    /** Pixels of the screen the panel currently covers. */
    private val exposure = Animatable(0f)

    internal var extentPx by mutableIntStateOf(0)

    /** The header's height: the panel's resting size when collapsed. */
    internal var headerPx by mutableIntStateOf(0)

    /**
     * Read this to lay out around the panel; it updates every frame of a drag.
     *
     * Clamped, because the settle spring is underdamped and undershoots past
     * zero on the way to hidden. Callers treat this as a length — a padding, a
     * share of the canvas — and a negative one either throws or silently
     * inflates the canvas past full size for a few frames.
     */
    val occupiedPx: Float get() = exposure.value.coerceIn(0f, extentPx.toFloat())

    val isExpanded: Boolean get() = extentPx > 0 && occupiedPx > (extentPx + headerPx) / 2f

    val isVisible: Boolean get() = occupiedPx > 0f || pending != PanelPosition.Hidden

    internal fun restingPx(position: PanelPosition): Float = when (position) {
        PanelPosition.Hidden -> 0f
        PanelPosition.Collapsed -> headerPx.toFloat()
        PanelPosition.Expanded -> extentPx.toFloat()
    }

    /**
     * Snaps to the requested position without animating. Used the first time the
     * panel is measured, when there is nothing to animate from — animating there
     * would slide the panel up from nowhere on the frame it first appears.
     */
    internal suspend fun settleIntoLayout() {
        val target = restingPx(pending)
        if (exposure.value != target && !exposure.isRunning) exposure.snapTo(target)
    }

    internal suspend fun dragBy(deltaPx: Float) {
        exposure.snapTo((exposure.value - deltaPx).coerceIn(0f, extentPx.toFloat()))
    }

    /**
     * Settles after a drag. A flick decides on its own, however far the panel
     * happened to travel — otherwise a fast short swipe leaves the panel stuck
     * where it was, which reads as the gesture not having worked.
     *
     * A downward flick dismisses rather than collapsing, because the thing a
     * thrown-away panel should do is go away.
     */
    internal suspend fun settle(velocityPx: Float, spec: AnimationSpec<Float>) {
        go(settleTarget(exposure.value, velocityPx, headerPx.toFloat(), extentPx.toFloat()), spec)
    }

    suspend fun show() = go(PanelPosition.Collapsed)

    suspend fun expand() = go(PanelPosition.Expanded)

    suspend fun hide() = go(PanelPosition.Hidden)

    suspend fun toggle() = if (isExpanded) hide() else expand()

    private suspend fun go(position: PanelPosition, spec: AnimationSpec<Float> = Motion.Panel.slide()) {
        pending = position
        exposure.animateTo(restingPx(position), spec)
    }
}

/**
 * Where a released panel lands.
 *
 * A flick decides on its own, however far the panel happened to travel —
 * otherwise a fast short swipe leaves it stuck, which reads as the gesture not
 * having worked. A downward flick dismisses rather than collapsing, because the
 * thing a thrown-away panel should do is go away. Below flick speed it goes
 * wherever it is closest to.
 */
internal fun settleTarget(
    exposurePx: Float,
    velocityPx: Float,
    headerPx: Float,
    extentPx: Float,
): PanelPosition {
    if (velocityPx > FlickVelocityPx) return PanelPosition.Hidden
    if (velocityPx < -FlickVelocityPx) return PanelPosition.Expanded

    val resting = mapOf(
        PanelPosition.Hidden to 0f,
        PanelPosition.Collapsed to headerPx,
        PanelPosition.Expanded to extentPx,
    )
    return resting.minBy { abs(it.value - exposurePx) }.key
}

/** Pixels per second past which a swipe is a decision rather than a nudge. */
private const val FlickVelocityPx = 400f

@Composable
fun rememberPanelState(initial: PanelPosition = PanelPosition.Hidden): PanelState =
    remember { PanelState(initial) }
