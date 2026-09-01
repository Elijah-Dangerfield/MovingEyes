package com.dangerfield.movingeyes.features.paywall.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import com.dangerfield.movingeyes.features.paywall.PaywallRoute
import com.dangerfield.movingeyes.libraries.billing.PurchaseOutcome
import com.dangerfield.movingeyes.libraries.billing.RestoreOutcome
import com.dangerfield.movingeyes.libraries.navigation.FeatureEntryPoint
import com.dangerfield.movingeyes.libraries.navigation.Router
import com.dangerfield.movingeyes.libraries.navigation.screen
import me.tatarka.inject.annotations.Inject
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.paywall_nothing_to_restore
import movingeyes.libraries.resources.generated.resources.paywall_restored
import movingeyes.libraries.resources.generated.resources.paywall_store_unavailable
import org.jetbrains.compose.resources.stringResource
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true)
@Inject
class PaywallFeatureEntryPoint(
    private val viewModelFactory: () -> PaywallViewModel,
) : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        screen<PaywallRoute> {
            val paywallViewModel = viewModel { viewModelFactory() }
            val state by paywallViewModel.stateFlow.collectAsStateWithLifecycle()

            PaywallScreen(
                product = state.product,
                isPurchasing = state.isWorking,
                message = state.outcomeMessage(),
                onPurchase = { paywallViewModel.takeAction(PaywallAction.Purchase) },
                onRestore = { paywallViewModel.takeAction(PaywallAction.Restore) },
                onClose = router::goBack,
            )
        }
    }
}

/**
 * A cancelled purchase says nothing — the user closed the sheet and knows why.
 * "Couldn't reach the store" and "nothing to restore" are deliberately
 * different messages: telling someone they never bought it when the truth is
 * we couldn't check is how refund requests start.
 */
@Composable
private fun PaywallViewState.outcomeMessage(): String? = when {
    restoreOutcome is RestoreOutcome.Restored -> stringResource(Res.string.paywall_restored)
    restoreOutcome is RestoreOutcome.NothingToRestore ->
        stringResource(Res.string.paywall_nothing_to_restore)
    restoreOutcome is RestoreOutcome.StoreUnavailable ->
        stringResource(Res.string.paywall_store_unavailable)
    outcome is PurchaseOutcome.StoreUnavailable ->
        stringResource(Res.string.paywall_store_unavailable)
    outcome is PurchaseOutcome.Failed -> stringResource(Res.string.paywall_store_unavailable)
    else -> null
}
