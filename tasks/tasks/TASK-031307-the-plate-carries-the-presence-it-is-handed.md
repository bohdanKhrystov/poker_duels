---
schema: 2
id: TASK-031307
title: The plate carries the presence it is handed
type: task
status: done
parent: STORY-0313
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, duel, ui, presence]
depends_on: [TASK-031306]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +[0-9]+ passed \([0-9]+\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the presence on the plate ahead of the turn'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'gives a present seat its ordinary status back'
  - cd web-client && npm run check
---

## Goal

`SeatPlate` passes a presence through to `seatStatus`, so a plate can read `Away` or `Timed out`.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/SeatPlate.tsx` | modify — one optional prop, one argument |
| `web-client/src/table/SeatPlate.test.tsx` | modify — one helper prop, two tests added |
| `web-client/src/table/seat-status.ts` | read — the four-argument signature `TASK-031301` shipped |

## Scope

- `SeatPlate`'s props gain one, optional:

  ```tsx
    presence?: SeatPresence | null;
  ```

  and the call becomes
  `seatStatus(props.seat, props.isToAct, props.isViewer, props.presence ?? null)`.
- **Optional on purpose.** `DuelTable.tsx` renders two `SeatPlate`s and is not edited here —
  `TASK-031308` is what hands one of them a presence. A required prop would drag `DuelTable.tsx`
  and `DuelTable.test.tsx` into a ticket with no budget for them.
- `SeatPresence` is imported as a type from `../protocol`.
- The `onTurn` accent stays exactly as it is: it compares `status` against `"Your turn"` and
  `"Their turn"`, so a plate showing `Away` loses the accent by construction and no new branch is
  written for it.

## Out of scope

- Choosing which plate gets a presence. `DuelTable` does that in `TASK-031308`.
- The two words themselves — `seat-status.ts` owns them, and this file must not repeat either
  literal.
- Colour, weight and placement of the word on the plate. `EPIC-06`'s.

## Tests

`web-client/src/table/SeatPlate.test.tsx`, describe block `"a seat plate"`. The file's `plate`
helper gains `presence` to its options object, defaulted to `null`, and passes it through; the three
tests already there keep their names, inputs and expectations, because `null` is what they meant.

`a seat plate`

| Test | Proves |
| --- | --- |
| `puts the presence on the plate ahead of the turn` | with `presence: "AWAY"` and `isToAct: true`, `Away` is on the plate and neither `Their turn` nor `Your turn` is — asserted for `isViewer: false` **and** `isViewer: true`, so a plate that let the viewer's own turn win fails. With `presence: "ABSENT"` and `isToAct: true`, `Timed out` is on the plate |
| `gives a present seat its ordinary status back` | with `presence: "PRESENT"` and `isToAct: true`, `Their turn` is on the plate; with `presence: "PRESENT"` and `hasFolded: true`, `Folded` is; and neither `Away` nor `Timed out` appears in either case |

Two tests. Six hundred and five exist after `TASK-031306`, so the suite reports **607**.

## Proof

| Command | Proves |
| --- | --- |
| a green `Tests N passed (N)` line | two ran and every test before them still does |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | the optional prop typechecks and `DuelTable.tsx`'s two existing call sites still compile untouched |

**Name the edit that makes each assertion red:**

1. Drop the fourth argument — call `seatStatus(props.seat, props.isToAct, props.isViewer)` → `puts
   the presence on the plate ahead of the turn` fails, `Their turn` on the plate against the `Away`
   it looks for. Revert.
2. Pass `props.presence ?? "PRESENT"` instead of `?? null` → nothing fails, which is the finding
   worth recording: `seatStatus` treats both the same, so the default is a readability choice and
   not a behavioural one. Leave `?? null` and say so in the PR rather than claiming a test guards it.

## Acceptance criteria

- [ ] `a seat plate > puts the presence on the plate ahead of the turn` passes
- [ ] `a seat plate > gives a present seat its ordinary status back` passes
- [ ] The three tests already in the file keep their names and their expectations, and the only edit
      to them is the helper's new pass-through prop
- [ ] `SeatPlate.tsx` contains neither the literal `Away` nor the literal `Timed out`
- [ ] `DuelTable.tsx` is unchanged from `develop`
- [ ] `npm run --silent test` reports `Tests  N passed (N)` with no failures
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
