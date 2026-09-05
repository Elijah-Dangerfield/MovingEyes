# Store listings

Copy for both stores, plus the answers to the forms beside them. Kept here so it
is versioned and recoverable when a listing gets edited by hand and nobody
remembers why.

## The one thing this has to say

You cut the eyes out of a painting and put a screen behind it.

That is the product. Everything else is detail, and the first draft of this
listing buried it under nine hundred words about snapping and millimetres.
Somebody scrolling a store gives you one sentence. Spend it on the trick.

## Google Play

### App name (30), filled

```
Halloween Eyes Decoration
```

25 characters. Keyword-rich on purpose, and different from the in-app name,
which stays "Moving Eyes". The listing gets found by search; the app gets
remembered by brand.

### Short description (80), filled

```
Put a tablet behind a painting, cut out the eyes, and watch people notice.
```

74 characters.

### Full description, filled

```
Cut two eye holes in a painting, a poster, or a sheet of cardboard. Tape a phone
or tablet behind it. The eyes blink, look around the room, and follow whoever
walks past.

That is the whole app.

Lining the eyes up with the holes is the fiddly part, so the app measures in real
millimetres and snaps the eyes into line with each other. Mark the cardboard,
then cut it.

When it's set up, hit play. The controls disappear, the screen stays awake all
night, and taps do nothing, so nobody can drag an eye out of its hole by
touching the painting.

FREE

Three eye styles, as many eyes as you want, and everything above.

FULL VERSION, ONE PAYMENT

Ten more eyes: bloodshot, cat, reptile, demon, spider, doll and more. Six moods,
from a slow scan to Possessed. And eyes that turn toward a sound and look at
whoever just came in.

No subscription, no ads, no account, no internet needed.

If you switch on sound reactivity, audio is read on your device and thrown away
immediately. Nothing is recorded or uploaded, and it cannot understand speech.
```

About 200 words, down from 2,300. The cut list was: the OLED paragraph, the
"built for the cut" heading, the free-tier feature inventory, and the flashing
note. None of them survive the test of "would somebody read this before
deciding?" The flashing setting still exists and is still disclosed in the app.

## App Store

### Name (30)

```
Halloween Eyes Decoration
```

### Subtitle (30)

```
Cut out the eyes. Add these.
```

28 characters.

### Promotional text (170, editable without review)

```
Cut two eye holes in a painting and tape a tablet behind it. The eyes blink,
look around, and follow whoever walks past.
```

120 characters. Worth swapping for something seasonal on 30 Oct, since this
field does not need a review.

### Keywords (100, comma separated, no spaces)

```
halloween,decoration,spooky,eyes,haunted,portrait,prop,creepy,party,scare,animatronic,jackolantern
```

97 characters. No need to repeat words already in the name or subtitle; Apple
indexes those.

### Description

Use the Play description above, unchanged. It mentions no platform by name, so
it needs no Apple-specific edit.

## Review notes (App Store)

This app is exposed to a **4.3 minimum functionality** rejection. A reviewer
opens it, sees a black screen with two eyes, and has no way to know what it is
for. The fix is to show them.

Paste into App Review Notes:

```
This app is a physical Halloween decoration rather than a screensaver. You cut
two eye holes in a painting or a sheet of cardboard and tape the device behind
it, so the eyes appear to belong to a face watching the room.

That is why most of the app is an alignment tool: the canvas renders at the
device's true physical size, distances are shown in millimetres, and the
snapping and measuring exist so the eyes line up with holes cut by hand.

A short video of the finished effect is here: [LINK]

To review the paid features without buying them, use the sandbox account
provided, or ask us for a build with the entitlement forced on.
```

**Record that video before submitting.** It is the highest-value item in this
document. A reviewer looking at a tablet on a desk cannot see what this is.

## Data safety and privacy labels

Both must match `pages/privacy.html`, published at
https://elijah-dangerfield.github.io/MovingEyes/privacy.html

### Google Play Data Safety

| Question | Answer |
|---|---|
| Collects or shares user data? | Yes, crash logs and diagnostics only |
| Data types | App activity: crash logs, diagnostics |
| Shared with third parties? | No |
| Optional? | No, but no personal data is included |
| Encrypted in transit? | Yes |
| Deletion requests? | Yes, via Report a bug and Send feedback in the app |
| Microphone / audio | **Not collected.** Processed on device and discarded. Play's guidance is that on-device-only processing is not collection. |

### Apple privacy labels

Everything is **Data Not Collected** except **Diagnostics: Crash Data**, not
linked to identity, not used for tracking. Audio is not collected. Tracking: no,
there is no advertising identifier and no third-party SDK that would use one.

## Console state

Play, saved as draft on 2026-09-05: name, both descriptions, category
(Entertainment, App not Game), contact email, website.

Still to do, all of them developer declarations rather than copy:

- [ ] Content rating questionnaire
- [ ] Data safety form, answers above
- [ ] Target audience and content
- [ ] Privacy policy URL under App content
- [ ] Icon, feature graphic, screenshots

App Store Connect is blocked before the listing matters. New App leads to a
blank page, most likely because the bundle identifier
`com.dangerfield.movingeyes` was never registered as an App ID. iOS has never
been built, so nothing has created it. Register it at developer.apple.com under
Certificates, Identifiers and Profiles, and the dropdown will offer it.

## Assets still needed

- [ ] The demo video, for review notes and probably the listing too
- [ ] Screenshots. The first should be a photo of the tablet behind a painting,
      not a screenshot of the editor. Both stores allow a framed marketing image
      in the first slot, and the trick is the thing worth showing.
- [ ] Feature graphic for Play, 1024x500
- [ ] App icon, confirmed as yours rather than the template's
- [ ] IAP in both consoles: `moving_eyes_unlock_everything`, non-consumable,
      $4.99
