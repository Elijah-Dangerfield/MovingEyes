package com.dangerfield.movingeyes.libraries.device

/**
 * The parts of display mode that only Swift can do.
 *
 * On iOS the status bar, home indicator and orientation are view-controller
 * overrides with no imperative UIKit equivalent, so Kotlin/Native can't reach
 * them. Keep-awake and brightness are plain properties and are set directly by
 * `IosDisplayController`.
 *
 * Plain callbacks, no suspend: a Kotlin suspend function reaches Swift as a
 * completion handler that must fire exactly once, and getting that wrong from a
 * UIKit delegate hangs rather than crashes.
 */
interface DisplayHost {

    fun setChromeHidden(hidden: Boolean)

    fun setOrientationLocked(locked: Boolean)
}
