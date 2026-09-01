package com.dangerfield.movingeyes.libraries.core

import com.dangerfield.movingeyes.buildinfo.MovingEyesBuildConfig

/**
 * Build-time-injected telemetry credentials. CI reads repo secrets, local
 * builds read `local.properties` (`sentry.dsn`, `grafana.*` — see
 * `loadTelemetryMetadata` in build-logic). Blank values leave the
 * corresponding pipe dormant rather than failing, so a fresh clone builds and
 * runs with zero setup.
 */
object TelemetryInfo {
    /** Single Sentry DSN for all platforms and build types. The `environment`
     *  tag (releaseChannel-platform-buildType) separates them within one
     *  project. Blank → crash reporting disabled. */
    val sentryDsn: String
        get() = MovingEyesBuildConfig.SENTRY_DSN

    val grafanaOtlpBaseUrl: String
        get() = MovingEyesBuildConfig.GRAFANA_OTLP_BASE_URL

    val grafanaOtlpInstanceId: String
        get() = MovingEyesBuildConfig.GRAFANA_OTLP_INSTANCE_ID

    val grafanaLogsWriteToken: String
        get() = MovingEyesBuildConfig.GRAFANA_LOGS_WRITE_TOKEN
}
