# Architecture decisions

Append-only log. Add an entry whenever you make a non-trivial architectural call
(new module boundary, library choice, scope cut, schema shape). Each entry: date,
the decision, alternatives considered, and *why*. Newest first.

---

## 2026-09-01 — No accounts, no backend, no sync

**Decision:** deleted `:apps:server`, `:apps:admin`, `:apps:integration`,
`:libraries:identity`, `:libraries:networking`, `:libraries:config`,
`:libraries:telemetry:impl`, `:features:onboarding` and `:features:home`, plus the
triggered-sync engine, the user-scoped data-reset dump, and the Supabase build
plumbing. This supersedes every server-side decision below, which is kept only as
history.

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

**Cost accepted:** `logEvent` calls are emitted with nowhere to go until an
analytics sink lands; `docs/practices/app-events.md` says so explicitly rather than
implying a pipeline exists.

## 2026-09-01 — Grafana/OTel client pipeline cut with the server

**Decision:** deleted `:libraries:telemetry:impl` (the OTLP exporter, disk-backed
durable log buffer, and Grafana log tree) rather than untangling it.

**Alternatives:** keep it and keep `:libraries:networking` + `:libraries:config`
alive purely to feed it, or rewrite its `InstallIdProvider` / `SessionIdProvider` /
HTTP-engine dependencies against new seams.

**Why cut:** the module existed to correlate client logs with *server* traces, and
there is no server. What it uniquely provided — off-device visibility into
non-crashing errors — is worth real money in a networked product and worth very
little in a decoration that makes no requests. Sentry already carries errors, and
the in-memory ring buffer attached to user feedback covers the "what led up to
this" question. Untangling would have kept two modules alive to serve one consumer.

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
