---
schema: 2
id: TASK-130202
title: The card's three remaining host-alone variants, and the arrival
type: task
status: ready
parent: STORY-1302
module: design
estimate: S
tier: haiku
review: light
files_touched: 1
labels: [design, table, client]
depends_on: [TASK-130201]
verify:
  - ./design/check-drift.sh
  - awk 'index($0, "Host alone") { n++ } END { exit (n != 4) }' design/screens/duel-table.html
  - awk 'index($0, "Waiting for your rival") { n++ } END { exit (n != 4) }' design/screens/duel-table.html
  - awk 'index($0, "Invite link") { n++ } END { exit (n != 4) }' design/screens/duel-table.html
  - awk 'index($0, "Back to the lobby") { n++ } END { exit (n != 4) }' design/screens/duel-table.html
  - awk 'index($0, "The room stays open. That link still works for your rival, and it brings you back.") { n++ } END { exit (n != 4) }' design/screens/duel-table.html
  - awk 'index($0, "Copy the link") { n++ } END { exit (n != 3) }' design/screens/duel-table.html
  - awk 'index($0, "Link copied.") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "Copy it from the box above.") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "the waiting furniture is gone") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, ".note {") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "viewport phone") { n++ } END { exit (n != 5) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 6) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"pot\"") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"bar\"") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"board\"") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"dealer\"") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "Pot&nbsp;") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "role=\"img\"") { n++ } END { exit (n != 16) }' design/screens/duel-table.html
  - awk 'index($0, "10,000") { n++ } END { exit (n != 0) }' design/screens/duel-table.html
  - awk 'index($0, "Open seat") { n++ } END { exit (n != 0) }' design/screens/duel-table.html
  - awk 'index($0, "minutes") { n++ } END { exit (n != 0) }' design/screens/duel-table.html
  - sh -c '! grep -qiE "@keyframes|animation:|transition:" design/screens/duel-table.html'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/screens/duel-table.html` draws all four of
[`ADR-0110`](../../docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md) §8.1's named
host-alone variants, and says in one line what the live phone frame beside them is: §8.2's
arrival. The card `STORY-1302`'s client tickets conform to is then complete.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | modify |
| `design/screens/duel-table-states.html` | read |
| `docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md` | read |

## Scope

- **Three frames**, each a copy of `TASK-130201`'s `Host alone — at rest` frame with one thing
  different, appended after it and headed exactly:

  | `<h2>` | What differs from *at rest* |
  | --- | --- |
  | `Host alone — the copy succeeded` | a feedback line under the button reading `Link copied.` |
  | `Host alone — the copy was refused` | a feedback line under the button reading `Copy it from the box above.` |
  | `Host alone — no clipboard API` | **the `Copy the link` button is absent**; the link box is the whole invite |

  The button still stands in the first two — the client keeps it and adds the line below it — which
  is why `Copy the link` is gated at **3** and not 4.
- **Nothing else about the three copies differs.** Same seats, same words, same code well, same
  link box, same way out, same promise line, same `.viewport.phone` box. `ADR-0110` §6's
  enumeration is exhaustive: a variant that seems to want an extra word is a stop and a new ADR,
  never an invented sentence.
- **The feedback line reuses an existing paint.** Style it with the `.promise` rule
  `TASK-130201` added, or add one small rule beside it; do not introduce a colour or size that
  `design/tokens/tokens.css` does not declare.
- **One `.note` rule and one note**, discharging `ADR-0110` §8.2. Copy the rule verbatim from
  `design/screens/duel-table-states.html` (`.note { font-size: 0.75rem; font-family:
  var(--pd-font-mono); color: var(--pd-text-faint); margin-top: var(--pd-space-4); }`) — this file
  declares none today, measured. Put the note inside the existing `Phone — 390 × 664` frame,
  under the table, saying that this is what the four frames above become the moment the rival
  arrives, and containing the literal phrase **`the waiting furniture is gone`** followed by what
  leaves: the empty seat's line, the invite's three parts, `Back to the lobby` and its promise.
- **The existing `Phone — 390 × 664` heading does not change.** That frame is the live table at the
  phone, and `TASK-121302` conforms the client to it; narrowing its title to the arrival would
  narrow what it is for.

## Out of scope

- **A fifth drawn frame for the arrival.** `ADR-0110` §8.2 settles it by pointing: *"which is the
  live table `design/screens/duel-table.html` already draws; the card owes the transition nothing
  more than showing that the waiting furniture is gone from it."* Since `TASK-130201` put the
  host-alone frames in this same file, the adjacency is the showing and the note names it. A
  redrawn copy of the live table would be a second maintenance site for the same picture.
- **The two live frames' contents.** Six count gates pin `class="pot"`, `class="bar"`,
  `class="board"`, `class="dealer"`, `Pot&nbsp;` and `role="img"` at their measured values.
- **Any game fact, and any duration.** `ADR-0110` §3 and `ADR-0072` §6; `10,000` and `minutes`
  stay at zero.
- **Motion.** Nothing here moves, so `ADR-0115` owes no still form; the gate keeps it that way.
- **The client.** `TASK-130203` onwards.

## Tests

**No test file, and none is possible** — the same reason as `TASK-130201`: a design card is HTML
nobody imports, and `ADR-0089` §2b forbids a browser measurement being a gate. The `verify:` block
is exhaustive for three near-duplicate frames plus a note: seven gates say what must now be there
at what multiplicity, eleven say what must not have moved or appeared.

The refusal counts are `TASK-130201`'s measured baseline, unchanged by that ticket by construction
(its own gates pinned them). The required counts are this ticket's own arithmetic over frames it
specifies exactly, not a guess: one occurrence per frame for the four strings every frame carries,
three for `Copy the link`, one each for the two feedback lines.

## Acceptance criteria

- [ ] `Host alone`, `Waiting for your rival`, `Invite link`, `Back to the lobby` and the promise
      sentence each appear exactly four times in `design/screens/duel-table.html`
- [ ] `Copy the link` appears exactly three times
- [ ] `Link copied.` and `Copy it from the box above.` each appear exactly once
- [ ] `the waiting furniture is gone` appears exactly once, and `.note {` exactly once
- [ ] `viewport phone` appears five times and `class="frame"` six times
- [ ] `class="pot"`, `class="bar"`, `class="board"`, `class="dealer"` and `Pot&nbsp;` still appear
      twice each, and `role="img"` still sixteen times
- [ ] `10,000`, `Open seat` and `minutes` appear zero times
- [ ] `@keyframes`, `animation:` and `transition:` appear zero times
- [ ] `./design/check-drift.sh` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
