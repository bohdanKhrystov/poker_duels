---
schema: 2
id: TASK-030905
title: Whose rematch offer it is, read from the seat the server gave this client
type: task
status: backlog
parent: STORY-0309
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, result, rooms]
depends_on: [TASK-030904]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +548 passed \(548\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads an offer from your own seat as yours'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads the very same offer from the other side as your rivals'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads an offer from seat zero the same way round'
  - cd web-client && npm run check
---

## Goal

One pure function turns the seats the server named into *yours* and *your rival's*, so no component
ever compares a seat number by hand.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/rematch-stand.ts` | create |
| `web-client/src/result/rematch-stand.test.ts` | create |

## Scope

- One exported interface and one exported function, nothing else:

  ```ts
  export interface RematchStand {
    readonly mine: boolean;
    readonly theirs: boolean;
  }

  export function rematchStand(
    offers: readonly number[],
    mySeat: number | null,
  ): RematchStand;
  ```

- `mine` is `mySeat !== null && offers.includes(mySeat)`.
- `theirs` is `mySeat !== null && offers.some((seat) => seat !== mySeat)`.
- **Both false when `mySeat` is `null`**, with a comment saying why: a client that never received
  `RoomJoined` holds no seat, cannot tell whose an offer is, and has no room to offer one in —
  `ADR-0044` §1 puts the room on the socket, not on the frame.
- Nothing else. No React, no strings, no `ProtocolError`, no default parameters.

## Out of scope

- Rendering. `TASK-030907`–`TASK-030909` own the control.
- The words. They live in `RematchControl.tsx` beside the markup, as `DuelResult.tsx`'s own copy
  does.
- Any notion of "both have offered means it has begun". The `Snapshot` is what says that
  (`ADR-0044` §4, `TASK-030903`), and this function makes no claim about it.

## Tests

`web-client/src/result/rematch-stand.test.ts`, describe block `"whose rematch offer it is"`. Six.

The whole risk this function carries is a seat number that is only ever exercised at one value:
`STORY-0213` shipped nine server tests of which eight passed against a hard-coded `seat = 0`. So
the first three tests hold the **offers array constant and flip the viewer**, and the third repeats
the flip around seat 0, so neither seat number can be the constant.

| Test | Proves |
| --- | --- |
| `reads an offer from your own seat as yours` | `[1]`, `mySeat` 1 ⇒ `{ mine: true, theirs: false }` |
| `reads the very same offer from the other side as your rivals` | `[1]`, `mySeat` 0 ⇒ `{ mine: false, theirs: true }` — the identical `offers` array, one viewer moved, the opposite reading |
| `reads an offer from seat zero the same way round` | `[0]`, `mySeat` 0 ⇒ mine; `[0]`, `mySeat` 1 ⇒ theirs. With the test above, neither `0` nor `1` can be hard-coded anywhere |
| `reads an offer from each seat as one apiece` | `[0, 1]` ⇒ `{ mine: true, theirs: true }` from **either** seat |
| `claims neither offer for a client that holds no seat` | `[0, 1]`, `mySeat` `null` ⇒ `{ mine: false, theirs: false }` |
| `claims nothing before anyone has offered` | `[]` ⇒ `{ mine: false, theirs: false }` from seat 0 and from seat 1 |

Note the third test's name in `verify` is grepped without the apostrophe in *rival's* — write the
name exactly as the table spells it: `reads the very same offer from the other side as your rivals`.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 548 passed (548)` | six added to 542 |
| the three `--reporter=verbose` greps | the three seat-flip names exist |

**Name the edit that makes each assertion red:**

1. Replace `mySeat` with the literal `0` in `mine` → `reads an offer from your own seat as yours`
   fails. Revert.
2. Replace `theirs` with `offers.length > 0` → `reads an offer from your own seat as yours` fails,
   because your own offer would read as your rival's. Revert.
3. Drop the `mySeat !== null` guard → `claims neither offer for a client that holds no seat` fails.
   Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `whose rematch offer it is > reads an offer from your own seat as yours` passes
- [ ] `whose rematch offer it is > reads the very same offer from the other side as your rivals` passes
- [ ] `whose rematch offer it is > reads an offer from seat zero the same way round` passes
- [ ] `whose rematch offer it is > reads an offer from each seat as one apiece` passes
- [ ] `whose rematch offer it is > claims neither offer for a client that holds no seat` passes
- [ ] `whose rematch offer it is > claims nothing before anyone has offered` passes
- [ ] `rematch-stand.ts` exports exactly `rematchStand` and the `RematchStand` type, and imports nothing from `react`
- [ ] `rematch-stand.ts` contains no numeric literal
- [ ] `npm run --silent test` reports `Tests  548 passed (548)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
