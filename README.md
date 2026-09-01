# Moving Eyes

Halloween decoration for tablets. Fill the screen with animated eyes, tape the device behind a painting with the eyes cut out, and walk away for five hours.

Android and iOS from one Kotlin Multiplatform codebase.

## The reframe

This is a **display appliance**, not a drawing app. The user configures it once, mounts the device, and leaves. Everything follows from that:

- It runs unattended, so it needs keep-awake, immersive fullscreen, and a brightness override that can go below the OS minimum.
- A kid will poke the screen, so the canvas is lockable.
- A tablet at full brightness animating for five hours dies by 9pm, so battery is a first-class concern.
- The screen sits behind cardboard, so aligning eyes to physical holes is the single most important interaction. The canvas is the screen at 1:1 — no zoom, no pan — because a pixel here is a millimetre of cardboard.
- Pure black is the default canvas. On OLED those pixels are off, and the illusion becomes eyes floating in a void rather than a glowing rectangle behind a painting.

There are **no accounts and no backend**. The only network traffic is crash reporting and one in-app purchase.

## Build & run

```shell
./gradlew :apps:compose:assembleDebug
```

```shell
./gradlew :apps:compose:compileKotlinIosSimulatorArm64
```

```shell
open apps/ios/iosApp.xcodeproj
```

```shell
./gradlew testDebugUnitTest
```

Before your first commit:

```shell
./scripts/install_hooks.sh
```

## Project structure

```
apps/compose/          # KMP entry point (Android + iOS)
apps/ios/              # Swift/Xcode wrapper
features/<name>/       # Routes and public API
features/<name>/impl/  # Screens and ViewModels
libraries/<name>/      # Interfaces
libraries/<name>/impl/ # Implementations
```

Architecture rules (enforced at Gradle configuration time), the ViewModel/DI/navigation patterns, and every convention live in **[AGENTS.md](AGENTS.md)** — written for AI agents and humans alike, and the single source of truth for how code here is shaped.

## Doc map

| Doc | What it covers |
|---|---|
| [SETUP.md](SETUP.md) | Init → running app → first release, step by step |
| [AGENTS.md](AGENTS.md) | Architecture, conventions, testing rules |
| [docs/practices/testing.md](docs/practices/testing.md) | Which layer catches which bug; fakes |
| [docs/practices/observability.md](docs/practices/observability.md) | Crash reporting and the session_id pivot |
| [docs/practices/app-events.md](docs/practices/app-events.md) | The structured-event registry + `logEvent` discipline |
| [docs/swift-kotlin-communication-patterns.md](docs/swift-kotlin-communication-patterns.md) | Exposing Kotlin to Swift and vice versa |

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
