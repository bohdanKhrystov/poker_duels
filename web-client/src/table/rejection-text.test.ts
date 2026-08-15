import { describe, it, expect } from "vitest";
import { rejectionText } from "./rejection-text";

describe("the rejection text", () => {
  it("names the action the server refused and the ones it allows", () => {
    expect(
      rejectionText({
        type: "ActionNotAllowed",
        attempted: "BET",
        allowed: ["CHECK", "ALL_IN"],
      }),
    ).toBe("Bet was refused. The server allows Check, All in.");

    expect(
      rejectionText({
        type: "ActionNotAllowed",
        attempted: "RAISE",
        allowed: ["FOLD", "CHECK", "CALL"],
      }),
    ).toBe("Raise to was refused. The server allows Fold, Check, Call.");
  });

  it("states the minimum the server sent", () => {
    expect(
      rejectionText({
        type: "AmountTooSmall",
        attempted: 900,
        minimum: 1200,
      }),
    ).toBe("900 is under the minimum of 1,200.");

    expect(
      rejectionText({
        type: "AmountTooSmall",
        attempted: 500,
        minimum: 2500,
      }),
    ).toBe("500 is under the minimum of 2,500.");
  });

  it("states the maximum the server sent", () => {
    expect(
      rejectionText({
        type: "AmountTooLarge",
        attempted: 20000,
        maximum: 13400,
      }),
    ).toBe("20,000 is over the maximum of 13,400.");

    expect(
      rejectionText({
        type: "AmountTooLarge",
        attempted: 15000,
        maximum: 10000,
      }),
    ).toBe("15,000 is over the maximum of 10,000.");
  });

  it("names the seat the server says is to act", () => {
    expect(
      rejectionText({
        type: "NotYourTurn",
        seatToAct: 1,
      }),
    ).toBe("The server says it is seat 1's turn.");

    expect(
      rejectionText({
        type: "NotYourTurn",
        seatToAct: 0,
      }),
    ).toBe("The server says it is seat 0's turn.");
  });

  it("says so when the server names no seat to act", () => {
    expect(
      rejectionText({
        type: "NotYourTurn",
        seatToAct: null,
      }),
    ).toBe("The server says it is nobody's turn.");
  });

  it("says a finished hand is finished", () => {
    expect(
      rejectionText({
        type: "HandComplete",
      }),
    ).toBe("The server says that hand is already over.");
  });
});
