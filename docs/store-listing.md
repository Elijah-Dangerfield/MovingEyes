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
Moving Eyes Painting
```

Elijah renamed it from my "Halloween Eyes Decoration" on 2026-09-07. Fewer
keywords, but it survives November, and the App Store name he picked separately
is "Moving Eyes for Paintings". Two names for one app is fine; the stores don't
compare them.

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

## App Store

### Name (30), filled

```
Moving Eyes for Paintings
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

The live version also names the attached video and the IAP by id, and closes
with "There is no account and no sign-in anywhere in the app." Sign-in required
is unchecked, which is true and saves a round trip.

`IMG_0050.mov` is attached to the review, which is better than a link: it can't
rot, and a reviewer who never leaves App Store Connect still sees it. That video
was the highest-risk gap in this document and it is now closed.

## Data safety and privacy labels

Both must match `pages/privacy.html`, published at
https://elijah-dangerfield.github.io/MovingEyes/privacy.html

The first draft of this section was wrong, and so was the privacy policy it was
written against. Both said crash logs and nothing else. The app also ships app
events and Warn+ logs to Grafana Cloud, with an `install_id` and a `session_id`
on every record (`GrafanaLogTree.kt`), and the policy went as far as claiming no
analytics SDKs at all. A Data safety form that contradicts the linked policy is
its own policy violation, so the policy was rewritten first and both labels
follow it.

### Google Play Data Safety, filed

| Question | Answer |
|---|---|
| Collects or shares user data? | Yes |
| Data types | App interactions; Crash logs; Diagnostics; Device or other IDs |
| Purpose, all four | Analytics |
| Shared with third parties? | No |
| Optional? | No, required. There is no analytics opt-out in Settings |
| Processed ephemerally? | No. It is stored by Sentry and Grafana |
| Encrypted in transit? | Yes |
| Deletion requests? | Yes, https://elijah-dangerfield.github.io/MovingEyes/privacy.html#delete-data |
| Microphone / audio | **Not collected.** Processed on device and discarded. Play's guidance is that on-device-only processing is not collection. |

`install_id` is a random per-install UUID, not the advertising ID, but Play's
"Device or other IDs" covers app-generated identifiers that persist across
sessions, so it is declared. Over-declaring costs nothing; under-declaring is
the violation.

### Apple privacy labels, filed

Four types, each **Used for Analytics**, **not linked to the user's identity**,
and **not used for tracking**: Device ID, Product Interaction, Crash Data,
Performance Data. Everything else is Data Not Collected, audio included. The
product page preview reads "Data Not Linked to You".

**Not published yet.** The labels are complete and the Publish button is live,
but publishing them is Elijah's click, not mine.

## Console state

**Play: App content is fully declared.** As of 2026-09-07 the dashboard shows no
outstanding setup tasks. Privacy policy URL, Sign in details, Ads, Government
apps, Financial features, Health apps, Advertising ID and Data safety were filed
in this pass; Elijah did Content ratings and Target audience alongside, and the
store listing with its graphics is saved.

One answer worth remembering, because it is a judgment call rather than a fact:
**Sign in details is "No"**. Google's Yes branch lists "payments... or access
tiers", which the Pro unlock is, but choosing Yes forces a checkbox attesting
that the details you supply "provide full access to all the features and
content, including premium or paid content". With no credentials and no bypass
build, that attestation would be false. The declaration is fundamentally about
login credentials, there are none, and Play's reviewers test in-app products
through their own test-purchase flow. If you would rather take the strict
reading, add the review account as a licence tester first and then switch it.

**App Store Connect.** The blank New App page was exactly what it looked like:
no registered App ID, so the bundle-ID dropdown had nothing to offer and the
form never rendered. Registering the identifier fixed it in one step.

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
  no third-party content, review notes, review contact, and 7 screenshots.
- **Age rating 4+**, in 172 countries. Every question answered None or No,
  including horror and fear themes: the app renders animated eyes on black with
  no gore and no jump scares. "Infrequent/Mild" would have been the cautious
  read and would have cost a 9+ rating for nothing.
- **Pricing: free, all 175 territories.** The app is free; the Pro unlock is the
  in-app product.

Still to do on the Apple side:

- [ ] Publish the App Privacy labels. One button, and it's yours.
- [ ] The IAP needs a review screenshot before it can be submitted.
- [ ] A build. Nothing here can be submitted until iOS has been run once.

## The in-app purchase

One non-consumable, in both stores, and the id has to match the code exactly or
the store returns no product and the paywall has nothing to sell.

```
Product ID    movingeyespro
Display name  Moving Eyes Pro
Description   Unlock every feature including sound reactivity!
Price         $0.99
Type          Non-consumable, all countries
```

`MovingEyesProduct.UnlockEverything` in
[BillingModels.kt](../libraries/billing/src/commonMain/kotlin/com/dangerfield/movingeyes/libraries/billing/BillingModels.kt)
was `moving_eyes_unlock_everything` and is now `movingeyespro`. That mismatch
would have shipped as a paywall with no price and a dead buy button, and it
would not have shown up until a real device hit a real store.

Apple's copy exists; Play's still needs creating with the same id and price.
Apple also wants a review screenshot on the IAP itself before it will submit.
