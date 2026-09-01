# Client app events

The registry of structured events the client emits for product analytics. One event = one
`logEvent(name, attrs)` call (the extension in `:libraries:core` `logging/AppEvents.kt`) riding
the normal KLog tree system: it lands in logcat/os_log and as a Sentry breadcrumb, so a crash
report carries the last few things the user did.

**There is no analytics backend yet.** Events are emitted and locally visible today; a sink is
scheduled for the paywall/settings phase. Emitting them now costs nothing and means the names
and attributes are already right when the sink lands. Whatever ships must honour the privacy
promise in the store listing: an install-scoped random id and nothing else, no accounts, no
personal data, no device fingerprinting.

Names are dot-namespaced snake_case. Every record carries `session_id` and `install_id`
automatically (see `SessionTelemetryBinder`).

**Rules for adding events:** emit through the `logEvent` extension only (never a raw
`EXTRA_APP_EVENT` extra), fire on user actions and state transitions — never per-frame,
per-poll, or per-flow-emission — and add the event to this file in the same change. The
per-frame rule matters more here than in a normal app: the renderer runs a 30fps loop for five
hours, and one stray event in the frame path is 540,000 records an evening.

## Engagement & session shape

| Event | Attributes | Fires |
|---|---|---|
| `app.launched` | `cold_start` (always true), `previous_exit` (clean/crash/anr/oom/unknown) | Once per cold start, on the boot foreground — after the session tracker rolls session #1, so it shares the boot's `session_id`. `previous_exit` comes from Android's historical exit reasons (API 30+; older devices report `unknown`) |
| `app.foregrounded` | `cold_start` | Every foreground (`LifecycleAppEventLogger`); `cold_start=true` on the boot foreground. Count users/sessions from this, not `app.launched` |
| `app.backgrounded` | `session_duration_sec` | Every background; whole seconds since the matching foreground (monotonic clock). Omitted in the shouldn't-happen case of a background with no prior foreground |

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
