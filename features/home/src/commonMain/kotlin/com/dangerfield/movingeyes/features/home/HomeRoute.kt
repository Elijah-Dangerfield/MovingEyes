package com.dangerfield.movingeyes.features.home

import com.dangerfield.movingeyes.libraries.navigation.Route
import com.dangerfield.movingeyes.libraries.navigation.TrackableRoute
import kotlinx.serialization.Serializable

@Serializable
class HomeRoute : Route()

@Serializable
data class SettingsRoute(
    val visitCount: Int = 1,
) : TrackableRoute("settingsVisits")

@Serializable
class FeedbackRoute : TrackableRoute("feedbackScreenOpens")
