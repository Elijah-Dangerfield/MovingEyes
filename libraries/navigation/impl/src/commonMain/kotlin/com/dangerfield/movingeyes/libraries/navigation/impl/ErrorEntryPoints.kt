package com.dangerfield.movingeyes.libraries.navigation.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import com.dangerfield.movingeyes.libraries.navigation.BlockingErrorRoute
import com.dangerfield.movingeyes.libraries.navigation.ErrorDialogAction
import com.dangerfield.movingeyes.libraries.navigation.ErrorDialogRoute
import com.dangerfield.movingeyes.libraries.navigation.FeatureEntryPoint
import com.dangerfield.movingeyes.libraries.navigation.Router
import com.dangerfield.movingeyes.libraries.navigation.dialog
import com.dangerfield.movingeyes.libraries.navigation.screen
import com.dangerfield.movingeyes.libraries.navigation.serializableType
import com.dangerfield.movingeyes.libraries.navigation.toRouteOrNull
import me.tatarka.inject.annotations.Inject
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.error_dialog_body
import movingeyes.libraries.resources.generated.resources.error_dialog_title
import movingeyes.libraries.resources.generated.resources.error_dismiss
import movingeyes.libraries.resources.generated.resources.error_generic_body
import movingeyes.libraries.resources.generated.resources.error_generic_title
import org.jetbrains.compose.resources.stringResource
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.reflect.typeOf

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true)
@Inject
class ErrorEntryPoints : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        screen<BlockingErrorRoute>(
            typeMap = mapOf()
        ) { backStackEntry ->
            // Fallback copy is resolved here rather than held on a top-level
            // constant, because a string resource can only be read from a
            // composable.
            val route = backStackEntry.toRouteOrNull<BlockingErrorRoute>()
            BlockingErrorScreen(
                title = route?.title ?: stringResource(Res.string.error_generic_title),
                subtitle = route?.subtitle ?: stringResource(Res.string.error_generic_body),
                errorCode = route?.errorCode ?: FallbackErrorCode,
            )
        }

        dialog<ErrorDialogRoute>(
            typeMap = mapOf(typeOf<ErrorDialogAction>() to serializableType<ErrorDialogAction>(),
            )
        ) { backStackEntry, dialogState ->
            val route = backStackEntry.toRouteOrNull<ErrorDialogRoute>()
            val title = route?.title ?: stringResource(Res.string.error_dialog_title)
            val subtitle = route?.subtitle ?: stringResource(Res.string.error_dialog_body)
            val actionTitle = route?.actionTitle ?: stringResource(Res.string.error_dismiss)
            var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

            fun dismissWithAction(action: () -> Unit) {
                pendingAction = action
                dialogState.dismiss()
            }

            ErrorDialog(
                state = dialogState,
                title = title,
                subtitle = subtitle,
                actionTitle = actionTitle,
                errorCode = route?.errorCode ?: FallbackErrorCode,
                onDismissRequest = {
                    val action = pendingAction ?: router::goBack
                    pendingAction = null
                    action()
                },
                onAction = {
                    dismissWithAction {
                        handleDialogAction(route?.action ?: ErrorDialogAction.Dismiss, router)
                    }
                },
            )
        }
    }
}

private fun handleDialogAction(action: ErrorDialogAction, router: Router) {
    when (action) {
        ErrorDialogAction.Dismiss,
        ErrorDialogAction.GoBack -> router.goBack()

        is ErrorDialogAction.Navigate -> {
            router.goBack()
            router.navigate(action.route)
        }
    }
}

/** Shown when a route arrives without one — never blank, so triage always has
 *  something to search for. */
private const val FallbackErrorCode = 1000
