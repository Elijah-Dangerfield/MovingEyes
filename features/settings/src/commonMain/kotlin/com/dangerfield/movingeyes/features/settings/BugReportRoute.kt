package com.dangerfield.movingeyes.features.settings

import com.dangerfield.movingeyes.libraries.navigation.NavigableWhileBlocked
import com.dangerfield.movingeyes.libraries.navigation.TrackableRoute
import kotlinx.serialization.Serializable

@Serializable
data class BugReportRoute(
	val logId: String? = null,
	val errorCode: Int? = null,
	val contextMessage: String? = null,
) : TrackableRoute("bugReportScreenOpens"), NavigableWhileBlocked
