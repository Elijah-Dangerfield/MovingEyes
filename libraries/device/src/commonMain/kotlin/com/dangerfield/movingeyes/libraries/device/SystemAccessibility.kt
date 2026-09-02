package com.dangerfield.movingeyes.libraries.device

/**
 * Accessibility preferences the OS already knows about, so the app doesn't ask
 * a second time for something the user has set once system-wide.
 */
interface SystemAccessibility {

    /** Reduce motion, which this app honours by capping the parameters that
     *  drive rapid movement — the same cap as the in-app Reduce flashing. */
    val isReduceMotionEnabled: Boolean
}
