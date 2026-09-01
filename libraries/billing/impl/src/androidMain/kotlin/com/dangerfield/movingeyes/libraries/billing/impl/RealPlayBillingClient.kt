package com.dangerfield.movingeyes.libraries.billing.impl

import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient as PlayBilling
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.dangerfield.movingeyes.libraries.billing.BillingClient
import com.dangerfield.movingeyes.libraries.billing.BillingPlatform
import com.dangerfield.movingeyes.libraries.billing.BillingProduct
import com.dangerfield.movingeyes.libraries.billing.ConnectionState
import com.dangerfield.movingeyes.libraries.billing.PurchaseRecord
import com.dangerfield.movingeyes.libraries.billing.PurchaseResult
import com.dangerfield.movingeyes.libraries.billing.QueryOwnedResult
import com.dangerfield.movingeyes.libraries.billing.QueryProductsResult
import com.dangerfield.movingeyes.libraries.movingeyes.ActivityProvider
import com.dangerfield.movingeyes.libraries.core.Catching
import com.dangerfield.movingeyes.libraries.core.logging.KLog
import com.dangerfield.movingeyes.libraries.flowroutines.DispatcherProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Play Billing v9, driving a single non-consumable.
 *
 * Two mechanics worth knowing:
 *
 *  - [purchase] needs the foreground Activity, and Play reports its result
 *    through a listener rather than the call. A [CompletableDeferred] bridges
 *    that back to a suspend function, and a [Mutex] serialises purchases so
 *    two fast taps can't cross their callbacks.
 *  - [queryOwnedSkus] is `queryPurchasesAsync`, which reads the signed-in
 *    Google account's owned products. It doesn't prompt, so it's safe to run
 *    silently at launch — that's what makes a reinstall Just Work.
 */
