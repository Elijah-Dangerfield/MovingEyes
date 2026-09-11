package com.dangerfield.movingeyes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dangerfield.movingeyes.libraries.core.Catching
import com.dangerfield.movingeyes.libraries.core.logOnFailure
import com.dangerfield.movingeyes.libraries.core.BuildInfo
import com.dangerfield.movingeyes.libraries.core.Platform
import com.dangerfield.movingeyes.libraries.core.logging.KLog
import com.dangerfield.movingeyes.libraries.navigation.AnimationType
import com.dangerfield.movingeyes.libraries.navigation.FeatureEntryPoint
import com.dangerfield.movingeyes.libraries.navigation.Route
import com.dangerfield.movingeyes.libraries.navigation.floatingwindow.FloatingWindowHost
import com.dangerfield.movingeyes.libraries.navigation.floatingwindow.FloatingWindowNavigator
import com.dangerfield.movingeyes.libraries.navigation.impl.DelegatingRouter
import com.dangerfield.movingeyes.libraries.navigation.serializableType
import com.dangerfield.movingeyes.libraries.navigation.toEnterTransition
import com.dangerfield.movingeyes.libraries.navigation.toExitTransition
import com.dangerfield.movingeyes.libraries.navigation.toRouteOrNull
import com.dangerfield.movingeyes.libraries.movingeyes.Telemetry
import com.dangerfield.movingeyes.libraries.ui.components.Screen
import com.dangerfield.movingeyes.libraries.ui.components.SnackbarDuration
import com.dangerfield.movingeyes.libraries.ui.components.dialog.DialogHost
import com.dangerfield.movingeyes.libraries.ui.components.dialog.LocalDialogHostState
import com.dangerfield.movingeyes.libraries.ui.components.dialog.rememberDialogHostState
import com.dangerfield.movingeyes.libraries.ui.debug.RecompositionCounter
import com.dangerfield.movingeyes.libraries.ui.snackbar.PresenterSnackbarHost
import com.dangerfield.movingeyes.libraries.ui.snackbar.showDebugSnackBar
import com.dangerfield.movingeyes.libraries.ui.system.LocalBuildInfo
import com.dangerfield.movingeyes.libraries.ui.system.LocalClock
import com.dangerfield.movingeyes.system.AppThemeProvider
import kotlin.reflect.typeOf
import kotlin.time.Duration.Companion.seconds

@Composable
fun App(appComponent: AppComponent) {
    val appViewModel = appComponent.appViewModel
    val floatingWindowNavigator = remember { FloatingWindowNavigator() }
    val navController = rememberNavController(floatingWindowNavigator)
    val appRecomposeLogger = remember { KLog.withTag("AppRecompose") }
    val router = remember { appComponent.delegatingRouter }
    val dialogHostState = rememberDialogHostState()

    val shakeHandler = remember { appComponent.shakeHandler }
    val deepLinkBridge = remember { appComponent.deepLinkBridge }

    // Boot-warm every @AutoInit singleton. Resolving the Set forces each
    // contributor to construct, running their `init {}` blocks —
    // AppEventDispatcher attaches its lifecycle observer, connectivity
    // watchers arm, etc. Wrapped in `remember` so this resolves exactly once
    // per composition lifetime, even though App() recomposes. See [AutoInit]
    // for the contract. (Android also resolves this in Application.onCreate,
    // before the first Activity; resolving twice is a no-op.)
    remember { appComponent.autoInits }

    // Lifecycle, not composition. A DisposableEffect only tears down when the
    // composition goes away, which backgrounding does not do, so the
    // accelerometer stayed live in a pocket while DelegatingRouter held its
    // queue under repeatOnLifecycle(STARTED). A jostle queued a navigation
    // that fired the moment the app came back, which looks like a bug report
    // opening itself on resume.
    LifecycleStartEffect(shakeHandler) {
        shakeHandler.start()
        onStopOrDispose {
            shakeHandler.stop()
        }
    }

    LaunchedEffect(navController, deepLinkBridge) {
        deepLinkBridge.urls.collect { url ->
            Catching {
                val request = NavDeepLinkRequest.Builder.fromUri(NavUri(url)).build()
                navController.handleDeepLink(request)
            }.logOnFailure { "Failed to handle deep link: $url" }
        }
    }

    RecompositionCounter(
        tag = "App",
        logEvery = 1,
        rapidRecompositionThreshold = 6,
        rapidRecompositionWindow = 60.seconds,
        onRecompose = { count ->
            val message = if (count == 1L) {
                "App recomposed (this should be rare)"
            } else {
                "App recomposed $count times"
            }
            appRecomposeLogger.w { message }
        },
        onRapidRecomposition = { info ->
            appRecomposeLogger.e {
                "Rapid recompositions: ${info.countInWindow} in ${info.windowMillis}ms (total=${info.totalCount})"
            }
            showDebugSnackBar(
                title = "Performance hiccup",
                message = "App recomposed ${info.countInWindow}× in ${info.windowMillis}ms.",
                duration = SnackbarDuration.Long,
                withDismissAction = true,
            )
        }
    )

    CompositionLocalProvider(
        LocalClock provides appComponent.provideClock(),
        LocalBuildInfo provides BuildInfo,
        LocalDialogHostState provides dialogHostState
    ) {
        AppThemeProvider {
            Box(modifier = Modifier.fillMaxSize()) {
                // Two-stage boot: the platform splash (keyed on
                // appViewModel.isReady) covers the start-destination resolve,
                // then this gate covers whatever boot work follows. Both are
                // instant today; the gate is what a scene hydrate would hang
                // off so the canvas renders the real scene on frame one.
                val bootComplete by appViewModel.isBootComplete.collectAsState()
                val startDestination by appViewModel.startDestination.collectAsState()
                val route = startDestination
                if (bootComplete && route != null) {
                    AppNavigation(
                        navController = navController,
                        floatingWindowNavigator = floatingWindowNavigator,
                        featureEntryPoints = appComponent.featureEntryPoints,
                        startDestination = route,
                        router = router,
                        telemetry = appComponent.telemetry,
                    )
                } else {
                    BootLoadingScreen()
                }

                SplashGate()

                DialogHost(
                    modifier = Modifier.matchParentSize(),
                    hostState = dialogHostState
                )
            }
        }
    }
}

