package com.dangerfield.movingeyes.libraries.device

import kotlinx.coroutines.flow.StateFlow

interface BatteryStatus {
    val state: StateFlow<BatteryState>
}

/** [percent] is null until the platform has reported. */
data class BatteryState(
    val percent: Int? = null,
    val isCharging: Boolean = false,
) {
    val isLow: Boolean get() = !isCharging && (percent ?: 100) <= LowBatteryPercent

    companion object {
        const val LowBatteryPercent = 20
    }
}

/**
 * Decides when the battery pill should appear during a display session: once
 * the charge is low, and then once per ten-percent band rather than on every
 * reading.
 *
 * Stateful, so one instance per session.
 */
class BatteryNotice {

    private var lastAnnouncedStep: Int? = null

    fun shouldAnnounce(state: BatteryState): Boolean {
        val percent = state.percent ?: return false

        if (!state.isLow) {
            // Forget the band so a device unplugged again later warns afresh.
            lastAnnouncedStep = null
            return false
        }

        // Offset by one so the bands are 20..11 and 10..1. Dividing the raw
        // percentage puts 20 and 19 in different bands and warns twice on the
        // way in.
        val step = (percent - 1).coerceAtLeast(0) / StepPercent
        if (step == lastAnnouncedStep) return false
        lastAnnouncedStep = step
        return true
    }

    private companion object {
        const val StepPercent = 10
    }
}
