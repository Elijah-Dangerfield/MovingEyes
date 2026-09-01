package com.dangerfield.movingeyes.libraries.billing

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The thirty-second demo.
 *
 * ## What this is instead of
 *
 * A greyed-out control that opens a paywall teaches someone nothing about what
 * they'd be buying. Every paid control in this app is *motion*, and motion is
 * the one thing a screenshot on a store page cannot show. So a locked control
 * doesn't refuse — it works, for real, on the user's own scene, for thirty
 * seconds.
 *
 * ## The rules, and why each one is there
 *
 * - **The real value is applied**, not a canned preview. The point is to see
 *   Frantic running on *your* eyes at *your* size in *your* hallway.
 * - **One demo at a time.** Two countdowns running at once is a puzzle, not an
 *   advert.
 * - **One demo per control per session.** Not per app-lifetime, because being
 *   told "you already tried this" on a different evening is mean. Not
 *   unlimited, because then it isn't a demo, it's the product.
 * - **It reverts visibly, over [RevertDuration].** The settings animate back
 *   while you watch. Snapping them back instantly reads as a bug; leaving them
 *   applied would be a lie. Watching the thing you liked leave is the honest
 *   version of a trial, and it's the moment that sells.
 * - **No dimming and no modal** on expiry. A bar slides in and can be ignored.
 */
interface FeatureTrial {

    /** The demo currently running, with its countdown. Null when none is. */
    val active: StateFlow<ActiveTrial?>

    /**
     * The control whose demo just expired, for the "Demo ended / Keep it" bar.
     * Clears itself after [EndedBannerDuration].
     */
    val justEnded: StateFlow<DemoControl?>

    /** False when this control has already had its demo this session, or
     *  another demo is running. */
    fun isAvailable(control: DemoControl): Boolean

    /**
     * Start a demo. [revert] is called when it expires and is the caller's job
     * because only the caller knows what it changed.
     *
     * Returns false when [isAvailable] would have said no, so a caller can
     * apply-then-check in one step without racing.
     */
    fun start(control: DemoControl, revert: () -> Unit): Boolean

    /**
     * The user bought it mid-demo. Drops the pending revert and leaves the
     * value applied — nobody should watch their new purchase get taken away.
     */
    fun keep()

    /** End early and revert now, e.g. the user changed the control back. */
    fun cancel()

    companion object {
        val Duration: Duration = 30.seconds

        /** Long enough to read as leaving rather than as a glitch. Matches the
         *  design's `demoRevert` timing. */
        val RevertDuration: Duration = 400.milliseconds

        val EndedBannerDuration: Duration = 6.seconds

        /** The countdown goes mono and amber for the last stretch. */
        val UrgentBelow: Duration = 5.seconds
    }
}

data class ActiveTrial(
    val control: DemoControl,
    val remaining: Duration,
) {
    val isUrgent: Boolean get() = remaining <= FeatureTrial.UrgentBelow
}

/**
 * A control that can be demoed.
 *
 * A closed set rather than free-form strings, because "one demo per control per
 * session" only means anything if two call sites for the same control agree on
 * what to call it.
 */
enum class DemoControl {
    Mood,
    BlinkRate,
    WanderRadius,
    Restlessness,
    GazeCenter,
    EyeStyle,
    Reactivity,
}
