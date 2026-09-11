package com.dangerfield.movingeyes.libraries.billing.impl

import com.dangerfield.movingeyes.libraries.billing.BillingClient
import com.dangerfield.movingeyes.libraries.billing.ConnectionState
import com.dangerfield.movingeyes.libraries.billing.MovingEyesProduct
import com.dangerfield.movingeyes.libraries.billing.PurchaseOutcome
import com.dangerfield.movingeyes.libraries.billing.PurchaseRecord
import com.dangerfield.movingeyes.libraries.billing.PurchaseResult
import com.dangerfield.movingeyes.libraries.billing.QueryOwnedResult
import com.dangerfield.movingeyes.libraries.billing.QueryProductsResult
import com.dangerfield.movingeyes.libraries.billing.RestoreOutcome
import com.dangerfield.movingeyes.libraries.flowroutines.AppCoroutineScope
import com.dangerfield.movingeyes.libraries.flowroutines.testing.CoroutineTest
import com.dangerfield.movingeyes.libraries.movingeyes.AppCache
import com.dangerfield.movingeyes.libraries.movingeyes.AppData
import com.dangerfield.movingeyes.libraries.movingeyes.AppEvent
import com.dangerfield.movingeyes.libraries.movingeyes.AppEventBus
import com.dangerfield.movingeyes.libraries.movingeyes.AppEvents
import com.dangerfield.movingeyes.libraries.storage.Cache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The money rules. Everything here is a decision someone could plausibly
 * "clean up" into a bug, so each test names the failure it prevents.
 */
class EntitlementsImplTest : CoroutineTest() {

    @Test
    fun `a cached unlock is live before the store is ever asked`() = runUnitTest {
        val client = FakeStore(connection = ConnectionState.Unavailable)
        val entitlements = entitlements(client, cached = true)

        // No store, no network, first frame: still unlocked. Anything else
        // flashes the free tier at a paying customer.
        assertTrue(entitlements.isUnlocked.value)
    }

    @Test
    fun `a store that says not-owned does not revoke a cached unlock`() = runUnitTest {
        val client = FakeStore(owned = QueryOwnedResult.Success(emptySet(), emptyList()))
        val entitlements = entitlements(client, cached = true)

        entitlements.refresh()

        // The signed-in store account isn't necessarily the one that paid — a
        // shared family tablet is ordinary. Revoking here turns a mounted
        // decoration back into the free tier on Halloween night.
        assertTrue(entitlements.isUnlocked.value)
    }

    @Test
    fun `clearing the entitlement stops the store re-granting it`() = runUnitTest {
        val client = FakeStore(owned = QueryOwnedResult.Success(MovingEyesProduct.All, listOf(record())))
        val cache = FakeAppCache(AppData(ignoreStoreGrants = true))
        val entitlements = entitlements(client, cache = cache)

        entitlements.refresh()

        // The whole point: an account that already owns the unlock can still
        // reach the paywall. Without this there is no way back to a locked
        // state on TestFlight, where the purchase belongs to a real Apple
        // Account that cannot be cleared.
        assertFalse(entitlements.isUnlocked.value)
    }

    @Test
    fun `buying lifts the suppression`() = runUnitTest {
        val client = FakeStore(purchase = PurchaseResult.Success(record()))
        val cache = FakeAppCache(AppData(ignoreStoreGrants = true))
        val entitlements = entitlements(client, cache = cache)

        entitlements.purchase()

        // Otherwise a tester who cleared, then bought, would be locked out of
        // the thing they just paid for on the next launch.
        assertTrue(entitlements.isUnlocked.value)
        assertFalse(cache.get().ignoreStoreGrants)
    }

    @Test
    fun `suppressing the grant leaves the price alone`() = runUnitTest {
        val client = FakeStore(owned = QueryOwnedResult.Success(MovingEyesProduct.All, listOf(record())))
        val cache = FakeAppCache(AppData(ignoreStoreGrants = true))
        val entitlements = entitlements(client, cache = cache)

        entitlements.refresh()

        // Suppressing the grant must not suppress the catalog, or the paywall
        // under test stops being the paywall a real customer sees.
        assertNotNull(entitlements.product.value)
    }

