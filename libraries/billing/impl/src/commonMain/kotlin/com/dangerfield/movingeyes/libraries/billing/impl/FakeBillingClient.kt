package com.dangerfield.movingeyes.libraries.billing.impl

import com.dangerfield.movingeyes.libraries.billing.BillingClient
import com.dangerfield.movingeyes.libraries.billing.BillingPlatform
import com.dangerfield.movingeyes.libraries.billing.BillingProduct
import com.dangerfield.movingeyes.libraries.billing.ConnectionState
import com.dangerfield.movingeyes.libraries.billing.MovingEyesProduct
import com.dangerfield.movingeyes.libraries.billing.PurchaseRecord
import com.dangerfield.movingeyes.libraries.billing.PurchaseResult
import com.dangerfield.movingeyes.libraries.billing.QueryOwnedResult
import com.dangerfield.movingeyes.libraries.billing.QueryProductsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stands in for a real store. Used whenever `billing.realPurchasesEnabled` is
 * off, which by default is every debug build.
 *
 * It models ownership rather than just always succeeding, because the
 * interesting bugs live in the second purchase: buy, kill the app, relaunch,
 * and this reports the SKU as owned exactly like a real store would. That's
 * the reinstall path, testable without deleting anything.
 *
 * [forcedOutcome] drives the branches that are otherwise a pain to reach —
 * cancel, store-unavailable, hard failure — from the QA menu or a test.
 */
class FakeBillingClient(
    private val catalog: Map<String, BillingProduct> = DefaultCatalog,
    private val nowEpochMs: () -> Long = { 0L },
    var forcedOutcome: FakeOutcome = FakeOutcome.Succeed,
) : BillingClient {

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /**
     * In-memory, so a process restart looks like a fresh store account. That's
     * the wrong lifetime to model a real non-consumable, but it's the right
     * one for a fake: the *cached entitlement* is what carries an unlock
     * across launches in the real app, and this way a dev can exercise both
     * "store says owned" and "store says nothing" without clearing app data.
     */
    private val owned = mutableMapOf<String, PurchaseRecord>()

    private var sequence = 0L

    override suspend fun connect(): ConnectionState {
        val state = if (forcedOutcome == FakeOutcome.StoreUnavailable) {
            ConnectionState.Unavailable
        } else {
            ConnectionState.Connected
        }
        _connectionState.value = state
        return state
    }

    override suspend fun queryProducts(skus: Set<String>): QueryProductsResult = when {
        _connectionState.value != ConnectionState.Connected -> QueryProductsResult.NotConnected
        else -> QueryProductsResult.Success(catalog.filterKeys { it in skus })
    }

    override suspend fun purchase(sku: String): PurchaseResult {
        if (_connectionState.value != ConnectionState.Connected) return PurchaseResult.NotConnected
        val product = catalog[sku] ?: return PurchaseResult.Failed("Unknown SKU: $sku")

        owned[sku]?.let { return PurchaseResult.AlreadyOwned(it) }

        return when (val outcome = forcedOutcome) {
            FakeOutcome.Cancel -> PurchaseResult.UserCancelled
            FakeOutcome.StoreUnavailable -> PurchaseResult.NotConnected
            is FakeOutcome.Fail -> PurchaseResult.Failed(outcome.reason)
            FakeOutcome.Succeed -> {
                val record = recordFor(product)
                owned[sku] = record
                PurchaseResult.Success(record)
            }
        }
    }

    override suspend fun queryOwnedSkus(): QueryOwnedResult = when {
        _connectionState.value != ConnectionState.Connected -> QueryOwnedResult.NotConnected
        forcedOutcome is FakeOutcome.Fail -> QueryOwnedResult.Failed("Fake store query failed")
        else -> QueryOwnedResult.Success(owned.keys.toSet(), owned.values.toList())
    }

    override suspend fun acknowledge(purchaseToken: String): Boolean {
        val entry = owned.entries.firstOrNull { it.value.purchaseToken == purchaseToken }
            ?: return false
        owned[entry.key] = entry.value.copy(isAcknowledged = true)
        return true
    }

    private fun recordFor(product: BillingProduct): PurchaseRecord {
        sequence += 1
        return PurchaseRecord(
            sku = product.sku,
            orderId = "fake-order-${product.sku}-$sequence",
            purchaseToken = "fake-token-${product.sku}-$sequence",
            platform = BillingPlatform.Fake,
            purchasedAtEpochMs = nowEpochMs(),
        )
    }

    companion object {
        /**
         * A plausible price so the paywall's layout is exercised honestly. Not
         * localized — the real store owns that, and a fake that pretended to
         * would hide currency-width bugs rather than surface them.
         */
        val DefaultCatalog: Map<String, BillingProduct> = mapOf(
            MovingEyesProduct.UnlockEverything to BillingProduct(
                sku = MovingEyesProduct.UnlockEverything,
                displayPrice = "$4.99",
                currencyCode = "USD",
                priceMicros = 4_990_000L,
            ),
        )
    }
}

/** Forces a branch that's otherwise awkward to reach on a real store. */
sealed interface FakeOutcome {
    data object Succeed : FakeOutcome
    data object Cancel : FakeOutcome
    data object StoreUnavailable : FakeOutcome
    data class Fail(val reason: String) : FakeOutcome
}
