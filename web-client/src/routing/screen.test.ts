import { describe, it, expect } from "vitest";
import { screenFromHash, hashForScreen } from "./screen";

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

    expect(screenFromHash(hashForScreen("first"))).toBe("first");
    expect(screenFromHash(hashForScreen("duels"))).toBe("duels");
    expect(screenFromHash(hashForScreen("leaderboard"))).toBe("leaderboard");
  });
});
