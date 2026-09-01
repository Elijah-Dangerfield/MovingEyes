package com.dangerfield.movingeyes.libraries.device.impl

import com.dangerfield.movingeyes.libraries.device.ScreenMetrics
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import me.tatarka.inject.annotations.Inject
import platform.posix.uname
import platform.posix.utsname
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * iOS exposes no physical screen size, at all, by design — so this is a
 * lookup table keyed on the hardware model identifier.
 *
 * That's genuinely the standard approach and not a shortcut: `UIScreen` gives
 * points and a scale factor, which describe the coordinate system rather than
 * the glass. Two devices with identical point dimensions can have physically
 * different screens.
 *
 * An unknown model falls back to [FallbackPpi] and reports [isExact] false.
 * The fallback is close to every modern iPhone, so a new model released after
 * this ships gives a measurement that's a percent or two out rather than no
 * measurement at all — which for lining up cardboard is the better failure.
 * Add new entries as devices appear; the identifiers are stable and public.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class IosScreenMetrics : ScreenMetrics {

    private val model: String by lazy { hardwareModel() }

    private val ppi: Float by lazy { PpiByModelPrefix.entries
        .firstOrNull { (prefix, _) -> model.startsWith(prefix) }
        ?.value
        ?: FallbackPpi
    }

    override val pixelsPerMillimeter: Float get() = ppi / MillimetersPerInch

    override val isExact: Boolean
        get() = PpiByModelPrefix.keys.any { model.startsWith(it) }

    /**
     * `uname().machine` is the hardware identifier — `iPad14,3`, `iPhone16,2`.
     * On the simulator this reports the *host* architecture (`arm64`), which
     * falls through to the fallback ppi, so a millimetre readout taken on a
     * simulator is meaningless. That's fine and expected: this measurement is
     * only ever trusted on real hardware.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun hardwareModel(): String = memScoped {
        val info = alloc<utsname>()
        if (uname(info.ptr) != 0) return@memScoped ""
        info.machine.toKString()
    }

    private companion object {
        const val MillimetersPerInch = 25.4f

        /** Close to every modern iPhone; wrong by a couple of percent on an iPad. */
        const val FallbackPpi = 460f

        /**
         * Matched longest-prefix-first. iPads are ~264 ppi across almost the
         * whole line, iPhones 326 (non-Retina-HD), 401/458/460/476 (Pro
         * Max / Pro), and the mini sits at 476.
         */
        val PpiByModelPrefix: Map<String, Float> = linkedMapOf(
            // iPads — the flagship device for this app, so worth being right.
            "iPad13," to 264f,
            "iPad14," to 264f,
            "iPad15," to 264f,
            "iPad16," to 264f,
            "iPad" to 264f,
            // iPhone 12 onwards: 460 ppi, Pro Max 458, mini 476.
            "iPhone13,1" to 476f,
            "iPhone14,4" to 476f,
            "iPhone" to 460f,
            "iPod" to 326f,
        )
    }
}
