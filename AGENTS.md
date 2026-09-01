# AGENTS.md

Guidelines for AI agents working in the Moving Eyes repository.

## Overview

KMP (Kotlin Multiplatform) app with Compose Multiplatform. Modular architecture with Room database, navigation, and SEAViewModel pattern.

This is **Kotlin Multiplatform**—most code is shared, but some platform features (permissions, sensors, native APIs) require platform-specific implementations. When implementing something not inherently cross-platform, follow the patterns in `docs/swift-kotlin-communication-patterns.md`.

### What this app is

Moving Eyes turns a phone or tablet into a Halloween decoration: the screen fills with animated eyes, the device gets taped behind a painting with the eyes cut out, and it runs unattended for five hours.

It is a **display appliance**, not a drawing app, and that reframe drives everything. There are **no accounts and no backend**. Don't reintroduce an auth layer, a sync engine, or a server — they were deliberately stripped. The canvas is the screen at 1:1 (no zoom, no pan), because a pixel here is a physical millimetre on a device about to be aligned with holes cut in cardboard.

The app makes exactly three kinds of outbound request, none on the critical path and all failing closed:

- **Sentry** — crashes and user feedback (`:libraries:movingeyes:impl`).
- **Grafana** — `logEvent` app events and Warn+ logs over OTLP, disk-buffered (`:libraries:telemetry:impl`). This is what answers "is the product working"; see `docs/practices/observability.md`.
- **Remote config** — one static JSON file on GitHub Pages (`:libraries:config`). The only way to change shipped behaviour without a store review, which matters when the season is ten days long.

Anything else that phones home needs a reason, because near-empty store privacy labels are a deliberate marketing asset for an app that asks for a microphone.

## Feature flags (`:libraries:config`)

Every flag is an injectable `ConfiguredValue` subclass contributed into `Set<QaConfigValue>`, so it appears in the QA menu (shake → Config) automatically. Resolution order, highest first: local QA override → `debugOverride` (debug builds only) → `pages/app-config.json` → the in-code `default`.

`debugOverride` is the important one for anything store-backed: a sideloaded debug build has no provisioned Play/StoreKit catalog, so a real billing client returns nothing. Default those flags to the fake on debug and the real path on shippable builds. Full reference in [`libraries/config/README.md`](libraries/config/README.md).

## Build Commands

```shell
./gradlew :apps:compose:assembleDebug          # Android
./gradlew :apps:compose:compileKotlinIosSimulatorArm64  # iOS Kotlin
xcodebuild -project apps/ios/iosApp.xcodeproj -scheme iOS -sdk iphonesimulator  # iOS full
```

## Module Structure

```
apps/compose/          # KMP entry point (Android + iOS)
apps/ios/              # Swift wrapper
features/<name>/       # Routes, public API
features/<name>/impl/  # Screens, ViewModels
libraries/<name>/      # Interfaces
libraries/<name>/impl/ # Implementations
```

**Rules** — enforced at Gradle configuration by the convention plugins:

- Only `:apps:*` may depend on `*:impl`. Impls are DI wiring composed by the app, not consumed by other modules.
- Feature `impl` modules may depend on another feature's `api`. Feature `api` modules may **not** depend on other feature `api`s (api-to-api is a cycle risk — shared types go in a library).
- Sub-modules of the same feature (`:features:foo:storage` → `:features:foo`) are allowed.
- `:libraries:storage:impl` is the one shared impl — it owns the `AppDatabase`.

Shared code → libraries. Main modules expose interfaces only; impl modules contain implementations.

## Conventional Commits (required)

