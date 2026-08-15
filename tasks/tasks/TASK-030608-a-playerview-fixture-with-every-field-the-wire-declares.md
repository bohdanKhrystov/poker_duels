---
schema: 2
id: TASK-030608
title: A PlayerView fixture with every field the wire declares
type: task
status: done
parent: STORY-0306
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, duel, testing]
depends_on: [TASK-030607]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +162 passed \(162\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'carries every field a PlayerView declares'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'lets a test bend any field it names'
  - cd web-client && npm run check
---

## Goal

One builder for the twelve-field `PlayerView` and the seven-field `SeatView`, so the six test files
after this one bend one field at a time instead of each retyping a literal that `tsc` rejects and
Vitest never sees.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/view-fixture.ts` | create |
| `web-client/src/table/view-fixture.test.ts` | create |
| `web-client/src/protocol/protocol.gen.ts` | read — the `PlayerView`, `SeatView` and `Board` field lists |

## Scope

- A test double that lives in `src/`, in the tradition of `src/protocol/fake-socket.ts`: source a
  test drives, with its own test, not a `.test.ts` file.
- The whole module, verbatim:

  ```ts
  import type { PlayerView, SeatView } from "../protocol";

  /**
   * A `SeatView` carrying every field the wire declares, for a test to bend.
   *
   * A hand-written literal that misses a field is a `tsc` error the test runner
   * never sees, so the fixture is written once — in the tradition of the protocol
   * module's `FakeSocket`, which is also source a test drives rather than a test.
   */
  export function aSeat(overrides: Partial<SeatView> = {}): SeatView {
    return {
      index: 0,
      stack: 500,
      committedThisStreet: 0,
      committedThisHand: 0,
      hasFolded: false,
      isAllIn: false,
      holeCards: [],
      ...overrides,
    };
  }

  /** A `PlayerView` carrying every field the wire declares, for a test to bend. */
  export function aView(overrides: Partial<PlayerView> = {}): PlayerView {
    return {
      viewerSeat: 0,
      handNumber: 1,
      buttonSeat: 0,
      street: "PREFLOP",
      board: { cards: [] },
      pot: 30,
      betToMatch: 20,
      minRaiseTo: 40,
      seatToAct: 0,
      smallBlind: 10,
      bigBlind: 20,
      seats: [aSeat({ index: 0 }), aSeat({ index: 1 })],
      ...overrides,
    };
  }
  ```

  The defaults are `Lobby.test.tsx`'s existing `SNAPSHOT` fixture, so the two agree.
- **Types come in by `import type … from "../protocol"`.** Importing a wire type is exactly what
  `src/protocol/boundary.ts` is there to require; *declaring* one is what it forbids, so nothing here
  may be named `PlayerView`, `SeatView` or `Board`.

## Out of scope

- A `ServerMessage` or `Snapshot` builder. Nothing in this story applies a frame to a store —
  components take a `view` prop — and `Lobby.test.tsx` already has its own `SNAPSHOT`.
- Changing `Lobby.test.tsx` to use this. Its fixtures are `STORY-0305`'s and stay where they are.

## Tests

`web-client/src/table/view-fixture.test.ts`, describe block `"the view fixture"`.

| Test | Proves |
| --- | --- |
| `carries every field a SeatView declares` | `Object.keys(aSeat()).sort()` equals the seven names, spelled out in the test |
| `carries every field a PlayerView declares` | `Object.keys(aView()).sort()` equals the twelve names, spelled out in the test |
| `seats two players, indexed nought and one` | `aView().seats.map((seat) => seat.index)` is `[0, 1]` |
| `lets a test bend any field it names` | `aView({ pot: 4850 }).pot` is `4850`, `aView({ street: "RIVER" }).street` is `"RIVER"`, `aSeat({ index: 1, holeCards: ["Ah"] }).holeCards` is `["Ah"]`, and `aSeat({ stack: 13000 }).committedThisHand` is still `0` |

Four tests. One hundred and fifty-eight exist, so the suite reports **162**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 162 passed (162)` | the four ran and the hundred-and-fifty-eight before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks — and typechecking is half of what this fixture buys |

**Name the edit that makes each assertion red** — both were run against this exact test file:

1. Drop `...overrides` from `aSeat` → `seats two players, indexed nought and one` fails with
   `expected [ +0, +0 ] to deeply equal [ +0, 1 ]` and `lets a test bend any field it names` with
   `expected [] to deeply equal [ 'Ah' ]`; `tsc` additionally reports
   `error TS6133: 'overrides' is declared but its value is never read`. Revert.
2. Delete the `committedThisHand: 0,` line → `carries every field a SeatView declares` fails with
   `expected [ 'committedThisStreet', …(5) ] to deeply equal [ 'committedThisHand', …(6) ]`, and
   `tsc` reports `error TS2322: Type '{ … committedThisHand?: number | undefined; … }' is not
   assignable to type 'SeatView'`. Revert. That pair is the point: the runner sees the missing key,
   the compiler sees the missing field.

Quote both in the PR.

## Acceptance criteria

- [ ] `the view fixture > carries every field a SeatView declares` passes
- [ ] `the view fixture > carries every field a PlayerView declares` passes
- [ ] `the view fixture > seats two players, indexed nought and one` passes
- [ ] `the view fixture > lets a test bend any field it names` passes
- [ ] `view-fixture.ts` declares no type or interface at all
- [ ] `npm run --silent test` reports `Tests  162 passed (162)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
