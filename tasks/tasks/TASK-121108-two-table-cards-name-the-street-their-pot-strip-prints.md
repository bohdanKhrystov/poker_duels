---
schema: 2
id: TASK-121108
title: Two table cards name the street their pot strip prints
type: task
status: backlog
parent: STORY-1211
module: design
estimate: XS
tier: sonnet
review: light
files_touched: 2
labels: [qa, uat, bug, low, design]
depends_on: []
verify:
  - grep -qF 'Blinds 75/150 · Hand 14 · Turn' design/screens/duel-table.html
  - grep -qF 'Blinds 75/150 · Hand 14 · Turn' design/screens/duel-table-states.html
  - test "$(grep -c '<span class="meta">Blinds 75/150 · Hand 14</span>' design/screens/duel-table.html)" -eq 0
  - test "$(grep -c '<span class="meta">Blinds 75/150 · Hand 14</span>' design/screens/duel-table-states.html)" -eq 0
  - sh design/check-drift.sh
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`duel-table.html` and `duel-table-states.html` draw the third segment their own pot strip has always
printed, so a later round stops filing the client for it.

## This is a card in arrears, not a product defect

Round 3 reported the shipped meta line's third segment — `Blinds 50/100 · Hand 1 · Flop` — as a
divergence from the cards' two-segment `Blinds 75/150 · Hand 14`. **The client is right and the cards
are stale**, which is the same ruling two rounds have already made about *Back to the lobby*.

The street segment is required by a **merged test**:

```
web-client/src/table/PotStrip.test.tsx > "names the street the view names"
```

which renders all six streets and asserts `· Preflop`, `· Flop`, `· Turn`, `· River`, `· Showdown`
and `· Hand complete` in turn, and a sibling case pins the `Blinds 75/150 · Hand 14` prefix the cards
already draw. `PotStrip.tsx`'s `STREET_NAMES` is a side table precisely so `tsc` fails the day the
wire grows a street with no word.

So this contradicts no merged source *about the product*, counts in no `B(N)`, and is filed at `low`
against `design/` rather than at any severity against `web-client/`.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | edit |
| `design/screens/duel-table-states.html` | edit |

## Scope

- **Add the street segment to every `.meta` line that carries the blinds-and-hand prefix**, in the
  form the client prints: `Blinds 75/150 · Hand 14 · Turn`. The frames draw a turn board — three
  community cards plus one — so `Turn` is the street those frames are actually in; read each frame's
  own board before choosing its word rather than pasting one.
- **Leave the two banner frames' `.meta` alone.** `Two pair, aces and sevens` and `Nobody shows — …`
  are the banner's own meta line, not the pot's, and they are the subject of `TASK-121101`.

## Out of scope

- **The client.** Change no file under `web-client/`. If the cards and the client ever have to
  disagree here, that is a decision, not an edit.
- **Whether the banner exists at all.** `TASK-121101`, which is blocked on a `DEC`.
- **`TASK-121007`'s three corrections.** Still open, different literals, different files.

## Tests

Cards are checked by grep, per `ADR-0091` §4 and the pattern `TASK-121007` set: `verify:` asserts the
new three-segment string is present in both files **and** that the old two-segment string is gone
from both. The second half is what makes the gate fail today and stops an additive edit passing while
the stale line survives beside it.

## Acceptance criteria

- [ ] Both cards contain `Blinds 75/150 · Hand 14 · Turn`
- [ ] Neither card still contains the bare `<span class="meta">Blinds 75/150 · Hand 14</span>`
- [ ] `sh design/check-drift.sh` exits 0 — the token copies are untouched
- [ ] Every command in `verify:` exits 0
