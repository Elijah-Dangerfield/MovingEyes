package com.dangerfield.movingeyes.libraries.billing

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over the platform's in-app purchase system (Play Billing on
 * Android, StoreKit 2 on iOS). Feature code never touches a store directly —
 * and in practice feature code doesn't touch this either, it goes through
 * [Entitlements], which is the whole product-facing surface.
 *
 * Bound per platform: `PlayBillingClient` (Android) and `StoreKitBillingClient`
 * (iOS). Both pick their delegate per call off [RealPurchasesEnabled] — a
 * sideloaded debug build has no provisioned catalog, so a real client returns
 * nothing and the paywall renders empty. The fake stands in with a seeded SKU
 * so the whole flow is exercisable off-store.
 *
 * Moving Eyes sells exactly one thing: a non-consumable that unlocks
 * everything. That shapes this interface in three ways worth knowing before
 * extending it.
 *
 *  - **There is no `consume()`.** Consumables can be re-bought; this can't.
 *  - **There is no `userId`.** The app has no accounts, so there's no id to
 *    pin a receipt to and no server to validate against. The store account is
 *    the identity.
 *  - **[queryOwnedSkus] exists**, which a consumables-only client wouldn't
 *    need. It's how the unlock survives a reinstall: the entitlement lives on
 *    the store account, so a fresh install just asks.
 *
 * Errors are sealed results rather than exceptions, because callers render
 * different UI for "user cancelled" (say nothing) than for "store unavailable"
 * (say something).
 */
interface BillingClient {

    /**
     * Lifecycle of the underlying store connection. The paywall reads it to
     * decide between showing a price and showing "store unavailable".
     */
    val connectionState: StateFlow<ConnectionState>

    /** Idempotent — safe to call on every launch and every paywall mount. */
    suspend fun connect(): ConnectionState

    /**
     * Ask the store about [skus]. The store owns the localized price string;
     * the client never formats currency itself. SKUs the store doesn't
     * recognize are simply absent from the result rather than an error.
     */
    suspend fun queryProducts(skus: Set<String>): QueryProductsResult

    /**
     * Run the platform purchase sheet for [sku], suspending until the user is
     * done with it.
     */
    suspend fun purchase(sku: String): PurchaseResult

    /**
     * Every non-consumable this store account already owns.
     *
     * **This is the reinstall story.** A non-consumable belongs to the Apple
     * ID / Google account, not the install, so a user who deletes the app and
     * reinstalls gets their unlock back from here without signing into
     * anything, without a redemption code, and without tapping Restore.
     * Neither platform prompts for this query, which is why it can run
     * silently at launch.
     */
    suspend fun queryOwnedSkus(): QueryOwnedResult

    /**
     * Acknowledge a purchase. **Android auto-refunds anything unacknowledged
     * after three days**, so a missed call here is a refund, not a warning.
     * No-op on iOS, where StoreKit finishing is handled inside the coordinator.
     */
    suspend fun acknowledge(purchaseToken: String): Boolean
}

enum class ConnectionState {
    /** [BillingClient.connect] hasn't run yet. */
    Disconnected,

    /** [BillingClient.connect] in progress. */
    Connecting,

    /** Store is reachable and queries can be issued. */
    Connected,

    /**
     * The platform reported a permanent failure: device unsupported, Play
     * Services missing, store account unavailable. Hide the buy button rather
     * than retrying — but note this must never hide the *unlocked features* of
     * someone who already paid. See [Entitlements].
     */
    Unavailable,
}
