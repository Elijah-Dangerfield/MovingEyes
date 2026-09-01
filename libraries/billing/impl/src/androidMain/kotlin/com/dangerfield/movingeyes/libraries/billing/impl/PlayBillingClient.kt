package com.dangerfield.movingeyes.libraries.billing.impl

import android.content.Context
import com.dangerfield.movingeyes.libraries.billing.BillingClient
import com.dangerfield.movingeyes.libraries.billing.ConnectionState
import com.dangerfield.movingeyes.libraries.billing.PurchaseResult
import com.dangerfield.movingeyes.libraries.billing.QueryOwnedResult
import com.dangerfield.movingeyes.libraries.billing.QueryProductsResult
import com.dangerfield.movingeyes.libraries.billing.RealPurchasesEnabled
import com.dangerfield.movingeyes.libraries.movingeyes.ActivityProvider
import com.dangerfield.movingeyes.libraries.flowroutines.DispatcherProvider
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * The Android [BillingClient] binding. Mirror of `StoreKitBillingClient` on iOS.
 *
 * Picks its delegate per call off [RealPurchasesEnabled], so flipping the flag
 * in the QA menu takes effect without a restart. The flag defaults off on debug
 * builds because Play returns nothing for an app that isn't on a track yet —
 * see the flag's own doc.
 *
 * The real client is `by lazy` so a debug build never constructs a Play
 * connection it isn't going to use.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class PlayBillingClient(
    context: Context,
    activityProvider: ActivityProvider,
    dispatchers: DispatcherProvider,
    private val realPurchasesEnabled: RealPurchasesEnabled,
) : BillingClient {

    private val fake = FakeBillingClient()
    private val real by lazy { RealPlayBillingClient(context, activityProvider, dispatchers) }

    private fun delegate(): BillingClient = if (realPurchasesEnabled()) real else fake

    override val connectionState: StateFlow<ConnectionState> get() = delegate().connectionState
    override suspend fun connect(): ConnectionState = delegate().connect()
    override suspend fun queryProducts(skus: Set<String>): QueryProductsResult = delegate().queryProducts(skus)
    override suspend fun purchase(sku: String): PurchaseResult = delegate().purchase(sku)
    override suspend fun queryOwnedSkus(): QueryOwnedResult = delegate().queryOwnedSkus()
    override suspend fun acknowledge(purchaseToken: String): Boolean = delegate().acknowledge(purchaseToken)
}
