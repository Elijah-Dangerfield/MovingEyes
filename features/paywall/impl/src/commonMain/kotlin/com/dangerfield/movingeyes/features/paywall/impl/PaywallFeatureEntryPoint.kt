package com.dangerfield.movingeyes.features.paywall.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import com.dangerfield.movingeyes.features.paywall.PaywallRoute
import com.dangerfield.movingeyes.features.paywall.PaywallTrigger
import com.dangerfield.movingeyes.libraries.billing.PurchaseOutcome
import com.dangerfield.movingeyes.libraries.billing.RestoreOutcome
import com.dangerfield.movingeyes.libraries.navigation.FeatureEntryPoint
import com.dangerfield.movingeyes.libraries.navigation.Router
import com.dangerfield.movingeyes.libraries.navigation.screen
import com.dangerfield.movingeyes.libraries.navigation.serializableType
import me.tatarka.inject.annotations.Inject
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.paywall_nothing_to_restore
import movingeyes.libraries.resources.generated.resources.paywall_restored
import movingeyes.libraries.resources.generated.resources.paywall_store_unavailable
import movingeyes.libraries.resources.generated.resources.paywall_failed_title
import movingeyes.libraries.resources.generated.resources.paywall_failed_body
import movingeyes.libraries.resources.generated.resources.paywall_unavailable_title
import movingeyes.libraries.resources.generated.resources.error_dismiss
import com.dangerfield.movingeyes.libraries.ui.components.dialog.BasicDialog
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.reflect.typeOf

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true)
@Inject
class PaywallFeatureEntryPoint(
    private val viewModelFactory: () -> PaywallViewModel,
) : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        // `trigger` is an enum, and androidx.navigation cannot resolve a NavType
        // for it on its own: its built-in enum handling leans on JVM reflection,
        // which Kotlin/Native does not have. Omitting it throws
        // "could not find any NavType for argument trigger" while the graph is
        // being built, which means at launch — the app never draws a frame.
        // Android tolerates the omission, so this is invisible until iOS runs.
        screen<PaywallRoute>(
            typeMap = mapOf(typeOf<PaywallTrigger>() to serializableType<PaywallTrigger>()),
        ) {
            val paywallViewModel = viewModel { viewModelFactory() }
            val state by paywallViewModel.stateFlow.collectAsStateWithLifecycle()

            PaywallScreen(
                product = state.product,
                isPurchasing = state.isWorking,
                isUnlocked = state.isUnlocked,
                message = state.outcomeMessage(),
                onPurchase = { paywallViewModel.takeAction(PaywallAction.Purchase) },
                onRestore = { paywallViewModel.takeAction(PaywallAction.Restore) },
                onClose = router::goBack,
            )

            // Dismiss clears the outcome rather than navigating, so the paywall
            // is still there to try again on.
            state.outcome.asError()?.let { error ->
                BasicDialog(
                    title = stringResource(error.title),
                    description = stringResource(error.body),
                    primaryButtonText = stringResource(Res.string.error_dismiss),
                    onPrimaryButtonClicked = {
                        paywallViewModel.takeAction(PaywallAction.DismissOutcome)
                    },
                    onDismissRequest = {
                        paywallViewModel.takeAction(PaywallAction.DismissOutcome)
                    },
                )
            }
        }
    }
}

/**
 * A cancelled purchase says nothing: the user closed the sheet and knows why.
 * "Couldn't reach the store" and "nothing to restore" are deliberately
 * different messages, because telling someone they never bought it when the
 * truth is we couldn't check is how refund requests start.
 *
 * Restores stay inline. They are a quiet confirmation next to the button that
 * was pressed, and a modal for "restored" would be in the way.
 */
@Composable
private fun PaywallViewState.outcomeMessage(): String? = when {
    restoreOutcome is RestoreOutcome.Restored -> stringResource(Res.string.paywall_restored)
    restoreOutcome is RestoreOutcome.NothingToRestore ->
        stringResource(Res.string.paywall_nothing_to_restore)
    restoreOutcome is RestoreOutcome.StoreUnavailable ->
        stringResource(Res.string.paywall_store_unavailable)
    else -> null
}

/**
 * A purchase that did not happen gets a dialog, not a line of text.
 *
 * Someone who taps buy has decided to pay, and the old inline message sat above
 * the button in the same weight as the marketing copy, so the most likely read
 * of a failure was that nothing happened at all. It also told everyone to check
 * their network: [PurchaseOutcome.Failed] was mapped to "couldn't reach the
 * store", which is unhelpful when the store answered and said no.
 */
private data class PurchaseError(val title: StringResource, val body: StringResource)

private fun PurchaseOutcome?.asError(): PurchaseError? = when (this) {
    is PurchaseOutcome.StoreUnavailable -> PurchaseError(
        title = Res.string.paywall_unavailable_title,
        body = Res.string.paywall_store_unavailable,
    )

    is PurchaseOutcome.Failed -> PurchaseError(
        title = Res.string.paywall_failed_title,
        body = Res.string.paywall_failed_body,
    )

    else -> null
}
