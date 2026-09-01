package com.dangerfield.movingeyes

import androidx.lifecycle.ViewModel
import com.dangerfield.movingeyes.features.editor.EditorRoute
import com.dangerfield.movingeyes.libraries.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * App-level ViewModel. There is exactly one destination to resolve — Moving
 * Eyes opens on the canvas, always — so boot has nothing to wait on: no
 * account, no remote config, no profile fetch. Both readiness signals flip
 * immediately, which is what keeps cold start inside the 1.5s budget.
 *
 * The two signals are kept distinct because they release different things:
 * [isReady] releases the platform splash, [isBootComplete] releases the
 * Compose boot gate. If a future phase adds real boot work — hydrating the
 * autosaved scene from Room, say — it belongs behind [isBootComplete] so the
 * canvas renders the user's real scene on frame one rather than flashing an
 * empty one.
 *
 * Scoped as a singleton so Android's splash-screen API can read the same
 * instance the App composable uses.
 */
@SingleIn(AppScope::class)
@Inject
class AppViewModel : ViewModel() {

    private val _startDestination = MutableStateFlow<Route?>(EditorRoute())
    val startDestination: StateFlow<Route?> = _startDestination.asStateFlow()

    private val _isReady = MutableStateFlow(true)

    /**
     * Exposed to Android's splash screen API for keepOnScreenCondition.
     */
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isBootComplete = MutableStateFlow(true)

    /**
     * The App composable holds the boot-loading screen until this flips.
     */
    val isBootComplete: StateFlow<Boolean> = _isBootComplete.asStateFlow()
}
