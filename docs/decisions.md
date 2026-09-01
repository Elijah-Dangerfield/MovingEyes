# Architecture decisions

Append-only log. Add an entry whenever you make a non-trivial architectural call
(new module boundary, library choice, scope cut, schema shape). Each entry: date,
the decision, alternatives considered, and *why*. Newest first.

---

## 2026-09-01 — No accounts, no backend, no sync

**Decision:** deleted `:apps:server`, `:apps:admin`, `:apps:integration`,
`:libraries:identity`, `:libraries:networking`, `:features:onboarding` and
`:features:home`, plus the triggered-sync engine, the user-scoped data-reset dump,
and the Supabase build plumbing. This supersedes every server-side decision below,
which is kept only as history.

`:libraries:config` and `:libraries:telemetry:impl` went in this same pass and were
brought back within the day — see the reversal entry above for why that was a
mistake.

**Why:** Moving Eyes is a local display appliance. Purchases restore through the
store account, scenes live in Room on the device, and roughly 90% of a year's
installs land in a ten-day window — so an off-season hosting bill buys nothing.
Keeping the stack "just in case" costs a slower build, a wider attack surface, and
store privacy labels that stop being close to empty, which is a genuine marketing
asset for an app whose category is "spooky app wants your microphone."

**What survived and why:** Sentry crash reporting (in `:libraries:movingeyes:impl`,
which never depended on the deleted modules), `SessionTracker` for correlating a
run of the app, `logEvent` for analytics that currently have no sink, and all of the
CI / release-please / TestFlight / Play automation — that automation is the reason
to start from the template at all.

**Cost accepted:** nothing observes the app in production except Sentry and the
Grafana pipe, both of which are best-effort and kill-switchable. There is no
server-side view of anything, by design.

## 2026-09-01 — Grafana client observability and remote config kept (reversal)

**Decision:** `:libraries:telemetry:impl` and `:libraries:config` are back, after
being deleted earlier the same day. `RemoteConfigRemoteDataSource` now GETs a
static `pages/app-config.json` from GitHub Pages instead of the deleted server's
`/v1/app-config`; everything else in both modules is unchanged.

**Why the deletion was wrong:** the stated reason was that the Grafana pipe
existed to correlate client logs with server traces, so it had no purpose without
a server. `GrafanaLogTree`'s own header says the opposite — it is deliberately
direct-to-Grafana precisely so it survives a backend being down. It is a *client*
observability sink, and it answers a question Sentry cannot: not "did it crash"
but "did the product work". `display_session_ended` is the example that matters.
A user who enters display mode and leaves after thirty seconds has not crashed,
has not complained, and has not left a review; without this pipe they are
invisible, and the spec names that metric as the most important one in the app.

Deleting it also silently took the `ConfiguredValue` / QA-override system with it,
which is what makes a `debugOverride`-style billing flag possible on a device with
no provisioned store catalog. That dependency wasn't noticed at deletion time.

**What it cost to bring back:** four references. `SessionIdProvider` became
`SessionTracker.current.uuid`, `InstallIdProvider` had already moved to
`:libraries:movingeyes`, the ktor engine now resolves from the classpath instead of
a `platformHttpEngineFactory` in the deleted networking module, and `is_offline`
was dropped (below). Roughly an hour, against a rebuild-from-scratch that had been
scheduled for the paywall phase.

**Cost accepted:** two small outbound network dependencies in an app that
otherwise makes none — a config GET at foreground and an OTLP export. Both are
off the critical path, both fail closed to cached or bundled values, and both are
kill-switchable.

## 2026-09-01 — No `is_offline` attribute on telemetry records

**Decision:** dropped the `is_offline` attribute that used to ride every OTLP
record, rather than reintroducing a connectivity observer to feed it.

**Why:** it was read from `AppState.isOffline`, which was backed by the networking
module's connectivity watcher combined with witnessed request reachability. This
app makes no requests of its own, so there is nothing to witness, and a hardcoded
`false` in a telemetry field is worse than an absent field — it reads as data.

What the attribute was actually for is already covered: the durable disk buffer
means a record emitted with no network survives process death and ships later, and
`OfflineDurabilityTest` still pins that behaviour. If per-record connectivity ever
matters again, a platform connectivity shim belongs with the other device shims,
not resurrected from the networking module.

## 2026-06-21 — Server mirrors client conventions

**Decision:** `:apps:server` reuses the client's stack — kotlin-inject + anvil DI
(`ServerScope`/`ServerComponent`), the `domain/` interface + `data/` impl split,
conventional commits, the version catalog. It's a plain JVM `application` module
(no convention plugin; those are KMP-only).

**Why:** one mental model across client and server. An agent (or human) moving
between them doesn't re-learn DI, error handling, or module layout. The cost —
the server can't use the KMP `:libraries:core` (`Catching`, logging) because that
module has no JVM target — was accepted; the server keeps a couple of small local
equivalents rather than forcing a `jvm()` target onto every client library.

## 2026-06-21 — Graceful degradation over required config

**Decision:** `DATABASE_URL`, `SUPABASE_URL`, `SENTRY_DSN`, and the OTLP endpoint
are all optional. With none set, the server boots and serves `/_health` +
`/v1/example`; DB-backed and authenticated routes simply aren't mounted, Sentry
no-ops, and OpenTelemetry exports to stdout.

**Alternatives:** require `DATABASE_URL` + `SUPABASE_URL` like the Cards origin
(fail-fast). **Why optional:** this is a template — "clone and run, see it boot"
beats a fail-fast error on first run. The fail-fast discipline still applies per
field via `Env.require` when a future field genuinely can't be defaulted.

## 2026-06-21 — Auth is JWKS verification, never a shared secret

**Decision:** the server verifies Supabase JWTs against the project's public keys
(JWKS / ES256). The `JwtVerification` sealed seam has `Jwks` (prod) and `Static`
(tests mint HS256 tokens against a known verifier).

**Why:** no Supabase secret ever lives on the server, and auth — the highest-risk
surface — is fully testable offline (route tests + `FullStackMeTest` run the real
validate/challenge path with no network).

## 2026-06-21 — `NoOpAuthTokenProvider` lives in the `:networking` api module

**Decision:** the default no-op `AuthTokenProvider` binding sits in
`:libraries:networking` (api), not `:impl`.

**Why:** the module-boundary rule forbids one `:impl` depending on another, but
`:libraries:identity:impl` must reference `NoOpAuthTokenProvider` to override it
with `@ContributesBinding(replaces = [NoOpAuthTokenProvider::class])`. Putting the
default binding next to the interface it defaults keeps the replacement
boundary-clean. (See also the `enforceModuleBoundaries` self-edge fix in
`build-logic`.)

## 2026-06-21 — `serverOnly` build slimming

**Decision:** `-Dmovingeyes.serverOnly=true` makes `settings.gradle.kts` include
only `:apps:server`, so a Docker image build needs no Android/iOS toolchain.

**Why:** this is a KMP monorepo; without slimming, a server image build would
configure every client module and need the Android SDK + Kotlin/Native. The
server has no client-library deps today, so the gate is a pure settings change;
if it gains one, add an always-included `include(...)` + a Dockerfile `COPY`.

## 2026-06-21 — Flyway SQL is the schema source of truth

**Decision:** migrations under `resources/db/migration` define the schema; the
Exposed `Tables.kt` objects are read-side projections kept honest by
`DatabaseSchemaTest`. Repositories treat a unique-violation (SQLSTATE `23505`) as
the arbiter rather than pre-checking for races.

**Why:** one procedure for schema change (add the next `V##__name.sql`, never edit
an applied one), and idempotency that's correct under concurrency.
