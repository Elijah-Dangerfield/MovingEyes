package com.dangerfield.movingeyes.libraries.telemetry.impl

import com.dangerfield.movingeyes.libraries.core.BuildInfo
import com.dangerfield.movingeyes.libraries.core.Catching
import com.dangerfield.movingeyes.libraries.core.Platform
import com.dangerfield.movingeyes.libraries.core.logging.EXTRA_APP_EVENT
import com.dangerfield.movingeyes.libraries.core.logging.LogEntry
import com.dangerfield.movingeyes.libraries.core.logging.LogId
import com.dangerfield.movingeyes.libraries.core.logging.LogLevel
import com.dangerfield.movingeyes.libraries.core.logging.LogTree
import com.dangerfield.movingeyes.libraries.core.versionString
import io.opentelemetry.kotlin.OpenTelemetry
import io.opentelemetry.kotlin.createOpenTelemetry
import io.opentelemetry.kotlin.export.TelemetryCloseable
import io.opentelemetry.kotlin.init.LogExportConfigDsl
import io.opentelemetry.kotlin.logging.SeverityNumber
import io.opentelemetry.kotlin.logging.export.LogRecordProcessor

/**
 * Forwards `logEvent` entries (those carrying [EXTRA_APP_EVENT]) to Grafana
 * Cloud as OTLP log records. Planted alongside KermitLogTree/SentryLogTree,
 * so the same entry reaches logcat and Sentry breadcrumbs through those
 * trees regardless of what this one does.
 *
 * Second mode ([klogForwardingEnabled], `telemetry.klogForwardingEnabled`):
 * plain Warn+ entries are forwarded too, as ordinary OTLP logs — body only,
 * no event name — so client errors land in Loki without waiting on a Sentry
 * crash. Events are never gated by that flag; both modes sit behind the
 * kill switch and per-session sampling.
 *
 * Direct to Grafana, because there is nowhere else to send it — this app has
 * no backend. Sentry answers "what crashed"; this answers "what did people
 * actually do", which for a decoration used one night a year is the whole
 * question. `display_session_ended` in particular: if people enter display
 * mode and leave after thirty seconds, the illusion isn't working, and no
 * amount of new eye styles fixes that.
 *
 * `session_id` and `install_id` ride on every record rather than sitting on
 * the resource, because the session rolls over mid-process after a 15-minute
 * background. There is deliberately no `is_offline` attribute: the app makes
 * no requests of its own, so it has no connectivity observer to read, and a
 * hardcoded `false` would be worse than an absent field. The durable buffer
 * already covers what that attribute was really for — a record emitted with
 * no network survives process death and ships on a later launch.
 *
 * All OTel types stay confined to this class: if 0.5.0 misbehaves, the tree
 * gets re-backed without touching call sites.
 */
