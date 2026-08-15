import { describe, it, expect } from "vitest";
import { aSeat, aView } from "./view-fixture";

describe("the view fixture", () => {
  it("carries every field a SeatView declares", () => {
    expect(Object.keys(aSeat()).sort()).toEqual([
      "committedThisHand",
      "committedThisStreet",
      "hasFolded",
      "holeCards",
      "index",
      "isAllIn",
      "stack",
    ]);
  });

  it("carries every field a PlayerView declares", () => {
    expect(Object.keys(aView()).sort()).toEqual([
      "betToMatch",
      "bigBlind",
      "board",
      "buttonSeat",
      "handNumber",
      "minRaiseTo",
      "pot",
      "seatToAct",
      "seats",
      "smallBlind",
      "street",
      "viewerSeat",
    ]);
  });

  it("seats two players, indexed nought and one", () => {
    expect(aView().seats.map((seat) => seat.index)).toEqual([0, 1]);
  });

  it("lets a test bend any field it names", () => {
    expect(aView({ pot: 4850 }).pot).toBe(4850);
    expect(aView({ street: "RIVER" }).street).toBe("RIVER");
    expect(aSeat({ index: 1, holeCards: ["Ah"] }).holeCards).toEqual(["Ah"]);
    expect(aSeat({ stack: 13000 }).committedThisHand).toBe(0);
  });
});
