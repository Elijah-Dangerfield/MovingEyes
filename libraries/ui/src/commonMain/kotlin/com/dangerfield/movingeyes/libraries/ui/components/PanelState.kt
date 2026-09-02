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
import kotlin.math.roundToInt

/**
 * How far the panel is slid away, and how much of the screen it still covers.
 *
 * One value drives both the panel's position and the canvas's scale, so they
 * move together frame for frame. Two independent animations — one for the
 * sheet, one for the canvas reacting to its measured height — is exactly how
 * you get the lag and the stutter: the canvas would always be chasing a number
 * the sheet had already moved past.
 */
@Stable
class PanelState internal constructor(initiallyExpanded: Boolean) {

    /** 0 fully open, 1 collapsed to the grab edge. */
    internal val travel = Animatable(if (initiallyExpanded) 0f else 1f)

    internal var extentPx by mutableIntStateOf(0)

    /** The header's height. It never slides away, so it bounds the travel. */
    internal var headerPx by mutableIntStateOf(0)

    /** How much the panel covers right now, in pixels. Read this to lay out
     *  around it; it updates every frame of a drag. */
    val occupiedPx: Float
        get() = (extentPx - slidePx * travel.value).coerceAtLeast(0f)

    val isExpanded: Boolean get() = travel.value < 0.5f

    /** The distance the panel can travel: everything but the grab edge, which
     *  always stays on screen. */
    private val slidePx: Float get() = (extentPx - headerPx).coerceAtLeast(0).toFloat()

    internal fun offsetPx(): Int = (slidePx * travel.value).roundToInt()

    internal suspend fun dragBy(deltaPx: Float) {
        if (slidePx <= 0f) return
        travel.snapTo((travel.value + deltaPx / slidePx).coerceIn(0f, 1f))
    }

    /**
     * Settles after a drag. A flick decides on its own, however far the panel
     * happened to have travelled — otherwise a fast short swipe leaves the
     * sheet stuck where it was, which reads as the gesture not having worked.
     */
    internal suspend fun settle(velocityPx: Float, spec: AnimationSpec<Float>) {
        travel.animateTo(settleTarget(travel.value, velocityPx), spec)
    }

    suspend fun expand() = travel.animateTo(0f, Motion.Panel.slide())

    suspend fun collapse() = travel.animateTo(1f, Motion.Panel.slide())

    suspend fun toggle() = if (isExpanded) collapse() else expand()

}

/**
 * Where a released panel lands.
 *
 * A flick decides on its own, however far the panel happened to travel —
 * otherwise a fast short swipe leaves it stuck, which reads as the gesture not
 * having worked. Below that, it goes wherever it is closest to.
 */
internal fun settleTarget(travel: Float, velocityPx: Float): Float = when {
    abs(velocityPx) > FlickVelocityPx -> if (velocityPx > 0f) 1f else 0f
    travel > 0.5f -> 1f
    else -> 0f
}

/** Pixels per second past which a swipe is a decision rather than a nudge. */
private const val FlickVelocityPx = 400f

@Composable
fun rememberPanelState(initiallyExpanded: Boolean = true): PanelState =
    remember { PanelState(initiallyExpanded) }
