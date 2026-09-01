# :libraries:config

Remote config / feature flags for the client. The API module defines the
`ConfiguredValue` convention and the repository interfaces; `:libraries:config:impl`
owns fetching, caching, and QA overrides.

## The one convention: injectable `ConfiguredValue` subclasses

Every flag is its own injectable class, contributed into the app graph's
`Set<QaConfigValue>` multibinding so it appears in the QA menu automatically.
The typed bases in `TypedConfiguredValues.kt` (`FlagConfigValue`,
`IntConfigValue`, `LongConfigValue`, `DoubleConfigValue`, `StringConfigValue`)
fill in `resolveValue` for scalars, so a concrete flag only declares
`name` / `path` / `default`:

```kotlin
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = QaConfigValue::class, multibinding = true)
class GoogleSignInEnabled(appConfigMap: AppConfigMap) : FlagConfigValue(appConfigMap) {
    override val name = "Google sign-in enabled"
    override val path = "identity.googleSignInEnabled"
    override val default = false
}

@Inject
class SignInViewModel(googleSignInEnabled: GoogleSignInEnabled) {
    val showGoogle = googleSignInEnabled()   // invoke() resolves the current value
}
```

Metadata on `ConfiguredValue` drives the QA menu: `group` (defaults to the
first path segment), `allowedValues` (renders a chip selector for enum-like
strings), `showInQADashboard` (default true), `description`, and a
debug-build-only `debugOverride`.

For structured payloads (a level ladder, a reward table) subclass
`JsonConfigValue<T>` with a `@Serializable` model and a serializer. It resolves
one path to a JSON subtree and falls back to the bundled `default` on any
decode failure, so a bad remote value can never brick the client. JSON values
hide from the QA dashboard by default.

## Resolution order

`AppConfigMap` is the merged snapshot a flag reads from. Highest wins:

1. QA override (`ConfigOverrideRepository`, persisted locally, editable from
   the QA menu)
2. `debugOverride` (debug builds only)
3. Remote value from `pages/app-config.json` (a sparse tree keyed by dotted
   `ConfiguredValue.path`)
4. The in-code `default`

An empty remote file is legitimate: the client always works on defaults alone.

## Offline-first, throttled-foreground refresh

`OfflineFirstAppConfigRepository` (impl) persists the last fetched tree via
`CacheFactory`, so the first frame never blocks on the network. That matters
more here than in a normal app: this one opens straight onto a live canvas and
has a 1.5s cold-start budget, so config is never allowed on the critical path.

It refetches on every app foreground (cold boot included), gated by
`ConfigRefreshThrottleMs` (a config value itself: 5 min default). Fetches carry
a 5s timeout. If a fetch fails and there is no cached snapshot, the bundled
`fallback_app_config.json` is persisted so subscribers still get a usable map;
with a cached snapshot, failures are logged and the cache stays.

The fetch is a plain unauthenticated GET. There is no targeting and no rollout
bucketing — one file, every install, same values.

## Observing changes

- `AppConfigMap` for point-in-time reads (what the typed bases use).
- `AppConfigFlow` / `AppConfigRepository.configStream()` for reactive
  consumers that need to react to a mid-session override edit.

## Publishing a change

There is no server and no admin console. The remote tree is
[`pages/app-config.json`](../../pages/app-config.json), served by the same
GitHub Pages site as the privacy policy:

1. Edit the file. Keys are dotted `ConfiguredValue.path`s; omit anything you
   don't want to override.
2. Merge to `main`. The Pages workflow publishes it.
3. Devices pick it up on their next foreground past the refresh throttle.

**This is the only lever that changes shipped behaviour without a store
review**, which is why it survived the strip. The app's whole year of traffic
arrives in a ten-day window, and Apple's review queue is longer than that
window can spare. Worth keeping honest: if a value is important enough to flip
mid-season, it should be a `ConfiguredValue` rather than a constant.

A malformed or missing file is safe — the fetch fails, the cache or the bundled
fallback stands, and every value resolves to its in-code default.
