---
schema: 2
id: TASK-130405
title: The mark's words are the button's words, and its figure is the event's own total
type: task
status: done
parent: STORY-1304
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, table]
depends_on: [TASK-130404]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/action-text.test.ts 2>&1 | grep -qE '^ *Tests +11 passed \(11\)$'
  - awk 'index($0, "lastActText") { n++ } END { exit (n < 2) }' web-client/src/table/action-text.ts
  - awk 'index($0, "actionVerb(") { n++ } END { exit (n < 11) }' web-client/src/table/action-text.ts
  - awk 'index($0, "event.to") { n++ } END { exit (n != 4) }' web-client/src/table/action-text.ts
  - awk 'index($0, "amount: null") { n++ } END { exit (n < 3) }' web-client/src/table/action-text.ts
  - sh -c 'grep -q "lastActText" web-client/src/table/action-text.ts && ! grep -q "event.to -" web-client/src/table/action-text.ts'
  - awk 'index($0, "lastActText(") { n++ } END { exit (n < 10) }' web-client/src/table/action-text.test.ts
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`action-text.ts` can say what the last-act mark says: `lastActText(event)` turns one of the six act
events into the `ActionText` the actor's own button carried — `actionVerb`'s verb, and the event's
own `to` on `Call`, `Bet`, `Raise to` and `All in`, nothing on `Fold` and `Check`.

## Why here, and why it computes nothing

`ADR-0109` §2 names this file: the six words are already its, and *"a mark that used different
words, or a different figure convention, would put two names for one act on one screen."* This
function is a **translation of the event's own token** and a **copy of the event's own number** —
that is the whole of it, and it is what keeps `no-derivation.test.tsx`'s invariant true of the mark
by construction rather than by inspection.

**No arithmetic anywhere.** `ADR-0109` §Alternative 7 refuses the increment (*Raise by 140*) by name
because the server never sent it. Three gates pin that shape rather than the absence of a character:
`event.to` appears **exactly four times** — one per figure-bearing arm and nowhere else, so
**do not write the literal `event.to` in a comment** — the string `event.to -` appears nowhere, and
`amount: null` appears at least three times (the merged `actionText` default, plus fold and check).
A grep over a character class would be no gate at all here: this file's KDoc already contains `/`
and `*`.

## What is already true, measured on `develop` 2026-09-02

- `action-text.test.ts` reports **5** tests. `action-text.ts` exports `ActionText`, `actionVerb` and
  `actionText`; `actionVerb(` appears **5** times, `amount: null` **1**, and `event.to` **0**. The
  counts after are therefore at least 11, at least 3, and exactly 4 — measured plus the six arms
  below, never estimated.
- `actionVerb` takes an `ActionType` (`"FOLD"`…`"ALL_IN"`) — the **bar's** vocabulary. The wire's act
  events are spelled differently (`PlayerFolded`…`PlayerAllIn`), and bridging the two spellings is
  exactly what this function does. `e2e/drive-duel.tsx` already keeps a third bridge of its own
  (`ACTION_TYPE_OF`) for the recorded `PlayerAction` spelling — do **not** import or move it; it
  belongs to the driver.
