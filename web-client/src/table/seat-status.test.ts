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

  it("says a seat whose window is still running is away", () => {
    // Not the viewer
    expect(seatStatus(aSeat(), false, false, "AWAY")).toBe("Away");
    // Is the viewer
    expect(seatStatus(aSeat(), false, true, "AWAY")).toBe("Away");
  });

  it("says a seat whose window ran out timed out", () => {
    expect(seatStatus(aSeat(), false, false, "ABSENT")).toBe("Timed out");
  });

  it("says nothing of its own for a seat that is present", () => {
    // With null presence
    expect(seatStatus(aSeat(), false, false, null)).toBe("");
    expect(seatStatus(aSeat(), false, true, null)).toBe("");
    // With PRESENT presence
    expect(seatStatus(aSeat(), false, false, "PRESENT")).toBe("");
    expect(seatStatus(aSeat(), false, true, "PRESENT")).toBe("");
    // Both give "Their turn" / "Your turn" when isToAct is true
    expect(seatStatus(aSeat(), true, false, null)).toBe("Their turn");
    expect(seatStatus(aSeat(), true, true, null)).toBe("Your turn");
    expect(seatStatus(aSeat(), true, false, "PRESENT")).toBe("Their turn");
    expect(seatStatus(aSeat(), true, true, "PRESENT")).toBe("Your turn");
  });

  it("prefers the presence over the turn", () => {
    // AWAY with isToAct -> "Away"
    expect(seatStatus(aSeat(), true, false, "AWAY")).toBe("Away");
    expect(seatStatus(aSeat(), true, true, "AWAY")).toBe("Away");
    // ABSENT with isToAct -> "Timed out"
    expect(seatStatus(aSeat(), true, false, "ABSENT")).toBe("Timed out");
    expect(seatStatus(aSeat(), true, true, "ABSENT")).toBe("Timed out");
  });

  it("prefers a fact about the hand over the presence", () => {
    // hasFolded with AWAY -> "Folded"
    expect(seatStatus(aSeat({ hasFolded: true }), false, false, "AWAY")).toBe(
      "Folded",
    );
    // isAllIn with ABSENT -> "All in"
    expect(seatStatus(aSeat({ isAllIn: true }), false, false, "ABSENT")).toBe(
      "All in",
    );
    // hasFolded with ABSENT -> "Folded"
    expect(seatStatus(aSeat({ hasFolded: true }), false, false, "ABSENT")).toBe(
      "Folded",
    );
  });
});
