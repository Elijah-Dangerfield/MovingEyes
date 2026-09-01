# Testing approach

How this codebase tests, layer by layer, and the conventions every new test
follows. The goal is a pyramid where each bug class is caught at exactly one
layer — the cheapest layer that can fail when it breaks.

Moving Eyes has no server and no network, so the pyramid is short. Most of the
risk lives in two places that unit tests reach easily (the behaviour engine and
the scene model) and two that they don't (rendering and gestures). Be honest
about which is which rather than writing tests that assert the compiler.

## The layers

- **Pure-logic unit tests** (`commonTest` in each module) — the behaviour
  engine, the scene codec, snapping maths. `:libraries:eyes` is deliberately
  Compose-free so its engine can be driven by a synthetic clock: assert that
  saccade intervals fall inside their configured range, that two eyes seeded
  with different phase offsets never blink in lockstep, and that a startle
  decays back to the configured mood over three seconds. These are the tests
  that catch "the eyes look like a screensaver."
- **ViewModel tests** (`commonTest` in each feature's `impl`) — extend
  `CoroutineTest` (from `:libraries:flowroutines:testing`), hand-roll a fake
  per repository dependency, drive actions, assert on `vm.state`.
- **Scenario harness** — when a feature's tests keep re-wiring the same fakes,
  grow a tiny builder + verbs + `assertState {}` DSL next to them so tests read
  as user scenarios. It's a pattern, not a framework: copy and adapt, don't
  generalize.
- **On-device verification** — gestures, frame timing, battery drain, and the
  five-hour unattended run. These cannot be unit tested and shouldn't be
  faked. The acceptance test for the whole product is a tablet behind cardboard
  for an evening.

## What catches what

| Bug class | Where it's caught | Not here |
|---|---|---|
| Saccade/blink timing, phase drift, startle decay | `:libraries:eyes` `commonTest` with a synthetic clock | on a device, by staring at it |
| Scene serialization round-trip, version compatibility | `:libraries:scene` `commonTest` | in the editor by hand |
| Action → state derivation in a VM | feature `commonTest` | anywhere else |
| Snap thresholds, mm conversion | pure-function unit tests | in the gesture handler |
| Gesture feel, frame time, thermals, battery | a real device, measured | unit tests |

## Conventions

- **Hand-rolled fakes only.** No mocking framework. A fake is a small class in
  the test source set that implements the interface and exposes whatever the
  test needs to assert. If a fake is getting complicated, the interface it's
  faking probably is too.
- **`CoroutineTest` for anything with a dispatcher.** It installs a test
  dispatcher and scope so tests are deterministic and fast.
- **Never sleep.** Await state (`awaitState` / `awaitUntil`) or advance a test
  clock. A fixed sleep is a flake with a delay fuse.
- **A test that only exercises the type system isn't worth its maintenance.**
  Prefer one test that would actually fail if the behaviour regressed.
