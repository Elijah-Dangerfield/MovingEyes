@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.system

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Every duration, easing and threshold the design specifies, in one place.
 *
 * These numbers are not arbitrary and several of them are load-bearing, so
 * change them here rather than inlining a `tween(300)` at a call site. Where a
 * value exists for a non-obvious reason, the reason is written down next to it.
 */
object Motion {

    /** The design's house easing. Standard material-ish accelerate-decelerate. */
    val Standard: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    /**
     * Entering and leaving display mode.
     *
     * Chrome **dissolves, it does not slide**: rail, toolbar and readout fade
     * over [ChromeDissolveMillis], staggered [ChromeStaggerMillis] from the
     * outside in so attention lands on the eyes last. The canvas itself never
     * moves by a pixel and never scales — a transform here would undo the
     * alignment the user just spent four minutes on, which is the one thing
     * this app must never do.
     */
    object Chrome {
        const val DissolveMillis = 320
        const val StaggerMillis = 40
        const val ReturnMillis = 240

        fun <T> dissolve(): FiniteAnimationSpec<T> = tween(DissolveMillis, easing = Standard)
        fun <T> restore(): FiniteAnimationSpec<T> = tween(ReturnMillis, easing = Standard)
    }

    /**
     * Snap guides in the editor.
     *
     * The guide draws from the snap point outward and holds while the finger
     * stays within [ThresholdDp]. One light haptic on capture, **nothing on
     * release** — a tick in both directions turns a careful nudge into a
     * buzzing mess. Guides always clear on touch-up; they never persist.
     */
    object Snap {
        const val GuideDrawMillis = 90
        val ThresholdDp: Dp = 6.dp
        /** Selection rotation clicks to this increment, and holds any angle past it. */
        const val RotationDetentDegrees = 15f

        fun <T> guideDraw(): FiniteAnimationSpec<T> = tween(GuideDrawMillis, easing = LinearEasing)
    }

    /**
     * The editor sheet (phone) and rail (tablet).
     *
     * Contents cross-fade at 60% of the travel so no text is legible mid-slide,
     * and both leave a [GrabEdgeDp] handle so collapsed is always one tap from
     * open.
     */
    object Panel {
        const val SpringStiffness = 320f

        /**
         * The design gives a damping *coefficient* of 34 against a stiffness of
         * 320; Compose wants a damping *ratio*. For unit mass that's
         * `c / (2·√k)` = `34 / (2·√320)` ≈ 0.95 — just under critical, so the
         * panel arrives with a hint of overshoot instead of a dead stop.
         */
        const val SpringDampingRatio = 0.95f
        const val ContentFadeMillis = 120
        const val ContentFadeAtTravel = 0.6f
        val GrabEdgeDp: Dp = 44.dp
        val RailWidthDp: Dp = 340.dp

        fun <T> slide(): FiniteAnimationSpec<T> =
            spring(dampingRatio = SpringDampingRatio, stiffness = SpringStiffness)

        fun <T> contentFade(): FiniteAnimationSpec<T> =
            tween(ContentFadeMillis, easing = Standard)
    }

    /** Tips sheet paging: cross-fade plus a small horizontal offset. */
    object Tips {
        const val PageMillis = 220
        val PageOffsetDp: Dp = 24.dp
    }

    /**
     * The 30-second demo of a locked control.
     *
     * Tapping a locked control applies it **for real** and starts an amber
     * countdown pill; the last five seconds tick in mono. On expiry the
     * controls animate back over [RevertMillis] rather than snapping, so the
     * user watches the thing leave. That visible loss is the honest version of
     * a trial, and it's why the revert is slow enough to notice.
     */
    object Demo {
        const val DurationSeconds = 30
        const val TickCountdownUnderSeconds = 5
        const val PillFadeInMillis = 250
        const val RevertMillis = 400
        /** How long the "demo ended" bar sits before dismissing itself. */
        const val EndedBarSeconds = 6

        fun <T> revert(): FiniteAnimationSpec<T> = tween(RevertMillis, easing = Standard)
    }

    /**
     * Eye behaviour timings, shared by the renderer and the behaviour engine.
     *
     * A saccade overshoots and settles rather than easing flat — a pupil that
     * glides to a stop reads as a screensaver, one that arrives slightly hot
     * reads as alive.
     */
    object Eye {
        const val SaccadeMillis = 90
        const val SaccadeSettleMillis = 60
        const val BlinkMillis = 140
        const val StartleDilateMillis = 120
        const val StartleGazeSnapMillis = 70
        const val StartleDecayMillis = 3000
    }

    /** Transient chrome in display mode: battery pill, toasts, sleep fade. */
    object Notice {
        /** Battery state on entering display mode. */
        const val BatteryOnEntrySeconds = 3
        /** Low-battery warning, re-shown once per 10% step below 20%. */
        const val LowBatterySeconds = 6
        /** The keep-your-sizes / Refit toast after a device rotation. */
        const val RotationToastSeconds = 4
        /** Sleep timer fades to black rather than cutting. */
        const val SleepFadeMillis = 8000
    }
}
