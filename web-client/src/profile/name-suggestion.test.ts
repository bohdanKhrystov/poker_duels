import { describe, expect, it } from "vitest";

import { NAME_VOCABULARY } from "./name-vocabulary";
import { SUGGESTION_SPACE_FLOOR, suggestName } from "./name-suggestion";

/** Turns a fixed sequence of `[0, 1)` draws into the `random` source `suggestName` takes. */
function scripted(values: readonly number[]): () => number {
  let call = 0;
  return () => values[call++];
}

/** ADR-0137 §2's index expression, computed here so a test can name the entry it expects. */
function indexFor(list: readonly string[], draw: number): number {
  return Math.min(list.length - 1, Math.floor(draw * list.length));
}

function entryFor(list: readonly string[], draw: number): string {
  return list[indexFor(list, draw)];
}

describe("suggestName", () => {
  it("holds the vocabulary at the floor", () => {
    const arity = NAME_VOCABULARY.reduce(
      (product, list) => product * list.length,
      1,
    );

    expect(SUGGESTION_SPACE_FLOOR).toBe(1_000_000);
    expect(arity).toBeGreaterThanOrEqual(SUGGESTION_SPACE_FLOOR);
  });

  it("draws one entry from each list, in order", () => {
    const firstDraws = [0, 0, 0];
    const middleDraws = [0.41, 0.63, 0.87];

    const first = NAME_VOCABULARY.map((list, i) =>
      entryFor(list, firstDraws[i]),
    ).join(" ");
    const middle = NAME_VOCABULARY.map((list, i) =>
      entryFor(list, middleDraws[i]),
    ).join(" ");

    expect(suggestName(scripted(firstDraws))).toBe(first);
    expect(suggestName(scripted(middleDraws))).toBe(middle);
    expect(first).not.toBe(middle);
  });

  it("clamps a source that returns one", () => {
    const last = NAME_VOCABULARY.map((list) => list[list.length - 1]);

    const name = suggestName(scripted([1, 1, 1]));

    expect(name).toBe(last.join(" "));
    expect(name).not.toContain("undefined");
  });

  it("draws a name the server's canonical form accepts", () => {
    const firstDraws = [0, 0, 0];
    const lastDraws = [0.999999999, 0.999999999, 0.999999999];
    const middleDraws = [0.29, 0.55, 0.72];

    for (const draws of [firstDraws, lastDraws, middleDraws]) {
      const name = suggestName(scripted(draws));

      expect(name.length).toBeGreaterThan(0);
      expect(name.normalize("NFC")).toBe(name);
      expect([...name].length).toBeLessThanOrEqual(32);
      expect(name).toMatch(/^[^\p{Cc}\p{Cf}]+$/u);
      expect(name.replace(/ /g, "")).not.toMatch(/\s/);
      expect(name).not.toContain("  ");
      expect(name.trim()).toBe(name);
    }
  });
});
