---
schema: 2
id: TASK-130207
title: The retired frame leaves the card, and the inventory names where the host waits
type: task
status: done
parent: STORY-1302
module: design
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [design, docs, qa]
depends_on: [TASK-130206]
verify:
  - ./design/check-drift.sh
  - awk 'index($0, "Created — waiting for your rival") { n++ } END { exit (n != 0) }' design/screens/create-duel.html
  - awk 'index($0, "Before — the front door") { n++ } END { exit (n != 1) }' design/screens/create-duel.html
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 1) }' design/screens/create-duel.html
  - awk 'index($0, "class=\"mark\"") { n++ } END { exit (n != 1) }' design/screens/create-duel.html
  - awk 'index($0, "Back to the lobby") { n++ } END { exit (n != 0) }' design/screens/create-duel.html
  - awk 'index($0, "The room stays open.") { n++ } END { exit (n != 0) }' design/screens/create-duel.html
  - awk 'index($0, "Copy the link") { n++ } END { exit (n != 0) }' design/screens/create-duel.html
  - awk 'index($0, "Open seat") { n++ } END { exit (n != 0) }' design/screens/create-duel.html
  - awk 'index($0, "linkline") { n++ } END { exit (n != 0) }' design/screens/create-duel.html
  - awk 'index($0, "SMK-04") && index($0, "design/screens/duel-table.html") { n++ } END { exit (n != 1) }' docs/test-plan.md
  - awk 'index($0, "SMK-04") && index($0, "design/screens/create-duel.html") { n++ } END { exit (n != 0) }' docs/test-plan.md
  - awk 'index($0, "CORE-20") && index($0, "waiting screen") { n++ } END { exit (n != 0) }' docs/test-plan.md
  - awk 'index($0, "CORE-20") && index($0, "Back to the lobby") { n++ } END { exit (n != 1) }' docs/test-plan.md
  - awk 'index($0, "Host alone") { n++ } END { exit (n != 4) }' design/screens/duel-table.html
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The screen `ADR-0110` retired stops being drawn and stops being pointed at:
`design/screens/create-duel.html` loses its *Created — waiting for your rival* frame, and
`docs/test-plan.md`'s screen inventory names the card that now draws where a host waits.

## Why it is last, and not first

`ADR-0110`'s *Consequences* accepts that *"the design tree goes briefly out of step with the
client"* — but the direction of that step matters. Adding the new frames first (`TASK-130201`) puts
the card **ahead** of the client, which `ADR-0091` §2 requires. Removing the old frame first would
leave a **gap**: a shipped screen with no frame drawing it, and a `docs/test-plan.md` inventory row
pointing at a card that no longer draws its state — which `ADR-0092` §4 files as a `high` for a
screen in scope with no merged card. So the retirement follows the client, and a gate pins
`Host alone` at 4 to prove it is running after `TASK-130202`, not instead of it.

## Files

| File | Action |
| --- | --- |
| `design/screens/create-duel.html` | modify |
| `docs/test-plan.md` | modify |
| `docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md` | read |

## Scope

- **Delete the second frame** of `design/screens/create-duel.html` — the whole
  `<div class="frame">` headed `Created — waiting for your rival`, its code well, its link line,
  its two seats, its way back, its promise line and its two notes. `ADR-0110` *Amends*:
  `ADR-0073` §6's frame is retired with the screen it draws.
- **Delete the CSS only that frame used**, and nothing else: `.code`, `.linkline`,
  `.linkline .path`, and the whole `.seat` block including `.seat.seat-empty` and
  `.seat.seat-empty .name`. **Keep** `.mark` and `.mark .coin` — `check-drift.sh` clause 6 compares
  that lockup copy against `graphics/wordmark.html`, and the front door still shows it — and keep
  `.btn`, `.btn.fill`, `.btn.ghost`, `.hero-h`, `.sub`, `.note`, `.eyebrow`, which the front-door
  frame uses.
