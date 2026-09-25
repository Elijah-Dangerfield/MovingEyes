# Agent TODO

The queue an agent works from: things found outside the work that found them.

Not a changelog. **Delete an entry when it lands** rather than ticking it off,
so this stays a queue. A stale queue is worse than no queue, because the next
reader redoes finished work.

Created 2026-09-21.

---

## Answer export compliance in the plist rather than by hand

**What to do.** Add to `apps/ios/iosApp/Info.plist`:

```xml
<key>ITSAppUsesNonExemptEncryption</key>
<false/>
```

**Why.** Every build of this app carries `usesNonExemptEncryption: false` with
no such key in the plist, which means somebody answered the question in App
Store Connect by hand each time. Miss it once and the build finishes
processing, reads VALID, and is installable by nobody, with nothing anywhere
saying so: App Store Connect calls it "Missing Compliance" and the testers just
never get a build. Sodogku lost time to exactly that on 2026-09-25, and
Doublestack has two builds sitting in that state now.

**The answer is not a formality.** False is right because the only encryption
here is HTTPS through the system's own TLS, which Apple exempts under Category
5 Part 2. An app that implements or bundles a cipher has to say otherwise.

**Provenance.** Fixed in Sodogku and in the template on 2026-09-25, after
checking every app on the account through the App Store Connect API. This one
is prevention, not a live failure: its builds are in beta testing.
