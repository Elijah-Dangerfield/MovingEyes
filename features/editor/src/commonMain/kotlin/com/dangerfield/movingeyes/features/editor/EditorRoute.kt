package com.dangerfield.movingeyes.features.editor

import com.dangerfield.movingeyes.libraries.navigation.Route
import kotlinx.serialization.Serializable

/**
 * The app's only start destination. Moving Eyes opens straight onto a live
 * canvas — there is no gallery, no home, and no onboarding in front of it.
 */
@Serializable
class EditorRoute : Route()
