package com.dangerfield.movingeyes.libraries.device

/**
 * The parts of display mode iOS will only do from Swift.
 *
 * Hiding the status bar and the home indicator, and freezing rotation, are all
 * driven on iOS by overrides on the hosting `UIViewController`
 * (`prefersStatusBarHidden`, `prefersHomeIndicatorAutoHidden`,
 * `supportedInterfaceOrientations`) plus a call to
 * `setNeedsUpdateOfHomeIndicatorAutoHidden`. There is no imperative UIKit call
 * for any of them, so Kotlin/Native cannot reach them and the implementation
 * has to live over there — the same reason `StoreKitCoordinator` does.
 *
 * Keep-awake and brightness are *not* here: `isIdleTimerDisabled` and
 * `UIScreen.brightness` are plain properties that Kotlin/Native sets directly.
 *
 * Android binds a no-op, because `AndroidDisplayController` does all of this
 * itself through window flags.
 *
 * Plain callbacks and no suspend functions, deliberately: a Kotlin `suspend`
 * function is exposed to Swift as a completion handler that must be called
 * exactly once, and getting that wrong from a UIKit delegate is a hang rather
 * than a crash.
 */
interface DisplayHost {

    /** Hide or show the status bar and home indicator. */
    fun setChromeHidden(hidden: Boolean)

    /** Freeze rotation at whatever is on screen now, or release it. */
    fun setOrientationLocked(locked: Boolean)
}

/** What Android binds, and what iOS falls back to if Swift never supplies one. */
object NoOpDisplayHost : DisplayHost {
    override fun setChromeHidden(hidden: Boolean) = Unit
    override fun setOrientationLocked(locked: Boolean) = Unit
}
