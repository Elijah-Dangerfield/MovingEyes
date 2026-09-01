package com.dangerfield.movingeyes.libraries.device

import kotlinx.coroutines.flow.StateFlow

/**
 * Charge level and whether it's going up.
 *
 * Exists for one narrow purpose: telling someone their decoration is about to
 * die, while it is behind a painting and they are not looking at it. That
 * shapes the whole design — see [BatteryNotice].
 */
interface BatteryStatus {
    val state: StateFlow<BatteryState>
}

/**
 * @param percent 0..100, or null when the platform hasn't reported yet
 * @param isCharging true when plugged in and gaining charge
 */
data class BatteryState(
    val percent: Int? = null,
    val isCharging: Boolean = false,
) {
    /** Below this and unplugged, the user needs telling. */
    val isLow: Boolean get() = !isCharging && (percent ?: 100) <= LowBatteryPercent

    companion object {
        const val LowBatteryPercent = 20
    }
}

/**
 * Decides when the battery pill should appear during a display session.
 *
 * Pulled out of the UI and made pure because the rule is fussy and the failure
 * modes are opposite and both bad: nagging every minute over a scene someone
 * is trying to enjoy, or saying nothing until the tablet dies at 9pm on the
 * 31st.
 *
 * The rule: show once on entry as reassurance, then stay silent until the
 * charge is genuinely low, and after that speak **once per ten-percent step**
 * rather than continuously. Someone who has been told at 20% does not need
 * telling again at 19%.
 */
class BatteryNotice {

    private var lastAnnouncedStep: Int? = null

    /**
     * True when [state] deserves a pill right now. Call on every change; it
     * remembers what it has already said.
     */
    fun shouldAnnounce(state: BatteryState): Boolean {
        val percent = state.percent ?: return false

        if (!state.isLow) {
            // Charging or comfortable: forget what we announced, so a device
            // that is unplugged again later gets a fresh warning rather than
            // being silenced by a step it crossed hours ago.
            lastAnnouncedStep = null
            return false
        }

        // Offset by one so the bands are 20..11 and 10..1. Dividing the raw
        // percentage instead puts 20 and 19 in different bands, which fires two
        // warnings back to back the moment the battery crosses the threshold.
        val step = (percent - 1).coerceAtLeast(0) / StepPercent
        if (step == lastAnnouncedStep) return false
        lastAnnouncedStep = step
        return true
    }

    private companion object {
        const val StepPercent = 10
    }
}
