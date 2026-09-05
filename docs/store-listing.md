# Store listings

Copy for both stores, plus the answers to the forms beside them. Kept here so it
is versioned and recoverable when a listing gets edited by hand and nobody
remembers why.

## The one thing this has to say

You cut the eyes out of a painting and put a screen behind it.

That is the product. Everything else is detail, and the first draft buried it
under nine hundred words about snapping and millimetres. Somebody scrolling a
store gives you one sentence.

## Watch for the rule of three

The second draft was short but still read as machine-written, and the reason was
structural rather than lexical. It kept reaching for triplets: "put a tablet
behind a painting, cut out the eyes, and watch people notice", "a painting, a
poster, or a sheet of cardboard", "blink, look around the room, and follow".

Two things give that away. The third item is usually a payoff rather than a
peer, so it is doing rhythm instead of work. And the rhythm arrives before the
meaning does, which is how the short description ended up telling people to put
the tablet in before cutting the holes. The cadence was right and the
instruction was backwards.

If a sentence has three of anything, check whether the third earns its place.

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
Bring your Halloween decorations to life with moving eyes!
```

58 characters. Elijah's, and it does something mine did not: it names the
category people are actually searching in. "Halloween decorations" is the phrase
somebody types; "a tablet behind a painting" is the phrase somebody uses after
they already own the app.

Capitalised Halloween, which was the only edit.

### Full description, filled

```
Moving Eyes brings your Halloween decorations to life. All you have to do is cut
two holes in a painting, line the eyes up with the holes, and then put this app
behind it. It's the final touch you need to make your Halloween decorations POP.

Paid users get access to more eye styles & behaviors, including eyes that react
to sound!
```

Elijah's, written and chosen by him. Two short paragraphs, 57 words. Everything I
drafted before this was longer and more explanatory, and none of it was better:
the product is one physical trick and it takes one sentence to describe.

Note the opening names Moving Eyes while the store name is Halloween Eyes
Decoration. The listing gets found by the store name and the app gets remembered
by the brand.

## App Store

### Name (30)

```
Halloween Eyes Decoration
```

### Subtitle (30), filled

```
Bring your decorations to life
```

Exactly 30, which is Elijah's own short description with "Halloween" dropped;
the store name already carries that word twice over.

### Promotional text (170, editable without review), filled

```
The final touch for your Halloween decorations. Cut two holes in a painting,
line the eyes up with them, and let it run all night.
```

128 characters, and mine rather than his, so worth a look. Worth swapping for
something seasonal on 30 Oct, since this field does not need a review.

### Keywords (100, comma separated, no spaces)

```
halloween,decoration,spooky,eyes,haunted,portrait,prop,creepy,party,scare,animatronic,jackolantern
```

97 characters. No need to repeat words already in the name or subtitle; Apple
indexes those.

### Description, filled

The Play description above, unchanged. It names no platform, so it needed no
Apple-specific edit.

## Review notes (App Store)

This app is exposed to a **4.3 minimum functionality** rejection. A reviewer
opens it, sees a black screen with two eyes, and has no way to know what it is
for. The fix is to show them.

Already in the App Review Notes field:

```
This app is a physical Halloween decoration rather than a screensaver. You cut
two eye holes in a painting or a sheet of cardboard and tape the device behind
it, so the eyes appear to belong to a face watching the room.

That is why most of the app is an alignment tool. The canvas renders at the
device's true physical size, distances are shown in millimetres, and the
snapping and measuring exist so the eyes line up with holes cut by hand.

The paid unlock is a single non-consumable that adds extra eye styles, the
motion controls, and microphone reactivity. Microphone audio is analysed on
device for loudness and direction only. It is never recorded, stored, or
transmitted, and everything else in the app works if the permission is denied.
```

The video link is deliberately not in there yet, because a review-notes field
with a dead placeholder in it is worse than one without the sentence. Add this
line before submitting:

```
A short video of the finished effect is here: <url>
```

**Record that video.** It is the highest-value item in this document. A reviewer
looking at a tablet on a desk cannot see what this is.

Sign-in required is unchecked, which is true and saves a round trip: there is no
account in this app.

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

Play, saved as draft on 2026-09-05: name, both descriptions exactly as they
appear above, category (Entertainment, App not Game), contact email, website.
The listing page still reports errors because the graphics are missing, but
"Save as draft" goes through regardless, so the copy is safe.

Still to do, all of them developer declarations rather than copy:

- [ ] Content rating questionnaire
- [ ] Data safety form, answers above
- [ ] Target audience and content
- [ ] Privacy policy URL under App content
- [ ] Icon, feature graphic, screenshots

App Store Connect, also 2026-09-05. The blank New App page was exactly what it
looked like: no registered App ID, so the bundle-ID dropdown had nothing to
offer and the form never rendered. Registering the identifier fixed it in one
step.

- App ID `com.dangerfield.movingeyes.MovingEyes`, description "Moving Eyes", no
  extra capabilities. Note this is the *iOS* bundle ID, from
  `apps/ios/Configuration/Config.xcconfig`; Android's applicationId is
  `com.dangerfield.movingeyes`, one component shorter. Inherited from the
  template and harmless, since the two stores never compare them.
- App record: Apple ID `6809042247`, SKU `movingeyes-ios-001`, iOS only,
  English (U.S.).
- Saved: name, subtitle, promotional text, description, keywords, support and
  marketing URLs (both the Pages site), version 1.0, copyright
  "2026 Nightjar Labs LLC", category Entertainment, content rights declared as
  no third-party content, and the review notes above.

Still to do on the Apple side:

- [ ] Age rating questionnaire. The one real judgment call is horror and fear
      themes: the app is animated eyes on black with no gore and no jump scares,
      so "None" is defensible and "Infrequent/Mild" is the cautious read. The
      cautious read costs a 9+ rating.
- [ ] App Privacy: Diagnostics / Crash Data, not linked, not tracking.
      Everything else Data Not Collected.
- [ ] Pricing and Availability: free, all territories.
- [ ] App Review contact name, phone and email.
- [ ] Screenshots, 6.5-inch iPhone at minimum.
- [ ] A build. Nothing here can be submitted until iOS has been run once.

## Assets still needed

- [ ] The demo video, for review notes and probably the listing too
- [ ] Screenshots. The first should be a photo of the tablet behind a painting,
      not a screenshot of the editor. Both stores allow a framed marketing image
      in the first slot, and the trick is the thing worth showing.
- [ ] Feature graphic for Play, 1024x500
- [ ] App icon, confirmed as yours rather than the template's
- [ ] IAP in both consoles: `moving_eyes_unlock_everything`, non-consumable,
      $4.99
