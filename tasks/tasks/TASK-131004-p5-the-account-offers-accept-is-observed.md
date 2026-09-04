---
schema: 2
id: TASK-131004
title: P5 — the account offer's accept, observed rather than derived
type: task
status: done
parent: STORY-1310
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [qa, refresh, manual-verify]
depends_on: [TASK-131003]
verify:
  - awk '/^\| `P5`/ { if (index($0, "NOT-YET-DRIVEN")) bad = 1; else ok = 1 } END { exit (bad || !ok) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '{ n += gsub(/NOT-YET-DRIVEN/, "&") } END { exit (n > 5) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '/^\| `P[1-6]/ { n++ } END { exit (n != 7) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1310*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The `AccountOffer` accept on the result screen is **pressed on a running stack** and what it does is
written into `STORY-1310`'s `P5` row — because `ADR-0112` resolved a collision on this exact anchor
by derivation, and said so in its own words.

## Why this path is the sharpest of the six

`ADR-0112` §Context, about the accept: ***"Derived, not driven**: the result screen is precisely the
path the measurement did not cover, which is why §6 owes it a reproduction rather than a belief."*
§5 then resolves the collision **in `ADR-0086`'s favour** — the anchor lands on the account screen it
names — and `ADR-0114` §6 records that nothing in the mechanism rests on the derived claim. So the
merged answer is settled either way; what is missing is the observation, and this ticket is it.

**What is on the screen.** `ADR-0086` §6 makes the accept an anchor with an `href` to the account
screen plus a click handler that settles the offer before the browser navigates: *"the anchor still
loads the account screen, and that page load is what replaces this tree."* The derivation `ADR-0112`
wrote down is that the page load boots, rejoins the remembered `FINISHED` room, the restored
`outcome` re-seats the tree, and `Lobby.tsx`'s effect erases the fragment — so the account screen
never shows.

**That derivation is a race, and the bare stack is the wrong place to judge it.** Whether the account
screen paints before the frames land is a matter of milliseconds on loopback. Drive it delayed as
well as bare; the delayed reading is what tells a screen that never rendered from one that rendered
and was taken away.

## The stack

As `TASK-131003` sets it out — bare, and `delayed 300ms` with Vite on `5273` and
`node scripts/qa/delay.mjs 5173 5273 300 6173` in front of it. Fresh Chrome profiles from
`mktemp -d` every time (`ADR-0089` §3).

## Files

| File | Action |
| --- | --- |
| `tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md` | modify |

Read: `docs/adr/ADR-0112-only-a-running-duel-refuses-another-screen.md` §5 and §Context,
`docs/adr/ADR-0086-the-offers-answer-is-one-key-owned-beside-the-predicate-it-feeds.md` §6, and
`scripts/qa/drive.mjs`. No client source is opened, and none is changed.

## Scope

- **Reach the offer.** Play a duel to a winner on two fresh profiles. `offerAccount` decides which
  browser is shown the offer; read both screens and drive the one that has it. If neither does, that
  is itself the row's result and the PR body says what each browser showed instead.
- **Press it, and watch the page load.** In order:
  1. `X text` — the result screen with the offer on it.
  2. `X click` on the accept control, labelled by whatever that `text` showed — a real click on the
     anchor, the way a player reaches it. **Do not navigate by hand.** An `open` or a
     `location.hash` write is a different act: it skips the click handler that settles the offer, and
     it would prove something else.
  3. No `open` is issued — the anchor's own page load is the event. Immediately after the click:
     `X text`, then `X record`, then `X frames`, then `X text` again once things settle.
  4. `X eval "location.hash"` — the address after it settles.
- **Both layouts**, bare then `delayed 300ms`.
- **Write the `P5` row**, and phrase it against the merged answer: does the accept land on the
  account screen `ADR-0112` §5 says it lands on, or does it bounce the way §Context derived? Name
  which, name the layout, name the short commit.
- Also record whether the **offer was settled** by the press — readable from the offer's absence on a
  later return to the result screen. `ADR-0086` §6 has the handler run before navigation, and a
  bounce that also spent the offer is a second finding.

## Out of scope

- **Any repair, and any change to `Lobby.tsx`.** The branch order is `ADR-0114` §2's and lands in
  `STORY-1311`. This ticket observes the product as it stands at the commit it names.
- **Re-opening §5.** `ADR-0112` resolved the collision in `ADR-0086`'s favour; a drive that finds the
  bounce is **confirming a known cost**, not reopening a decision. Write it down and stop.
- **Claiming a profile, signing in, or anything else on the account screen.** The observation ends at
  which screen is standing and what the address says.
- **`/qa-cycle`, `docs/test-plan.md`, `A(N)`/`B(N)`, and any coverage claim** (`ADR-0089` §§2b, 2c).

**If the reading needs a decision** — for instance the accept spends the offer and lands nowhere, so
a player loses the offer without ever reaching the screen, and no merged source says what should
happen — write the row, register the next free `DEC` in `docs/adr/README.md`'s `## Open decisions`,
and leave this ticket `blocked`. That is a legitimate ending, not a failure.

## Tests

No test can be written, for `TASK-131003`'s merged reason: `ADR-0089` §2b keeps a browser out of
every `verify:` block, and jsdom boots no socket, computes no navigation and has no second browser.
The gates are the same four and mean the same four things — the `P5` row is filled, at most five
placeholders remain, the table still has its seven rows — the row gate already fails on a **missing**
row (`exit (bad || !ok)`, probed), so this one guards the other six, and no `verify:` block in this story runs a browser.

## Acceptance criteria

**Who runs the measurement:** the implementer, before opening the PR, on a running stack.
**Paste every reading into the PR body as text**, unedited, for both layouts.

- [ ] The accept control was reached by **clicking it on the result screen**, and the PR body shows
      the `text` that named it and the `click` line that pressed it
- [ ] The `frames` transcript after the click is in the PR body for both layouts, and the row says
      whether the account screen appeared in it at all
- [ ] `location.hash` after the page settles is in the PR body for both layouts
- [ ] The `P5` row says, in one sentence, whether the observation agrees with `ADR-0112` §5's
      resolution in `ADR-0086`'s favour or with §Context's derived bounce — and names the layout and
      the short commit
- [ ] The row or the PR body records whether the offer was settled by the press
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
