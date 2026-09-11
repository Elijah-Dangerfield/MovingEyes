package com.dangerfield.movingeyes

import com.dangerfield.movingeyes.libraries.core.BuildInfo
import com.dangerfield.movingeyes.libraries.core.ShakeDetector
import com.dangerfield.movingeyes.libraries.core.isQaBuild
import com.dangerfield.movingeyes.libraries.core.ShakeEvent
import com.dangerfield.movingeyes.features.settings.BugReportRoute
import com.dangerfield.movingeyes.libraries.navigation.NavigationOptions
import com.dangerfield.movingeyes.libraries.navigation.Router
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Shake files a bug report, and only on a QA build (debug or the beta
 * channel). Store builds keep the accelerometer idle: this device spends Halloween night taped behind a
 * painting, and the one thing worse than a dialog over a mounted scene is a
 * dialog that appeared because someone bumped the wall.
 */
@Inject
@SingleIn(AppScope::class)
class ShakeHandler(
    private val shakeDetector: ShakeDetector,
    private val router: Router,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Held so [stop] can cancel it. Without this every foreground cycle
     *  launched another collector onto the same stream and none of them ever
     *  went away. */
    private var collection: Job? = null

    fun start() {
        if (!BuildInfo.isQaBuild) return
        shakeDetector.start()
        collection?.cancel()
        collection = scope.launch {
            shakeDetector.shakeEvents.collect { handleShake() }
        }
    }

    fun stop() {
        if (!BuildInfo.isQaBuild) return
        collection?.cancel()
        collection = null
        shakeDetector.stop()
    }

    /**
     * Straight to the report. It used to open a menu of debug destinations
     * fronted by a randomised joke, which is two things a bug report is not:
     * a decision to make, and funny. The debug screens it listed are all
     * reachable by deep link anyway — `movingeyes://design-system`,
     * `movingeyes://eyes`, `movingeyes://qa-config`.
     *
     * The duplicate is refused by the navigator rather than by a flag here.
     * The flag this replaces was set on the first shake and cleared only by an
     * `onDialogDismissed()` that nothing ever called, so shaking worked once
     * per process and then silently stopped for good — which reads as a broken
     * sensor rather than a stuck boolean.
     */
    private fun handleShake() {
        router.navigate(BugReportRoute(), NavigationOptions(launchSingleTop = true))
    }
}
