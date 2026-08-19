import { describe, it, expect } from "vitest";

import {
  HISTORY_HEADING,
  LOADING_RECORD,
  NO_DUELS,
  NO_MATCH,
  READ_FAILED,
  MORE,
  OUTCOME_LEGEND,
  EVERY_OUTCOME,
  OPPONENT_LABEL,
  emptyLine,
} from "./history-text";

describe("the history screen's words", () => {
  it("states every sentence exactly, character for character", () => {
    expect(HISTORY_HEADING).toBe("Your duels");
    expect(LOADING_RECORD).toBe("Loading your duels…");
    expect(NO_DUELS).toBe("No duels yet.");
    expect(NO_MATCH).toBe("No duel matches this.");
    expect(READ_FAILED).toBe(
      "Your duels did not load. Reload the page to try again.",
    );
    expect(MORE).toBe("Show more");
    expect(OUTCOME_LEGEND).toBe("Outcome");
    expect(EVERY_OUTCOME).toBe("All");
    expect(OPPONENT_LABEL).toBe("Opponent name");
  });

  it("tells an empty record from an empty filter", () => {
    expect(emptyLine(false)).toBe(NO_DUELS);
    expect(emptyLine(true)).toBe(NO_MATCH);
    expect(emptyLine(false)).not.toBe(emptyLine(true));
  });

  it("gives the four states four different sentences", () => {
    const sentences = new Set([
      LOADING_RECORD,
      emptyLine(false),
      emptyLine(true),
      READ_FAILED,
    ]);
    expect(sentences.size).toBe(4);
    sentences.forEach((sentence) => {
      expect(sentence.length).toBeGreaterThan(0);
    });
  });
});
