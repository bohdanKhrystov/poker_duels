import { fireEvent, render, screen, within } from "@testing-library/react";
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
  // Nor is everything a player receives a word. `min`, `max` and `value` reach
  // the DOM as attributes and nothing prints them, so a bound the client worked
  // out for itself is invisible to a text-and-aria scan. Measured, not reasoned
  // about: with BET or RAISE allowed and ALL_IN withheld, `max={allInTo}` is the
  // only place that ceiling appears anywhere in the bar.
  const bounds = [...container.querySelectorAll("[min], [max], [value]")]
    .flatMap((element) => [
      element.getAttribute("min"),
      element.getAttribute("max"),
      element.getAttribute("value"),
    ])
    .filter((value): value is string => value !== null);
  const digits =
    [...parts, ...spoken, ...bounds].join(" ").match(/\d[\d,]*/g) ?? [];
  return digits.map((run) => Number(run.replaceAll(",", "")));
}

describe("the bar offers and derives nothing", () => {
  it("shows no number the turn does not carry", () => {
    const turn = aTurn();
    const { container } = render(
      <ActionBar
        turn={turn}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={null}
        refusal={null}
        send={vi.fn()}
      />,
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

  it("counts the ceiling that reaches the player only after they press for it", () => {
    for (const allowed of [
      ["CHECK", "BET"],
      ["FOLD", "CALL", "RAISE"],
    ] as const) {
      const turn = aTurn({
        legalActions: aLegalActions({ allowed: [...allowed] }),
      });
      const { container, getByRole, unmount } = render(
        <ActionBar
          turn={turn}
          potIncludingStreet={2850}
          committedThisStreet={0}
          rejection={null}
          refusal={null}
          send={vi.fn()}
        />,
      );
      const ceiling = turn.legalActions.allInTo;

      // Before a press: nowhere at all — not printed, not spoken, and (there
      // being no bound-carrying control left in this bar) not sitting in an
      // attribute either.
      expect(numbersOnScreen(container)).not.toContain(ceiling);

      fireEvent.click(
        within(getByRole("group", { name: "amount" })).getByRole("button", {
          name: "all-in",
        }),
      );

      const fromTheTurn = new Set([
        turn.legalActions.callTo,
        turn.legalActions.minBetTo,
        turn.legalActions.minRaiseTo,
        turn.legalActions.allInTo,
      ]);
      const afterPress = numbersOnScreen(container);
      // Only after asking for it does the ceiling reach the player — as the
      // action button's own printed total.
      expect(afterPress).toContain(ceiling);
      expect(afterPress.filter((n) => !fromTheTurn.has(n))).toEqual([]);

      unmount();
    }
  });

  it("offers no control the turn did not allow", () => {
    const { container } = render(
      <ActionBar
        turn={aTurn({ legalActions: aLegalActions({ allowed: ["CHECK"] }) })}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={null}
        refusal={null}
        send={vi.fn()}
      />,
    );

    expect(screen.getAllByRole("button").map((b) => b.textContent)).toEqual([
      "Check",
    ]);
    expect(container.textContent).not.toMatch(
      /\b(Fold|Call|Bet|Raise|All in)\b/,
    );
  });
});
