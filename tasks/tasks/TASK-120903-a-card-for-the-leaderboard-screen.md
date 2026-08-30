---
schema: 2
id: TASK-120903
title: A card for the `leaderboard` screen
type: task
status: done
parent: STORY-1209
module: design
estimate: S
tier: sonnet
review: light
files_touched: 1
labels: [qa, uat, bug, high, design]
depends_on: []
verify:
  - test -f design/screens/leaderboard.html
  - grep -qF 'design/tokens/tokens.css' design/screens/leaderboard.html
  - test "$(grep -o -- '--pd-[a-z0-9-]*' design/screens/leaderboard.html | sort -u | wc -l)" -ge 20
  - sh design/check-drift.sh
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/screens/leaderboard.html` exists: a rendered reference for the `leaderboard` screen, composed from
the settled vocabulary, so the next UAT round can judge that screen's conformance instead of
recording `BLOCKED — no card`.

## The defect

Round 1 of `/qa-cycle uat regression`, 2026-08-30, commit `c05ee695`. **The `leaderboard` screen — the season standings — has no merged card.**

```
$ ls design/screens/
create-duel.html  duel-end.html  duel-table-states.html  duel-table.html
enter-code.html   join-duel.html rematch-states.html
```

The screen is in scope — `docs/test-plan.md` §UAT's inventory lists it, routed by `05-01` and `05-03` — and it
has no merged card, so check **a** could not be made at all. `ADR-0092` §4 makes that a `high`
finding whose **repair is the card**, and `ADR-0091` §3 makes composing one an ordinary dispatched
ticket: `module: design`, `review: light`, the human's visual verdict trailing the merge.

**It is excluded from `B(1)`.** `ADR-0091` §5 registered this screen as debt before this round
existed; collecting registered debt is not the product decaying, and counting four such findings
would set round 2 the bar of *beat five* over a queue of design authoring.

## Files

| File | Action |
| --- | --- |
| `design/screens/leaderboard.html` | create |

## Scope

- **Compose, do not mint** (`ADR-0091` §3). Every value comes from `design/tokens/tokens.css`;
  conventions per `design/README.md` and `ADR-0033`. A new token or a new component is a different
  kind of ticket and is worked with the human.
- **Draw the states this screen actually has**: the standings table, the season line, and the viewer’s own rank line — including the state in which the viewer has no place yet.
- **Copy is transcribed, never invented.** Take every string from the module that owns it or from
  the merged ADR that settled it — `web-client/src/ladder/`, `ADR-0061` and `ADR-0062`. A string with no merged source is not drawn; it is a
  `DEC` for the product owner (`ADR-0090` §4's rule, applied to a card).
- Follow the existing cards' shape: a lede naming what the screen promises, one frame per state,
  and margin notes carrying the decisions behind the choices.

## Out of scope

- **Changing the client.** This ticket creates a reference; nothing under `web-client/` is touched.
  Where the shipped screen and the finished card disagree, that is a **finding for the next round**,
  which is the whole point of composing it.
- **Drawing the screen you wish existed.** The card is a reference a coder transcribes, so a frame
  that needs a server fact the wire does not carry is a decision, not a drawing —
  `design/screens/join-duel.html` is the cautionary example, and `TASK-120907` is the ticket it
  produced.
- **The human's visual verdict.** It trails the merge (`ADR-0091` §3) and no gate here stands in for
  it.

## Tests

No test file: a card is a rendered artefact, and `ADR-0024` §3 puts its taste judgment with the
human at the pane. The `verify:` block gates what a command honestly can, and each line fails today:

| Command | Proves |
| --- | --- |
| `test -f …` | the card exists at the path `docs/test-plan.md`'s inventory will name |
| `grep -qF 'design/tokens/tokens.css' …` | it declares the canonical sheet as its source, the way every merged card does |
| `… --pd- … -ge 20` | it is composed from the settled vocabulary rather than stubbed — the seven merged cards carry 26 to 33 distinct token names, so 20 is a floor no real card misses and no stub reaches |
| `sh design/check-drift.sh` | every token name it mentions is declared in the sheet, and every value it inlines equals the sheet's (`ADR-0024` §2, clauses 1 and 3) |

## Acceptance criteria

- [ ] `design/screens/leaderboard.html` exists and opens in a browser as a rendered card
- [ ] It carries at least 20 distinct `--pd-` token names, all declared in `design/tokens/tokens.css`
- [ ] `sh design/check-drift.sh` exits 0 with the new card in the tree
- [ ] Every string on the card is traceable to the module or ADR that owns it, named in a margin note
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged. The human's visual verdict may trail it
(`ADR-0091` §3).
