---
schema: 2
id: TASK-030710
title: The bar shows no number and offers no action the turn did not carry
type: task
status: ready
parent: STORY-0307
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [client, duel, test]
depends_on: [TASK-030709]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +230 passed \(230\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows no number the turn does not carry'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers no control the turn did not allow'
  - cd web-client && npm run check
---

## Goal

One whole-bar guard, the counterpart of `TASK-030615`'s for the table: every figure the bar puts in
front of a player is a figure the server sent, and every control it offers is an action the server
allowed.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/bar-no-derivation.test.tsx` | create |
| `web-client/src/table/no-derivation.test.tsx` | read — the scanning helpers this narrows |
| `web-client/src/table/turn-fixture.ts` | read — `aTurn`, `aLegalActions` |

## Scope

- The whole file, verbatim. It is already in Prettier's shape; run `npm run format` and expect no
  diff:

  ```tsx
  import { render, screen } from "@testing-library/react";
  import { describe, it, expect, vi } from "vitest";
  import { ActionBar } from "./ActionBar";
  import { aLegalActions, aTurn } from "./turn-fixture";

  /**
   * Every number the bar puts in front of the player, printed or spoken.
   *
   * `aria-label` is read aloud and `title` is shown on hover, so a figure the
   * client worked out for itself reaches a player from either just as surely as
   * from print — the table's guard learned that the expensive way.
   */
  function numbersOnScreen(container: HTMLElement): number[] {
    const walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT);
    const parts: string[] = [];
    for (let node = walker.nextNode(); node !== null; node = walker.nextNode()) {
      parts.push(node.textContent ?? "");
    }
    const spoken = [...container.querySelectorAll("[aria-label], [title]")]
      .flatMap((element) => [
        element.getAttribute("aria-label"),
        element.getAttribute("title"),
      ])
      .filter((value): value is string => value !== null);
    const digits = [...parts, ...spoken].join(" ").match(/\d[\d,]*/g) ?? [];
    return digits.map((run) => Number(run.replaceAll(",", "")));
  }

  describe("the bar offers and derives nothing", () => {
    it("shows no number the turn does not carry", () => {
      const turn = aTurn();
      const { container } = render(
        <ActionBar turn={turn} rejection={null} refusal={null} send={vi.fn()} />,
      );

      const allowed = new Set([
        turn.legalActions.callTo,
        turn.legalActions.minBetTo,
        turn.legalActions.minRaiseTo,
        turn.legalActions.allInTo,
      ]);
      const shown = numbersOnScreen(container);

      expect(shown.length).toBeGreaterThan(0);
      expect(shown.filter((n) => !allowed.has(n))).toEqual([]);
    });

    it("offers no control the turn did not allow", () => {
      const { container } = render(
        <ActionBar
          turn={aTurn({ legalActions: aLegalActions({ allowed: ["CHECK"] }) })}
          rejection={null}
          refusal={null}
          send={vi.fn()}
        />,
      );

      expect(screen.getAllByRole("button").map((b) => b.textContent)).toEqual([
        "Check",
      ]);
      expect(screen.queryByRole("slider")).toBeNull();
      expect(container.textContent).not.toMatch(
        /\b(Fold|Call|Bet|Raise|All in)\b/,
      );
    });
  });
  ```

- The allowed set is the **four amounts only**. `handNumber` (14) and `actionSequence` (27) are
  deliberately outside it: they identify the turn, they are not figures a player is shown, and
  `TASK-030701`'s independence property is what stops either colliding with a legitimate amount.
- `shown.length` is asserted greater than nought first, so a bar that renders nothing at all cannot
  pass by having no numbers to check.

## Out of scope

- Widening the table's `no-derivation.test.tsx`. The bar is rendered beside `DuelTable`, not inside
  it (`TASK-030711`), so that file's fixture and its five tests are untouched by this story.
- Card names and hand talk. The bar draws no card and names no made hand; the table's guard already
  covers the surface that does.

## Tests

`web-client/src/table/bar-no-derivation.test.tsx`, describe block
`"the bar offers and derives nothing"`.

| Test | Proves |
| --- | --- |
| `shows no number the turn does not carry` | every digit run in the rendered bar — text, `aria-label` and `title` alike — is `callTo`, `minBetTo`, `minRaiseTo` or `allInTo`. A pot fraction, a stack, a hand number or an amount short of the minimum all land outside the set |
| `offers no control the turn did not allow` | given `allowed: ["CHECK"]` the bar has exactly one button, no slider, and the words `Fold`, `Call`, `Bet`, `Raise` and `All in` appear nowhere in it |

Two tests. Two hundred and twenty-eight exist, so the suite reports **230**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 230 passed (230)` | the two ran and the two hundred and twenty-eight before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — both were run against this exact test file:

1. Render the six `ActionType`s instead of `actions.allowed` → `offers no control the turn did not
   allow` fails with `expected [ 'Fold', 'Check', 'Call 400', …(3) ] to deeply equal [ 'Check' ]`.
   Revert.
2. Add the design's `½` chip as `Math.round(pot / 2)` — any figure not in the four — → `shows no
   number the turn does not carry` fails on the filtered array. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the bar offers and derives nothing > shows no number the turn does not carry` passes
- [ ] `the bar offers and derives nothing > offers no control the turn did not allow` passes
- [ ] `no-derivation.test.tsx` is byte-identical to `TASK-030618`'s version
- [ ] `npm run --silent test` reports `Tests  230 passed (230)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
