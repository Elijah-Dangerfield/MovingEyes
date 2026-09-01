package com.dangerfield.movingeyes

import com.dangerfield.movingeyes.libraries.core.BuildInfo
import com.dangerfield.movingeyes.libraries.core.ShakeDetector
import com.dangerfield.movingeyes.libraries.core.ShakeEvent
import com.dangerfield.movingeyes.libraries.core.ShakeMessageContext
import com.dangerfield.movingeyes.libraries.core.ShakeMessageProvider
import com.dangerfield.movingeyes.libraries.navigation.Router
import com.dangerfield.movingeyes.libraries.navigation.ShakeDialogRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Shake opens the debug menu, and only in debug builds. Release keeps the
 * accelerometer idle: this device spends Halloween night taped behind a
 * painting, and the one thing worse than a dialog over a mounted scene is a
 * dialog that appeared because someone bumped the wall.
 */
@Inject
@SingleIn(AppScope::class)
class ShakeHandler(
    private val shakeDetector: ShakeDetector,
    private val shakeMessageProvider: ShakeMessageProvider,
    private val router: Router,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isShowingDialog = false
    // Process-local flavor counter for the shake easter-egg copy.
    private var shakeCount = 0

    fun start() {
        if (!BuildInfo.isDebug) return
        shakeDetector.start()
        scope.launch {
            shakeDetector.shakeEvents.collect { event ->
                handleShake(event)
            }
        }
    }

    fun stop() {
        if (!BuildInfo.isDebug) return
        shakeDetector.stop()
    }

    fun onDialogDismissed() {
        isShowingDialog = false
    }

    private fun handleShake(event: ShakeEvent) {
        if (isShowingDialog) return

        val context = ShakeMessageContext(
            shakeCount = shakeCount,
            intensity = event.intensity,
            isLateNight = false,
            isFirstSession = false,
            userName = null,
        )

        val message = shakeMessageProvider.getMessage(context)

        isShowingDialog = true
        router.navigate(
            ShakeDialogRoute(
                headline = message.headline,
                subtext = message.subtext,
            )
        )

        shakeCount++
    }
}
