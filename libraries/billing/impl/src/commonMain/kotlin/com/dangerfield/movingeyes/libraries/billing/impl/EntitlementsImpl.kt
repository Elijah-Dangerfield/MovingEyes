package com.dangerfield.movingeyes.libraries.billing.impl

import com.dangerfield.movingeyes.libraries.billing.BillingClient
import com.dangerfield.movingeyes.libraries.billing.BillingProduct
import com.dangerfield.movingeyes.libraries.billing.ConnectionState
import com.dangerfield.movingeyes.libraries.billing.Entitlements
import com.dangerfield.movingeyes.libraries.billing.MovingEyesProduct
import com.dangerfield.movingeyes.libraries.billing.PurchaseOutcome
import com.dangerfield.movingeyes.libraries.billing.PurchaseRecord
import com.dangerfield.movingeyes.libraries.billing.PurchaseResult
import com.dangerfield.movingeyes.libraries.billing.QueryOwnedResult
import com.dangerfield.movingeyes.libraries.billing.QueryProductsResult
import com.dangerfield.movingeyes.libraries.billing.RestoreOutcome
import com.dangerfield.movingeyes.libraries.core.AutoInit
import com.dangerfield.movingeyes.libraries.core.Catching
import com.dangerfield.movingeyes.libraries.core.logging.KLog
import com.dangerfield.movingeyes.libraries.core.logging.logEvent
import com.dangerfield.movingeyes.libraries.flowroutines.AppCoroutineScope
import com.dangerfield.movingeyes.libraries.movingeyes.AppCache
import com.dangerfield.movingeyes.libraries.movingeyes.AppEvent
import com.dangerfield.movingeyes.libraries.movingeyes.AppEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Clock

