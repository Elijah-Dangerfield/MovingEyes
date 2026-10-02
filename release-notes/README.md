# Release notes

One file per version, named `<version>.txt`, holding the text a player reads on
the store page and in TestFlight. Plain text. No markdown: the stores render
none of it, so a `##` reaches the customer as two hash marks.

`release.yml` uses `<version>.txt` when it exists and falls back to "Small
fixes." when it does not. Writing one is optional, and skipping it is a normal
outcome for a release with nothing a player would notice.

It never falls back to the changelog. That file is release-please's, written
for whoever maintains this, and it carries commit subjects, shas and repo
links. App Store version 1.2.0 shipped it verbatim, headings and all, and could
not be corrected: `whatsNew` is locked once a version reaches review and stays
locked after release. The only remedy is another version.

Keyed by version rather than a single `next.txt` so a forgotten update cannot
ship the previous release's notes, which is worse than shipping the fallback.

Play truncates at 500 characters.

## Write them unslopped

These are copy, so the `unslop-text` rules apply in full. The failure mode is
specific and worth naming, because it is what shipped first:

**A real line followed by a vague one.** "Settings now shows a reference ID.
Plus a handful of fixes." The first sentence earns its place. The second is
padding, and padding next to substance is what makes the whole thing read as
generated. If there is one thing worth saying, say one thing and stop.

Stock closers to avoid, all of which say nothing: "bug fixes and
improvements", "plus a handful of fixes", "under the hood", "we've been busy",
"squashed some bugs". The fallback is deliberately dull because it ships when
nobody wrote anything, which is not the same as choosing it.

Write in the app's voice. This one is first person and slightly dry, because
one person answers the support mail and the copy says so.
