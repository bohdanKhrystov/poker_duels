---
schema: 2
id: TASK-030702
title: Each action says its verb and carries the server's figure
type: task
status: ready
parent: STORY-0307
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, duel, ui]
depends_on: [TASK-030701]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +199 passed \(199\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names each of the six actions the wire declares'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'prices a call from the server'
  - cd web-client && npm run check
---

## Goal

One place turns an `ActionType` into what a button says: the verb, and the figure beside it when
there is one. Every figure it returns came off the wire or off the player's own control.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/action-text.ts` | create |
| `web-client/src/table/action-text.test.ts` | create |
| `web-client/src/table/turn-fixture.ts` | read — `aLegalActions` |
| `design/components/action-bar.html` | read — the `.btn` and `.amt` wording. **Read only: never edit anything under `design/`** |

## Scope

- The whole file, verbatim. It is already in Prettier's shape; run `npm run format` and expect no
  diff:

  ```ts
  import type { ActionType, LegalActions } from "../protocol";

  /** What a button says: the verb, and the figure beside it when there is one. */
  export interface ActionText {
    readonly verb: string;
    readonly amount: number | null;
  }

  /**
   * One action, in the player's language.
   *
   * A translation of the server's own token and nothing more: no action gains a
   * verb the server did not name, and none loses one.
   */
  export function actionVerb(type: ActionType): string {
    switch (type) {
      case "FOLD":
        return "Fold";
      case "CHECK":
        return "Check";
      case "CALL":
        return "Call";
      case "BET":
        return "Bet";
      case "RAISE":
        return "Raise to";
      case "ALL_IN":
        return "All in";
    }
  }

  /**
   * What one button says, given the turn the server opened and the total the
   * player has dialled in.
   *
   * Every figure here is the server's or the player's own: `callTo` and `allInTo`
   * came off the wire, and `to` is what the player set on the amount control.
   * Nothing is priced, netted or worked out.
   */
  export function actionText(
    type: ActionType,
    actions: LegalActions,
    to: number,
  ): ActionText {
    switch (type) {
      case "CALL":
        return { verb: actionVerb(type), amount: actions.callTo };
      case "ALL_IN":
        return { verb: actionVerb(type), amount: actions.allInTo };
      case "BET":
      case "RAISE":
        return { verb: actionVerb(type), amount: to };
      default:
        return { verb: actionVerb(type), amount: null };
    }
  }
  ```

- `"Raise to"` is the design's own wording (`Raise to 1,200`), and it is the wording that makes the
  amount a **street total** rather than an increment. Do not shorten it to `"Raise"`.
- No arithmetic appears in this file. `callTo` and `allInTo` are printed as sent; `to` is passed
  through untouched.

## Out of scope

- Formatting the figure. `formatChips` (`TASK-030601`) does that where it is rendered, so this
  module returns a number and never a string.
- Deciding which actions exist. That is `legalActions.allowed`, and `TASK-030705` reads it.
- The `Rejection` wording — `TASK-030708`, which imports `actionVerb` from here.

## Tests

`web-client/src/table/action-text.test.ts`, describe block `"the action text"`.

| Test | Proves |
| --- | --- |
| `names each of the six actions the wire declares` | mapping `actionVerb` over all six `ActionType`s gives `Fold`, `Check`, `Call`, `Bet`, `Raise to`, `All in` — the whole union, enumerated, not a sample |
| `prices a call from the server's callTo` | `actionText("CALL", aLegalActions(), 9999)` is `{ verb: "Call", amount: 400 }` — the dialled-in total is ignored |
| `prices an all-in from the server's allInTo` | the same, giving `13400` |
| `prices a bet and a raise from the total the player dialled in` | both return `3250` when `to` is `3250` |
| `puts no figure on a fold or a check` | both amounts are `null` |

```ts
it("names each of the six actions the wire declares", () => {
  const named: ActionType[] = ["FOLD", "CHECK", "CALL", "BET", "RAISE", "ALL_IN"];
  expect(named.map(actionVerb)).toEqual([
    "Fold",
    "Check",
    "Call",
    "Bet",
    "Raise to",
    "All in",
  ]);
});
```

Five tests. One hundred and ninety-four exist, so the suite reports **199**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 199 passed (199)` | the five ran and the hundred-and-ninety-four before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | the switch is exhaustive over the generated union, so a seventh `ActionType` would fail to typecheck |

**Name the edit that makes each assertion red:**

1. Return `actions.callTo` for `BET` → `prices a bet and a raise from the total the player dialled
   in` fails with `expected 400 to be 3250`. Revert.
2. Return `"Raise"` for `RAISE` → `names each of the six actions the wire declares` fails on the
   array comparison. Revert.
3. Delete the `"ALL_IN"` case → `npm run check` fails: not every code path returns a value. Revert.

## Acceptance criteria

- [ ] `the action text > names each of the six actions the wire declares` passes
- [ ] `the action text > prices a call from the server's callTo` passes
- [ ] `the action text > prices an all-in from the server's allInTo` passes
- [ ] `the action text > prices a bet and a raise from the total the player dialled in` passes
- [ ] `the action text > puts no figure on a fold or a check` passes
- [ ] `action-text.ts` contains no `+`, `-`, `*` or `/` between two numbers
- [ ] `npm run --silent test` reports `Tests  199 passed (199)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