/**
 * The simple class name of a destination's route, for telemetry tagging.
 *
 * Type-safe nav stores the route as its serializer name — the fully-qualified
 * class name followed by argument placeholders, e.g.
 * `com.dangerfield.movingeyes.features.editor.EditorRoute`. We strip the args
 * and the package to get just `EditorRoute`, keeping the tag low-cardinality and
 * readable. Returns null for unnamed/graph destinations.
 */
private fun NavDestination.routeClassNameOrNull(): String? =
    route
        ?.substringBefore('/')
        ?.substringBefore('?')
        ?.substringAfterLast('.')
        ?.takeIf { it.isNotBlank() }

@Composable
private fun AppNavigation(
    navController: NavHostController,
    floatingWindowNavigator: FloatingWindowNavigator,
    featureEntryPoints: Set<FeatureEntryPoint>,
    startDestination: Route,
    router: DelegatingRouter,
    telemetry: Telemetry,
) {
    // Tag every crash/error with the route the user is currently on. Sheets
    // and dialogs are real destinations on this same back stack (see
    // `bottomSheet`/`dialog` nav builders), so an open sheet wins over the
    // screen beneath it — exactly the granularity we want for triage. Pushed
    // from a LaunchedEffect keyed on the name so we only touch the Sentry
    // scope when the route actually changes, not on every recomposition.
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteName = currentBackStackEntry?.destination?.routeClassNameOrNull()
    LaunchedEffect(currentRouteName) {
        currentRouteName?.let { telemetry.setCurrentRoute(it) }
    }

    // Remember the graph-builder lambda so recompositions of AppNavigation
    // hand NavHost the SAME builder instance. A fresh lambda each pass makes
    // NavHost treat the graph as changed and re-push the start destination
    // onto the back stack.
    val graph: NavGraphBuilder.() -> Unit = remember(featureEntryPoints, router) {
        {
            featureEntryPoints.forEach { entryPoint ->
                with(entryPoint) {
                    buildNavGraph(router)
                }
            }
        }
    }

    Screen(
        snackbarHost = {
            PresenterSnackbarHost()
        },
        content = {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                //To make this more readable consider Screens A and B
                enterTransition = {
                    // A -> B
                    // How should we animate the B screen?
                    // Enter animation should match B's Enter
                    val targetRoute = targetState.toRouteOrNull<Route>()
                    val (animationType, reason) = when {
                        targetRoute != null -> targetRoute.enter to "Using target route enter animation"
                        else -> AnimationType.None to "Target destination is not a Route; default to none"
                    }

                    animationType.toEnterTransition()
                },
                popEnterTransition = {
                    // Popping from B back to A
                    // How should we animate the A screen?
                    // Enter animation should match initials pop EXIT transition
                    // AKA if B slides out, A should slide IN
                    val initialRoute = initialState.toRouteOrNull<Route>()
                    val targetRoute = targetState.toRouteOrNull<Route>()
                    val (animationType, reason) = when {
                        initialRoute != null -> initialRoute.popExit.opposite() to "Mirroring initial popExit animation"
                        targetRoute != null -> targetRoute.enter to "Fallback to target route enter animation"
                        else -> AnimationType.None to "No route metadata; default to none"
                    }

                    animationType.toEnterTransition()
                },
                exitTransition = {
                    // A -> B
                    // Initial: A | Target B
                    // How should we animate the A screen
                    // Exit animation should match A's Exit
                    val initialRoute = initialState.toRouteOrNull<Route>()
                    val (animationType, reason) = when {
                        initialRoute != null -> initialRoute.exit to "Using initial route exit animation"
                        else -> AnimationType.None to "Initial destination is not a Route; default to none"
                    }

                    animationType.toExitTransition()
                },
                popExitTransition = {
                    // Popping from B back to A
                    // Initial: B | Target A
                    // How should we animate the B screen
                    // Exit animation should match B's pope Exit
                    val initialRoute = initialState.toRouteOrNull<Route>()

                    val (animationType, reason) = when {
                        initialRoute != null -> initialRoute.popExit to "Using initial route popExit animation"
                        else -> AnimationType.None to "Initial destination is not a Route; default to none"
                    }

                    animationType.toExitTransition()
                },
                typeMap = mapOf(
                    typeOf<AnimationType>() to serializableType<AnimationType>()
                ),
                builder = graph
            )

            FloatingWindowHost(floatingWindowNavigator)

            router.Bind(navController)
        },
    )
}

/**
 * Isolates the splash-overlay's `hasShownSplash` state read into its own composable
 * so that flipping it cannot recompose `App` and, in particular, cannot cause
 * `AppNavigation` / `NavHost` to rebuild its graph — which was previously pushing
 * a second copy of the start destination onto the back stack.
 *
 * Only renders on iOS. Android uses the native splash API instead.
 */
@Composable
private fun SplashGate() {
    if (BuildInfo.platform != Platform.iOS) return
    var hasShownSplash by rememberSaveable { mutableStateOf(false) }
    if (!hasShownSplash) {
        SplashOverlay(onComplete = { hasShownSplash = true })
    }
}