class GrafanaLogTree(
    private val exportEnabled: () -> Boolean,
    private val sampleRate: () -> Double,
    private val klogForwardingEnabled: () -> Boolean,
    private val currentSessionId: () -> String?,
    private val currentInstallId: () -> String?,
    private val processorFactory: LogExportConfigDsl.() -> LogRecordProcessor,
) : LogTree() {

    private val openTelemetryLazy: Lazy<OpenTelemetry> = lazy {
        createOpenTelemetry {
            loggerProvider {
                serviceName = SERVICE_NAME
                resource {
                    setStringAttribute("service.version", BuildInfo.versionString())
                    setStringAttribute("deployment.environment", if (BuildInfo.isDebug) "dev" else "prod")
                    setStringAttribute(
                        "platform",
                        when (BuildInfo.platform) {
                            Platform.Android -> "android"
                            Platform.iOS -> "ios"
                        },
                    )
                    setLongAttribute("build_number", BuildInfo.buildNumber.toLong())
                    setStringAttribute("commit_sha", BuildInfo.commitSha)
                    setStringAttribute("release_channel", BuildInfo.releaseChannel)
                }
                export { processorFactory() }
            }
        }
    }

    private val openTelemetry: OpenTelemetry by openTelemetryLazy

    private val eventLogger by lazy {
        openTelemetry.loggerProvider.getLogger(name = SERVICE_NAME, version = BuildInfo.versionString())
    }

    /**
     * Pushes any batched-in-RAM records through the disk buffer and attempts
     * an export. Called on app background ([TelemetryBackgroundFlusher]) so
     * the tail of a session doesn't ride only in memory when the OS suspends
     * or kills the process. A tree that never exported anything has no SDK to
     * flush — don't construct one just to flush it.
     */
    suspend fun flushExports() {
        if (!openTelemetryLazy.isInitialized()) return
        Catching { (openTelemetry as? TelemetryCloseable)?.forceFlush() }
    }

    override fun log(entry: LogEntry): LogId? {
        val eventName = entry.context.extras[EXTRA_APP_EVENT] as? String
        val forwardAsPlainLog = eventName == null &&
            entry.level.priority >= LogLevel.Warn.priority &&
            klogForwardingEnabled()
        if (eventName != null || forwardAsPlainLog) {
            Catching { forward(eventName, entry) }
        }
        return null
    }

    private fun forward(eventName: String?, entry: LogEntry) {
        if (!exportEnabled()) return
        val sessionId = currentSessionId()
        if (!isSessionSampledIn(sessionId)) return

        eventLogger.emit(
            body = entry.message ?: entry.throwable?.toString(),
            eventName = eventName,
            severityNumber = entry.level.toSeverityNumber(),
            attributes = {
                sessionId?.let { setStringAttribute(SESSION_ID_KEY, it) }
                currentInstallId()?.let { setStringAttribute(INSTALL_ID_KEY, it) }
                if (eventName == null) {
                    entry.tag?.let { setStringAttribute(TAG_KEY, it) }
                    entry.throwable?.let {
                        setStringAttribute(EXCEPTION_TYPE_KEY, it::class.simpleName ?: "Throwable")
                        it.message?.let { m -> setStringAttribute(EXCEPTION_MESSAGE_KEY, m) }
                    }
                }
                entry.context.tags.forEach { (key, value) -> setStringAttribute(key, value) }
                entry.context.extras.forEach { (key, value) ->
                    if (key == EXTRA_APP_EVENT) return@forEach
                    when (value) {
                        null -> Unit
                        is String -> setStringAttribute(key, value)
                        is Boolean -> setBooleanAttribute(key, value)
                        is Int -> setLongAttribute(key, value.toLong())
                        is Long -> setLongAttribute(key, value)
                        is Float -> setDoubleAttribute(key, value.toDouble())
                        is Double -> setDoubleAttribute(key, value)
                        else -> setStringAttribute(key, value.toString())
                    }
                }
            },
        )
    }

    /**
     * Stable per-session decision: hash the session id into [0, 1) and
     * compare against the rate, so one session's events are all-or-nothing
     * and a funnel never loses its middle step to per-event dice rolls.
     */
    private fun isSessionSampledIn(sessionId: String?): Boolean {
        val rate = sampleRate().coerceIn(0.0, 1.0)
        if (rate >= 1.0) return true
        if (rate <= 0.0) return false
        val id = sessionId ?: return false
        val bucket = (id.hashCode().toLong() and 0x7FFFFFFFL).toDouble() / Int.MAX_VALUE.toDouble()
        return bucket < rate
    }

    private fun LogLevel.toSeverityNumber(): SeverityNumber = when (this) {
        LogLevel.Verbose -> SeverityNumber.TRACE
        LogLevel.Debug -> SeverityNumber.DEBUG
        LogLevel.Info -> SeverityNumber.INFO
        LogLevel.Warn -> SeverityNumber.WARN
        LogLevel.Error -> SeverityNumber.ERROR
        LogLevel.Assert, LogLevel.Fatal -> SeverityNumber.FATAL
    }

    companion object {
        const val SERVICE_NAME = "movingeyes-client"
        private const val SESSION_ID_KEY = "session_id"
        private const val INSTALL_ID_KEY = "install_id"
        private const val TAG_KEY = "tag"
        private const val EXCEPTION_TYPE_KEY = "exception_type"
        private const val EXCEPTION_MESSAGE_KEY = "exception_message"
    }
}
