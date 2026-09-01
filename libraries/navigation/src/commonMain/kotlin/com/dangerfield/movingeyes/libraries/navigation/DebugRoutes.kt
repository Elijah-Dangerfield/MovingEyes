package com.dangerfield.movingeyes.libraries.navigation

import kotlinx.serialization.Serializable

/**
 * Debug-only destinations, reachable from the shake menu. They live in
 * navigation rather than a feature because the menu that opens them is
 * app-level and the screens they open aren't a feature of their own.
 */

/** The design-system catalog, rendered from `:libraries:ui`. */
@Serializable
class DesignSystemRoute : Route(
    enter = AnimationType.SlideUp,
    exit = AnimationType.SlideDown,
    popExit = AnimationType.SlideDown,
)

/** Every ConfiguredValue in the graph, with a local override per row. */
@Serializable
class QaConfigRoute : Route(
    enter = AnimationType.SlideUp,
    exit = AnimationType.SlideDown,
    popExit = AnimationType.SlideDown,
)
