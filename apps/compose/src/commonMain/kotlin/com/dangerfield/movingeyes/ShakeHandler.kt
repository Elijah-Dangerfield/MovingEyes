package com.dangerfield.movingeyes

import com.dangerfield.movingeyes.libraries.core.BuildInfo
import com.dangerfield.movingeyes.libraries.core.ShakeDetector
import com.dangerfield.movingeyes.libraries.core.isQaBuild
import com.dangerfield.movingeyes.libraries.core.ShakeEvent
import com.dangerfield.movingeyes.features.settings.BugReportRoute
import com.dangerfield.movingeyes.libraries.navigation.Router
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Shake files a bug report, and only in debug builds. Release keeps the
 * accelerometer idle: this device spends Halloween night taped behind a
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
    private var isShowingDialog = false

    fun start() {
        if (!BuildInfo.isQaBuild) return
        shakeDetector.start()
        scope.launch {
            shakeDetector.shakeEvents.collect { handleShake() }
        }
    }

    fun stop() {
        if (!BuildInfo.isQaBuild) return
        shakeDetector.stop()
    }

    fun onDialogDismissed() {
        isShowingDialog = false
    }

    /**
     * Straight to the report. It used to open a menu of debug destinations
     * fronted by a randomised joke, which is two things a bug report is not:
     * a decision to make, and funny. The debug screens it listed are all
     * reachable by deep link anyway — `movingeyes://design-system`,
     * `movingeyes://eyes`, `movingeyes://qa-config`.
     */
    private fun handleShake() {
        if (isShowingDialog) return
        isShowingDialog = true
        router.navigate(BugReportRoute())
    }
}
