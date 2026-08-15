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
