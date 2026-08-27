import { describe, it, expect } from "vitest";
import { offerAccount } from "./account-offer";

describe("the account offer", () => {
  it("is made on a win nobody has settled, and on nothing else", () => {
    // The four verdict values under the same other inputs all tell different stories
    expect(
      offerAccount({
        verdict: "win",
        signedIn: false,
        settled: false,
      }),
    ).toBe(true);

    expect(
      offerAccount({
        verdict: "loss",
        signedIn: false,
        settled: false,
      }),
    ).toBe(false);

    expect(
      offerAccount({
        verdict: "draw",
        signedIn: false,
        settled: false,
      }),
    ).toBe(false);

    expect(
      offerAccount({
        verdict: "unknown",
        signedIn: false,
        settled: false,
      }),
    ).toBe(false);
  });

  it("is withheld from a browser holding a credential", () => {
    // Same as the true case in the first test, but signedIn is true
    expect(
      offerAccount({
        verdict: "win",
        signedIn: true,
        settled: false,
      }),
    ).toBe(false);
  });

  it("is withheld once it has been settled", () => {
    // Same as the true case in the first test, but settled is true
    expect(
      offerAccount({
        verdict: "win",
        signedIn: false,
        settled: true,
      }),
    ).toBe(false);
  });
});
