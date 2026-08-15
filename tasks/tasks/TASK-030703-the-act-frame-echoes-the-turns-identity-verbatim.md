---
schema: 2
id: TASK-030703
title: The Act frame echoes the turn's identity verbatim
type: task
status: ready
parent: STORY-0307
module: web-client
estimate: S
tier: haiku
review: deep
files_touched: 2
labels: [client, duel, protocol]
depends_on: [TASK-030702]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +204 passed \(204\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "copies the turn's identity into the frame"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends a bet and a raise as a street total'
  - cd web-client && npm run check
---

## Goal

The one function in this client that builds the one frame in which a player asserts anything:
`Act`, carrying the `handNumber` and `actionSequence` of the `YourTurn` that opened the turn, and a
`PlayerAction` seated where the server said.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/act-frame.ts` | create |
| `web-client/src/table/act-frame.test.ts` | create |
| `web-client/src/table/turn-fixture.ts` | read — `aTurn`, `aLegalActions` |
| `web-client/src/protocol/protocol.gen.ts` | read — `Act`, `PlayerAction`. **Read only: this file is generated and byte-compared in CI** |

## Scope

- The whole file, verbatim. It is already in Prettier's shape; run `npm run format` and expect no
  diff:

  ```ts
  import type { Act, ActionType, PlayerAction } from "../protocol";
  import type { PendingTurn } from "../store/duel-state";

  /**
   * The one frame in which this client asserts anything.
   *
   * `handNumber` and `actionSequence` are copied from the `YourTurn` that opened
   * the turn — never from the view, which the server may have replaced since —
   * and the seat is `legalActions.seat`, which is the server's own word for who
   * is being asked. The client never names its seat from anywhere else.
   *
   * @param turn The pending turn the server opened.
   * @param type The action the player chose, as the server named it.
   * @param to The **total committed on this street** after a bet or a raise, not
   *   the amount added: the field is named `to` for that reason, and a delta is
   *   rejected as `AmountTooSmall`.
   */
  export function actFrame(turn: PendingTurn, type: ActionType, to: number): Act {
    return {
      type: "Act",
      handNumber: turn.handNumber,
      actionSequence: turn.actionSequence,
      action: playerAction(type, turn.legalActions.seat, to),
    };
  }

  function playerAction(
    type: ActionType,
    seat: number,
    to: number,
  ): PlayerAction {
    switch (type) {
      case "FOLD":
        return { type: "Fold", seat };
      case "CHECK":
        return { type: "Check", seat };
      case "CALL":
        return { type: "Call", seat };
      case "BET":
        return { type: "Bet", seat, to };
      case "RAISE":
        return { type: "Raise", seat, to };
      case "ALL_IN":
        return { type: "AllIn", seat };
    }
  }
  ```

- **The seat is `turn.legalActions.seat`.** Not `state.mySeat`, not `view.viewerSeat`, not a
  parameter the caller passes: the server named the seat it is asking, and echoing that is the only
  reading that cannot drift.
- `to` reaches `Bet` and `Raise` and nothing else. `Call` and `AllIn` carry no amount on the wire,
  so none is invented for them.

## Out of scope

- Sending. This function returns a frame; `TASK-030707` is what hands it to `send`.
- Deciding whether the action is legal, or whether `to` is within bounds. `LegalActions` came from
  the server and the server checks it again — a client-side pre-check would be the client deciding
  a rule.
- Anything about the view. This module never imports `PlayerView`.

## Tests

`web-client/src/table/act-frame.test.ts`, describe block `"the act frame"`.

| Test | Proves |
| --- | --- |
| `copies the turn's identity into the frame` | `handNumber` is `14` and `actionSequence` is `27` — the fixture's, unchanged |
| `takes the seat from the legal actions and nowhere else` | with `aLegalActions({ seat: 1 })`, the action's `seat` is `1` |
| `builds each of the six actions the wire declares` | mapping all six `ActionType`s gives `Fold`, `Check`, `Call`, `Bet`, `Raise`, `AllIn` — the whole union |
| `sends a bet and a raise as a street total` | both are `{ type, seat: 0, to: 3250 }` for `to = 3250`: the total, not `3250 − callTo` |
| `puts no amount on an action that carries none` | `ALL_IN` with `to = 3250` is exactly `{ type: "AllIn", seat: 0 }` |

```ts
it("builds each of the six actions the wire declares", () => {
  const built = (
    ["FOLD", "CHECK", "CALL", "BET", "RAISE", "ALL_IN"] as const
  ).map((type) => actFrame(aTurn(), type, 3250).action.type);
  expect(built).toEqual(["Fold", "Check", "Call", "Bet", "Raise", "AllIn"]);
});
```

Five tests. One hundred and ninety-nine exist, so the suite reports **204**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 204 passed (204)` | the five ran and the hundred-and-ninety-nine before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | the returned object typechecks as the generated `Act`, so a misspelled discriminator fails the build rather than the server |

**Name the edit that makes each assertion red** — both were run against this exact test file:

1. `handNumber: turn.handNumber + 1` → `copies the turn's identity into the frame` fails with
   `expected 15 to be 14`. Revert.
2. Send a delta — `to` becomes `to - actions.callTo` at the call site — → `sends a bet and a raise
   as a street total` fails on the frame comparison. Revert. This is the mistake the wire is named
   `to` to prevent, and the server answers it with `AmountTooSmall`.

Quote both in the PR.

## Acceptance criteria

- [ ] `the act frame > copies the turn's identity into the frame` passes
- [ ] `the act frame > takes the seat from the legal actions and nowhere else` passes
- [ ] `the act frame > builds each of the six actions the wire declares` passes
- [ ] `the act frame > sends a bet and a raise as a street total` passes
- [ ] `the act frame > puts no amount on an action that carries none` passes
- [ ] `act-frame.ts` contains no `mySeat`, no `viewerSeat` and no arithmetic
- [ ] `npm run --silent test` reports `Tests  204 passed (204)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
