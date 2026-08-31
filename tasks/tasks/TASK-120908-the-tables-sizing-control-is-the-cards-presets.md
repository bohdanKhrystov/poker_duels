---
schema: 2
id: TASK-120908
title: The table's sizing control is the card's presets, not a range slider
type: task
status: backlog
parent: STORY-1209
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [qa, uat, bug, medium]
depends_on: []
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/ActionBar.test.tsx 2>&1 | grep -qF "the sizing row offers the card's five presets"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/ActionBar.test.tsx 2>&1 | grep -qF "each preset sets the amount its own name states"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/table/ActionBar.test.tsx 2>&1 | grep -qF "a preset the stack cannot afford is not offered"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/table/ActionBar.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The action bar sizes a raise the way `design/screens/duel-table.html` draws it — named presets a
player can hit in one press — instead of a native range slider a player has to aim.

## The defect

Round 1 of `/qa-cycle uat regression`, 2026-08-30, commit `c05ee695`, against
`design/screens/duel-table.html`.

**Shipped**, read from the running client:

    <input aria-label="raise to" class="flex-1" max="10000" min="200" step="1" type="range" value="200">
    <span class="font-mono tabular-nums">200</span>

plus four buttons — *Fold*, *Call 100*, *Raise to 200*, *All in 10,000*.

**The card** draws `.sizing` holding five `.chip` presets — min, ⅓, ½, pot, all-in — with a `+`/`−`
stepper beside them, and an `.actions` row of exactly three: *Fold*, *Call*, *Raise to*. The card's
all-in is a **sizing preset**, not a fourth action button.

Every preset the card names is computable from the snapshot the client already holds, so this is a
transcription that was not made, not a fact the wire withholds.

## Why `medium` and not `high`

`uat` reported `high`. **Severity lowered.** Every action remains available and correct: a player can
fold, call, raise to any legal amount and go all in, and the raise is still bounded by `min`/`max`
the server enforces. No promise in `EPIC-12`'s `high` row is touched and nothing is a regression.
The card is transcribed on this screen — seats, pot, cards, actions are all there — and one control
inside it diverges, which is `medium` on the line the round story states.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/ActionBar.tsx` | modify |
| `web-client/src/table/ActionBar.test.tsx` | modify |

## Scope

- **Replace the range input with the card's five presets** plus its stepper, each preset computing
  its amount from the snapshot already in hand.
- **A preset that is not legal is not shown.** The min raise, the pot and the stack all move between
  streets; a chip that sets an amount the server would refuse is worse than no chip.
- **The actions row becomes the card's three.** All-in moves into the sizing row as a preset; the
  action a press produces is unchanged on the wire.

## Out of scope

- **Anything the server decides.** Legal amounts stay the server's; this ticket changes how a player
  reaches one, never which ones exist.
- **The rest of the table.** Seats, pot strip, board and the presence line are transcribed already
  and are not touched.
- **`design/screens/duel-table.html`.** The card is not in arrears here — no ADR merged after it
  settles the sizing control — so the client is the side that moves.

## Tests

`ActionBar.test.tsx`

| Test | Proves |
| --- | --- |
| `the sizing row offers the card's five presets` | min, ⅓, ½, pot and all-in are each present as their own control, and no `type="range"` input remains |
| `each preset sets the amount its own name states` | pressing each of at least three presets — min, ½ and pot — over a snapshot with a known pot and stack sets three **different** amounts, so one hard-coded value cannot pass |
| `a preset the stack cannot afford is not offered` | over a short stack where the pot-sized raise exceeds it, that preset is absent while the others remain |

## Acceptance criteria

- [ ] `ActionBar.test.tsx > the sizing row offers the card's five presets` passes
- [ ] `ActionBar.test.tsx > each preset sets the amount its own name states` passes over three
      distinct expected amounts, not one
- [ ] `ActionBar.test.tsx > a preset the stack cannot afford is not offered` passes
- [ ] Reverting `ActionBar.tsx` alone reddens all three
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
