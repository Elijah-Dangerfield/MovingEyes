package com.dangerfield.movingeyes.libraries.ui.system

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.dangerfield.movingeyes.libraries.ui.system.color.ColorResource
import com.dangerfield.movingeyes.libraries.core.BuildInfo
import com.dangerfield.movingeyes.system.color.Colors
import com.dangerfield.movingeyes.system.typography.Typography
import kotlin.time.Clock

val LocalColors = compositionLocalOf<Colors> {
    error("Theme wasn't applied")
}

val LocalContentColor = compositionLocalOf<ColorResource> {
    error("Theme wasn't applied")
}

val LocalTypography = compositionLocalOf<Typography> {
    error("Theme wasn't applied")
}

val LocalBuildInfo = staticCompositionLocalOf<BuildInfo> {
    error("No LocalBuildInfo provided")
}

val LocalClock = staticCompositionLocalOf<Clock> {
    error("No LocalClock provided")
}