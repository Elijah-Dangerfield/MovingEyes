package com.dangerfield.movingeyes.libraries.core

import com.dangerfield.movingeyes.buildinfo.MovingEyesBuildConfig

/**
 * Build-time-injected telemetry credentials. CI reads repo secrets, local
 * builds read `local.properties` (`sentry.dsn` — see `loadTelemetryMetadata`
 * in build-logic). A blank value leaves crash reporting dormant rather than
 * failing, so a fresh clone builds and runs with zero setup.
 */
object TelemetryInfo {
    /** Single Sentry DSN for all platforms and build types. The `environment`
     *  tag (releaseChannel-platform-buildType) separates them within one
     *  project. Blank → crash reporting disabled. */
    val sentryDsn: String
        get() = MovingEyesBuildConfig.SENTRY_DSN
}
