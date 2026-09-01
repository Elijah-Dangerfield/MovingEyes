package com.dangerfield.movingeyes.libraries.config.impl.data

import com.dangerfield.movingeyes.libraries.config.AppConfigMap
import com.dangerfield.movingeyes.libraries.config.impl.model.BasicMapAppConfig
import com.dangerfield.movingeyes.libraries.config.impl.serialization.ConfigJsonConverter
import com.dangerfield.movingeyes.libraries.core.Catching
import com.dangerfield.movingeyes.libraries.core.flatMap
import com.dangerfield.movingeyes.libraries.core.logging.KLog
import com.dangerfield.movingeyes.libraries.flowroutines.DispatcherProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Fetches the app config tree from a **static JSON file on GitHub Pages** —
 * the same site that already serves the privacy policy. There is no server.
 *
 * Returns a sparse JSON object whose keys are the dotted
 * [com.dangerfield.movingeyes.libraries.config.ConfiguredValue.path]s to
 * override. Anything missing falls back to the client-side default declared on
 * the value itself, so an empty `{}` (or an unreachable file, or a mangled one)
 * behaves exactly like "no overrides" rather than breaking the app.
 *
 * **Why this exists at all** for an app with no backend: the entire year's
 * traffic arrives in a ten-day window, and a store review takes days. If a
 * mood turns out to strobe harder than the photosensitivity gate assumed, or a
 * paywall line needs rewording, or the telemetry pipe needs muting because
 * ingest is on fire, editing one file in this repo beats waiting on Apple.
 *
 * Deliberately unauthenticated and deliberately dumb: one GET, a short
 * timeout, no retry beyond what the caller does. It is never on the critical
 * path — [com.dangerfield.movingeyes.libraries.config.impl.repository
 * .OfflineFirstAppConfigRepository] serves the cached or bundled map first and
 * folds this in when it lands.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RemoteConfigRemoteDataSource @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val converter: ConfigJsonConverter,
) : RemoteConfigDataSource {

    private val logger = KLog.withTag("RemoteConfigDataSource")

    /**
     * Built here rather than injected. `HttpClient` is a generic type with no
     * qualifier, so putting one in the app graph would make the *next* module
     * that wants a client with different plugins a collision. Telemetry owns
     * its client for the same reason. Lazy so an app that never reaches a
     * refresh never constructs an engine.
     *
     * No engine is named: exactly one is on the classpath per platform (OkHttp
     * on Android, Darwin on iOS), so Ktor resolves it and this stays in
     * commonMain with no expect/actual.
     */
    private val httpClient by lazy {
        HttpClient {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = RequestTimeoutMillis
                connectTimeoutMillis = RequestTimeoutMillis
            }
        }
    }

    override suspend fun getConfig(): Catching<AppConfigMap> = withContext(dispatcherProvider.io) {
        Catching {
            val response = httpClient.get(RemoteConfigUrl)
            when {
                response.status.isSuccess() -> response.bodyAsText()

                // Nothing published yet, or the file was deliberately removed.
                // That is a legitimate state meaning "no overrides", identical
                // to an empty object — not a failure. Returning one would make
                // the repository log a warning with a stack trace on every
                // single launch, which then ships to Loki forever.
                response.status == HttpStatusCode.NotFound -> EmptyConfig

                // Anything else (5xx, a captive-portal redirect) is a real
                // fetch failure: the cache or the bundled fallback stands.
                else -> error("Config fetch returned ${response.status}")
            }
        }
            // Pages serves an HTML error page rather than a bare status, so a
            // non-2xx body would hand the converter a chunk of HTML and produce
            // a confusing decode error instead of an honest fetch failure.
            .flatMap { raw -> converter.decodeToMap(raw).map { BasicMapAppConfig(it) } }
            .onSuccess { logger.d { "Fetched remote app config" } }
    }

    private companion object {
        /**
         * Update the host if the repo or GitHub user changes. Publishing a new
         * config is: edit `pages/app-config.json`, merge, and the Pages
         * workflow does the rest.
         */
        const val RemoteConfigUrl = "https://elijahdangerfield.github.io/MovingEyes/app-config.json"

        /** Short on purpose: a slow fetch must never delay the first frame. */
        const val RequestTimeoutMillis = 5_000L

        /** What a 404 resolves to — the same thing an empty published file means. */
        const val EmptyConfig = "{}"
    }
}

