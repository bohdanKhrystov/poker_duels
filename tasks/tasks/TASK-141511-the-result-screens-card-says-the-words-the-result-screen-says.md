---
schema: 2
id: TASK-141511
title: The result screen's card says the words the result screen says
type: task
status: done
parent: STORY-1415
module: design
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [design, rematch, drift]
depends_on: [TASK-141510]
verify:
  - ./design/check-drift.sh
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 4) }' design/screens/rematch-states.html
  - awk 'index($0, "<h2>") { n++ } END { exit (n != 4) }' design/screens/rematch-states.html
  - awk 'index($0, "ImKate") { n++ } END { exit (n != 0) }' design/screens/rematch-states.html
  - awk 'index($0, "Your rival offers a rematch") { n++ } END { exit (n != 1) }' design/screens/rematch-states.html
  - awk 'index($0, "Rematch offered — waiting for your rival") { n++ } END { exit (n != 1) }' design/screens/rematch-states.html
  - awk 'index($0, "That duel room is gone.") { n++ } END { exit (n != 1) }' design/screens/rematch-states.html
  - awk 'index($0, "Rematch. The button changes sides —") { n++ } END { exit (n != 1) }' design/screens/rematch-states.html
  - awk 'index($0, "dealing hand 1…") { n++ } END { exit (n != 1) }' design/screens/rematch-states.html
  - awk 'index($0, "class=\"btn fill\"") { n++ } END { exit (n != 1) }' design/screens/rematch-states.html
  - awk 'index($0, "class=\"offered\"") { n++ } END { exit (n != 1) }' design/screens/rematch-states.html
  - awk 'index($0, "class=\"theirs\"") { n++ } END { exit (n != 1) }' design/screens/rematch-states.html
  - awk 'index($0, "class=\"dealing\"") { n++ } END { exit (n != 2) }' design/screens/rematch-states.html
  - awk 'tolower($0) ~ /@keyframes|animation|transition/ { n++ } END { exit (n != 0) }' design/screens/rematch-states.html
  - sh -c '! grep -qiE "countdown|expires|time left|seconds left|decline|reject" design/screens/rematch-states.html'
  - git diff --quiet develop -- design/screens/rematch-panel.html
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/screens/rematch-states.html` says the four sentences the shipped result screen says, and
draws the fourth state it already has. The drift this story found, closed.

## Why this is a ticket at all

Found while writing `STORY-1415`, by reading the merged card beside the merged component. Measured
on `develop` at `f20d07ed`, the card says **`ImKate offers a rematch`** and **`Rematch offered —
waiting for ImKate`** where `RematchControl.tsx:65,74` say *your rival*, and it draws **no frame at
all** for `That duel room is gone.`, which `TASK-030909` shipped and `RematchControl.test.tsx`
asserts.

**Which side is stale is not a judgement call.** `ADR-0123` §4 quotes *Your rival offers a rematch*
and calls it *"the result screen's own line"*, so a merged ADR states the client's spelling as the
product's. The card is behind, and this is a transcription repair with nothing to decide.

This is also `ADR-0142` §7's named reopening trigger — *"the first time a stale sentence is found on
a card"* — firing for the first time. That trigger asks for a **register**, which is a mechanism
question and is registered as `DEC-158` for the architect. This ticket fixes the sentences; it does
not build the register and does not wait for it.

## Files

| File | Action |
| --- | --- |
| `design/screens/rematch-states.html` | modify |

Read [`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md) §4,
[`ADR-0044`](../../docs/adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) §6,
`web-client/src/result/RematchControl.tsx`, and `design/screens/rematch-panel.html` — the sibling
card `TASK-141501` minted, whose third frame draws the same fourth state and is what this one
matches. **Nothing outside the table above is changed**; a `verify:` line diffs the panel card
against `develop`.

## Scope

- **Two sentences are corrected to the shipped strings**, character for character, em dash included:
  - `ImKate offers a rematch` → `Your rival offers a rematch`
  - `Rematch offered — waiting for ImKate` → `Rematch offered — waiting for your rival`

  A gate holds `ImKate` at **0** across the whole file, including the `<p class="note">` blocks and
  the lede, so a name left in prose fails too.
- **A fourth frame is added, last**, with its own `<h2>`: the room is gone. It carries
  `<div class="dealing">That duel room is gone.</div>` and **no button** — `ADR-0044` §6 retires the
  control, and `RematchControl.tsx:39-45` already does exactly that. The `.dealing` treatment is
  reused rather than a new one minted: this is **composing**, not minting (`ADR-0091` §3), which is
  why this ticket is dispatched and `TASK-141501` was not.
- **Nothing else moves.** The three existing frames keep their headings, their treatments and their
  notes; the stylesheet gains no rule; no token is minted; and the card stays motionless. Gates hold
  `class="offered"` at 1, `class="theirs"` at 1, `class="btn fill"` at 1, and `class="dealing"` at
  **2** — the existing dealing frame plus the new one.
- **The name in the note stays out.** If a note names a rival to explain the layout, rewrite it
  without a name; the gate on `ImKate` is file-wide by design, because a card that says *your rival*
  in the frame and *ImKate* in the margin is the same drift one line over.

## Out of scope

- **`design/screens/rematch-panel.html`.** The new panel's card is `TASK-141501`'s and
  `TASK-141502`'s, and a `verify:` line refuses a byte of it.
- **`web-client/`.** No client file is opened; the client is the side that is right.
- **`ADR-0142` §7's register** — `DEC-158`, the architect's. It blocks nothing.
- **Whether the product should name the rival on the result screen.** `ADR-0123` §4 settles that it
  says *your rival*; reopening it is a product decision and not this ticket's.

## Tests

None: a design card's structural gates are its `verify:` block, and its visual verdict is the
human's (`ADR-0024` §3).

## What would still pass if this were done wrong

`ImKate` at **0** is file-wide rather than frame-scoped, because the two occurrences measured at
`f20d07ed` are both in frames and a repair that moved one into a note would satisfy a frame-scoped
gate. The two replacement sentences are pinned at **1** each rather than `>= 1`, so a repair that
pasted the new sentence beside the old one fails.

The four *unchanged* counts — `offered` 1, `theirs` 1, `btn fill` 1, `frame` 4 against `dealing` 2 —
are what stop the fourth frame being a copy of the third: a duplicated dealing frame passes a frame
count of 4 and fails `That duel room is gone.` at 1 only if that gate exists, which it does, and
fails `dealing` at 2 only if it is exact, which it is.

## Acceptance criteria

- [ ] `ImKate` appears **0** times anywhere in the file
- [ ] `Your rival offers a rematch` and `Rematch offered — waiting for your rival` each appear
      exactly **once**
- [ ] `That duel room is gone.` appears exactly **once**, in a fourth `class="frame"` with its own
      `<h2>` and no button
- [ ] `class="frame"` is **4**, `<h2>` is **4**, `class="dealing"` is **2**, and `class="offered"`,
      `class="theirs"` and `class="btn fill"` are **1** each
- [ ] `Rematch. The button changes sides —` and `dealing hand 1…` are still **1** each
- [ ] `./design/check-drift.sh` exits 0
- [ ] `git diff --quiet develop -- design/screens/rematch-panel.html` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
