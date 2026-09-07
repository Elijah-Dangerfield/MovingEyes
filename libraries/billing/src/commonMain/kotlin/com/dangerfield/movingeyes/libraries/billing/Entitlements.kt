package com.dangerfield.movingeyes.libraries.billing

import kotlinx.coroutines.flow.StateFlow

/**
 * Whether this device has the unlock. The only billing surface feature code
 * should touch.
 *
 * ## The rule that matters
 *
 * **A cached grant is never revoked by the store.** Once [isUnlocked] is true
 * it stays true until someone explicitly clears it from the QA menu. The store
 * can grant; it cannot take away.
 *
 * That is a deliberate asymmetry and it costs real money in the refund case —
 * someone who refunds keeps the unlock. It is worth it. The failure it
 * prevents is a paying customer whose decoration goes back to the free tier at
 * 8pm on Halloween because their tablet dropped off wifi, or Play Services
 * updated mid-evening, or a shared family tablet was signed into a different
 * store account. That user has a mounted device behind a painting and no
 * appetite for troubleshooting, and they leave a one-star review that costs
 * far more than the 99 cents.
 *
 * Concretely: a [QueryOwnedResult.Failed] must never be treated as "doesn't
 * own it", and even a clean [QueryOwnedResult.Success] with an empty set does
 * not clear an existing grant.
 *
 * ## Reinstall
 *
 * There is no account and no redemption code, and none is needed. A
 * non-consumable belongs to the store account, so [refresh] on a fresh install
 * asks the store and gets it back — silently, no prompt, no tap. [restore] is
 * the manual fallback for someone who switched store accounts or was signed
 * out at launch, and it stays visible in Settings because Apple requires a
 * restore mechanism for non-consumables regardless of whether anyone needs it.
 */
interface Entitlements {

    /**
     * True when everything is unlocked. Hot, cached, and readable
     * synchronously on the first frame — gating UI must never flash the free
     * tier at someone who paid while an async check resolves.
     */
    val isUnlocked: StateFlow<Boolean>

    /** The store's price for the unlock, once known. Null before the first
     *  successful product query, or when the store is unreachable. */
    val product: StateFlow<BillingProduct?>

    /**
     * Silently re-check the store and fold the answer in. Grants only; never
     * revokes. Safe to call on every launch and every foreground, and cheap
     * enough that it should be.
     */
    suspend fun refresh()

    /** Run the purchase sheet. */
    suspend fun purchase(): PurchaseOutcome

    /**
     * The user-initiated version of [refresh], behind the Settings button.
     * Differs only in that it reports its outcome so the UI can say something
     * — a silent refresh that finds nothing says nothing, but a person who
     * taps Restore and gets no feedback taps it four more times.
     */
    suspend fun restore(): RestoreOutcome
}

/** What came of [Entitlements.purchase]. */
sealed interface PurchaseOutcome {
    /** Paid, or already owned. Either way [Entitlements.isUnlocked] is now true. */
    data object Unlocked : PurchaseOutcome

    /** Sheet dismissed. Show nothing at all. */
    data object Cancelled : PurchaseOutcome

    /** Store couldn't be reached. Worth a retry. */
    data object StoreUnavailable : PurchaseOutcome

    /** Anything else, with the store's own words for the log. */
    data class Failed(val reason: String) : PurchaseOutcome
}

/** What came of [Entitlements.restore]. */
sealed interface RestoreOutcome {
    /** Found a purchase. Unlocked. */
    data object Restored : RestoreOutcome

    /**
     * The store answered and this account owns nothing. Say so plainly, and
     * point at the likely cause: they're probably signed into a different
     * store account than the one that paid.
     */
    data object NothingToRestore : RestoreOutcome

    /** Couldn't ask. Distinct from [NothingToRestore] on purpose — telling
     *  someone "you never bought this" when the truth is "we couldn't check"
     *  is the kind of thing that produces refund requests. */
    data object StoreUnavailable : RestoreOutcome
}
