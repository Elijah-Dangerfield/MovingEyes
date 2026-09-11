# Testing purchases

Four ways to exercise the money path, in the order you'll reach for them. The
first needs no Apple or Google account at all; the last is the only one that
proves the real store works.

The thing to understand before any of it: **a grant is never revoked.** See
`Entitlements` for why. Once `isUnlocked` is true it stays true, and every route
back to a locked state is listed here. That asymmetry is deliberate and it is
also why testing the paywall twice takes more setup than you'd expect.

## 1. The fake store: zero setup, for UI work

Debug builds resolve `RealPurchasesEnabled` to false, so `FakeBillingClient`
serves a seeded catalog at $0.99 and starts owning nothing. Buy, and it grants
instantly. To go round again: QA menu (`movingeyes://qa-config`) → Clear
entitlement → relaunch.

Nothing in the real StoreKit or Play path runs, so this proves the paywall looks
right and proves nothing else.

## 2. StoreKit configuration file: the local iOS loop

`apps/ios/Products.storekit` defines `movingeyespro` locally, and the Run action
of the `iosApp` scheme points at it. Xcode serves the catalog itself: no App
Store Connect, no Apple Account, no network.

This is the one that makes purchases genuinely easy to revoke. With the app
running from Xcode:

**Debug → StoreKit → Manage Transactions** lists every transaction, with Delete
and Refund on each. Delete one and the app is locked again on the next launch, with
no account state anywhere to clean up.

The file also fakes the conditions that are otherwise almost impossible to
reproduce. In Xcode, select `Products.storekit` and open the editor's settings
to turn on Ask to Buy, force **Load Products** to fail, which reproduces the
rejection we hit exactly: a paywall showing no price that errors on tap. Or make
verification fail.

Two things to know:

- Debug builds pick the fake store by default, so flip **Real purchases
  enabled** on in the QA menu first, otherwise this file is never consulted. The
  override persists, so it's once per install.
- If Xcode doesn't pick the file up, set it by hand: **Product → Scheme → Edit
  Scheme → Run → Options → StoreKit Configuration**. The scheme lives in
  `xcuserdata` and is not checked in, so every machine sets this once.

## 3. Sandbox testers: a disposable Apple Account

For running against real App Store Connect rather than a local file, with a
purchase history you can wipe.

1. App Store Connect → **Users and Access → Sandbox → Test Accounts → +**. Use
   any email you control that is not already an Apple Account; it never receives
   mail and never needs verifying.
2. On the device: **Settings → Developer → Sandbox Apple Account**, sign in
   there. Not the App Store settings. Signing a sandbox account into the real
   App Store breaks it.
3. Run a build from Xcode. Purchases are free and hit real ASC.
4. To reset: App Store Connect → the tester → **Clear Purchase History**.

Sandbox testers are not the same thing as the people under **Users and Access →
People**. That list is your development team, and having your own Apple Account
there means nothing for purchases.

## 4. TestFlight: closest to production, and impossible to reset

TestFlight purchases are free and run in the sandbox environment, but they bill
against the tester's **real Apple Account**, not a sandbox tester. There is no
Clear Purchase History for a real account.

So the first time anyone buys the unlock on a TestFlight build, that Apple
Account owns `movingeyespro` for good. Clearing the entitlement in the QA menu
does not help: `refresh()` runs on the next launch, `Transaction.currentEntitlements`
still reports the purchase, and `grant()` puts it straight back. That is not a
bug, and it is the single most confusing thing about testing this app.

Plan for it: do exploratory paywall work in 1 or 2, and spend the TestFlight
purchase deliberately, once, when you actually want to verify the production
path end to end.

## Android

`FakeBillingClient` covers debug the same way. For the real Play path, Play
returns nothing from `queryProductDetails` until a build is on a track, so:
publish to internal testing, then add the account to **Play Console → Setup →
License testing**. Licensed testers buy for free and can refund and re-buy from
the Play Store's order history, which makes Android the easier of the two to
loop on.

## Deep links

Debug and beta builds only (`BuildInfo.isQaBuild`):

- `movingeyes://qa-config`: config overrides, purchase, restore, clear entitlement
- `movingeyes://design-system`
- `movingeyes://eyes`
