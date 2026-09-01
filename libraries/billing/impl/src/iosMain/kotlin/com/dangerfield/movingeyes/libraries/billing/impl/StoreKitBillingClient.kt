package com.dangerfield.movingeyes.libraries.billing.impl

import com.dangerfield.movingeyes.libraries.billing.BillingClient
import com.dangerfield.movingeyes.libraries.billing.ConnectionState
import com.dangerfield.movingeyes.libraries.billing.PurchaseResult
import com.dangerfield.movingeyes.libraries.billing.QueryOwnedResult
import com.dangerfield.movingeyes.libraries.billing.QueryProductsResult
import com.dangerfield.movingeyes.libraries.billing.RealPurchasesEnabled
import com.dangerfield.movingeyes.libraries.billing.StoreKitCoordinator
import com.dangerfield.movingeyes.libraries.billing.awaitCurrentEntitlements
import com.dangerfield.movingeyes.libraries.billing.awaitFinish
import com.dangerfield.movingeyes.libraries.billing.awaitProducts
import com.dangerfield.movingeyes.libraries.billing.awaitPurchase
import com.dangerfield.movingeyes.libraries.billing.toBillingProduct
import com.dangerfield.movingeyes.libraries.billing.toPurchaseResult
import com.dangerfield.movingeyes.libraries.billing.toRecord
import com.dangerfield.movingeyes.libraries.core.Catching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * The iOS [BillingClient] binding. Mirror of `PlayBillingClient` on Android.
 *
 * Same delegate switch off [RealPurchasesEnabled]. iOS is more forgiving than
 * Play — sandbox works against a "Ready to Submit" product, and Xcode StoreKit
 * configuration files need no App Store Connect at all — but keeping one
 * behaviour across both platforms is worth more than exploiting that.
 *
 * There is no connect step in StoreKit, so [connect] reports Connected as soon
 * as a coordinator exists. If Swift never handed one in (a build that forgot to
 * wire it), that's Unavailable rather than a crash.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class StoreKitBillingClient(
    private val coordinator: StoreKitCoordinator,
    private val realPurchasesEnabled: RealPurchasesEnabled,
) : BillingClient {

    private val fake = FakeBillingClient()

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)

    override val connectionState: StateFlow<ConnectionState>
        get() = if (realPurchasesEnabled()) _connectionState.asStateFlow() else fake.connectionState

    override suspend fun connect(): ConnectionState {
        if (!realPurchasesEnabled()) return fake.connect()
        _connectionState.value = ConnectionState.Connected
        return ConnectionState.Connected
    }

    override suspend fun queryProducts(skus: Set<String>): QueryProductsResult {
        if (!realPurchasesEnabled()) return fake.queryProducts(skus)
        if (skus.isEmpty()) return QueryProductsResult.Success(emptyMap())

        return coordinator.awaitProducts(skus.toList()).fold(
            onSuccess = { products ->
                QueryProductsResult.Success(
                    products.map { it.toBillingProduct() }.associateBy { it.sku },
                )
            },
            onFailure = { QueryProductsResult.Failed(it.message ?: "StoreKit product lookup failed") },
        )
    }

    override suspend fun purchase(sku: String): PurchaseResult {
        if (!realPurchasesEnabled()) return fake.purchase(sku)

        val result = Catching { coordinator.awaitPurchase(sku) }
            .getOrElse { return PurchaseResult.Failed(it.message ?: "StoreKit purchase failed") }
            .toPurchaseResult()

        // Apple replays an unfinished transaction forever. Finish it as soon as
        // the receipt is in hand — for a non-consumable the entitlement lives
        // on the account, so nothing is lost by finishing immediately.
        when (result) {
            is PurchaseResult.Success -> finish(result.purchase.purchaseToken)
            is PurchaseResult.AlreadyOwned -> finish(result.purchase.purchaseToken)
            else -> Unit
        }
        return result
    }

    override suspend fun queryOwnedSkus(): QueryOwnedResult {
        if (!realPurchasesEnabled()) return fake.queryOwnedSkus()

        return Catching { coordinator.awaitCurrentEntitlements() }.fold(
            onSuccess = { transactions ->
                val records = transactions.mapNotNull { it.toRecord() }
                QueryOwnedResult.Success(records.map { it.sku }.toSet(), records)
            },
            // Never an empty Success: "we couldn't ask" must not read as
            // "they don't own it" anywhere in this stack.
            onFailure = { QueryOwnedResult.Failed(it.message ?: "StoreKit entitlement lookup failed") },
        )
    }

    /**
     * StoreKit has no three-day acknowledge window, so there is nothing to
     * acknowledge. Finishing happens in [purchase], where the JWS is in scope.
     */
    override suspend fun acknowledge(purchaseToken: String): Boolean {
        if (!realPurchasesEnabled()) return fake.acknowledge(purchaseToken)
        return true
    }

    private suspend fun finish(jws: String) {
        Catching { coordinator.awaitFinish(jws) }
    }
}
