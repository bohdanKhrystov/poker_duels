import { describe, it, expect, vi } from "vitest";
import { LADDER_HEADING, MORE, seasonName, rowLine } from "./ladder-text";

describe("the words the ladder says", () => {
  it("states every sentence exactly, character for character", () => {
    expect(LADDER_HEADING).toBe("Leaderboard");
    expect(MORE).toBe("Show more");
  });

  it("spells a season as a month and a year, and hands back a wire string it cannot spell", () => {
    expect(seasonName("2026-01")).toBe("January 2026");
    expect(seasonName("2026-08")).toBe("August 2026");
    expect(seasonName("2026-12")).toBe("December 2026");
    expect(seasonName("2026-13")).toBe("2026-13");
  });

  it("names the season the response carried while the browser clock says another month", () => {
    vi.useFakeTimers();
    try {
      // Set the browser's clock to December 2026
      vi.setSystemTime(new Date("2026-12-15T00:00:00Z"));
      // Even though the client thinks it's December, the response says August
      expect(seasonName("2026-08")).toBe("August 2026");
    } finally {
      vi.useRealTimers();
    }
  });

  it("builds a row line from the rank, the name and the standing the server sent", () => {
    expect(
      rowLine({ rank: 4, playerId: "p", displayName: "Ada", coins: 3 }),
    ).toBe("4 Ada 3");
    expect(
      rowLine({ rank: 215, playerId: "q", displayName: null, coins: -1 }),
    ).toBe("215 No name −1");
  });
});
