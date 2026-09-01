package com.dangerfield.movingeyes.features.paywall

import com.dangerfield.movingeyes.libraries.navigation.AnimationType
import com.dangerfield.movingeyes.libraries.navigation.Route
import kotlinx.serialization.Serializable

/**
 * [trigger] names what the user was reaching for, so the paywall can lead with
 * the thing they already wanted rather than a generic pitch.
 */
@Serializable
data class PaywallRoute(
    val trigger: PaywallTrigger = PaywallTrigger.Unspecified,
) : Route(
    enter = AnimationType.SlideUp,
    exit = AnimationType.SlideDown,
    popExit = AnimationType.SlideDown,
)

@Serializable
enum class PaywallTrigger {
    Motion,
    EyeStyle,
    Reactivity,
    Settings,
    Unspecified,
}