/**
 * The cached-grant entitlement store. See [Entitlements] for why the cache is
 * sticky and what that trade costs.
 *
 * [AutoInit] so the cached value is hydrated and the silent store re-check has
 * started before the user can reach a paywall. Without it the first gated
 * control someone touches would inject this class for the first time and
 * briefly report "locked" to a customer who paid — which is exactly the flash
 * this class exists to prevent.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = Entitlements::class)
@ContributesBinding(AppScope::class, boundType = AutoInit::class, multibinding = true)
@Inject
class EntitlementsImpl(
    private val billingClient: BillingClient,
    private val appCache: AppCache,
    private val clock: Clock,
    private val appEvents: AppEvents,
    private val appScope: AppCoroutineScope,
) : Entitlements, AutoInit {

    private val logger = KLog.withTag("Entitlements")

    private val _isUnlocked = MutableStateFlow(false)
    override val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _product = MutableStateFlow<BillingProduct?>(null)
    override val product: StateFlow<BillingProduct?> = _product.asStateFlow()

    init {
        appScope.launch {
            // Disk first and fast: this is the value that must be correct on
            // the first frame. The store round-trip that follows can take
            // seconds and is allowed to.
            _isUnlocked.value = Catching { appCache.get().isUnlocked }
                .onFailure { logger.e(it) { "Failed to hydrate cached entitlement" } }
                .getOrDefault(false)
            refresh()
        }
        observeForegroundForRefresh()
        observeCacheForRevocation()
    }

    /**
     * Mirror the cache, so clearing the grant takes effect on the spot.
     *
     * This does not weaken the never-revoke rule. The store still cannot take
     * an unlock away: [refresh] only ever grants. The cache is the grant, and
     * the only thing that writes false to it is the QA menu, which is a human
     * deliberately asking. Before this the QA screen had to say "relaunch to
     * see it take effect", which read as the button not working.
     */
    private fun observeCacheForRevocation() {
        appCache.updates
            .map { it.isUnlocked }
            .distinctUntilChanged()
            .onEach { _isUnlocked.value = it }
            .launchIn(appScope)
    }

    /**
     * Re-ask the store every time the app comes forward.
     *
     * Two things go stale between launches and neither is ours to control. The
     * price is set in the consoles and changes without an app update, so a
     * session that started before a price change would otherwise show the old
     * one until the process died. And the grant can appear elsewhere: the same
     * store account buying on a second device, or a family member's purchase
     * landing through Family Sharing.
     *
     * It also repairs the launched-offline case. [refresh] returns early when
     * the store is unreachable, so before this the paywall would show "Unlock
     * everything" with no price for the rest of the session even after the
     * network came back.
     *
     * Unthrottled on purpose, unlike remote config: a foreground is not
     * frequent, both platforms serve this from their own local cache, and the
     * cost of being wrong here is showing someone the wrong price or telling a
     * paying customer they haven't paid.
     */
    private fun observeForegroundForRefresh() {
        appEvents.live()
            .filterIsInstance<AppEvent.OnForeground>()
            .onEach { event ->
                // Cold boot already refreshed in init; don't pay for it twice.
                if (!event.isColdBoot) refresh()
            }
            .launchIn(appScope)
    }

    override suspend fun refresh() {
        if (billingClient.connect() != ConnectionState.Connected) {
            logger.d { "Store unreachable; keeping the cached entitlement" }
            return
        }

        when (val products = billingClient.queryProducts(MovingEyesProduct.All)) {
            is QueryProductsResult.Success -> {
                val product = products.products[MovingEyesProduct.UnlockEverything]
                _product.value = product
                if (product == null) reportUnsellable("store returned no matching product")
            }

            // Error, not debug. Both of these mean nobody on this build can buy
            // anything, and the old debug level meant Sentry never saw it: an
            // App Store reviewer hit exactly this, we shipped it, and the first
            // we knew was the rejection email.
            is QueryProductsResult.Failed -> reportUnsellable(products.message)
            QueryProductsResult.NotConnected -> Unit
        }

        val suppressed = Catching { appCache.get().ignoreStoreGrants }.getOrDefault(false)

        when (val owned = billingClient.queryOwnedSkus()) {
            is QueryOwnedResult.Success -> {
                // Grant only. An empty result is a legitimate "this account
                // owns nothing", but it does not clear an existing grant —
                // the account signed into the store is not necessarily the
                // account that paid, and a shared family tablet is a normal
                // case rather than an exotic one.
                if (MovingEyesProduct.UnlockEverything in owned.skus) {
                    owned.purchases
                        .firstOrNull { it.sku == MovingEyesProduct.UnlockEverything }
                        ?.let { acknowledgeIfNeeded(it) }
                    if (suppressed) {
                        logger.d { "Store says owned; QA asked us to ignore that" }
                    } else {
                        grant(source = "restore")
                    }
                }
            }

            is QueryOwnedResult.Failed -> logger.d { "Owned query failed: ${owned.message}" }
            QueryOwnedResult.NotConnected -> Unit
        }
    }

    /**
     * The paywall is up and the buy button cannot work. Reported at error level
     * so it becomes a Sentry event, and as an analytics event so the rate is
     * visible on a board rather than one issue at a time.
     *
     * Once per process. [refresh] runs on every foreground, and a store that is
     * unhappy stays unhappy, so reporting each time would turn one broken build
     * into thousands of identical events and teach everyone to ignore them.
     */
    private fun reportUnsellable(reason: String?) {
        if (hasReportedUnsellable) return
        hasReportedUnsellable = true
        logger.e { "Nothing to sell: ${reason ?: "unknown"}. The paywall cannot complete a purchase." }
        logger.logEvent("purchase_unavailable", "reason" to (reason ?: "unknown"))
    }

    private var hasReportedUnsellable = false

    override suspend fun purchase(): PurchaseOutcome {
        if (billingClient.connect() != ConnectionState.Connected) {
            return PurchaseOutcome.StoreUnavailable
        }

        return when (val result = billingClient.purchase(MovingEyesProduct.UnlockEverything)) {
            is PurchaseResult.Success -> {
                acknowledgeIfNeeded(result.purchase)
                grant(source = "purchase")
                PurchaseOutcome.Unlocked
            }

            // Not an error. The usual cause is someone who reinstalled and
            // tapped buy before the silent refresh landed, and charging them
            // twice is not an option the store offers anyway.
            is PurchaseResult.AlreadyOwned -> {
                acknowledgeIfNeeded(result.purchase)
                grant(source = "already_owned")
                PurchaseOutcome.Unlocked
            }

            PurchaseResult.UserCancelled -> PurchaseOutcome.Cancelled
            PurchaseResult.NotConnected -> PurchaseOutcome.StoreUnavailable

            // Someone tapped buy, meant it, and did not get the thing. That is
            // the most expensive failure in the app and it gets an event.
            is PurchaseResult.Failed -> {
                logger.e { "Purchase failed: ${result.reason}" }
                logger.logEvent("purchase_failed", "reason" to result.reason)
                PurchaseOutcome.Failed(result.reason)
            }
        }
    }

    override suspend fun restore(): RestoreOutcome {
        if (billingClient.connect() != ConnectionState.Connected) {
            return RestoreOutcome.StoreUnavailable
        }

        return when (val owned = billingClient.queryOwnedSkus()) {
            is QueryOwnedResult.Success -> {
                if (MovingEyesProduct.UnlockEverything in owned.skus) {
                    owned.purchases
                        .firstOrNull { it.sku == MovingEyesProduct.UnlockEverything }
                        ?.let { acknowledgeIfNeeded(it) }
                    grant(source = "restore")
                    RestoreOutcome.Restored
                } else {
                    RestoreOutcome.NothingToRestore
                }
            }

            // Never NothingToRestore. Telling someone "you never bought this"
            // when the truth is "we couldn't ask" is how refund requests start.
            is QueryOwnedResult.Failed -> RestoreOutcome.StoreUnavailable
            QueryOwnedResult.NotConnected -> RestoreOutcome.StoreUnavailable
        }
    }

    /**
     * Play auto-refunds an unacknowledged purchase after three days, so a
     * missed acknowledgement isn't a warning, it's a reversed sale. There's no
     * server to acknowledge from, so it happens here, right after the grant.
     */
    private suspend fun acknowledgeIfNeeded(purchase: PurchaseRecord) {
        if (purchase.isAcknowledged) return
        val acknowledged = Catching { billingClient.acknowledge(purchase.purchaseToken) }
            .getOrDefault(false)
        if (!acknowledged) {
            logger.w { "Purchase ${purchase.orderId} not acknowledged; Play will refund it in 3 days" }
        }
    }

    private suspend fun grant(source: String) {
        if (_isUnlocked.value) return

        _isUnlocked.value = true
        Catching {
            appCache.update {
                // Lifts the QA suppression too: someone who just bought it
                // means it, and leaving the flag set would hide the thing they
                // paid for on the next launch.
                it.copy(
                    isUnlocked = true,
                    unlockedAtEpochMs = clock.now().toEpochMilliseconds(),
                    ignoreStoreGrants = false,
                )
            }
        }.onFailure { error ->
            // The in-memory flag still stands for this run, so the user gets
            // what they paid for now; the next launch re-grants from the store.
            logger.e(error) { "Unlocked but failed to persist the entitlement" }
        }

        logger.logEvent("purchase_completed", "source" to source)
    }
}
