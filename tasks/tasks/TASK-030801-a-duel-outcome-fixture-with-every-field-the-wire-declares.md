---
schema: 2
id: TASK-030801
title: A DuelOutcome fixture with every field the wire declares
type: task
status: ready
parent: STORY-0308
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, duel, test]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +250 passed \(250\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'carries every field DuelOutcome declares'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps its numbers independent of one another, and of the coin'
  - cd web-client && npm run check
---

## Goal

The result screen's tests get one `DuelOutcome` to bend, whose numbers are independent enough that a
figure the screen works out for itself cannot collide with a legitimate one.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/result/outcome-fixture.ts` | create |
| `web-client/src/result/outcome-fixture.test.ts` | create |
| `web-client/src/table/turn-fixture.ts` | read — the fixture shape this copies |
| `web-client/src/table/turn-fixture.test.ts` | read — the independence property this copies |

`web-client/src/result/` is new and is this story's home, beside `src/lobby/` and `src/table/`. No
config names it: Vitest, ESLint, Prettier and the colour-literal guard all walk `src/`.

## Scope

- The whole of `outcome-fixture.ts`, verbatim:

  ```ts
  import type { DuelOutcome } from "../protocol";

  /**
   * A `DuelOutcome` carrying every field the wire declares, for a test to bend.
   *
   * Its three numbers — the hand count, both final stacks — are mutually
   * independent, and so is the `1` the coin moves by: no two of them add,
   * subtract, double or halve into a third. A figure the result screen worked
   * out for itself therefore lands outside the set instead of colliding with a
   * legitimate one.
   */
  export function anOutcome(overrides: Partial<DuelOutcome> = {}): DuelOutcome {
    return {
      winner: 0,
      handsPlayed: 17,
      finalStacks: [19400, 4600],
      ...overrides,
    };
  }
  ```

- `winner` is a **seat index**, not a figure: it is deliberately outside the independence property,
  exactly as `LegalActions.seat` is outside `turn-fixture.ts`'s. `0` is chosen so that a screen
  printing the winner's seat prints a number the guard's allowed set does not hold
  (`TASK-030808`).
- Neither stack is `0`: a zero stack would make the pair's sum equal the other stack, and a screen
  totalling the chips would pass.

## Out of scope

- Any component. This is source a test drives, in the tradition of `turn-fixture.ts` and
  `view-fixture.ts`, and nothing imports it outside a test.
- A `DuelFinished` fixture. The store already folds that frame (`TASK-030406`); the screens take the
  outcome, not the frame.

## Tests

`web-client/src/result/outcome-fixture.test.ts`, describe block `"the outcome fixture"`.

| Test | Proves |
| --- | --- |
| `carries every field DuelOutcome declares` | `Object.keys(anOutcome()).sort()` equals `["finalStacks", "handsPlayed", "winner"]` — a hand-written literal that misses a field is a `tsc` error no test runner sees |
| `lets a test bend any field it names` | `anOutcome({ winner: null }).winner` is `null`, `anOutcome({ handsPlayed: 3 }).handsPlayed` is `3`, and `anOutcome({ winner: 1, handsPlayed: 3 }).finalStacks` is still `[19400, 4600]` |
| `keeps its numbers independent of one another, and of the coin` | over `[handsPlayed, ...finalStacks, 1]`, no value equals another doubled, any two summed, or any two subtracted |

The third is `turn-fixture.test.ts`'s property with the coin's `1` added to the list, because `+1`
and `−1` are the only figures the result screen prints that the outcome does not carry:

```ts
it("keeps its numbers independent of one another, and of the coin", () => {
  const outcome = anOutcome();
  // The coin's 1 is in the list: it is the one figure ADR-0014 fixes and the
  // screen prints, so a derived total that happened to equal it would be
  // invisible to TASK-030808's guard.
  const money = [outcome.handsPlayed, ...outcome.finalStacks, 1];
  for (const a of money) {
    expect(money).not.toContain(a * 2);
    for (const b of money) {
      if (a === b) continue;
      expect(money).not.toContain(a + b);
      expect(money).not.toContain(Math.abs(a - b));
    }
  }
});
```

Three tests. Two hundred and forty-seven exist, so the suite reports **250**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 250 passed (250)` | the three ran and the two hundred and forty-seven before them still do |
| the two `--reporter=verbose` greps | the fixture test and the independence property exist by name |
| `npm run check` | typechecks — `Partial<DuelOutcome>` spreads over a complete literal |

**Name the edit that makes an assertion red:** change `finalStacks` to `[19400, 0]` → `keeps its
numbers independent of one another, and of the coin` fails, because `19400 + 0` is `19400`. Revert.
Quote it in the PR.

## Acceptance criteria

- [ ] `the outcome fixture > carries every field DuelOutcome declares` passes
- [ ] `the outcome fixture > lets a test bend any field it names` passes
- [ ] `the outcome fixture > keeps its numbers independent of one another, and of the coin` passes
- [ ] `outcome-fixture.ts` imports its type from `../protocol` and declares no wire type of its own
- [ ] `npm run --silent test` reports `Tests  250 passed (250)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
