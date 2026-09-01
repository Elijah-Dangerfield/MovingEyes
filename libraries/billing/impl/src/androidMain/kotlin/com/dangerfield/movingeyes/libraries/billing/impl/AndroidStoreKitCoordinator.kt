package com.dangerfield.movingeyes.libraries.billing.impl

import com.dangerfield.movingeyes.libraries.billing.StoreKitCoordinator
import com.dangerfield.movingeyes.libraries.billing.StoreKitProduct
import com.dangerfield.movingeyes.libraries.billing.StoreKitPurchaseResult
import com.dangerfield.movingeyes.libraries.billing.StoreKitPurchaseStatus
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * StoreKit doesn't exist on Android, but the DI graph is shared, so something
 * has to satisfy [StoreKitCoordinator]. Nothing on Android ever calls it —
 * `PlayBillingClient` is the binding there — so every method reports an empty
 * or failed result rather than throwing.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AndroidStoreKitCoordinator : StoreKitCoordinator {

    override fun loadProducts(
        productIds: List<String>,
        onComplete: (products: List<StoreKitProduct>, errorMessage: String?) -> Unit,
    ) = onComplete(emptyList(), NotOnAndroid)

    override fun purchase(
        productId: String,
        onComplete: (result: StoreKitPurchaseResult) -> Unit,
    ) = onComplete(
        StoreKitPurchaseResult(status = StoreKitPurchaseStatus.Failed, errorMessage = NotOnAndroid),
    )

    override fun loadCurrentEntitlements(
        onComplete: (transactions: List<StoreKitPurchaseResult>) -> Unit,
    ) = onComplete(emptyList())

    override fun restorePurchases(
        onComplete: (transactions: List<StoreKitPurchaseResult>, errorMessage: String?) -> Unit,
    ) = onComplete(emptyList(), NotOnAndroid)

    override fun finishTransaction(
        jwsRepresentation: String,
        onComplete: (finished: Boolean) -> Unit,
    ) = onComplete(false)

    private companion object {
        const val NotOnAndroid = "StoreKit is iOS-only"
    }
}
