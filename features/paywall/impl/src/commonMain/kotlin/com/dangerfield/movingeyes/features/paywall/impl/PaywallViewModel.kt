package com.dangerfield.movingeyes.features.paywall.impl

import com.dangerfield.movingeyes.libraries.billing.BillingProduct
import com.dangerfield.movingeyes.libraries.billing.Entitlements
import com.dangerfield.movingeyes.libraries.billing.PurchaseOutcome
import com.dangerfield.movingeyes.libraries.billing.RestoreOutcome
import com.dangerfield.movingeyes.libraries.flowroutines.SEAViewModel
import me.tatarka.inject.annotations.Inject

@Inject
class PaywallViewModel(
    private val entitlements: Entitlements,
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
                    action.updateState { it.copy(isUnlocked = true) }
                    // A demo running when the purchase lands must not then take
                    // the feature away.
                    sendEvent(PaywallEvent.Unlocked)
                }
            }

            PaywallAction.DismissOutcome -> {
                action.updateState { it.copy(outcome = null, restoreOutcome = null) }
            }

            PaywallAction.Restore -> {
                action.updateState { it.copy(isWorking = true, message = null) }
                val outcome = entitlements.restore()
                action.updateState { it.copy(isWorking = false, restoreOutcome = outcome) }

                if (outcome is RestoreOutcome.Restored) {
                    action.updateState { it.copy(isUnlocked = true) }
                    sendEvent(PaywallEvent.Unlocked)
                }
            }
        }
    }
}

data class PaywallViewState(
    val product: BillingProduct? = null,
    val isWorking: Boolean = false,

    /** Drives the receipt. Set from the outcome rather than read from
     *  Entitlements, so the screen only celebrates a purchase made *here*. */
    val isUnlocked: Boolean = false,

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

    /** Clears a failure so the dialog closes and the paywall stays put. */
    data object DismissOutcome : PaywallAction
}
