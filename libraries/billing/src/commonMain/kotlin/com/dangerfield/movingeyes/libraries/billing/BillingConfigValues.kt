package com.dangerfield.movingeyes.libraries.billing

import com.dangerfield.movingeyes.libraries.config.AppConfigMap
import com.dangerfield.movingeyes.libraries.config.FlagConfigValue
import com.dangerfield.movingeyes.libraries.config.QaConfigValue
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Picks the real platform store or the fake, and defaults to the fake on debug
 * builds.
 *
 * That default is not a convenience, it's the only way to build the paywall.
 * **Play returns nothing from `queryProductDetails` until a build is published
 * to at least the internal track**, so a sideloaded debug build talking to the
 * real client gets an empty catalog and a paywall with no price on it. Apple is
 * friendlier — sandbox works against a product in "Ready to Submit", and Xcode
 * StoreKit configuration files work with no App Store Connect at all — but one
 * behaviour across both platforms beats two.
 *
 * Release and beta builds default to the real store so the money path is
 * exercised end to end in TestFlight and internal testing, against sandbox
 * receipts, well before anyone pays for real.
 *
 * Flip it in the QA menu (shake → Config) when you want the real store on a
 * debug build — after the app is on a track, that's how you test an actual
 * sandbox purchase locally.
 */
/**
 * Stops [Entitlements] granting from the store, so a tester can reach the
 * paywall on an account that already owns the unlock.
 *
 * This exists because of a real dead end. A grant is never revoked, and on
 * TestFlight a sandbox purchase belongs to a real Apple Account with no way to
 * clear it, so Clear entitlement appears to do nothing: the next launch reads
 * `currentEntitlements`, still sees the purchase, and grants it straight back.
 * Without this flag the only way back to a locked state is a second Apple
 * Account.
 *
 * Deliberately does not touch the product query. The point is to see the
 * paywall exactly as someone who has not paid sees it, price and all, which is
 * the only way to tell "the catalog is broken" from "I already own this".
 *
 * No `debugOverride`: leaving it off by default everywhere means a forgotten
 * override cannot lock out a paying customer.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = QaConfigValue::class, multibinding = true)
open class IgnoreStoreGrants(appConfigMap: AppConfigMap) : FlagConfigValue(appConfigMap) {
    override val name = "Ignore store grants (QA)"
    override val description = "Stops the store re-granting the unlock, so the paywall is " +
        "reachable on an account that already owns it. Clear the entitlement after turning this on."
    override val path = "billing.ignoreStoreGrants"
    override val default = false
    override val debugOverride: Boolean? = null
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = QaConfigValue::class, multibinding = true)
open class RealPurchasesEnabled(appConfigMap: AppConfigMap) : FlagConfigValue(appConfigMap) {
    override val name = "Real purchases enabled"
    override val description = "Off uses a fake store with a seeded SKU, so the paywall works " +
        "before the app is on a store track."
    override val path = "billing.realPurchasesEnabled"
    override val default = true
    override val debugOverride: Boolean? = false
}
