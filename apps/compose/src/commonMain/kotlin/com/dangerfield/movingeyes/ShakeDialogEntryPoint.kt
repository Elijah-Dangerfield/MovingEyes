package com.dangerfield.movingeyes

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import com.dangerfield.movingeyes.libraries.core.BuildInfo
import com.dangerfield.movingeyes.libraries.navigation.DesignSystemRoute
import com.dangerfield.movingeyes.libraries.navigation.FeatureEntryPoint
import com.dangerfield.movingeyes.libraries.navigation.Router
import com.dangerfield.movingeyes.libraries.navigation.ShakeDialogRoute
import com.dangerfield.movingeyes.libraries.navigation.dialog
import com.dangerfield.movingeyes.libraries.navigation.routeDeepLink
import com.dangerfield.movingeyes.libraries.navigation.screen
import com.dangerfield.movingeyes.libraries.navigation.toRouteOrNull
import com.dangerfield.movingeyes.libraries.ui.catalog.CatalogScreen
import com.dangerfield.movingeyes.libraries.ui.components.Screen
import com.dangerfield.movingeyes.libraries.ui.components.dialog.ShakeAction
import com.dangerfield.movingeyes.libraries.ui.components.dialog.ShakeDialog
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
class ShakeDialogEntryPoint : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        dialog<ShakeDialogRoute> { backStackEntry, dialogState ->
            val route = backStackEntry.toRouteOrNull<ShakeDialogRoute>()

            ShakeDialog(
                state = dialogState,
                headline = route?.headline ?: "I felt that.",
                subtext = route?.subtext,
                onDismiss = { router.goBack() },
                actions = if (BuildInfo.isDebug) {
                    listOf(
                        ShakeAction(
                            label = "Design system",
                            onSelect = {
                                router.goBack()
                                router.navigate(DesignSystemRoute())
                            },
                        )
                    )
                } else {
                    emptyList()
                },
            )
        }

        // Also reachable without shaking, which matters on an emulator and in
        // scripted screenshot runs:
        //   adb shell am start -d "movingeyes://design-system"
        screen<DesignSystemRoute>(
            deepLinks = listOf(routeDeepLink<DesignSystemRoute>("movingeyes://design-system")),
        ) {
            Screen { padding -> CatalogScreen(modifier = Modifier.padding(padding)) }
        }
    }
}
