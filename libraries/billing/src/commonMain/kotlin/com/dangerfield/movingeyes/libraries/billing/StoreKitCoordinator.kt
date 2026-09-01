@file:OptIn(ExperimentalObjCName::class)

package com.dangerfield.movingeyes.libraries.billing

import kotlin.coroutines.resume
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Bridge to StoreKit 2, satisfied by a Swift class handed in from
 * `iOSApp.swift`. Android binds a no-op, because StoreKit is iOS-only.
 *
 * StoreKit 2's `Product.purchase()` and `Transaction` APIs are Swift-only
 * `async`, so the implementation has to be Swift. That constrains the shape of
 * this interface: **plain callbacks, not `suspend`, and primitive parameters
 * only.** A Swift class then satisfies it as an ordinary protocol conformance
 * with no coroutine or `Throwable` bridging across the boundary.
 * [StoreKitBillingClient] wraps these back into the suspend [BillingClient]
 * surface.
 *
 * The flattened types ([StoreKitProduct], [StoreKitPurchaseResult]) exist for
 * the same reason — the Swift side never has to construct a Kotlin sealed
 * hierarchy.
 */
@ObjCName("BillingStoreKitCoordinator", exact = true)
interface StoreKitCoordinator {

    /**
     * `Product.products(for:)`. Ids the store doesn't recognize are simply
     * absent; [errorMessage] is non-null only on a hard failure.
     */
    fun loadProducts(
        productIds: List<String>,
        onComplete: (products: List<StoreKitProduct>, errorMessage: String?) -> Unit,
    )

    /**
     * `Product.purchase()`. No `appAccountToken` is passed — this app has no
     * accounts, so there is no id to pin a receipt to and no server that would
     * check one.
     */
    fun purchase(
        productId: String,
        onComplete: (result: StoreKitPurchaseResult) -> Unit,
    )

    /**
     * `Transaction.currentEntitlements` — everything this Apple ID still owns.
     *
     * This is the reinstall path on iOS, and the reason it can run silently at
     * launch is that `currentEntitlements` does **not** prompt for a password.
     * `AppStore.sync()` does, which is why it belongs behind the Restore
     * button and not in the launch path.
     */
    fun loadCurrentEntitlements(
        onComplete: (transactions: List<StoreKitPurchaseResult>) -> Unit,
    )

    /**
     * `AppStore.sync()` then re-read entitlements. May prompt for an App Store
     * password, so only ever call this from an explicit user tap.
     */
    fun restorePurchases(
        onComplete: (transactions: List<StoreKitPurchaseResult>, errorMessage: String?) -> Unit,
    )

    /**
     * `Transaction.finish()`. Apple replays an unfinished transaction forever,
     * so a non-consumable is finished once the entitlement is granted. Keyed by
     * the JWS the purchase carried, which is the same value that rides
     * [BillingClient.acknowledge] as the `purchaseToken`.
     */
    fun finishTransaction(
        jwsRepresentation: String,
        onComplete: (finished: Boolean) -> Unit,
    )
}

/** A StoreKit `Product`, flattened for the Kotlin/Native boundary. */
@ObjCName("BillingStoreKitProduct", exact = true)
data class StoreKitProduct(
    val productId: String,
    val displayPrice: String,
    val currencyCode: String,
    val priceMicros: Long,
)

/**
 * A StoreKit transaction, flattened. On success the transaction fields carry
 * the verified receipt; [jwsRepresentation] doubles as the key for
 * [StoreKitCoordinator.finishTransaction].
 */
@ObjCName("BillingStoreKitPurchaseResult", exact = true)
data class StoreKitPurchaseResult(
    val status: StoreKitPurchaseStatus,
    val productId: String? = null,
    val transactionId: String? = null,
    val jwsRepresentation: String? = null,
    val purchasedAtEpochMs: Long = 0L,
    val displayPrice: String? = null,
    val errorMessage: String? = null,
)

