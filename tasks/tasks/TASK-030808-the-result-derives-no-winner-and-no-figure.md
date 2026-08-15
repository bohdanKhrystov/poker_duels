---
schema: 2
id: TASK-030808
title: The result derives no winner and shows no figure the outcome did not carry
type: task
status: backlog
parent: STORY-0308
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [client, duel, test, result]
depends_on: [TASK-030807]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +273 passed \(273\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows no number the outcome does not carry'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads the verdict off the winner, never off the stacks'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names no card and no made hand'
  - cd web-client && npm run check
---

## Goal

The third whole-surface guard, after the table's and the bar's: every figure the result panel puts
in front of a player is a figure the server sent, and the verdict it declares is the server's, not
one it worked out from the chips.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/result/result-no-derivation.test.tsx` | create |
| `web-client/src/table/no-derivation.test.tsx` | read — the scanning helpers this narrows, and why each exists |
| `web-client/src/result/outcome-fixture.ts` | read — `anOutcome` and its independence |

## Scope

- The whole file. It is already in Prettier's shape; run `npm run format` and expect no diff.

  ```tsx
  import { render, screen } from "@testing-library/react";
  import { describe, it, expect } from "vitest";
  import { DuelResult } from "./DuelResult";
  import { anOutcome } from "./outcome-fixture";

  /**
   * Every text node under `container`, joined by a space.
   *
   * Not `textContent`: that runs the last word of one element into the first of
   * the next, and `\b` then fails to see a boundary the player's eye sees
   * plainly. The table's guard says the same thing at more length.
   */
  function wordsOnScreen(container: HTMLElement): string {
    const walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT);
    const parts: string[] = [];
    for (let node = walker.nextNode(); node !== null; node = walker.nextNode()) {
      parts.push(node.textContent ?? "");
    }
    return parts.join(" ");
  }

  /** Every string the panel speaks without printing it: `aria-label` and `title`. */
  function spokenOnScreen(container: HTMLElement): string {
    return [...container.querySelectorAll("[aria-label], [title]")]
      .flatMap((element) => [
        element.getAttribute("aria-label"),
        element.getAttribute("title"),
      ])
      .filter((value): value is string => value !== null)
      .join(" ");
  }

  /**
   * Every number the panel puts in front of the player: printed, spoken, or
   * carried as an attribute nothing prints.
   *
   * All three, because the table's guard learned each the expensive way —
   * `aria-label` is read aloud, `title` is shown on hover, and `min`/`max`/
   * `value` reach the DOM with no echo in either.
   */
  function numbersOnScreen(container: HTMLElement): number[] {
    const attributes = [
      ...container.querySelectorAll("[aria-label], [title], [min], [max], [value]"),
    ].flatMap((element) =>
      ["aria-label", "title", "min", "max", "value"]
        .map((name) => element.getAttribute(name))
        .filter((value): value is string => value !== null),
    );
    const digits =
      [wordsOnScreen(container), ...attributes].join(" ").match(/\d[\d,]*/g) ??
      [];
    return digits.map((run) => Number(run.replaceAll(",", "")));
  }

  /** A card named in words, spelled as `cardText` spells it. */
  const CARD_NAME =
    /\b(ace|king|queen|jack|ten|nine|eight|seven|six|five|four|three|two) of (spades|hearts|diamonds|clubs)\b/i;

  /** Made hands — the vocabulary of *how* a duel was won, which no field carries. */
  const MADE_HAND =
    /\b(pair|trips|set|straight|flush|full house|quads|high card|kicker|showdown)\b/i;

  describe("the result declares and derives nothing", () => {
    it("shows no number the outcome does not carry", () => {
      const outcome = anOutcome();
      const { container } = render(
        <DuelResult outcome={outcome} mySeat={0} />,
      );

      // The coin's 1 is the one figure here that is not a field: ADR-0014 fixes
      // it, `coinLine` states it, and `outcome-fixture` keeps every other number
      // clear of it. The winner's seat is deliberately *not* allowed — it is 0
      // in the fixture, so a panel that printed which seat won lands outside.
      const allowed = new Set([outcome.handsPlayed, ...outcome.finalStacks, 1]);
      const shown = numbersOnScreen(container);

      expect(shown.length).toBeGreaterThan(0);
      expect(shown.filter((n) => !allowed.has(n))).toEqual([]);

      // The sweep's own reach, proven rather than assumed: a figure that arrives
      // as an attribute and is printed nowhere must still be counted.
      const probe = document.createElement("input");
      probe.setAttribute("max", "987654");
      container.appendChild(probe);
      expect(numbersOnScreen(container)).toContain(987654);
    });

    it("reads the verdict off the winner, never off the stacks", () => {
      // Level stacks: no comparison of chips can tell these two renders apart,
      // so the only thing left that can is the field the server sent.
      const level = anOutcome({ winner: 0, finalStacks: [12000, 12000] });

      const first = render(<DuelResult outcome={level} mySeat={0} />);
      expect(screen.getByRole("heading", { name: "Victory" })).toBeDefined();
      first.unmount();

      render(<DuelResult outcome={level} mySeat={1} />);
      expect(screen.getByRole("heading", { name: "Defeat" })).toBeDefined();
      expect(screen.queryByRole("heading", { name: "Victory" })).toBeNull();
    });

    it("names no card and no made hand", () => {
      const { container } = render(
        <DuelResult outcome={anOutcome()} mySeat={0} />,
      );

      for (const surface of [
        wordsOnScreen(container),
        spokenOnScreen(container),
      ]) {
        expect(surface).not.toMatch(CARD_NAME);
        expect(surface).not.toMatch(MADE_HAND);
      }
    });
  });
  ```

- The forbidden vocabulary here is **cards and made hands**, not winner-talk: unlike the table, this
  screen declares a winner on purpose, and the word it declares comes off the wire. What it may
  never say is *how* the duel was won — `DuelOutcome` is `{winner, handsPlayed, finalStacks}` and
  carries no card, no rank and no showdown.

## Out of scope

- Widening `no-derivation.test.tsx` or `bar-no-derivation.test.tsx`. The panel replaces the table
  rather than sitting inside it (`TASK-030809`), so both files stay byte-identical.
- Rendering anything. Every behaviour asserted here is already merged by `TASK-030805`–
  `TASK-030807`; if one of these tests is red, the bug is in those files, not in this one.

## Tests

`web-client/src/result/result-no-derivation.test.tsx`, describe block
`"the result declares and derives nothing"`.

| Test | Proves |
| --- | --- |
| `shows no number the outcome does not carry` | every digit run the panel emits — text, `aria-label`, `title`, `min`, `max`, `value` — is `handsPlayed`, a final stack, or the coin's `1`. A chip total, a difference, a balance, a percentage or a seat index all land outside the set. Ends by proving the sweep reaches attributes at all |
| `reads the verdict off the winner, never off the stacks` | with both stacks equal, seat `0` reads *Victory* and seat `1` reads *Defeat* from the same outcome — a verdict computed from chips cannot produce two different answers from one pair of equal stacks |
| `names no card and no made hand` | neither the printed text nor the spoken text matches a card name or the vocabulary of a made hand |

Three tests. Two hundred and seventy exist, so the suite reports **273**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 273 passed (273)` | the three ran and the two hundred and seventy before them still do |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks and lints the new file |

**Name the edit that makes each assertion red** — run all three against this exact file:

1. Add the chip total to the meta line — `formatChips(outcome.finalStacks.reduce((a, b) => a + b, 0))` → `shows no number the outcome does not carry` fails on the filtered array with `[24000]`. Revert.
2. Decide the verdict by stacks, tie-breaking to a win — `finalStacks[mySeat] >= finalStacks[1 - mySeat]` → `reads the verdict off the winner, never off the stacks` fails: both seats read *Victory*. Revert.
3. Add `title="two pair"` to the heading → `names no card and no made hand` fails on the spoken surface. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `the result declares and derives nothing > shows no number the outcome does not carry` passes
- [ ] `the result declares and derives nothing > reads the verdict off the winner, never off the stacks` passes
- [ ] `the result declares and derives nothing > names no card and no made hand` passes
- [ ] `no-derivation.test.tsx` and `bar-no-derivation.test.tsx` are byte-identical to their merged versions
- [ ] `npm run --silent test` reports `Tests  273 passed (273)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
