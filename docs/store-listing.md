# Store listings

Copy for both stores, plus the answers to the forms that sit alongside them.
Kept here rather than only in the consoles so it is reviewable, versioned, and
recoverable when a listing gets edited by hand and nobody remembers why.

Character limits are enforced by the stores and noted per field. Counts below
are current as of writing; re-check before pasting.

---

## The pitch, in one paragraph

Everything here is trying to say one thing: this is not a screensaver of eyes.
It is a physical trick. You tape a tablet behind a painting or a sheet of
cardboard, cut two holes, and the eyes look at people who walk past. The app's
job is to make the alignment easy and then get out of the way for five hours.

Lead with the trick, not the feature list. Somebody scrolling a store does not
want "12 animated eye styles"; they want to know what happens when their
neighbour walks up the path.

---

## Google Play

### App name (30) — FILLED

```
Halloween Eyes Decoration
```

*25 characters. The store name is deliberately keyword-rich and differs from the
in-app name, which stays "Moving Eyes". This is a normal split: the listing is
found by search, the app is remembered by brand.*

### Short description (80) — FILLED

```
Hide a tablet behind a painting. The eyes follow whoever walks past.
```

*68 characters.*

### Full description (4000) — FILLED

```
Tape a tablet behind a painting, cut two eye holes, and let it watch the room.

Halloween Eyes turns any phone or tablet into a pair of eyes that blink, glance
around, and react to what is happening in front of them. Put it behind a
portrait in the hallway, a sheet of cardboard in a window, or a jack-o'-lantern
on the porch. People notice the eyes before they notice the screen, which is the
entire point.


BUILT FOR THE CUT, NOT FOR THE SCREEN

Lining eyes up with holes you cut by hand is the hard part, so the app measures
in millimetres, not pixels. Drag an eye and it snaps to the middle of the canvas
or to another eye's edges. Turn on measurements and it draws the gap between
each pair and the width of each eye, so you can mark cardboard before you cut
it. Resize one eye near another and it locks to the same size.

The canvas is the screen at actual size. A millimetre in the app is a millimetre
on the cardboard.


THEN IT GETS OUT OF THE WAY

Hit play and every control disappears. The screen stays awake all night, the
brightness goes lower than Android normally allows so it does not glow through
thin paper, and taps do nothing so a curious guest cannot drag an eye out of its
hole. Set a sleep timer if you want it to fade out after the trick-or-treaters
stop coming.

The canvas is true black, so on an OLED screen the area around the eyes is
genuinely switched off. That is what makes the illusion work in a dark hallway.


WHAT YOU GET FOR FREE

Unlimited eyes, three eye styles, and every placement tool: drag, pinch,
rotate, snap, align, measure, undo. Save as many scenes as you like. Display
mode, the sleep timer and the brightness control are all included. The eyes
blink and look around on their own.

You can set up the whole trick and run it all night without paying anything.


WHAT THE FULL VERSION ADDS

One payment, no subscription:

- Ten more eye styles: bloodshot, feline, reptile, demon, spider, doll, bat,
  ghoul and more
- Six moods, from a slow idle scan to Suspicious, Sleepy, Frantic and Possessed,
  with blink rate, wander and restlessness under each one
- Sound reactivity: the eyes turn toward a door opening and look at whoever
  came in


NO ACCOUNT, NO INTERNET, NO ADS

There is no sign-up and no server. Your scenes live on your device. The app
works with the wifi off, which matters on a porch.

If you turn on sound reactivity, audio is analysed on your device moment to
moment and immediately discarded. Nothing is recorded, nothing is stored,
nothing is uploaded. The app reads how loud the room is and which microphone
heard a sound first. It cannot recognise speech.


A NOTE ON FLASHING

Some moods blink quickly by design. There is a Reduce flashing setting that caps
rapid movement in every mood, and the app respects your system's reduce-motion
setting automatically.
```

*Roughly 2,300 characters.*

---

## App Store

### Name (30)

```
Moving Eyes
```

### Subtitle (30)

```
Eyes behind your decorations
```

*28 characters.*

### Promotional text (170, editable without review)

