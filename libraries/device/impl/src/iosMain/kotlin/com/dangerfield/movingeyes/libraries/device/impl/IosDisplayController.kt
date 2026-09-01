package com.dangerfield.movingeyes.libraries.device.impl

import com.dangerfield.movingeyes.libraries.device.DisplayController
import com.dangerfield.movingeyes.libraries.device.DisplayHost
import me.tatarka.inject.annotations.Inject
import platform.UIKit.UIApplication
import platform.UIKit.UIScreen
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Split between Kotlin and Swift along the line UIKit draws: the two plain
 * properties are set here, the two view-controller overrides go through
 * [DisplayHost]. See its docs for why.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class IosDisplayController(
    private val host: DisplayHost,
) : DisplayController {

    /**
     * The system brightness this app found on entry, so leaving display mode
     * puts it back.
     *
     * iOS has no per-window brightness — `UIScreen.brightness` is the device's
     * real backlight and the change outlives the app. Failing to restore it
     * would leave someone's phone at 10% brightness after they closed a
     * Halloween decoration, with no clue why.
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
