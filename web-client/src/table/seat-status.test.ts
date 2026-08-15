import { describe, it, expect } from "vitest";
import { seatStatus } from "./seat-status";
import { aSeat } from "./view-fixture";

describe("a seat's status", () => {
  it("is empty when the seat is waiting", () => {
    expect(seatStatus(aSeat(), false, true)).toBe("");
  });

  it("says whose turn it is from the seat to act", () => {
    expect(seatStatus(aSeat(), true, true)).toBe("Your turn");
    expect(seatStatus(aSeat(), true, false)).toBe("Their turn");
  });

  it("says a folded seat folded, whatever cards it is holding", () => {
    // With holeCards unset (undefined in overrides)
    expect(seatStatus(aSeat({ hasFolded: true }), false, true)).toBe("Folded");

    // With two cards
    expect(
      seatStatus(
        aSeat({ hasFolded: true, holeCards: ["Ah", "Ks"] }),
        false,
        true,
      ),
    ).toBe("Folded");

    // With empty array
    expect(
      seatStatus(aSeat({ hasFolded: true, holeCards: [] }), false, true),
    ).toBe("Folded");
  });

  it("says an all-in seat is all in, whatever cards it is holding", () => {
    // With holeCards unset (default empty array)
    expect(seatStatus(aSeat({ isAllIn: true }), false, true)).toBe("All in");

    // With two cards
    expect(
      seatStatus(
        aSeat({ isAllIn: true, holeCards: ["Ah", "Ks"] }),
        false,
        true,
      ),
    ).toBe("All in");

    // With empty array
    expect(
      seatStatus(aSeat({ isAllIn: true, holeCards: [] }), false, true),
    ).toBe("All in");
  });

  it("prefers folded over all in and over the turn", () => {
    // folded and all in and to act -> "Folded"
    expect(
      seatStatus(aSeat({ hasFolded: true, isAllIn: true }), true, true),
    ).toBe("Folded");

    // all in and to act -> "All in"
    expect(seatStatus(aSeat({ isAllIn: true }), true, true)).toBe("All in");
  });
});
