# Observability

Moving Eyes ships no server, and the only things that leave the device are crash reports and
anonymous product events. The store privacy labels are close to empty and that's a marketing
asset worth protecting, so before adding anything that phones home, check it against the promise
in the listing.

Two sinks, answering two different questions.

## Sentry — what broke

Configured in `AppTelemetry` (`:libraries:movingeyes:impl`), gated on a `SENTRY_DSN` injected at
build time; blank leaves crash reporting off, which is what a local build gets by default.

Every event carries:

- `session_id` — a fresh UUID per app session, where a session is a cold boot or a return after
  15 minutes in the background (`SessionTracker`). Set on the Sentry scope by
  `SessionTelemetryBinder`, so a native crash surfaced on the next launch still carries the
  session it happened in.
- `install_id` — stable per install, dies on uninstall (`CachedInstallIdProvider`, backed by
  `AppData.installId`). Stable across sessions, so "every report from this tester" is answerable
  without knowing who the tester is.
- `route` — the current navigation destination, updated in `App.kt` as the back stack changes.
- `commit_sha` / `commit_branch` — build provenance, so triage can tell whether a report is
  already fixed on main.
- `environment` — `releaseChannel-platform-buildType`. All platforms and build types report into
  one project; this tag separates them.

## Grafana — what happened

`GrafanaLogTree` (`:libraries:telemetry:impl`) ships two things to Grafana Cloud over OTLP:
structured `logEvent` app events, and plain Warn+ log lines. Gated on the `GRAFANA_*` credentials
being present at build time; unconfigured builds never plant the tree.

This is the sink that answers "is the product working", as opposed to "did it crash" — most
importantly `display_session_ended`. Someone who enters display mode and leaves after thirty
seconds hasn't crashed, hasn't complained, and hasn't left a review. Without this they're
invisible. The event registry is [`app-events.md`](app-events.md).

Export is disk-buffered before it leaves, so an event emitted with no network survives process
death and ships on a later launch. Two kill switches — `telemetry.appEventsEnabled` and
`telemetry.appEventsSampleRate` — are flippable from `pages/app-config.json` with no store
review, which matters when the whole season is ten days long.

```
{service_name="movingeyes-client"} | event_name="display_session_ended"
```

```
{service_name="movingeyes-client"} | detected_level=~"warn|error"
```

## Log levels and what they do

`KLog` fans out to every planted tree. In practice:

- **Verbose / Debug** — logcat and os_log only; buffered in memory (debug builds buffer Verbose+,
  release buffers Debug+) and attached to a user feedback report if one is filed.
- **Info** — the same, plus a Sentry breadcrumb in release builds. `logEvent` lives here, and is
  the only level that reaches Loki as a structured event.
- **Warn** — breadcrumb, and forwarded to Loki when `telemetry.klogForwardingEnabled` is on.
- **Error** — a Sentry event, unless the throwable implements `ExpectedControlFlow`.

## The frame loop

The renderer advances state on a 30fps clock for hours at a stretch. **Nothing in the frame path
may log**, not even at Verbose: a Verbose line at 30fps is 108,000 entries an hour, which will
evict every useful line from the feedback ring buffer and burn measurable battery on string
formatting. Log the transitions around the loop (session started, mood changed, startle fired),
never the loop itself.

## Finding one session

1. In Sentry, filter issues by `session_id` or `install_id`.
2. In Loki, `{service_name="movingeyes-client"} | session_id="<uuid>"` gives that session's
   events and Warn+ lines — what the user was doing either side of the crash.
3. A user-feedback report carries a `session-log.txt` attachment — the in-memory ring buffer at
   the moment the report was filed, including the Debug/Verbose lines that never ship anywhere.
   That's usually the fastest route to what actually happened.

`install_id` is the wider net: same device across sessions, so "every report from this tester"
is answerable without knowing who the tester is.
