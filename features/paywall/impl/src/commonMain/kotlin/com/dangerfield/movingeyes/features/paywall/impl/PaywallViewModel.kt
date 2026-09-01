package com.dangerfield.movingeyes.features.paywall.impl

import com.dangerfield.movingeyes.libraries.billing.BillingProduct
import com.dangerfield.movingeyes.libraries.billing.Entitlements
import com.dangerfield.movingeyes.libraries.billing.FeatureTrial
import com.dangerfield.movingeyes.libraries.billing.PurchaseOutcome
import com.dangerfield.movingeyes.libraries.billing.RestoreOutcome
import com.dangerfield.movingeyes.libraries.flowroutines.SEAViewModel
import me.tatarka.inject.annotations.Inject

@Inject
class PaywallViewModel(
    private val entitlements: Entitlements,
    private val featureTrial: FeatureTrial,
) : SEAViewModel<PaywallViewState, PaywallEvent, PaywallAction>(
    initialStateArg = PaywallViewState(),
) {

    init {
        takeAction(PaywallAction.Observe)
    }

    override suspend fun handleAction(action: PaywallAction) {
        when (action) {
            PaywallAction.Observe -> {
                action.updateState { it.copy(product = entitlements.product.value) }
            }

            PaywallAction.Purchase -> {
                action.updateState { it.copy(isWorking = true, message = null) }
                val outcome = entitlements.purchase()
                action.updateState { it.copy(isWorking = false, outcome = outcome) }

                if (outcome is PurchaseOutcome.Unlocked) {
                    // A demo running when the purchase lands must not then take
                    // the feature away.
                    featureTrial.keep()
                    sendEvent(PaywallEvent.Unlocked)
                }
            }

            PaywallAction.Restore -> {
                action.updateState { it.copy(isWorking = true, message = null) }
                val outcome = entitlements.restore()
                action.updateState { it.copy(isWorking = false, restoreOutcome = outcome) }

                if (outcome is RestoreOutcome.Restored) {
                    featureTrial.keep()
                    sendEvent(PaywallEvent.Unlocked)
                }
            }
        }
    }
}

data class PaywallViewState(
    val product: BillingProduct? = null,
    val isWorking: Boolean = false,

    /** Cancelling shows nothing at all; only failures and restores speak. */
    val outcome: PurchaseOutcome? = null,
    val restoreOutcome: RestoreOutcome? = null,
    val message: String? = null,
)

sealed interface PaywallEvent {
    data object Unlocked : PaywallEvent
}

sealed interface PaywallAction {
    data object Observe : PaywallAction
    data object Purchase : PaywallAction
    data object Restore : PaywallAction
}
