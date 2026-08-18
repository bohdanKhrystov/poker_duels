import { describe, it, expect } from "vitest";
import {
  NAME_REMOVED_HEADING,
  NAME_REMOVED_BODY,
  PERMANENCE_LINE,
  refusalSentence,
  mayTryAgain,
  nameOrNone,
} from "./name-text";

describe("the name surface's words", () => {
  it("says the four sentences ADR-0052 shipped, and no fifth", () => {
    expect(NAME_REMOVED_HEADING).toBe("Your display name was removed.");

    const body =
      "A person running Poker Duels removed it — not a bug, and not another player. " +
      "That name cannot be used again, by you or by anyone. " +
      "Choose a new one whenever you like.";
    expect(NAME_REMOVED_BODY).toBe(body);

    // The body splits on ". " (period-space) into exactly three sentences
    const sentences = body.split(". ");
    expect(sentences).toHaveLength(3);
  });

  it("never says a name is taken, and never names a holder", () => {
    const conflict = refusalSentence("conflict");
    expect(conflict).toBe("That name is not available. Try another.");

    // Verify the exact sentence to ensure the contract is kept
    expect(conflict).not.toContain("taken");

    // Check all five refusal sentences for the forbidden words
    const allSentences = [
      refusalSentence("rejected"),
      refusalSentence("conflict"),
      refusalSentence("permanent"),
      refusalSentence("no-profile"),
      refusalSentence("unavailable"),
    ];

    for (const sentence of allSentences) {
      expect(sentence).not.toContain("taken");
      expect(sentence).not.toContain("someone");
      expect(sentence).not.toContain("somebody");
      expect(sentence).not.toContain("another player");
      expect(sentence).not.toContain("already has it");
    }
  });

  it("gives each refusal a sentence of its own", () => {
    const sentences = new Set([
      refusalSentence("rejected"),
      refusalSentence("conflict"),
      refusalSentence("permanent"),
      refusalSentence("no-profile"),
      refusalSentence("unavailable"),
    ]);

    // All five sentences must be distinct
    expect(sentences.size).toBe(5);

    // Each equals its literal
    expect(refusalSentence("rejected")).toBe(
      "That name cannot be used. Try another.",
    );
    expect(refusalSentence("conflict")).toBe(
      "That name is not available. Try another.",
    );
    expect(refusalSentence("permanent")).toBe(
      "You already have a display name. That choice is permanent and cannot be changed.",
    );
    expect(refusalSentence("no-profile")).toBe(
      "This browser has no profile. Reload the page and try again.",
    );
    expect(refusalSentence("unavailable")).toBe(
      "That did not reach the server. Reload the page to see whether the name was set.",
    );
  });

  it("leaves a way back from a refused name and a conflicting one, and from nothing else", () => {
    // mayTryAgain is true for exactly two cases
    expect(mayTryAgain("rejected")).toBe(true);
    expect(mayTryAgain("conflict")).toBe(true);
    expect(mayTryAgain("permanent")).toBe(false);
    expect(mayTryAgain("no-profile")).toBe(false);
    expect(mayTryAgain("unavailable")).toBe(false);
  });

  it("says a name can be taken away, before anything is sent", () => {
    const line =
      "A name is chosen once. You cannot change it later, and it can be taken away.";
    expect(PERMANENCE_LINE).toBe(line);
    expect(PERMANENCE_LINE).toContain("and it can be taken away");
  });

  it("says the same thing about a player with no name wherever it is asked", () => {
    // Named players pass through unchanged
    expect(nameOrNone("Ada")).toBe("Ada");
    expect(nameOrNone("Grace")).toBe("Grace");

    // A player with no name gets the same treatment everywhere (ADR-0058 §1)
    expect(nameOrNone(null)).toBe("No name");
  });

  it("says nothing about why a name is missing", () => {
    const result = nameOrNone(null);

    // Must not leak any hint about a takedown (ADR-0052 §5)
    expect(result).not.toContain("removed");
    expect(result).not.toContain("taken");
    expect(result).not.toContain("banned");
    expect(result).not.toContain("deleted");
    expect(result).not.toMatch(/moderat/i); // Match case-insensitively
    expect(result).not.toContain("former");
  });
});