- `ActEvent` is exported as a type from `../store/duel-state` (`TASK-130403`). Importing a store
  type from a table module is the merged idiom: `table/act-frame.ts` already imports `PendingTurn`
  from there.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/action-text.ts` | modify |
| `web-client/src/table/action-text.test.ts` | modify |
| `web-client/src/store/duel-state.ts` | read |
| `docs/adr/ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md` | read |

## Scope

- **`export function lastActText(event: ActEvent): ActionText`**, a `switch` over the event's own
  `type` with one arm per act and no `default` — an exhaustive `switch` is what makes a seventh act
  a compile error rather than a silent `undefined`:
  - `PlayerFolded` → `{ verb: actionVerb("FOLD"), amount: null }`
  - `PlayerChecked` → `{ verb: actionVerb("CHECK"), amount: null }`
  - `PlayerCalled` → `{ verb: actionVerb("CALL"), amount: event.to }`
  - `PlayerBet` → `{ verb: actionVerb("BET"), amount: event.to }`
  - `PlayerRaised` → `{ verb: actionVerb("RAISE"), amount: event.to }`
  - `PlayerAllIn` → `{ verb: actionVerb("ALL_IN"), amount: event.to }`
- **Every verb goes through `actionVerb`.** No string literal `"Fold"`, `"Raise to"` or any other
  verb is written in this function: one vocabulary, one place it is spelled. The gate wants
  `actionVerb(` on at least **11** lines — the 5 merged occurrences plus the 6 arms — so an arm that
  spelled its own word cannot reach it.
- **KDoc** on the new function citing `ADR-0109` §2: what the actor's own button said, and the
  server's own total — the mark translates the event's token and invents no case.
- **`actionText`, `actionVerb` and `ActionText` are untouched.** The five merged tests in
  `action-text.test.ts` assert them and none of their assertions moves.

## Out of scope

- **Rendering.** `SeatPlate.tsx` calls this in `TASK-130406`; nothing calls it in this ticket, and
  that is fine — an unused exported function with tests is not dead code, it is the next ticket's
  input.
- **Formatting the figure.** `formatChips` is the table's and stays there; this function returns a
  `number | null` exactly as `actionText` does.
- **Any second sentence about an act.** `ADR-0046` §4's server-action line lives in
  `absent-action-text.ts` and coexists with this (`ADR-0109` §6); it is not opened, moved or
  reworded.
- **A tense, a subject or a name.** The mark is the button's words, not a sentence: *Call 400*,
  never *Your rival called 400*.

## Tests

`action-text.test.ts` — six added to the five it has (measured 2026-09-02), so the file reports
**11**. Each builds its event inline; there is no fixture, because a fixture default is exactly what
would let a hard-coded figure pass.

`the action text`

| Test | Proves |
| --- | --- |
| `says Fold for a fold, bare` | `lastActText({ type: "PlayerFolded", sequence: 3, seat: 1 })` is `{ verb: "Fold", amount: null }` |
| `says Check for a check, bare` | the same for `PlayerChecked` → `{ verb: "Check", amount: null }` |
| `says Call with the call's own total` | **two inputs**: `to: 400` gives `{ verb: "Call", amount: 400 }` and `to: 925` gives `925`. One input cannot tell the event's field from a constant |
| `says Bet with the bet's own total` | two inputs, `to: 800` and `to: 3250` → `{ verb: "Bet", amount: … }` |
| `says Raise to with the raise's own total` | two inputs, `to: 1200` and `to: 4750` → `{ verb: "Raise to", amount: … }`. The verb is the button's three-word one, not `"Raise"` |
| `says All in with the all-in's own total` | two inputs, `to: 13400` and `to: 500` → `{ verb: "All in", amount: … }` |

Six tests for six acts, four of them with two figures each, is why the gate wants at least ten
`lastActText(` calls: a suite that dropped an act, or asserted a figure once, cannot reach it.

## Acceptance criteria

- [ ] `action-text.test.ts` reports `Tests  11 passed (11)`
- [ ] `the action text.says Fold for a fold, bare` passes
- [ ] `the action text.says Check for a check, bare` passes
- [ ] `the action text.says Call with the call's own total` passes
- [ ] `the action text.says Bet with the bet's own total` passes
- [ ] `the action text.says Raise to with the raise's own total` passes
- [ ] `the action text.says All in with the all-in's own total` passes
- [ ] `action-text.ts` mentions `lastActText` on at least two lines, carries `actionVerb(` on at
      least 11 and `amount: null` on at least 3, carries `event.to` on **exactly** 4, and contains no
      `event.to -`
- [ ] `action-text.test.ts` calls `lastActText(` at least ten times
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
