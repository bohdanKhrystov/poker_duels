import { describe, it, expect } from "vitest";
import { screenFromHash, hashForScreen, tokenFromHash } from "./screen";

// Fixed verbatim: neither resembles a word, a year or a slug the switch
// already knows, and AWKWARD_TOKEN was measured in this repo's jsdom to be
// exactly what a reset link's second segment looks like when the secret
// itself contains "?" and "=" (STORY-0417 §*A test this story must carry*).
const MAILED_TOKEN = "Xk93qQz7aa4bbCC1ddEE8ff2gg";
const AWKWARD_TOKEN = "ab?c=d";

describe("the address of a screen", () => {
  it("reads each address back to the screen it names", () => {
    expect(screenFromHash("")).toBe("first");
    expect(screenFromHash("#")).toBe("first");
    expect(screenFromHash("#/duels")).toBe("duels");
    expect(screenFromHash("#/leaderboard")).toBe("leaderboard");
  });

  it("renders the first screen for a fragment it does not know", () => {
    expect(screenFromHash("#/nope")).toBe("first");
    expect(screenFromHash("#duels")).toBe("first");
    expect(screenFromHash("#/")).toBe("first");
    expect(screenFromHash("#/LEADERBOARD")).toBe("first");
  });

  it("names the first segment and ignores whatever follows it", () => {
    expect(screenFromHash("#/duels/2026")).toBe("duels");
    expect(screenFromHash("#/leaderboard/anything")).toBe("leaderboard");
  });

  it("writes an address for every screen, and the first screen carries no fragment", () => {
    expect(hashForScreen("first")).toBe("/");
    expect(hashForScreen("duels")).toBe("#/duels");
    expect(hashForScreen("leaderboard")).toBe("#/leaderboard");
    expect(hashForScreen("verify")).toBe("#/verify");
    expect(hashForScreen("reset")).toBe("#/reset");

    expect(screenFromHash(hashForScreen("first"))).toBe("first");
    expect(screenFromHash(hashForScreen("duels"))).toBe("duels");
    expect(screenFromHash(hashForScreen("leaderboard"))).toBe("leaderboard");
    expect(screenFromHash(hashForScreen("verify"))).toBe("verify");
    expect(screenFromHash(hashForScreen("reset"))).toBe("reset");
  });

  it("names a screen from a slug followed by a secret nothing else reads", () => {
    expect(screenFromHash("#/verify/" + MAILED_TOKEN)).toBe("verify");
    expect(screenFromHash("#/reset/" + MAILED_TOKEN)).toBe("reset");
    expect(screenFromHash("#/verify")).toBe("verify");
    expect(screenFromHash("#/reset")).toBe("reset");
  });

  it("hands back the second segment, and nothing when there is none", () => {
    expect(tokenFromHash("#/verify/" + MAILED_TOKEN)).toBe(MAILED_TOKEN);
    expect(tokenFromHash("#/reset/" + MAILED_TOKEN)).toBe(MAILED_TOKEN);
    expect(tokenFromHash("#/reset")).toBeNull();
    expect(tokenFromHash("#/reset/")).toBeNull();
    expect(tokenFromHash("#/duels")).toBeNull();
  });

  it("keeps a token that would end a path segment or start a query whole", () => {
    expect(tokenFromHash("#/reset/" + AWKWARD_TOKEN)).toBe("ab?c=d");
    expect(screenFromHash("#/reset/" + AWKWARD_TOKEN)).toBe("reset");
  });
});
