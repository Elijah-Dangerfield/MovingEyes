# Agent TODO

The queue an agent works from: things found outside the work that found them.

Not a changelog. **Delete an entry when it lands** rather than ticking it off,
so this stays a queue. A stale queue is worse than no queue, because the next
reader redoes finished work.

Created 2026-09-21 with one entry, which arrived from a sibling app rather than
from this one.

---

## Delete the camera half of `PermissionLauncher`, before the next iOS upload

**What to do.** `libraries/ui/src/iosMain/.../PermissionLauncher.ios.kt` imports
`AVCaptureDevice` and calls `requestAccessForMediaType(AVMediaTypeVideo)`.
Delete `rememberCameraPermissionLauncher` from the `expect` in `commonMain` and
from both actuals, along with the three `AVFoundation` imports. Nothing calls
either launcher: `grep` for `rememberCameraPermissionLauncher` and
`rememberMicrophonePermissionLauncher` across the repo returns only their own
declarations.

**Leave the microphone alone.** This app records audio for real through
`IosAudioCapture` and `IOSAudioCapture.swift`, and `Info.plist` already carries
`NSMicrophoneUsageDescription`. Only the camera is dead here.

**Why it is worth doing now.** The next upload to App Store Connect will be
rejected. Apple scans the binary for **API references**, not call sites, so
unreachable code still counts, and `Info.plist` has no
`NSCameraUsageDescription`. The rejection arrives by email twenty minutes after
a delivery that looked successful:

> ITMS-90683: Missing purpose string in Info.plist. The Info.plist file for the
> "MovingEyes.app" bundle should contain a NSCameraUsageDescription key with a
> user-facing purpose string.

This is not a prediction. Two sibling apps generated from the same template hit
it within an hour of each other on 2026-09-21, Sodogku and Doublestack, and both
fixed it by deleting the scaffolding. This app has the same inheritance and no
purpose string, and it is mid-review, which is the worst moment to spend a round
trip on it.

**Do not add the purpose string.** It works, which is the trap. It declares
access to a sensor the app never touches, contradicts the App Privacy answers,
and leaves a reviewer a question with no good answer.

**How to check you are done.** Build the app and look at the binary, not the
diff:

```bash
strings <path>/MovingEyes.app/MovingEyes | grep -cE "AVCaptureDevice|requestAccessForMediaType"
```

Zero is the answer. The microphone symbols will still be there and should be.

**Provenance.** Sodogku, 2026-09-21, reported here rather than verified by
building this app. The claims about this repo's files were checked by reading
them. Queued for the template too, as "Remove the camera entirely" in
`KMPTemplate/docs/PORT-CANDIDATES.md`, which is where the fix belongs
permanently.