    @Test
    fun `a failed owned-query does not revoke a cached unlock`() = runUnitTest {
        val client = FakeStore(owned = QueryOwnedResult.Failed("store hiccup"))
        val entitlements = entitlements(client, cached = true)

        entitlements.refresh()

        assertTrue(entitlements.isUnlocked.value)
    }

    @Test
    fun `an owned purchase unlocks silently with no user action`() = runUnitTest {
        val client = FakeStore(owned = QueryOwnedResult.Success(MovingEyesProduct.All, listOf(record())))
        val entitlements = entitlements(client, cached = false)

        entitlements.refresh()

        // This is the reinstall path: no login, no redemption code, no tap.
        assertTrue(entitlements.isUnlocked.value)
    }

    @Test
    fun `a granted unlock is persisted so the next launch needs no store`() = runUnitTest {
        val cache = FakeAppCache(AppData())
        val client = FakeStore(owned = QueryOwnedResult.Success(MovingEyesProduct.All, listOf(record())))
        entitlements(client, cache = cache).refresh()

        assertTrue(cache.get().isUnlocked)
        assertEquals(FixedNowMs, cache.get().unlockedAtEpochMs)
    }

    @Test
    fun `already-owned is an unlock rather than a failure`() = runUnitTest {
        val client = FakeStore(purchase = PurchaseResult.AlreadyOwned(record()))
        val entitlements = entitlements(client, cached = false)

        // Someone who reinstalled and hit buy before the silent refresh landed.
        assertEquals(PurchaseOutcome.Unlocked, entitlements.purchase())
        assertTrue(entitlements.isUnlocked.value)
    }

    @Test
    fun `a cancelled purchase changes nothing`() = runUnitTest {
        val client = FakeStore(purchase = PurchaseResult.UserCancelled)
        val entitlements = entitlements(client, cached = false)

        assertEquals(PurchaseOutcome.Cancelled, entitlements.purchase())
        assertFalse(entitlements.isUnlocked.value)
    }

    @Test
    fun `a purchase is acknowledged so Play cannot auto-refund it`() = runUnitTest {
        val client = FakeStore(purchase = PurchaseResult.Success(record()))
        entitlements(client, cached = false).purchase()

        // Play reverses an unacknowledged purchase after three days. There is
        // no server to do this from, so it has to happen here.
        assertEquals(listOf(TestToken), client.acknowledged)
    }

    @Test
    fun `restore reports nothing-to-restore only when the store actually answered`() = runUnitTest {
        val answered = FakeStore(owned = QueryOwnedResult.Success(emptySet(), emptyList()))
        assertEquals(RestoreOutcome.NothingToRestore, entitlements(answered, cached = false).restore())

        // "You never bought this" is a very different sentence from "we
        // couldn't check", and getting it wrong generates refund requests.
        val failed = FakeStore(owned = QueryOwnedResult.Failed("offline"))
        assertEquals(RestoreOutcome.StoreUnavailable, entitlements(failed, cached = false).restore())

        val disconnected = FakeStore(connection = ConnectionState.Unavailable)
        assertEquals(RestoreOutcome.StoreUnavailable, entitlements(disconnected, cached = false).restore())
    }

    @Test
    fun `purchasing with an unreachable store is reported as unavailable rather than failed`() = runUnitTest {
        val client = FakeStore(connection = ConnectionState.Unavailable)
        assertEquals(PurchaseOutcome.StoreUnavailable, entitlements(client, cached = false).purchase())
    }

    @Test
    fun `foregrounding re-asks the store so a console price change lands without an app update`() = runUnitTest {
        val events = FakeAppEvents()
        val store = CountingStore()
        val subject = entitlements(store, events = events)
        runCurrent()

        val afterLaunch = store.productQueries
        events.foreground(isColdBoot = false)
        runCurrent()

        assertEquals(afterLaunch + 1, store.productQueries, "A warm foreground must re-query the store")
        assertNotNull(subject.product.value)
    }

    @Test
    fun `a cold boot foreground does not re-query because init already did`() = runUnitTest {
        val events = FakeAppEvents()
        val store = CountingStore()
        entitlements(store, events = events)
        runCurrent()

        val afterLaunch = store.productQueries
        events.foreground(isColdBoot = true)
        runCurrent()

        assertEquals(afterLaunch, store.productQueries, "Cold boot is already covered by init")
    }

