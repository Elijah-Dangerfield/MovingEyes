package com.dangerfield.movingeyes.libraries.device.impl

import android.content.Context
import com.dangerfield.movingeyes.libraries.device.ScreenMetrics
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Android reports physical pixel density directly, so the millimetre readout
 * here is real rather than inferred.
 *
 * `xdpi` is the manufacturer-declared horizontal density. It is occasionally
 * a rounded lie on cheap hardware, but it's the only physical figure the
 * platform exposes and it's close enough that a hole cut from it lines up.
 * `densityDpi` is deliberately not used as a fallback — it's a bucketed value
 * (160/240/320/…) for scaling UI, not a measurement, and treating it as one
 * would produce a number that looks precise and is off by centimetres.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AndroidScreenMetrics(private val context: Context) : ScreenMetrics {

    private val xdpi: Float? by lazy {
        context.resources.displayMetrics.xdpi.takeIf { it in PlausibleDpiRange }
    }

    override val pixelsPerMillimeter: Float?
        get() = xdpi?.let { it / MillimetersPerInch }

    override val isExact: Boolean get() = xdpi != null

    private companion object {
        const val MillimetersPerInch = 25.4f

        /**
         * Some devices report nonsense (0, or four digits). Anything outside
         * the range of real hardware is treated as "unknown" so the UI hides
         * the millimetre figure instead of printing a fantasy.
         */
        val PlausibleDpiRange = 80f..900f
    }
}
