# Setup checklist — Moving Eyes

Work through these in order.

**Hour 1 — a running app:**
- [ ] [Local dev](#local-dev) — hooks + first build

**Day 1 — pipelines + visibility:**
- [ ] [GitHub secrets](#github-secrets) — signing, store credentials, Sentry
- [ ] [Repo settings](#repo-settings) — Pages, Actions, branch protection
- [ ] [Day-1 verification](#day-1-verification) — prove crash reporting and deploys work

**Before shipping:**
- [ ] [Store listings](#store-listings) — Play Console + App Store Connect
- [ ] [In-app purchase](#in-app-purchase) — one non-consumable, both stores
- [ ] [First release](#first-release) — the manual-promotion gotcha
- [ ] [App icons](#app-icons)

---

## Local dev

```sh
./scripts/install_hooks.sh
```

```sh
./gradlew build
```

The Gradle build fails with an install-hooks message if you skip `install_hooks.sh`. That's intentional — release-please derives version bumps from commit history, so every commit must be in Conventional-Commits form (`feat:`, `fix:`, etc.).

To bypass in scripted contexts (not CI — the `CI` env var is honored): `-Dmovingeyes.skipGitHooksCheck=true`.

You also need `local.properties` pointing at your Android SDK:

```sh
printf 'sdk.dir=%s\n' "$HOME/Library/Android/sdk" > local.properties
```

---

## GitHub secrets

Set under **Settings → Secrets and variables → Actions**. All are required for `release.yml` to ship.

### Android signing

| Secret | How to get it |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | `base64 -i upload-keystore.jks \| pbcopy` |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias inside the keystore |
| `ANDROID_KEY_PASSWORD` | Key password |
| `PLAY_SERVICE_ACCOUNT_JSON` | Play Console → Setup → API access → create service account with *Release apps to testing tracks* + *Release apps to production*. Download the JSON. |

### Apple signing + App Store Connect

| Secret | How to get it |
| --- | --- |
| `APPLE_TEAM_ID` | Apple Developer → Membership → Team ID |
| `ASC_KEY_ID` | App Store Connect → Users and Access → Keys → Key ID |
| `ASC_ISSUER_ID` | Same page — Issuer ID (top of the Keys tab) |
| `ASC_KEY_P8_BASE64` | `base64 -i AuthKey_XXX.p8 \| pbcopy` |
| `APPLE_DIST_CERT_P12_BASE64` | Export your Apple Distribution cert from Keychain as .p12, then `base64 -i dist.p12 \| pbcopy` |
| `APPLE_DIST_CERT_PASSWORD` | Password you set when exporting the .p12 |
| `FASTLANE_APPLE_ID` *(optional)* | Apple ID email, for `fastlane deliver` |

### Sentry

| Secret | Notes |
| --- | --- |
| `SENTRY_AUTH_TOKEN` | Sentry → User Auth Tokens → scope: `project:releases`, `org:read`. Used by `beta.yml`/`release.yml` to create releases + upload mappings/dSYMs. |
| `SENTRY_DSN` | Sentry → Project Settings → Client Keys (DSN). Baked into store builds so crash reporting is live; blank leaves crash reporting dormant. |

And under **Settings → Secrets and variables → Actions → Variables** (not secrets):

| Var | Value |
| --- | --- |
| `SENTRY_ORG` | Your Sentry org slug |
| `SENTRY_PROJECT` | Your Sentry project slug |

---

## Repo settings

- **Actions** → enable workflows.
- **Pages** → Source: `Deploy from a branch`, Branch: `main` / folder: `/pages`. (The `pages.yml` workflow can also publish on push.)
- **Branch protection** on `main`:
  - Require PR.
  - Require status checks: `CI / Build + test`, `commitlint / Validate PR title`.
  - Require linear history (so release-please squash-merges cleanly).

---

## Store listings

The listing is not an afterthought here. A seasonal app's entire year of traffic arrives in a ten-day window and almost all of it comes through store search.

1. **Play Console** → Create app → store listing, data-safety form, content rating, pricing/distribution. Create at least one internal track tester.
2. **App Store Connect** → My Apps → New App → pick the bundle ID that matches `apps/ios/fastlane/Appfile`. Note: Apple checks the binary's bundle name for uniqueness at *delivery* time (ITMS-90129), not here.
3. **TestFlight** external group: create a group named `External Testers` (or change `TESTFLIGHT_EXTERNAL_GROUP` in `release.yml`).
4. Privacy policy + terms URLs — the `pages/` folder generates these; once Pages is enabled they're at `https://<you>.github.io/<repo>/privacy.html`. Paste into both listings.

Two things specific to this app:

- **The first screenshot must be a photograph of a tablet behind a painting in a real hallway**, not a screenshot of the UI. Nobody understands this product from a picture of eyes on a rectangle. Show the physical trick first, then the editor, then the style variety.
- **Include a demo video in the review notes.** An app that is "just some eyes" is exposed to Apple's 4.3 minimum-functionality rejection, and a reviewer looking at a tablet on a desk will not understand what they're holding. Make the depth obvious: twelve styles, a full editor, presets, reactivity, scheduling.

Expect a 12+ / Teen age rating given the horror imagery. Set it deliberately rather than letting the questionnaire surprise you.

---

## In-app purchase

One non-consumable, `$4.99`, unlocking everything. No subscription: roughly 90% of installs land between October 20 and November 1, and charging recurring money for a decoration used one night a year produces refunds, chargebacks, and one-star reviews.

1. **Play Console** → Monetize → In-app products → create the product.
2. **App Store Connect** → Features → In-App Purchases → Non-Consumable → same product id.
3. Wire both into RevenueCat and put the SDK key where the billing module reads it.

Test a real sandbox purchase *and* a restore-after-reinstall on both platforms before shipping. Then kill the network mid-session and confirm the cached entitlement still grants — nobody gets locked out of their decoration on Halloween night because RevenueCat had a blip.

---

## First release

> **!!! READ THIS BEFORE YOUR FIRST RELEASE !!!**
>
> Google Play and Apple both reject automated production uploads until a manually-promoted build exists. `release.yml` detects this and routes the **first** release to:
>
> - **Play internal track** (not production).
> - **TestFlight internal** (not external, not App Store submission).
>
> You must then, **once**:
>
> 1. **Play Console** → Internal testing → Promote release → Production. Fill out the production rollout form manually.
> 2. **App Store Connect** → TestFlight build → Submit for review manually.
>
> From release #2 onward, the pipeline uploads straight to Play production (10% staged rollout) and submits to the App Store with phased release.

The release-please PR shows a `!!! FIRST RELEASE !!!` banner the first time around so this is hard to miss.

**Submit by October 5** to leave room for one rejection cycle before the October 10 deadline. Store search ranking takes days to settle and the traffic spike starts around the 20th.

---

## App icons

Drop your icons into:

- **iOS** → `apps/ios/iosApp/Assets.xcassets/AppIcon.appiconset/` (replace the placeholder set).
- **Android** → `apps/compose/src/androidMain/res/mipmap-*/` (replace `ic_launcher*.webp`).
- **Shared** → `libraries/resources/src/commonMain/composeResources/drawable/`.
- **GitHub Pages** → `pages/app-icon.png`, `pages/favicon.png`, `pages/apple-touch-icon.png`.

---

## Deep links

Compose NavHost handles the routing once URLs reach it. Per-route deep links go on `screen<Route>(deepLinks = ...)` — use `routeDeepLink<T>()`, never bare `navDeepLink` (iOS crashes on the missing base-route NavTypes).

The custom-scheme wiring is enabled on both platforms and reserved for scene-code imports: the intent filter in `apps/compose/src/androidMain/AndroidManifest.xml` and `CFBundleURLTypes` in `apps/ios/iosApp/Info.plist`. `iOSApp.swift` forwards every `.onOpenURL` event to the Kotlin `DeepLinkBridge`; `App.kt` routes them into the nav graph.

---

## In-app review

Inject `ReviewPrompter` and call `requestReview()` after a display session longer than ten minutes — someone who has actually mounted the thing and watched it work. The OS owns the throttling decision; both stores rate-limit how often the dialog shows. Don't show your own UI before or after.

---

## Day-1 verification

1. **App boots to a black canvas** on an Android device and the iOS simulator. Cold start under 1.5s.
2. **Trigger a test crash → Sentry.** Debug builds: shake → debug menu. Expected: the event in Sentry within a minute, tagged with `session_id` and `commit_sha`.
3. **A beta build reaches TestFlight and Play internal** from a push to `main`.

See `docs/release-automation.md` for the full pipeline runbook.
