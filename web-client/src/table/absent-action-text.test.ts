import { describe, it, expect } from "vitest";
import type { ActedForAbsent } from "../protocol";
import { absentActionText } from "./absent-action-text";

/**
 * An `ActedForAbsent` carrying every field the wire declares, for a test to bend.
 * Following the tradition of `view-fixture.ts`.
 */
function aMark(overrides: Partial<ActedForAbsent> = {}): ActedForAbsent {
  return {
    type: "ActedForAbsent",
    seat: 0,
    handNumber: 1,
    actionSequence: 1,
    action: "FOLD",
    ...overrides,
  };
}

describe("an action the server took", () => {
  it("names the server acting for your rival, whichever seat the rival is", () => {
    // When the rival is at seat 1 and I'm at seat 0
    expect(absentActionText(aMark({ seat: 1 }), 0)).toBe(
      "The server folded for your rival.",
    );
    // When the rival is at seat 0 and I'm at seat 1
    expect(absentActionText(aMark({ seat: 0 }), 1)).toBe(
      "The server folded for your rival.",
    );
  });

  it("names the server acting for you, whichever seat you are", () => {
    // When I'm at seat 0
    expect(absentActionText(aMark({ seat: 0 }), 0)).toBe(
      "The server folded for you.",
    );
    // When I'm at seat 1
    expect(absentActionText(aMark({ seat: 1 }), 1)).toBe(
      "The server folded for you.",
    );
  });

  it("names an absent seat when this client holds none", () => {
    // When the server acted for seat 0 and I hold no seat
    expect(absentActionText(aMark({ seat: 0 }), null)).toBe(
      "The server folded for an absent seat.",
    );
    // When the server acted for seat 1 and I hold no seat
    // The null branch must win over the seat comparison (which would wrongly give "your rival" if 0 !== null)
    expect(absentActionText(aMark({ seat: 1 }), null)).toBe(
      "The server folded for an absent seat.",
    );
  });

  it("uses the verb the frame carried, and no other", () => {
    // With action: "CHECK" and different coordinates for the same subject
    // Seat 1, mySeat 0 -> "your rival"
    expect(
      absentActionText(
        aMark({ action: "CHECK", seat: 1, handNumber: 3, actionSequence: 7 }),
        0,
      ),
    ).toBe("The server checked for your rival.");
    // Seat 0, mySeat 0 -> "you"
    expect(
      absentActionText(
        aMark({ action: "CHECK", seat: 0, handNumber: 41, actionSequence: 2 }),
        0,
      ),
    ).toBe("The server checked for you.");
    // Seat 1, mySeat null -> "an absent seat"
    expect(
      absentActionText(
        aMark({ action: "CHECK", seat: 1, handNumber: 3, actionSequence: 7 }),
        null,
      ),
    ).toBe("The server checked for an absent seat.");
  });
});
