# Release notes

One file per version, named `<version>.txt`, holding the text a player reads on
the store page and in TestFlight. Plain text. No markdown: the stores render
none of it, so a `##` reaches the customer as two hash marks.

`release.yml` uses `<version>.txt` when it exists and falls back to "Bug fixes
and improvements." when it does not. Writing one is therefore optional, and
skipping it is a normal outcome for a release with nothing a player would
notice.

It never falls back to the changelog. That file is release-please's, written
for whoever maintains this, and it carries commit subjects, shas and repo
links. App Store version 1.2.0 shipped it verbatim, headings and all, before
this directory existed.

Keyed by version rather than a single `next.txt` so a forgotten update cannot
ship the previous release's notes, which is worse than shipping the generic
line.

Play truncates at 500 characters.
