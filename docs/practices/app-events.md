# Client app events

The registry of structured events the client emits for product analytics. One event = one
`logEvent(name, attrs)` call (the extension in `:libraries:core` `logging/AppEvents.kt`) riding
the normal KLog tree system: it lands in logcat/os_log, as a Sentry breadcrumb, and — via
`GrafanaLogTree` in `:libraries:telemetry:impl` — as an OTLP log record in Grafana Cloud Loki.
Query conventions are in [`observability.md`](observability.md).

The pipe goes straight to Grafana. There is no backend to route it through, and there doesn't
need to be: what this answers is "what did people actually do", which for a decoration used one
night a year is most of what there is to learn.

It honours the privacy promise in the store listing — an install-scoped random id and a
per-session id, nothing else. No accounts, no personal data, no device fingerprinting. Don't add
an attribute that would change that answer.

Names are dot-namespaced snake_case. Every record carries `session_id` and `install_id`
(see `SessionTelemetryBinder`) plus resource attributes for version, platform, and commit.

**Delivery is durable, effectively at-least-once.** Batches are written to a file-backed buffer
before export and deleted only once the gateway acknowledges them, so events emitted with no
network survive process death and ship on a later launch. `TelemetryBackgroundFlusher` force-
flushes on every app background, which is the last reliable moment before the OS suspends the
process. A record can rarely ship twice, so dashboards should tolerate the odd duplicate rather
than assume exactly-once.

There is deliberately **no `is_offline` attribute**. The app makes no requests of its own, so it
has no connectivity observer to read honestly, and the durable buffer already covers what that
attribute was for.

**Rules for adding events:** emit through the `logEvent` extension only (never a raw
`EXTRA_APP_EVENT` extra), fire on user actions and state transitions — never per-frame,
per-poll, or per-flow-emission — and add the event to this file in the same change. The
per-frame rule matters more here than in a normal app: the renderer runs a 30fps loop for five
hours, and one stray event in the frame path is 540,000 records an evening — a genuine ingest
bill and a flat battery.

Two kill switches exist for when that goes wrong anyway: `telemetry.appEventsEnabled` is an
instant off, and `telemetry.appEventsSampleRate` is a volume dial. They stay separate on
purpose — collapsing them makes the emergency lever a magic number. Both are flippable from
`pages/app-config.json` without a store review, and from the QA menu on a device.

## Engagement & session shape

| Event | Attributes | Fires |
|---|---|---|
| `app.launched` | `cold_start` (always true), `previous_exit` (clean/crash/anr/oom/unknown) | Once per cold start, on the boot foreground — after the session tracker rolls session #1, so it shares the boot's `session_id`. `previous_exit` comes from Android's historical exit reasons (API 30+; older devices report `unknown`) |
| `app.foregrounded` | `cold_start` | Every foreground (`LifecycleAppEventLogger`); `cold_start=true` on the boot foreground. Count users/sessions from this, not `app.launched` |
| `app.backgrounded` | `session_duration_sec` | Every background; whole seconds since the matching foreground (monotonic clock). Omitted in the shouldn't-happen case of a background with no prior foreground |

## Warn+ log forwarding (not events)

Besides events, `GrafanaLogTree` forwards plain KLog lines at Warn and above to Loki as ordinary
OTLP logs, so client errors are visible without waiting for a Sentry crash. These records have
**no `event_name`** — that's how you tell them apart — and carry `session_id`/`install_id`, the
logger `tag`, and `exception_type`/`exception_message` when a throwable was attached. Gated by
`telemetry.klogForwardingEnabled`, and still behind the `appEventsEnabled` kill switch.

## Product events

Seed these as the features land. The planned set, from the spec:

`scene_preset_selected {presetId}` · `scene_created` · `eye_added {styleId}` ·
`style_locked_tapped {styleId}` · `display_mode_entered {eyeCount, hasSound, hasReactivity}` ·
`display_session_ended {durationSeconds}` · `paywall_shown {trigger}` · `purchase_completed` ·
`purchase_restored` · `mic_permission {granted}` · `scene_code_imported` · `scene_code_exported`

**`display_session_ended` is the metric that matters most.** If people enter display mode and
leave after thirty seconds, the illusion isn't working, and no amount of new eye styles fixes
that. Everything else on this list is secondary to it.

## Feedback

In-app feedback goes straight to Sentry via `Telemetry.captureUserFeedback` (verbatim message,
screenshots, session-log attachment) — it is not an analytics event.
