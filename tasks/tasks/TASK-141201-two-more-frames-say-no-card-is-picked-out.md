---
schema: 2
id: TASK-141201
title: Two more frames say in their margin that no card is picked out
type: task
status: done
parent: STORY-1412
module: design
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [design, table]
depends_on: []
verify:
  - ./design/check-drift.sh
  - ./design/check-frame-cards.sh
  - awk 'index($0,"<h2>Showdown — you win, the loser mucks</h2>"){n++} END{exit (n!=1)}' design/screens/duel-table-states.html
  - awk 'index($0,"<h2>Fold — you win on the river, nobody shows</h2>"){n++} END{exit (n!=1)}' design/screens/duel-table-states.html
  - awk 'index($0,"<h2>Showdown — you win, the loser mucks</h2>"){f=1} index($0,"<h2>Showdown — ImKate shows"){f=0} f && index($0,"ADR-0126"){n++} END{exit (n<1)}' design/screens/duel-table-states.html
  - awk 'index($0,"<h2>Fold — you win on the river, nobody shows</h2>"){f=1} f && index($0,"ADR-0126"){n++} END{exit (n<1)}' design/screens/duel-table-states.html
  - awk 'index($0,"<h2>Showdown — ImKate shows"){f=1} index($0,"<h2>Showdown — a split pot"){f=0} f && index($0,"ADR-0126"){n++} END{exit (n<1)}' design/screens/duel-table-states.html
  - awk 'index($0,"<h2>Showdown — a split pot, both hands shown</h2>"){f=1} index($0,"<h2>Fold —"){f=0} f && index($0,"ADR-0126"){n++} END{exit (n<1)}' design/screens/duel-table-states.html
  - awk 'index($0,"ADR-0126"){n++} END{exit (n!=4)}' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 5) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"pc") { n++ } END { exit (n != 38) }' design/screens/duel-table-states.html
  - awk 'index($0, "back mucked") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The two frames of `duel-table-states.html` that draw face-up cards and do **not** yet say it — the
mucked showdown and the fold — carry `ADR-0126` §1's sentence in their own margin, so the drawing
is not re-decided at the pane.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table-states.html` | modify |

Read, do not edit: `docs/adr/ADR-0126-the-table-shows-the-cards-and-marks-none-of-them.md` §§1 and 4.

## Scope

- In the `<p class="note">` of the frame headed `Showdown — you win, the loser mucks`, add a
  sentence beginning `ADR-0126 §1:` stating that no card is picked out — the cards that made the
  winning hand are drawn exactly as the ones that did not, in the seat and on the board.
- In the `<p class="note">` of the frame headed `Fold — you win on the river, nobody shows`, add a
  sentence beginning `ADR-0126 §1:` stating the same for the hero's own two cards and the board:
  `ADR-0126` §1 names a fold as well as a showdown.
- Nothing else on the card moves. No frame is added or removed, no `<span class="pc…">` is added,
  removed or re-classed, and no card's markup changes at all — the two gates pinning
  `class="pc` at **38** and `class="frame"` at **5** are there to say so.
- The added prose carries **no** bare suit glyph (`♠♥♦♣` without a following `U+FE0E`) and **no**
  `--pd-` token name. `check-drift.sh` fails on either, and it is in the `verify:` block above.

## Out of scope

- The other two frames' notes. `Showdown — ImKate shows` and `Showdown — a split pot` already carry
  the sentence — the last two `verify:` awk gates over those frames pass **today**, and are in the
  block so that this ticket cannot satisfy itself by moving an existing line instead of adding two.
- The `Waiting — their turn on the turn card` frame: it draws no showdown and no fold.
- Any change to `design/check-frame-cards.sh`. Its per-frame counts are `TASK-141107`'s and this
  ticket must leave them green, not extend them.
- The client-side gate — that is `TASK-141202`.

## Tests

There is no test class: the gate is the `verify:` block, and every command in it is an `awk` over
the card plus the two merged design gates.

| Gate | Proves |
| --- | --- |
| the two anchor gates | each `<h2>` this ticket scopes to matches **exactly one** line, so a renamed heading fails loudly rather than gating nothing |
| the mucked-frame gate | `ADR-0126` appears **inside that frame's own span**, between its `<h2>` and the next one — measured red (exit 1) at `c6a41e6d` |
| the fold-frame gate | the same for the fold frame, from its `<h2>` to end of file — measured red (exit 1) at `c6a41e6d` |
| the two already-green frame gates | measured green (exit 0) at `c6a41e6d`, so the pair of red gates above is not an artefact of the `awk` scoping |
| `ADR-0126` total is **4** | two lines added, not one and not three; a file-wide count alone could not tell which frame gained them, which is why it sits beside the four frame-scoped gates and not instead of them |
| `class="frame"` = 5, `class="pc` = 38, `back mucked` = 4 | measured at `c6a41e6d`; no card markup moved |

## What would still pass if the coder were wrong

- **Adding both sentences to the lede or to one frame twice** passes the file-wide `ADR-0126` count
  of 4 and fails the two frame-scoped gates. That is why the count is not the gate.
- **Renaming a heading to something the anchor no longer matches** would make a frame-scoped gate
  vacuous — `awk` would simply never set `f`. The two anchor gates (`n != 1`) close that.
- **Deleting a sentence from a frame that already had one and pasting it into a frame that did
  not** passes both frame gates for the frames it touched and fails the two already-green gates.
  Those two gates pass today and are in the block for exactly this reason.

## Acceptance criteria

- [ ] `./design/check-drift.sh` exits 0
- [ ] `./design/check-frame-cards.sh` exits 0
- [ ] Both anchor `awk` gates exit 0
- [ ] All four frame-scoped `ADR-0126` gates exit 0
- [ ] The `ADR-0126` total gate (`n != 4`) exits 0
- [ ] The three card-markup gates (5 frames, 38 `class="pc`, 4 `back mucked`) exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
