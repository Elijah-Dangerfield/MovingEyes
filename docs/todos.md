# Agent TODO

The queue an agent works from: things found outside the work that found them.

Not a changelog. **Delete an entry when it lands** rather than ticking it off,
so this stays a queue. A stale queue is worse than no queue, because the next
reader redoes finished work.

Created 2026-09-21.

---

## Show the install ID in Settings, so a deletion request can name something

**What to do.** Add a row in Settings showing this install's random identifier,
with a copy action.

**Why.** `nightjarlabs.llc/delete-data` went live on 2026-09-25: a form that
emails a data-deletion request for any Nightjar app. It asks for the install ID
because that is the only key on anything we hold. Records sent to Sentry and
Grafana carry no name, email or account, because there is none to carry. If a
player cannot read the ID off their own phone, the request arrives with nothing
to search for and we cannot honour it.

**Where.** Whatever this app calls the identifier it stamps on telemetry, in the
About section of Settings. It is a random UUID with nothing behind it, so there
is nothing to leak. Word the supporting text so it reads as a reference number
for a support request rather than an account number, since this app has no
accounts.

**Worth doing at the same time.** Doublestack pairs this with a "Delete local
data" action that erases local state and issues a fresh ID
(`PlayerDataEraser.kt` in that repo). That covers the player who wants to be
forgotten going forward; the ID display covers the one who wants records already
sent removed. Either alone is half an answer.

**Provenance.** Raised by the owner on 2026-09-25 across three apps. Sodogku
carries it as SD-151 and Doublestack has its own entry.