@ObjCName("BillingStoreKitPurchaseStatus", exact = true)
enum class StoreKitPurchaseStatus {
    Success,
    AlreadyPurchased,
    UserCancelled,
    Pending,
    Failed,
}

fun StoreKitProduct.toBillingProduct(): BillingProduct = BillingProduct(
    sku = productId,
    displayPrice = displayPrice,
    currencyCode = currencyCode,
    priceMicros = priceMicros,
)

/**
 * Maps a flattened StoreKit result onto [PurchaseResult].
 *
 * A status claiming success without a verified transaction is downgraded to
 * [PurchaseResult.Failed] rather than unlocking on an unverifiable purchase —
 * the only integrity check this app has is "did StoreKit hand us a signed
 * transaction", so it may as well be enforced.
 */
fun StoreKitPurchaseResult.toPurchaseResult(): PurchaseResult = when (status) {
    StoreKitPurchaseStatus.Success -> toRecord()
        ?.let { PurchaseResult.Success(it) }
        ?: PurchaseResult.Failed("StoreKit success without a verified transaction")

    StoreKitPurchaseStatus.AlreadyPurchased -> toRecord()
        ?.let { PurchaseResult.AlreadyOwned(it) }
        ?: PurchaseResult.Failed("StoreKit reported already purchased without a transaction")

    StoreKitPurchaseStatus.UserCancelled -> PurchaseResult.UserCancelled

    // Ask-to-Buy and similar. Not a failure and not a grant: the transaction
    // arrives later through currentEntitlements, so the next silent refresh
    // picks it up.
    StoreKitPurchaseStatus.Pending -> PurchaseResult.Failed(
        errorMessage ?: "Purchase is pending approval",
    )

    StoreKitPurchaseStatus.Failed -> PurchaseResult.Failed(errorMessage ?: "StoreKit purchase failed")
}

fun StoreKitPurchaseResult.toRecord(): PurchaseRecord? {
    val productId = productId ?: return null
    val transactionId = transactionId ?: return null
    val jws = jwsRepresentation ?: return null
    return PurchaseRecord(
        sku = productId,
        orderId = transactionId,
        purchaseToken = jws,
        platform = BillingPlatform.Apple,
        purchasedAtEpochMs = purchasedAtEpochMs,
        // StoreKit has no three-day acknowledge window; finishing is handled
        // inside the coordinator, so nothing is ever left dangling here.
        isAcknowledged = true,
    )
}

suspend fun StoreKitCoordinator.awaitProducts(
    productIds: List<String>,
): Result<List<StoreKitProduct>> = suspendCancellableCoroutine { continuation ->
    loadProducts(productIds) { products, errorMessage ->
        if (errorMessage != null) {
            continuation.resume(Result.failure(StoreKitException(errorMessage)))
        } else {
            continuation.resume(Result.success(products))
        }
    }
}

suspend fun StoreKitCoordinator.awaitPurchase(
    productId: String,
): StoreKitPurchaseResult = suspendCancellableCoroutine { continuation ->
    purchase(productId) { result -> continuation.resume(result) }
}

suspend fun StoreKitCoordinator.awaitCurrentEntitlements(): List<StoreKitPurchaseResult> =
    suspendCancellableCoroutine { continuation ->
        loadCurrentEntitlements { transactions -> continuation.resume(transactions) }
    }

suspend fun StoreKitCoordinator.awaitRestore(): Result<List<StoreKitPurchaseResult>> =
    suspendCancellableCoroutine { continuation ->
        restorePurchases { transactions, errorMessage ->
            if (errorMessage != null) {
                continuation.resume(Result.failure(StoreKitException(errorMessage)))
            } else {
                continuation.resume(Result.success(transactions))
            }
        }
    }

suspend fun StoreKitCoordinator.awaitFinish(
    jwsRepresentation: String,
): Boolean = suspendCancellableCoroutine { continuation ->
    finishTransaction(jwsRepresentation) { finished -> continuation.resume(finished) }
}

/** Raised when the native product lookup or restore reports a hard failure. */
class StoreKitException(message: String) : Throwable(message)
