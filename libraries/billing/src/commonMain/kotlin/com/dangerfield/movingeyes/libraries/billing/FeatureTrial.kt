package com.dangerfield.movingeyes.libraries.billing

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The thirty-second demo.
 *
 * Every paid control in this app is motion, which a store screenshot cannot
 * show, so a locked control applies its real value to the user's own scene for
 * thirty seconds and then reverts while they watch. The rules that make that
 * work rather than give the product away:
 *
 * - one demo at a time, and one per control per *session* — not per lifetime,
 *   which would be mean on a second evening;
 * - it reverts visibly over [RevertDuration]; snapping back reads as a bug and
 *   leaving it applied would be a lie;
 * - no dimming and no modal on expiry.
 */
interface FeatureTrial {

    val active: StateFlow<ActiveTrial?>

    /** The control whose demo just expired, for the "keep it" bar. Clears
     *  itself after [EndedBannerDuration]. */
    val justEnded: StateFlow<DemoControl?>

    fun isAvailable(control: DemoControl): Boolean

    /**
     * [revert] is the caller's, because only the caller knows what it changed.
     * Returns false when [isAvailable] would have said no.
     */
    fun start(control: DemoControl, revert: () -> Unit): Boolean

    /** Bought mid-demo: drop the pending revert and leave the value applied. */
    fun keep()

    /** End early and revert now. */
    fun cancel()

    companion object {
        val Duration: Duration = 30.seconds
        val RevertDuration: Duration = 400.milliseconds
        val EndedBannerDuration: Duration = 6.seconds

        /** The countdown goes amber for the last stretch. */
        val UrgentBelow: Duration = 5.seconds
    }
}

data class ActiveTrial(
    val control: DemoControl,
    val remaining: Duration,
) {
    val isUrgent: Boolean get() = remaining <= FeatureTrial.UrgentBelow
}

/** Closed, so two call sites for the same control agree on what to call it. */
enum class DemoControl {
    Mood,
    BlinkRate,
    WanderRadius,
    Restlessness,
    GazeCenter,
    EyeStyle,
    Reactivity,
}