    @Test
    fun `a price missed because the store was down at launch appears on the next foreground`() = runUnitTest {
        val events = FakeAppEvents()
        // Offline at launch: refresh() bails before querying, so no price.
        val store = CountingStore(connection = ConnectionState.Unavailable)
        val subject = entitlements(store, events = events)
        runCurrent()
        assertNull(subject.product.value, "Nothing to show while the store is unreachable")

        store.connection = ConnectionState.Connected
        events.foreground(isColdBoot = false)
        runCurrent()

        assertNotNull(subject.product.value, "Reconnecting must fill in the price without a restart")
    }

    private fun entitlements(
        client: BillingClient,
        cached: Boolean = false,
        cache: FakeAppCache = FakeAppCache(AppData(isUnlocked = cached)),
        events: FakeAppEvents = FakeAppEvents(),
    ) = EntitlementsImpl(
        billingClient = client,
        appCache = cache,
        clock = FixedClock,
        appEvents = events.appEvents,
        appScope = AppCoroutineScope(dispatchers),
    )


    /** Drives [AppEvent.OnForeground] through the replay-free `live()` stream. */
    private class FakeAppEvents {
        private val flow = MutableSharedFlow<AppEvent>(replay = 0, extraBufferCapacity = 16)
        val appEvents = AppEvents(bus = LiveBus(flow), backingFlow = flow)

        fun foreground(isColdBoot: Boolean) {
            flow.tryEmit(AppEvent.OnForeground(isColdBoot = isColdBoot))
        }

        private class LiveBus(private val flow: Flow<AppEvent>) : AppEventBus {
            override fun dispatch(event: AppEvent) {}
            override fun eventStream(): Flow<AppEvent> = flow
            override fun liveEventStream(): Flow<AppEvent> = flow
        }
    }

    private fun record() = PurchaseRecord(
        sku = MovingEyesProduct.UnlockEverything,
        orderId = "order-1",
        purchaseToken = TestToken,
        platform = com.dangerfield.movingeyes.libraries.billing.BillingPlatform.Fake,
        purchasedAtEpochMs = FixedNowMs,
    )

    private companion object {
        const val TestToken = "token-1"
        const val FixedNowMs = 1_700_000_000_000L
        val FixedClock = object : Clock {
            override fun now(): Instant = Instant.fromEpochMilliseconds(FixedNowMs)
        }
    }
}

private class CountingStore(
    var connection: ConnectionState = ConnectionState.Connected,
) : BillingClient {

    var productQueries = 0
        private set

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override suspend fun connect(): ConnectionState = connection.also { _connectionState.value = it }

    override suspend fun queryProducts(skus: Set<String>): QueryProductsResult {
        productQueries++
        return QueryProductsResult.Success(FakeBillingClient.DefaultCatalog.filterKeys { it in skus })
    }

    override suspend fun purchase(sku: String): PurchaseResult = PurchaseResult.UserCancelled
    override suspend fun queryOwnedSkus(): QueryOwnedResult = QueryOwnedResult.Success(emptySet(), emptyList())
    override suspend fun acknowledge(purchaseToken: String): Boolean = true
}

private class FakeStore(
    private val connection: ConnectionState = ConnectionState.Connected,
    private val owned: QueryOwnedResult = QueryOwnedResult.Success(emptySet(), emptyList()),
    private val purchase: PurchaseResult = PurchaseResult.UserCancelled,
) : BillingClient {

    val acknowledged = mutableListOf<String>()

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override suspend fun connect(): ConnectionState =
        connection.also { _connectionState.value = it }

    override suspend fun queryProducts(skus: Set<String>): QueryProductsResult =
        QueryProductsResult.Success(FakeBillingClient.DefaultCatalog.filterKeys { it in skus })

    override suspend fun purchase(sku: String): PurchaseResult = purchase

    override suspend fun queryOwnedSkus(): QueryOwnedResult = owned

    override suspend fun acknowledge(purchaseToken: String): Boolean {
        acknowledged += purchaseToken
        return true
    }
}

private class FakeAppCache(initial: AppData) : AppCache, Cache<AppData> {
    private var value = initial
    private val flow = MutableStateFlow(initial)
    override val updates = flow.asStateFlow()
    override suspend fun get(): AppData = value
    override suspend fun set(value: AppData) {
        this.value = value
        flow.value = value
    }

    override suspend fun clear() = set(AppData())
}
