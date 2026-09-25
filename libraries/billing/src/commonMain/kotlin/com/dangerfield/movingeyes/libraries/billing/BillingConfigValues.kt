package com.dangerfield.movingeyes.libraries.billing

import com.dangerfield.movingeyes.libraries.config.AppConfigMap
import com.dangerfield.movingeyes.libraries.core.BuildInfo
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

    /**
     * Nothing outside a debug build can turn this off.
     *
     * The QA menu can override any config value, and this is the one whose
     * override is worth money: false swaps in [FakeBillingClient], whose
     * catalog grants the unlock for nothing. The menu itself is gated on
     * [BuildInfo.isQaBuild], which includes the `beta` channel, and a
     * TestFlight build promoted to the App Store keeps that channel. So the
     * gate alone would have left the live store build one toggle away from
     * free.
     *
     * A store build therefore ignores the override entirely rather than
     * trusting the gate above it.
     */
    override fun resolveValue(): Boolean =
        if (BuildInfo.isDebug) super.resolveValue() else true
}
