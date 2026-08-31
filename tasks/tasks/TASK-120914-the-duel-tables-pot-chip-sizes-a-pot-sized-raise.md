---
schema: 2
id: TASK-120914
title: The duel table card's selected pot chip reads the raise it sizes, not the pot
type: task
status: ready
parent: STORY-1209
module: design
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [qa, uat, bug, medium, design]
depends_on: []
verify:
  - grep -qF '<span aria-label="raise to amount">3,650</span>' design/screens/duel-table.html
  - grep -qF 'Raise to <span class="amt">3,650</span>' design/screens/duel-table.html
  - sh -c '! grep -q "3,250" design/screens/duel-table.html'
  - sh -c 'grep -cF "3,650" design/screens/duel-table.html | grep -qx 2'
  - sh -c 'grep -cF "3,250" design/screens/duel-table-states.html | grep -qx 6'
  - sh design/check-drift.sh
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/screens/duel-table.html`'s hero frame prints, in both places it names the dialled total, the
raise the selected `pot` chip actually sizes — **3,650** — so a client built to
[`ADR-0101`](../../docs/adr/ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md)
and the card a UAT round reads no longer disagree by 400 chips.

## The card is in arrears, and by how much

[`ADR-0101`](../../docs/adr/ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md)
§5, merged 2026-08-31 — **after** the card. The hero frame draws `Pot 2,450`, the rival
`committed 400`, `Call 400`, the hero holding 13,400, the `pot` chip selected. Under §§1–2:

```
P      = 2,450 + 400 + 0 = 2,850
toCall = 400 − 0         = 400
base   = 2,850 + 400     = 3,250
pot    = 400 + 3,250     = 3,650
```

**3,250 is the base, not the total** — the pot as it will be after the call. The card printed the pot
it was sizing *against* in the place where the raise that sizes *to* it belongs. That diagnosis is
`ADR-0101` §5's, quoted here so the edit is not mistaken for a taste change.

The two places, both on the hero frame, both currently `3,250` and nowhere else in the file:

| Line | Today |
| --- | --- |
| 180 | `<span aria-label="raise to amount">3,250</span>` — the stepper's readout |
| 185 | `<button class="btn fill">Raise to <span class="amt">3,250</span></button>` — the actions row |

Measured on 2026-08-31: `grep -cF '3,250' design/screens/duel-table.html` is **2**, one per line
above, so replacing both is the whole change and no third occurrence is at risk.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | modify |
| `docs/adr/ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md` | read |

## Scope

- Change `3,250` to `3,650` in the stepper's `<span aria-label="raise to amount">` and in the actions
  row's `<span class="amt">` inside the *Raise to* button. Two nodes.
- Nothing else. Same grouping (a comma at the thousand), same markup, same classes, same
  `aria-label`s.

## Out of scope

- **Every other number on the card.** `Pot 2,450`, `Call 400`, the seat stacks 4,150 and 13,400, the
  rival's `committed 400`, the blinds — `ADR-0101` §5 says in as many words that nothing else on the
  card moves, and §4 works the frame out against all five chips to show why.
- **The card's anatomy.** Five chips, their labels, their order, the stepper, the three drawn action
  buttons: all correct and all what `ADR-0101` builds on.
- **`design/screens/duel-table-states.html`.** It carries six `3,250`s of its own — a pot of 3,250, a
  stepper inside an `off` bar, and a `You win 3,250` — in different frames with different numbers. No
  merged ADR puts them in arrears, and the last `verify` command pins that they are still there, so
  the correction cannot be over-applied.
- **The client.** `web-client/` is `TASK-120908`, which depends on this ticket landing first so its
  coder reads a card that agrees with the ADR.

## Tests

A card is HTML, not code, so its gates are the literal strings and `design/check-drift.sh`, which is
what `.github/workflows/tickets.yml` runs on the pull request. There is no test file.

| Gate | Proves |
| --- | --- |
| `grep -qF '<span aria-label="raise to amount">3,650</span>' …` | the stepper's readout moved, with its `aria-label` and markup intact |
| `grep -qF 'Raise to <span class="amt">3,650</span>' …` | the actions row's button moved, with its `.amt` span intact |
| `! grep -q "3,250" …` | neither occurrence was left behind |
| `grep -cF "3,650" … \| grep -qx 2` | exactly two, so no third node was edited by accident |
| `grep -cF "3,250" duel-table-states.html \| grep -qx 6` | the states card's own six `3,250`s are untouched |
| `sh design/check-drift.sh` | tokens, values, suit glyphs, graphics mirrors and lockup anatomy still hold |

## Acceptance criteria

- [ ] `grep -qF '<span aria-label="raise to amount">3,650</span>' design/screens/duel-table.html`
      exits 0
- [ ] `grep -qF 'Raise to <span class="amt">3,650</span>' design/screens/duel-table.html` exits 0
- [ ] No `3,250` remains anywhere in `design/screens/duel-table.html`, and exactly two `3,650` do
- [ ] `design/screens/duel-table-states.html` still holds six lines containing `3,250` — it is not
      edited by this ticket
- [ ] `sh design/check-drift.sh` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