- **Trim the lede's last sentence**, *"The code is the invite (ADR-0022), big enough to read across
  a room."*, which describes the frame being removed, and say instead that creating a duel now
  lands the host at the table — `design/screens/duel-table.html`'s `Host alone` frames.
- **`docs/test-plan.md`, two edits and no more:**
  1. The screen-inventory row whose routes are `SMK-04`, `CORE-20`. Its *state* becomes the host at
     the table with the rival's seat empty and the invite on it, and its *card* becomes
     `design/screens/duel-table.html`.
  2. `CORE-20`'s `do` cell: *"on the waiting screen"* becomes *"at the table, while the room is
     still waiting"*. Its `expect` and `fails if` cells are **untouched** — the case still presses
     `Back to the lobby` and still fails if the room closes or the link stops working
     (`ADR-0073`). This is a locative correction, not a regrade (`ADR-0092` §7).

## Out of scope

- **`CORE-07`'s wording.** It also says *"the waiting screen"*, and it means something else — the
  live table when it is not your turn, whose bar reads `Waiting for your rival…`. Its ambiguity
  predates `ADR-0110` and is not this story's; the `CORE-20` gates are scoped to the `CORE-20` line
  precisely so this one is not swept along. Not yet ticketed.
- **`SMK-04`'s `do` cell.** `A wait "Waiting for your rival"` still passes: the string still
  renders, at the seat instead of as a heading. Nothing to change.
- **Every other row of the inventory and every other case.** Two edits, both named above.
- **`design/screens/duel-table.html`.** Read-only here; a gate pins `Host alone` at 4 so this
  ticket cannot be run before `TASK-130202` merged, and cannot quietly repair it either.
- **The front door's own frame**, which `ADR-0110` §8 says explicitly is untouched, and its
  wordmark lockup, which `check-drift.sh` clause 6 enforces.

## Tests

**No test file, and none is possible** — one card and one document, neither imported by anything.
The `verify:` block is the gate: nine assertions on `create-duel.html` (one of them, `class="mark"`
at 1, saying what must *survive*), four on `docs/test-plan.md`, one on `duel-table.html` proving
the predecessor merged, and `check-drift.sh`.

Every count was **measured on 2026-09-02**:

| Marker | File | Today | After |
| --- | --- | --- | --- |
| `class="frame"` | `create-duel.html` | 2 | **1** |
| `Created — waiting for your rival` | `create-duel.html` | 1 | **0** |
| `Before — the front door` | `create-duel.html` | 1 | 1 — survives |
| `class="mark"` | `create-duel.html` | 1 | 1 — survives |
| `Back to the lobby` | `create-duel.html` | 1 | **0** |
| `The room stays open.` | `create-duel.html` | 1 | **0** |
| `Copy the link` | `create-duel.html` | **2** — the button, and the frame's own note quoting `ADR-0073`'s choice of the definite article; both sit inside the frame and both go | **0** |
| `Open seat` | `create-duel.html` | 1 | **0** |
| `linkline` | `create-duel.html` | **3** — the `.linkline` rule, the `.linkline .path` rule, and the markup | **0** |
| `SMK-04` + `create-duel.html` on one line | `docs/test-plan.md` | 1 | **0** |
| `CORE-20` + `waiting screen` on one line | `docs/test-plan.md` | 1 | **0** |
| `CORE-20` + `Back to the lobby` on one line | `docs/test-plan.md` | 1 | 1 — survives |

## Acceptance criteria

- [ ] `Created — waiting for your rival`, `Back to the lobby`, `The room stays open.`,
      `Copy the link`, `Open seat` and `linkline` each appear zero times in
      `design/screens/create-duel.html`
- [ ] `Before — the front door`, `class="frame"` and `class="mark"` each appear exactly once in it
- [ ] The `SMK-04`/`CORE-20` inventory row names `design/screens/duel-table.html` and no longer
      names `design/screens/create-duel.html`
- [ ] The `CORE-20` row no longer says `waiting screen` and still says `Back to the lobby`
- [ ] `Host alone` still appears four times in `design/screens/duel-table.html`
- [ ] `./design/check-drift.sh` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
