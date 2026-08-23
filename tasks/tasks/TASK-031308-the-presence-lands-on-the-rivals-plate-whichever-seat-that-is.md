---
schema: 2
id: TASK-031308
title: The presence lands on the rival's plate, whichever seat that is
type: task
status: backlog
parent: STORY-0313
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, ui, presence]
depends_on: [TASK-031307]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +609 passed \(609\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the presence on the rival, from either seat'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts no presence on your own plate, from either seat'
  - cd web-client && npm run check
---

## Goal

`DuelTable` hands the presence it is given to the rival's plate and to no other, whichever of the
two seats the rival happens to be.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/DuelTable.tsx` | modify — one optional prop, one attribute |
| `web-client/src/table/DuelTable.test.tsx` | modify — two tests added, none changed |
| `web-client/src/table/SeatPlate.tsx` | read — the optional `presence` prop `TASK-031307` shipped |

## Scope

- `DuelTable`'s props gain one, optional:

  ```tsx
  export function DuelTable(props: {
    view: PlayerView;
    rivalPresence?: SeatPresence | null;
  }): ReactElement
  ```

- The rival's `<SeatPlate>` gains `presence={props.rivalPresence ?? null}`. **Your own plate gains
  nothing**: `OpponentPresence` carries no seat number because it is always about the opponent
  (`ADR-0028` §1), so there is no presence in this client's hands that is about its own seat.
- `SeatPresence` is imported as a type from `../protocol`.
- **Optional on purpose**, and this is not only about `Lobby.tsx`: `no-derivation.test.tsx` renders
  `<DuelTable view={…} />` six times with no other prop, and a required prop would fail `tsc` in a
  guard file this ticket has no budget to open.

## Out of scope

- The countdown and the explaining line. Those are `PresenceNotice`'s, a sibling of the table on the
  duel screen, and deliberately **not** inside `DuelTable` — the table's own no-derivation guard
  asserts that every number on it is a number the `PlayerView` carries, and a countdown is not.
- Wiring the store to this prop. `TASK-031309`.
- Any change to the two words. They come out of `seat-status.ts` and neither literal appears in this
  file.

## Tests

`web-client/src/table/DuelTable.test.tsx`, describe block `"the duel table"`. Two added; the nine
already there keep their names, inputs and expectations, because an absent optional prop is what
they meant.

**Both tests drive `viewerSeat: 0` and `viewerSeat: 1`.** The rival is defined as *the seat that is
not the viewer*, and every wrong implementation of that — a literal `1`, a literal `0`, `seats[1]`,
the seat with the button — agrees with the right one on exactly one of the two. One direction is not
a proof; it is a coin that came up heads.

The fixture also moves `seatToAct` onto the seat being asserted about, so that the *turn* word is
the thing the presence has to outrank or leave alone. With `aView()`'s default `seatToAct: 0` left
alone, a rival at seat 1 is never to act and both tests would pass against a component that had
dropped the prop on the floor.

`the duel table`

| Test | Proves |
| --- | --- |
| `puts the presence on the rival, from either seat` | with `rivalPresence: "AWAY"`: at `viewerSeat: 0` (rival at seat 1, `seatToAct: 1`) the plate named `Your rival` reads `Away`; at `viewerSeat: 1` (rival at seat 0, `seatToAct: 0`) the plate named `Your rival` reads `Away`. In both, `Their turn` is nowhere on screen |
| `puts no presence on your own plate, from either seat` | with `rivalPresence: "ABSENT"` and `seatToAct` set to the **viewer's** seat: at `viewerSeat: 0` the plate named `You` reads `Your turn`; at `viewerSeat: 1` the plate named `You` reads `Your turn`. `Timed out` appears exactly once on screen in each case, on the rival's plate — asserted with a count, because "it is on the rival's plate" is also true of a component that put it on both |

Two tests. Six hundred and seven exist after `TASK-031307`, so the suite reports **609**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 609 passed (609)` | two ran and the six hundred and seven before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | the optional prop typechecks and `no-derivation.test.tsx`'s six bare `<DuelTable view={…} />` renders still compile |

**Name the edit that makes each assertion red:**

1. Hand the presence to `view.seats[1]`'s plate instead of the rival's → `puts the presence on the
   rival, from either seat` fails on its `viewerSeat: 1` half and passes its `viewerSeat: 0` half.
   That asymmetry is the finding: run the mutation in both directions before believing the test.
2. Add `presence={props.rivalPresence ?? null}` to **your own** plate as well → `puts no presence on
   your own plate, from either seat` fails on the count, `2` against `1`, and on `Your turn`.
   Revert.

Quote both in the PR, including which half of test 1 survived mutation 1.

## Acceptance criteria

- [ ] `the duel table > puts the presence on the rival, from either seat` passes
- [ ] `the duel table > puts no presence on your own plate, from either seat` passes
- [ ] Both added tests assert at `viewerSeat: 0` **and** `viewerSeat: 1`
- [ ] The nine tests already in the file are byte-identical to `develop`
- [ ] `DuelTable.tsx` contains no literal seat index and no literal `Away` or `Timed out`
- [ ] `no-derivation.test.tsx` is unchanged from `develop` and still passes
- [ ] `npm run --silent test` reports `Tests  609 passed (609)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
