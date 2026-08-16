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
    ...container.querySelectorAll(
      "[aria-label], [title], [min], [max], [value]",
    ),
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
    const { container } = render(<DuelResult outcome={outcome} mySeat={0} />);

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
