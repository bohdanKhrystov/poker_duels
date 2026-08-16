import { describe, it, expect } from "vitest";
import {
  outcomeWord,
  coinDeltaText,
  coinBalanceText,
  finishedAtText,
} from "./profile-text";

describe("the profile's words", () => {
  it("names each outcome the wire can carry", () => {
    expect(outcomeWord("WON")).toBe("Won");
    expect(outcomeWord("LOST")).toBe("Lost");
    expect(outcomeWord("DREW")).toBe("Drew");
  });

  it("signs a coin delta the way the server signed it", () => {
    expect(coinDeltaText(1)).toBe("+1");
    expect(coinDeltaText(-1)).toBe("−1"); // U+2212 MINUS SIGN
    expect(coinDeltaText(0)).toBe("0");
  });

  it("states a balance with no plus in front of it", () => {
    expect(coinBalanceText(7)).toBe("7");
    expect(coinBalanceText(0)).toBe("0");
    expect(coinBalanceText(-1)).toBe("−1"); // U+2212 MINUS SIGN
  });

  it("states when a duel finished in the browser locale", () => {
    const result1 = finishedAtText("2026-08-14T21:03:05Z", {
      locales: "en-GB",
      timeZone: "UTC",
    });
    expect(result1).toContain("Aug");
    expect(result1).toContain("2026");
    expect(result1).toContain("21:03");
    expect(result1).not.toContain("2026-08-14T21:03:05Z");
    expect(result1).not.toMatch(/\d{2}T\d{2}/);

    const result2 = finishedAtText("2026-01-02T07:00:00Z", {
      locales: "en-GB",
      timeZone: "UTC",
    });
    expect(result2).toContain("Jan");
    expect(result2).toContain("2026");
    expect(result2).toContain("07:00");
    expect(result2).not.toContain("2026-01-02T07:00:00Z");
    expect(result2).not.toMatch(/\d{2}T\d{2}/);
  });

  it("says nothing about an instant it cannot read", () => {
    expect(finishedAtText("not a date")).toBe("");
  });
});