internal class RealPlayBillingClient(
    private val context: Context,
    private val activityProvider: ActivityProvider,
    private val dispatchers: DispatcherProvider,
) : BillingClient {

    private val logger = KLog.withTag("PlayBillingClient")

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val purchaseMutex = Mutex()
    private var pendingPurchase: CompletableDeferred<PurchaseResult>? = null

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        pendingPurchase?.complete(result.toPurchaseResult(purchases))
    }

    private val billing: PlayBilling by lazy {
        PlayBilling.newBuilder(context)
            .setListener(purchasesListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
            )
            .build()
    }

    override suspend fun connect(): ConnectionState {
        if (_connectionState.value == ConnectionState.Connected && billing.isReady) {
            return ConnectionState.Connected
        }
        _connectionState.value = ConnectionState.Connecting
        val state = withContext(dispatchers.io) {
            suspendCancellableCoroutine { cont ->
                billing.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (!cont.isActive) return
                        cont.resume(
                            if (result.responseCode == PlayBilling.BillingResponseCode.OK) {
                                ConnectionState.Connected
                            } else {
                                ConnectionState.Unavailable
                            },
                        )
                    }

                    override fun onBillingServiceDisconnected() {
                        _connectionState.value = ConnectionState.Disconnected
                    }
                })
            }
        }
        _connectionState.value = state
        return state
    }

    override suspend fun queryProducts(skus: Set<String>): QueryProductsResult {
        if (!billing.isReady) return QueryProductsResult.NotConnected
        if (skus.isEmpty()) return QueryProductsResult.Success(emptyMap())

        return Catching {
            withContext(dispatchers.io) { billing.queryProductDetails(productDetailsParams(skus)) }
        }.fold(
            onSuccess = { result ->
                if (result.billingResult.responseCode != PlayBilling.BillingResponseCode.OK) {
                    QueryProductsResult.Failed(result.billingResult.debugMessage)
                } else {
                    QueryProductsResult.Success(
                        result.productDetailsList.orEmpty()
                            .mapNotNull { it.toBillingProduct() }
                            .associateBy { it.sku },
                    )
                }
            },
            onFailure = { QueryProductsResult.Failed(it.message ?: "queryProducts failed") },
        )
    }

    override suspend fun purchase(sku: String): PurchaseResult = purchaseMutex.withLock {
        if (!billing.isReady) return PurchaseResult.NotConnected
        val activity = activityProvider.currentActivity()
            ?: return PurchaseResult.Failed("No foreground Activity for the purchase flow")

        val details = Catching {
            withContext(dispatchers.io) { billing.queryProductDetails(productDetailsParams(setOf(sku))) }
        }.getOrNull()?.productDetailsList?.firstOrNull()
        // Almost always means the app isn't on a Play track yet. That's the
        // whole reason `billing.realPurchasesEnabled` defaults off on debug.
            ?: return PurchaseResult.Failed("Play doesn't know $sku — is the app published to a track?")

        val deferred = CompletableDeferred<PurchaseResult>()
        pendingPurchase = deferred

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build(),
                ),
            )
            .build()

        val launch = withContext(dispatchers.main) {
            billing.launchBillingFlow(activity, flowParams)
        }
        if (launch.responseCode != PlayBilling.BillingResponseCode.OK) {
            pendingPurchase = null
            return launch.toPurchaseResult(purchases = null)
        }

        return deferred.await().also { pendingPurchase = null }
    }

    override suspend fun queryOwnedSkus(): QueryOwnedResult {
        if (!billing.isReady) return QueryOwnedResult.NotConnected

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(PlayBilling.ProductType.INAPP)
            .build()

        return Catching {
            withContext(dispatchers.io) { billing.queryPurchasesAsync(params) }
        }.fold(
            onSuccess = { result ->
                if (result.billingResult.responseCode != PlayBilling.BillingResponseCode.OK) {
                    QueryOwnedResult.Failed(result.billingResult.debugMessage)
                } else {
                    // PENDING purchases are not yet paid for (slow card, cash
                    // at a kiosk). Granting on one would hand out the unlock
                    // for free if the payment never lands.
                    val purchased = result.purchasesList
                        .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    QueryOwnedResult.Success(
                        skus = purchased.flatMap { it.products }.toSet(),
                        purchases = purchased.flatMap { it.toRecords() },
                    )
                }
            },
            onFailure = { QueryOwnedResult.Failed(it.message ?: "queryPurchases failed") },
        )
    }

    override suspend fun acknowledge(purchaseToken: String): Boolean {
        if (!billing.isReady) return false
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        return Catching {
            withContext(dispatchers.io) { billing.acknowledgePurchase(params) }
        }.getOrNull()?.responseCode == PlayBilling.BillingResponseCode.OK
    }

    private fun productDetailsParams(skus: Set<String>): QueryProductDetailsParams =
        QueryProductDetailsParams.newBuilder()
            .setProductList(
                skus.map { sku ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(sku)
                        .setProductType(PlayBilling.ProductType.INAPP)
                        .build()
                },
            )
            .build()

    private fun BillingResult.toPurchaseResult(purchases: List<Purchase>?): PurchaseResult =
        when (responseCode) {
            PlayBilling.BillingResponseCode.OK -> {
                val purchase = purchases?.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                val record = purchase?.toRecords()?.firstOrNull()
                if (record == null) {
                    logger.w { "Purchase OK but no PURCHASED item in the update" }
                    PurchaseResult.Failed("No completed purchase in store response")
                } else {
                    PurchaseResult.Success(record)
                }
            }

            PlayBilling.BillingResponseCode.USER_CANCELED -> PurchaseResult.UserCancelled

            // Not a failure — the caller grants on it. Play sends no receipt
            // with this code, so fall back to a fresh owned-query rather than
            // inventing a record.
            PlayBilling.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                val record = purchases?.firstOrNull()?.toRecords()?.firstOrNull()
                if (record != null) {
                    PurchaseResult.AlreadyOwned(record)
                } else {
                    PurchaseResult.Failed("Already owned; re-query to pick it up")
                }
            }

            PlayBilling.BillingResponseCode.SERVICE_DISCONNECTED,
            PlayBilling.BillingResponseCode.SERVICE_UNAVAILABLE,
            -> PurchaseResult.NotConnected

            else -> PurchaseResult.Failed(debugMessage.ifBlank { "Purchase failed (code $responseCode)" })
        }

    /** One Play purchase can cover several products, hence a list. */
    private fun Purchase.toRecords(): List<PurchaseRecord> = products.map { sku ->
        PurchaseRecord(
            sku = sku,
            orderId = orderId.orEmpty(),
            purchaseToken = purchaseToken,
            platform = BillingPlatform.Google,
            purchasedAtEpochMs = purchaseTime,
            isAcknowledged = isAcknowledged,
        )
    }

    private fun ProductDetails.toBillingProduct(): BillingProduct? {
        val offer = oneTimePurchaseOfferDetails ?: return null
        return BillingProduct(
            sku = productId,
            displayPrice = offer.formattedPrice,
            currencyCode = offer.priceCurrencyCode,
            priceMicros = offer.priceAmountMicros,
        )
    }
}
