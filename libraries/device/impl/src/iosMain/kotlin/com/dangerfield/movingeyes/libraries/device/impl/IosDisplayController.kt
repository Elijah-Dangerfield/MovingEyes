package com.dangerfield.movingeyes.libraries.device.impl

import com.dangerfield.movingeyes.libraries.device.DisplayController
import com.dangerfield.movingeyes.libraries.device.DisplayHost
import me.tatarka.inject.annotations.Inject
import platform.UIKit.UIApplication
import platform.UIKit.UIScreen
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

    /**
     * iOS has no per-window brightness: `UIScreen.brightness` is the real
     * backlight and the change outlives the app, so it has to be put back.
     */
    private var brightnessOnEntry: Double? = null

    override fun setKeepAwake(keepAwake: Boolean) {
        UIApplication.sharedApplication.idleTimerDisabled = keepAwake
    }

    override fun setImmersive(immersive: Boolean) = host.setChromeHidden(immersive)

    override fun setBrightness(brightness: Float?) {
        val screen = UIScreen.mainScreen
        if (brightness == null) {
            brightnessOnEntry?.let { screen.brightness = it }
            brightnessOnEntry = null
            return
        }
        if (brightnessOnEntry == null) brightnessOnEntry = screen.brightness
        screen.brightness = brightness.coerceIn(0f, 1f).toDouble()
    }

    override fun setOrientationLocked(locked: Boolean) = host.setOrientationLocked(locked)
}