```
Tape a tablet behind a painting, cut two eye holes, and let it watch the
hallway. Measures in millimetres so the eyes line up with the holes you cut.
```

*146 characters. Worth changing on 30 Oct to something seasonal, since this
field does not need a review.*

### Keywords (100, comma separated, no spaces)

```
halloween,decoration,spooky,eyes,haunted,portrait,prop,creepy,party,scare,animatronic,display,jackolantern
```

*104 characters, trim one before pasting.* No need to repeat the app name or
subtitle words; Apple already indexes those.

### Description (4000)

Use the Play full description above. It needs one change: Apple does not allow
"Android" in a description, so replace the brightness paragraph's "lower than
Android normally allows" with "lower than the system slider normally allows".

---

## Review notes (App Store) — do not skip this

Moving Eyes is at real risk of a **4.3 minimum functionality** rejection,
because a reviewer opening it sees a black screen with two eyes and no obvious
purpose. The mitigation is to show them the physical trick.

Paste into App Review Notes:

```
Moving Eyes is a physical decoration prop rather than a screensaver. The device
is taped behind a painting or a sheet of cardboard with two eye holes cut in it,
so the eyes appear to be a face watching the room.

Because of that, most of the app is an alignment tool: the canvas is rendered at
the device's true physical size, distances are shown in millimetres, and the
snapping and measurement tools exist so the eyes line up with holes the user
cuts by hand.

A 40 second video showing the finished effect is here: [LINK]

To review the paid features without purchasing, the unlock can be granted with
the sandbox account provided, or we can supply a build with the entitlement
forced on if that is easier.
```

**Record that video before submitting.** It is the single highest-leverage
thing in this document. A reviewer looking at a tablet on a desk cannot see
what this app is for.

---

## Data safety and privacy labels

Both must match `pages/privacy.html`, which is published at
https://elijah-dangerfield.github.io/MovingEyes/privacy.html

### Google Play Data Safety

| Question | Answer |
|---|---|
| Does your app collect or share user data? | Yes (crash logs and diagnostics only) |
| Data types collected | App activity → Crash logs, Diagnostics |
| Is it shared with third parties? | No |
| Is collection optional? | No (diagnostics), but no personal data is included |
| Is data encrypted in transit? | Yes |
| Can users request deletion? | Yes, via the in-app Report a bug / Send feedback screens |
| Microphone / audio | **Not collected.** Audio is processed on-device and discarded. Play's own guidance is that on-device-only processing is not collection. |

### Apple privacy labels

- **Data Not Collected** for everything except:
  - **Diagnostics → Crash Data**, not linked to identity, not used for tracking
- **Audio Data: not collected.** Processed on device and never leaves it.
- Tracking: **No**. There is no advertising identifier and no third-party SDK
  that would use one.

---

## Play console state

Filled and saved as draft on 2026-09-05:

- App name, short description, full description
- App category: Entertainment. App, not Game.
- Contact email and website (the Pages site)

Still to do there, all of which are developer declarations rather than copy:

- [ ] Content rating questionnaire
- [ ] Data safety form — answers are in the section above
- [ ] Target audience and content
- [ ] Privacy policy URL under App content
- [ ] Graphics: icon, feature graphic, screenshots

## App Store Connect state

**Blocked, and not by anything on the listing.** The New App dialog leads to a
blank page. The likely cause is that the bundle identifier
`com.dangerfield.movingeyes` does not yet exist as an App ID — iOS has never
been built, so nothing has ever registered it.

Create it first at
developer.apple.com → Certificates, Identifiers & Profiles → Identifiers, then
the ASC New App dialog will offer it in the Bundle ID dropdown. Everything else
on that form is in this document.

## Assets still needed

- [ ] The 40 second demo video for review notes (highest priority)
- [ ] Screenshots. The first one should show the trick, not the UI: a photo of
      the tablet behind a painting beats a screenshot of the editor. Both stores
      allow a framed marketing image as the first slot.
- [ ] Feature graphic for Play, 1024x500
- [ ] App icon, confirmed as final rather than the template's
- [ ] IAP configured in both consoles as `moving_eyes_unlock_everything`,
      non-consumable, $4.99
