import { describe, it, expect } from "vitest";
import { secondsRemaining } from "./countdown";

const NOW = 1_700_000_000_000;

describe("the countdown", () => {
  it("counts whole seconds up to the deadline", () => {
    expect(secondsRemaining(NOW + 47_000, NOW)).toBe(47);
    expect(secondsRemaining(NOW + 46_001, NOW)).toBe(47);
    expect(secondsRemaining(NOW + 46_000, NOW)).toBe(46);
  });

  it("reaches zero and stays there", () => {
    expect(secondsRemaining(NOW, NOW)).toBe(0);
    expect(secondsRemaining(NOW - 1, NOW)).toBe(0);
    expect(secondsRemaining(NOW - 600_000, NOW)).toBe(0);
  });

  it("reads both of its arguments", () => {
    expect(secondsRemaining(NOW + 47_000, NOW)).toBe(47);
    expect(secondsRemaining(NOW + 47_000, NOW + 20_000)).toBe(27);
  });
});
