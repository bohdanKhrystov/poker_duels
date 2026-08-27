import { describe, expect, it } from "vitest";
import * as offerText from "./account-offer-text";

describe("the offer's words", () => {
  it("states every sentence exactly, character for character, and names the stake", () => {
    // Exactly these exports, and no others — an extra or a missing one fails here
    // even if every literal below still matches, because `toBe` cannot see either.
    expect(Object.keys(offerText).sort()).toEqual(
      ["OFFER_ACCEPT", "OFFER_BODY", "OFFER_DISMISS", "OFFER_HEADING"].sort(),
    );

    expect(offerText.OFFER_HEADING).toBe(
      "Your duel coins are only in this browser",
    );
    expect(offerText.OFFER_BODY).toBe(
      "You have won a duel, so you have duel coins to lose. They belong to this browser and go with it. " +
        "A password keeps them, and your duels, on any browser you sign in from. You are never required to have one.",
    );
    expect(offerText.OFFER_ACCEPT).toBe("Keep them with a password");
    // ADR-0036 §Decision writes this word itself; it is not this file's to choose.
    expect(offerText.OFFER_DISMISS).toBe("Not now");
  });
});
