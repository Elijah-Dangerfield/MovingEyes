package com.dangerfield.movingeyes.libraries.billing

import kotlinx.serialization.Serializable

/**
 * The one thing this app sells: a non-consumable that unlocks the Motion tab,
 * the ten paid eye styles, and microphone reactivity.
 *
 * The id must match the product created in both App Store Connect and the Play
 * Console exactly. It is deliberately not versioned or tiered — a second SKU
 * would mean a migration for everyone who already owns this one.
 */
object MovingEyesProduct {
    const val UnlockEverything = "movingeyespro"

    val All: Set<String> = setOf(UnlockEverything)
}

/**
 * A product as the store knows it. [displayPrice] is already localized by the
 * store, currency symbol and all; never format it yourself. [priceMicros] is
 * for telemetry only — dividing it by a million to display loses the
 * fractional digits some currencies use.
 */
@Serializable
data class BillingProduct(
    val sku: String,
    val displayPrice: String,
    val currencyCode: String,
    val priceMicros: Long,
)

/**
 * Outcome of [BillingClient.queryProducts]. Sealed so the paywall can tell
 * "the store answered, with nothing" from "the store call failed" — the first
 * means the product isn't provisioned, the second is worth a retry.
 */
sealed interface QueryProductsResult {
    data class Success(val products: Map<String, BillingProduct>) : QueryProductsResult

    /** Connection isn't established. Reconnect and retry. */
    data object NotConnected : QueryProductsResult

    /** Transient store error. Every platform error code collapses to here. */
    data class Failed(val message: String) : QueryProductsResult
}

/** Outcome of [BillingClient.queryOwnedSkus]. */
sealed interface QueryOwnedResult {
    /**
     * The store answered. [skus] may legitimately be empty — that means "this
     * account owns nothing", which is different from not having asked.
     */
    data class Success(val skus: Set<String>, val purchases: List<PurchaseRecord>) : QueryOwnedResult

    data object NotConnected : QueryOwnedResult

    /**
     * The query failed. Critically **not** the same as [Success] with an empty
     * set: a failure must never be read as "they don't own it", or a store
     * hiccup would lock a paying customer out of their decoration.
     */
    data class Failed(val message: String) : QueryOwnedResult
}

/** Outcome of [BillingClient.purchase]. */
sealed interface PurchaseResult {
    data class Success(val purchase: PurchaseRecord) : PurchaseResult

    /** User dismissed the sheet. Say nothing — they know what they did. */
    data object UserCancelled : PurchaseResult

    /**
     * The store says this account already owns it. Not an error: grant the
     * unlock. This is the common path for someone who reinstalled and tapped
     * buy before the silent restore landed.
     */
    data class AlreadyOwned(val purchase: PurchaseRecord) : PurchaseResult

    data class Failed(val reason: String) : PurchaseResult

    data object NotConnected : PurchaseResult
}

/**
 * A completed purchase, as much of it as this app has any use for.
 *
 * There is no server, so nothing here is ever validated server-side and none
 * of it needs to be. The threat model is honest about that: a determined user
 * can unlock a 99-cent Halloween decoration by other means, and building
 * receipt validation infrastructure to stop them would cost more than it saves
 * and would mean standing up the backend this app deliberately doesn't have.
 *
 * [purchaseToken] is retained for one real reason — Android needs it to
 * [acknowledge][BillingClient.acknowledge] the purchase inside three days or
 * Play refunds it automatically.
 */
@Serializable
data class PurchaseRecord(
    val sku: String,
    /** Platform order id (Apple `transaction_id`, Google `orderId`). */
    val orderId: String,
    /** Apple: the signed JWS. Google: the purchase token. */
    val purchaseToken: String,
    val platform: BillingPlatform,
    val purchasedAtEpochMs: Long,
    /** True once Play has been told; always true on iOS. */
    val isAcknowledged: Boolean = false,
)

@Serializable
enum class BillingPlatform {
    Apple,
    Google,

    /**
     * Came from [com.dangerfield.movingeyes.libraries.billing.impl.FakeBillingClient],
     * not a real store. Only reachable when `billing.realPurchasesEnabled` is
     * off, which defaults to debug builds only.
     */
    Fake,
}
