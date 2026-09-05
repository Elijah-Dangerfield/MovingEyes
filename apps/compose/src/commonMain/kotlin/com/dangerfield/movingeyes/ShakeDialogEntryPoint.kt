package com.dangerfield.movingeyes

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.compose.runtime.getValue
import com.dangerfield.movingeyes.libraries.navigation.DesignSystemRoute
import com.dangerfield.movingeyes.libraries.navigation.FeatureEntryPoint
import com.dangerfield.movingeyes.libraries.navigation.Router
import com.dangerfield.movingeyes.libraries.navigation.EyeGalleryRoute
import com.dangerfield.movingeyes.libraries.navigation.QaConfigRoute
import com.dangerfield.movingeyes.libraries.navigation.dialog
import com.dangerfield.movingeyes.libraries.navigation.routeDeepLink
import com.dangerfield.movingeyes.libraries.navigation.screen
import com.dangerfield.movingeyes.libraries.navigation.toRouteOrNull
import com.dangerfield.movingeyes.libraries.ui.catalog.CatalogScreen
import com.dangerfield.movingeyes.libraries.ui.components.Screen
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * The debug menu. `ShakeHandler` only arms the accelerometer in debug builds,
 * so nothing here can surface in a store binary — but the actions are still
 * gated on [BuildInfo.isDebug] so a release build can't route to a screen that
 * exists purely for development.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true)
@Inject
class ShakeDialogEntryPoint(
    private val qaConfigViewModelFactory: () -> QaConfigViewModel,
) : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        // Reached by deep link only, now that shake files a bug report instead
        // of opening a menu. Which is also what makes them usable from a
        // script and on a device with no accelerometer:
        //   adb shell am start -d "movingeyes://design-system"
        screen<DesignSystemRoute>(
            deepLinks = listOf(routeDeepLink<DesignSystemRoute>("movingeyes://design-system")),
        ) {
            Screen { padding -> CatalogScreen(modifier = Modifier.padding(padding)) }
        }

        //   adb shell am start -d "movingeyes://eyes"
        screen<EyeGalleryRoute>(
            deepLinks = listOf(routeDeepLink<EyeGalleryRoute>("movingeyes://eyes")),
        ) {
            Screen { padding -> EyeGalleryScreen(modifier = Modifier.padding(padding)) }
        }

        //   adb shell am start -d "movingeyes://qa-config"
        screen<QaConfigRoute>(
            deepLinks = listOf(routeDeepLink<QaConfigRoute>("movingeyes://qa-config")),
        ) {
            val viewModel: QaConfigViewModel = viewModel { qaConfigViewModelFactory() }
            val overrides by viewModel.overrides.collectAsStateWithLifecycle()
            val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
            val lastBillingResult by viewModel.lastBillingResult.collectAsStateWithLifecycle()
            Screen { padding ->
                QaConfigScreen(
                    values = viewModel.values,
                    overrides = overrides,
                    isUnlocked = isUnlocked,
                    lastBillingResult = lastBillingResult,
                    onOverride = viewModel::override,
                    onClearAll = viewModel::clearAll,
                    onPurchase = viewModel::purchase,
                    onRestore = viewModel::restore,
                    onClearEntitlement = viewModel::clearEntitlement,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}
