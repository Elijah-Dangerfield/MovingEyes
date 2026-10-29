package com.dangerfield.movingeyes.libraries.device.impl

import com.dangerfield.movingeyes.libraries.device.DisplayController
import com.dangerfield.movingeyes.libraries.device.DisplayHost
import me.tatarka.inject.annotations.Inject
import platform.UIKit.UIApplication
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/** Plain properties here, view-controller overrides through [DisplayHost]. */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class IosDisplayController(
    private val host: DisplayHost,
) : DisplayController {

    override fun setKeepAwake(keepAwake: Boolean) {
        UIApplication.sharedApplication.idleTimerDisabled = keepAwake
    }

    override fun setImmersive(immersive: Boolean) = host.setChromeHidden(immersive)

    override fun setOrientationLocked(locked: Boolean) = host.setOrientationLocked(locked)
}
