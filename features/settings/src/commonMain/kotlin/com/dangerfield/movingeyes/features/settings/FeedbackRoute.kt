package com.dangerfield.movingeyes.features.settings

import com.dangerfield.movingeyes.libraries.navigation.TrackableRoute
import kotlinx.serialization.Serializable

/** Not a `data object`: a parameterless route has to stay a class, or iOS
 *  crashes resolving it. See the navigation notes in AGENTS.md. */
@Serializable
class FeedbackRoute : TrackableRoute("feedbackScreenOpens")