Every commit (and every PR title — PRs are squash-merged) must follow [Conventional Commits](https://www.conventionalcommits.org/). Release-please derives the next version bump from commit history.

| Type | When | Version bump |
| --- | --- | --- |
| `feat:` | User-visible new capability | minor |
| `fix:` | Bug fix | patch |
| `perf:` | Perf improvement, user-visible | patch |
| `feat!:` / `BREAKING CHANGE:` | Breaking change | major |
| `refactor:`, `style:`, `test:`, `docs:`, `ci:`, `build:`, `chore:`, `revert:` | No user impact | none |

A local `.githooks/commit-msg` hook enforces this on every commit. The Gradle build fails with an install-hooks message if the hook isn't wired — run `./scripts/install_hooks.sh`.

## Convention Plugins

| Plugin | Use |
|--------|-----|
| `movingeyes.kotlin.multiplatform` | Pure Kotlin |
| `movingeyes.compose.multiplatform` | Kotlin + Compose |
| `movingeyes.feature` | Feature modules |
| `movingeyes.application` | apps:compose only |

Use `/scripts/create_module` for new modules.

## DI (kotlin-inject-anvil)

```kotlin
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class MyImpl : MyInterface

// Multibinding for FeatureEntryPoints
@ContributesBinding(AppScope::class, multibinding = true)
```

No expect/actual for platform impls—bind different implementations per platform. iOS impls written in Swift get passed into the DI graph via `IosAppComponentFactory.create(...)`.

### Boot-time construction: the `AutoInit` marker

Kotlin-inject singletons are constructed lazily on first injection, so a repo that nobody touches until a deep nav target stays cold — a hydrate-from-disk or listener-registering `init {}` doesn't run until something injects the class.

For singletons where the warm path matters (app-lifecycle dispatchers, disk-backed repositories, anything whose `init {}` is load-bearing), implement [`AutoInit`](libraries/core/src/commonMain/kotlin/com/movingeyes/libraries/core/AutoInit.kt) and contribute a second binding via multibinding:

```kotlin
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = MyRepository::class)
@ContributesBinding(AppScope::class, boundType = AutoInit::class, multibinding = true)
@Inject
class MyRepositoryImpl(...) : MyRepository, AutoInit
```

The `Set<AutoInit>` is resolved at app start (`Application.onCreate` on Android, `iOSApp.init` on iOS, `App.kt` remember-block on first composition). Resolving the set forces every contributor to construct, which runs their `init {}` — that's where hydrate-from-disk and lifecycle-listener registration happen.

**Opt in** when there's first-touch latency the user notices, an `init {}` that registers a listener, or a cache that needs its observer running before the user can navigate. **Skip** for debug-only / QA-menu singletons and anything whose `init {}` is empty. Forgetting the marker is a perf regression, not a correctness one — the class still works lazily — so the bigger risk is overuse making boot slow.

## Testing

Conventions (hand-rolled fakes only, dispatcher choice, which layer catches which bug) live in [`docs/practices/testing.md`](docs/practices/testing.md) — read it before adding tests.

The behaviour engine in `:libraries:eyes` is the module that most repays testing: it's pure Kotlin with no Compose dependency, driven by an injected clock, so a synthetic clock can assert that saccade intervals fall in range, that phase offsets keep a pair from ever blinking in lockstep, and that a startle decays over three seconds. Rendering and gesture code are verified on a device, not in unit tests.

## SEAViewModel Pattern

```kotlin
class MyViewModel : SEAViewModel<State, Event, Action>(initialStateArg = State()) {
    override suspend fun handleAction(action: Action) {
        when (action) {
            is Action.Load -> action.updateState { it.copy(loading = true) }
        }
    }
}
```

- **State**: Immutable data class for UI
- **Event**: One-shot side effects (navigation, toasts)
- **Action**: Only way to mutate state via `action.updateState { }`

## Navigation

Routes are `@Serializable` data classes extending `Route`. Register in `FeatureEntryPoint.buildNavGraph()`:

```kotlin
screen<MyRoute> { backStackEntry -> MyScreen(...) }
bottomSheet<SheetRoute> { backStackEntry, sheetState -> ... }
dialog<DialogRoute> { backStackEntry, dialogState -> ... }
navigation<MyGraph>(startDestination = MyRoute()) { screen<...>; bottomSheet<...> }
```

### iOS/Native landmines (production crashes, both)

1. **Routes must be `class` (or `data class`), never `data object`.** A
   `data object` route SIGSEGVs at navigate time on iOS — Native's
   serialization of object routes crashes inside androidx.navigation. An
   arg-less route is still a `data class MyRoute(...)` extending `Route`
   with default args.
2. **Every enum (or other non-primitive) route arg must be `@Serializable`
   AND registered in a typeMap.** Base-class args (`enter`/`exit`/`popExit`)
   come from `baseRouteTypeMap`, which every `screen<>`/`dialog<>`/
   `bottomSheet<>`/`routeDeepLink<>` builder merges in automatically. Args
   you add to your own route need `typeMap = mapOf(typeOf<MyEnum>() to
   serializableType<MyEnum>())` at the registration site. Miss one and
   graph-build throws `could not find any NavType for argument …` — often
   naming a *different* arg than the one you forgot. Use `routeDeepLink<T>`
   for deep links, never bare `navDeepLink`.

**Use `bottomSheet<>` for transient picker / overlay UIs** (a settings list, a "select an item" sheet) rather than pushing a full screen. The backstack stays one entry deep, the underlying screen is visible under a scrim, and `sheetState.dismiss()` is a clean exit. Reach for full `screen<>` only when the destination is its own context (settings page, detail view).

**Open external URLs via `Router.openWebLink(url)`** — don't roll your own platform `Intent.ACTION_VIEW` / `UIApplication.shared.open` plumbing. The implementation is in `libraries/navigation/impl/.../{Android,Ios,Jvm}WebLinkLauncher.kt` and is already wired into the DI graph and the `Router` interface.

## App-wide state

`AppData` (in `libraries/<projectid>/.../AppCache.kt`) is a `@Serializable` data class persisted via `CacheFactory.persistent`. Add fields here for things like:

- Onboarding flags (`hasUserOnboarded`)
- User-facing setting toggles
- Counters / lightweight telemetry (`feedbacksGiven`, `bugsReported`)

Don't roll a new persistent cache for a single boolean — extend `AppData`. Round-trip is automatic via `versionedJsonSerializer` (missing fields fall back to defaults, so adding a field is non-breaking). For an example wrapper that exposes `StateFlow<Boolean>` for Compose, see how a feature-level store reads `AppCache.updates` and writes via `appCache.update { it.copy(...) }`.

## Cross-cutting state in Compose

When something (a service, a setting, a theme value) is needed by every composable in a subtree but doesn't belong on the screen-level ViewModel, prefer a `staticCompositionLocalOf` over threading parameters. Provide it once at the subtree root:

```kotlin
val LocalMyService = staticCompositionLocalOf<MyService> { NoopMyService }

// At the screen root:
CompositionLocalProvider(LocalMyService provides realService) {
    HorizontalPager(...) { … }
}
```

Default it to a noop, never `error("not provided")`. This keeps `@Preview` and unit tests trivial — they get the noop automatically.

## Coding Guidelines

- Code like a staff engineer
- Use `Catching { }` from libraries/core instead of `runCatching`
- No comments in code
- Custom UI components in libraries/ui—avoid Material directly
- Check `ComposeApp.h` for Swift names of Kotlin types before using in Swift

## iOS Notes

- iOS framework compiled from `apps/compose`, embedded as `ComposeApp.xcframework`
- Swift types passed to Kotlin via `IosAppComponentFactory.create(...)`
- Reference `apps/compose/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework/Headers/ComposeApp.h` for generated Swift interfaces
- **Use `@ObjCName("TypeName", exact = true)` on Kotlin types used from Swift** to give stable names that won't change when project is renamed:
  ```kotlin
  @file:OptIn(ExperimentalObjCName::class)
  import kotlin.experimental.ExperimentalObjCName
  import kotlin.native.ObjCName
  
  @ObjCName("MyType", exact = true)
  interface MyType { ... }
  ```
  Note: The `exact = true` parameter prevents module prefixes from being added. Without it, the Swift name would be `<ModuleName><ObjCName>` (e.g., `ComposeAppMyType`).

## Key Files

| Purpose | Path |
|---------|------|
| SEAViewModel | `libraries/flowroutines/src/.../SEAViewModel.kt` |
| App DI | `apps/compose/src/.../AppComponent.kt` |
| iOS entry | `apps/ios/iosApp/iOSApp.swift` |
| Swift↔Kotlin patterns | `docs/swift-kotlin-communication-patterns.md` |

