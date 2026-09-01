package com.dangerfield.movingeyes.libraries.navigation

import kotlinx.serialization.Serializable

/**
 * The design-system catalog, reachable from the shake menu in debug builds.
 * Lives in navigation rather than a feature because the shake menu that opens
 * it is app-level, and the screen it opens is in `:libraries:ui`.
 */
@Serializable
class DesignSystemRoute : Route(
    enter = AnimationType.SlideUp,
    exit = AnimationType.SlideDown,
    popExit = AnimationType.SlideDown,
)
