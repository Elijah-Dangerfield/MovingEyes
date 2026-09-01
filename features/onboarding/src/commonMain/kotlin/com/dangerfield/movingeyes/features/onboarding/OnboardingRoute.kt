package com.dangerfield.movingeyes.features.onboarding

import com.dangerfield.movingeyes.libraries.navigation.AnimationType
import com.dangerfield.movingeyes.libraries.navigation.Route
import kotlinx.serialization.Serializable

/**
 * First-launch landing + identity bootstrap. Only the start destination on
 * first install — once the user finishes the flow we set `hasUserOnboarded`,
 * navigate to Home, and never return here unless onboarding is reset.
 *
 * Fade transitions because the onboarding → home jump is conceptually a
 * mode switch, not a "drill into" navigation.
 */
@Serializable
class OnboardingRoute : Route(
    enter = AnimationType.FadeIn,
    exit = AnimationType.FadeOut,
    popExit = AnimationType.FadeOut,
)
