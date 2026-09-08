import { describe, it, expect } from "vitest";
import { NAME_VOCABULARY } from "./name-vocabulary";

// No control (Cc) or format (Cf) character, and no whitespace of any kind (Z categories plus
// the \s escape, which also reaches U+180E and friends outside Z) — this is what makes the
// space-joined name total: nothing an entry contributes can itself introduce a separator or a
// character the server's canonicalDisplayNameOrNull would refuse.
const SERVER_ACCEPTS = /^[^\p{Cc}\p{Cf}\s\p{Z}]+$/u;

const allEntries = (): readonly string[] => NAME_VOCABULARY.flat();

describe("the name vocabulary", () => {
  it("holds only entries the server would accept", () => {
    const bad = allEntries().filter((entry) => !SERVER_ACCEPTS.test(entry));
    expect(bad).toEqual([]);
  });

  it("holds only entries that equal their own NFC form", () => {
    // Pure ASCII entries are already in NFC — this test cannot distinguish "written that way on
    // purpose" from "happens to be true of ASCII". It still guards the vocabulary against a
    // future entry a curator normalizes wrong, which is the failure it exists to catch.
    const bad = allEntries().filter(
      (entry) => entry.normalize("NFC") !== entry,
    );
    expect(bad).toEqual([]);
  });

  it("holds no entry longer than ten code points", () => {
    // [...entry] iterates by code point, not UTF-16 code unit, so an astral character counts as
    // one — entry.length would silently double-count it and let an over-long entry through.
    const bad = allEntries().filter((entry) => [...entry].length > 10);
    expect(bad).toEqual([]);
  });

  it("joins its longest entries into a name that fits thirty-two code points", () => {
    // The joined name's length is the sum of each chosen entry's length plus one separator per
    // gap. Summing independently-maximized terms maximizes their sum, so the longest name the
    // whole Cartesian product can produce is exactly the join of each list's own longest entry —
    // no enumeration of the product itself is needed to bound it.
    const longestPerList = NAME_VOCABULARY.map((list) =>
      Math.max(...list.map((entry) => [...entry].length)),
    );
    const separators = Math.max(NAME_VOCABULARY.length - 1, 0);
    const longestJoined =
      longestPerList.reduce((sum, n) => sum + n, 0) + separators;
    expect(longestJoined).toBeLessThanOrEqual(32);
  });

  it("repeats no name inside a list under a case fold", () => {
    // Two entries differing only in case are one reachable name (ADR-0029 §1's fold), so the
    // uniqueness that matters is over the folded set, not over the raw strings.
    for (const list of NAME_VOCABULARY) {
      const folded = new Set(list.map((entry) => entry.toLowerCase()));
      expect(folded.size).toBe(list.length);
    }
  });

  it("holds the lists this story has shipped so far", () => {
    expect(NAME_VOCABULARY.map((list) => list.length)).toEqual([50, 100]);
  });
});
