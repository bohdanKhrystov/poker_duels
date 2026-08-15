---
schema: 2
id: TASK-030701
title: A turn fixture with every field the wire declares
type: task
status: done
parent: STORY-0307
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, duel, test]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +194 passed \(194\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'carries every field LegalActions declares'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps its amounts independent of one another'
  - cd web-client && npm run check
---

## Goal

Every test in this story builds its `LegalActions` and its `PendingTurn` from one place, and the
amounts in it are chosen so that a figure the bar works out for itself cannot collide with a
legitimate one.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/turn-fixture.ts` | create |
| `web-client/src/table/turn-fixture.test.ts` | create |
| `web-client/src/table/view-fixture.ts` | read — the shape this copies |

## Scope

- The whole file, verbatim. It is already in Prettier's shape; run `npm run format` and expect no
  diff:

  ```ts
  import type { LegalActions } from "../protocol";
  import type { PendingTurn } from "../store/duel-state";

  /**
   * A `LegalActions` carrying every field the wire declares, for a test to bend.
   *
   * The four amounts are mutually independent — no two of them add, subtract,
   * double or halve into a third — so a figure the bar works out for itself lands
   * outside the set instead of colliding with a legitimate one.
   */
  export function aLegalActions(
    overrides: Partial<LegalActions> = {},
  ): LegalActions {
    return {
      seat: 0,
      allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
      callTo: 400,
      minBetTo: 175,
      minRaiseTo: 1200,
      allInTo: 13400,
      ...overrides,
    };
  }

  /** A `PendingTurn` the store would hold after one `YourTurn`, for a test to bend. */
  export function aTurn(overrides: Partial<PendingTurn> = {}): PendingTurn {
    return {
      handNumber: 14,
      actionSequence: 27,
      legalActions: aLegalActions(),
      ...overrides,
    };
  }
  ```

- Both types are imported, never re-declared: `LegalActions` is generated (`ADR-0020`) and
  `PendingTurn` is the store's, from `TASK-030402`.
- The default `allowed` is the four-action set the engine actually produces facing a bet
  (`BettingRules` adds `ALL_IN` whenever the opponent is contestable, so four is the norm and not
  an edge case).

## Out of scope

- A `Rejection` fixture. Each variant is one literal in `TASK-030708`'s tests, and a fixture that
  builds a union member is a fixture that picks one.
- Any component. Nothing renders until `TASK-030704`.

## Tests

`web-client/src/table/turn-fixture.test.ts`, describe block `"the turn fixture"`.

| Test | Proves |
| --- | --- |
| `carries every field LegalActions declares` | `Object.keys(aLegalActions()).sort()` is the six wire names |
| `carries every field a pending turn declares` | `Object.keys(aTurn()).sort()` is `actionSequence`, `handNumber`, `legalActions` |
| `lets a test bend any field it names` | an override lands and the neighbouring fields do not move |
| `keeps its amounts independent of one another` | over `callTo`, `minBetTo`, `minRaiseTo`, `allInTo`, `handNumber` and `actionSequence`: no value is another's double, and no value is any pair's sum or difference |

```ts
it("keeps its amounts independent of one another", () => {
  const actions = aLegalActions();
  const money = [
    actions.callTo,
    actions.minBetTo,
    actions.minRaiseTo,
    actions.allInTo,
    aTurn().handNumber,
    aTurn().actionSequence,
  ];
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

Four tests. One hundred and ninety exist, so the suite reports **194**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 194 passed (194)` | the four ran and the hundred-and-ninety before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks against the generated `LegalActions`, lints, formats |

**Name the edit that makes each assertion red:**

1. Drop `minBetTo` from `aLegalActions` → `npm run check` fails to typecheck, and
   `carries every field LegalActions declares` fails on the key list. Revert.
2. Set `callTo: 600` → `keeps its amounts independent of one another` fails, because
   `1200 = 600 × 2`. Revert. That is the assertion earning its keep: the fixture's independence is
   what `TASK-030710`'s guard rests on, and the table's equivalent had already rotted once.

## Acceptance criteria

- [ ] `the turn fixture > carries every field LegalActions declares` passes
- [ ] `the turn fixture > carries every field a pending turn declares` passes
- [ ] `the turn fixture > lets a test bend any field it names` passes
- [ ] `the turn fixture > keeps its amounts independent of one another` passes
- [ ] `turn-fixture.ts` declares no wire type of its own — it imports `LegalActions` from
      `../protocol` and `PendingTurn` from `../store/duel-state`
- [ ] `npm run --silent test` reports `Tests  194 passed (194)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
