package com.dangerfield.movingeyes.libraries.config.impl.data

import com.dangerfield.movingeyes.libraries.core.logging.KLog
import com.dangerfield.movingeyes.libraries.config.AppConfigMap
import com.dangerfield.movingeyes.libraries.config.impl.model.BasicMapAppConfig
import com.dangerfield.movingeyes.libraries.config.impl.serialization.ConfigJsonConverter
import com.dangerfield.movingeyes.libraries.core.Catching
import com.dangerfield.movingeyes.libraries.core.flatMap
import com.dangerfield.movingeyes.libraries.flowroutines.DispatcherProvider
import com.dangerfield.movingeyes.libraries.networking.AuthTokenProvider
import com.dangerfield.movingeyes.libraries.networking.NetworkClient
import com.dangerfield.movingeyes.libraries.networking.retry.RetryPolicy
import com.dangerfield.movingeyes.libraries.networking.unauthedCall
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Fetches the app config tree from the app server.
 *
 * Returns a sparse JSON object whose keys are the dotted [com.dangerfield.movingeyes.libraries.config.ConfiguredValue.path]s
 * that the server wants to override. Anything missing falls back to the
 * client-side default declared on the [com.dangerfield.movingeyes.libraries.config.ConfiguredValue].
 *
 * The call stays **unauthenticated** so the kill-switch / forced-upgrade flags
 * still load when auth is down or before the first sign-in. But when a session
 * token is already on hand we attach it best-effort (a non-blocking peek — no
 * [AuthTokenProvider.awaitReady]), so the server can resolve the user id for
 * per-user targeting + rollout bucketing. No token → no header → the server
 * targets on the install-id / client-context axes instead.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RemoteConfigRemoteDataSource @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val networkClient: NetworkClient,
    private val authTokenProvider: AuthTokenProvider,
    private val converter: ConfigJsonConverter,
) : RemoteConfigDataSource {

    private val logger = KLog.withTag("RemoteConfigDataSource")

    override suspend fun getConfig(): Catching<AppConfigMap> = withContext(dispatcherProvider.io) {
        val token = authTokenProvider.accessToken()
        networkClient.unauthedCall(
            description = "appConfig.fetch",
            retry = RetryPolicy.idempotent(),
        ) { client ->
            client.get("/v1/app-config") {
                if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
            }.body<String>()
        }
            .flatMap { raw -> converter.decodeToMap(raw).map { BasicMapAppConfig(it) } }
            .onSuccess { logger.d { "Fetched remote app config" } }
    }
}
