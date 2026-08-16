import { describe, expect, it } from "vitest";
import { LONGEST_RETRY_MILLIS, retryDelayMillis } from "./retry-delay";

describe("the retry delay", () => {
  it("doubles its ceiling for every attempt until it caps", () => {
    const attempts = [0, 1, 2, 3, 4, 5, 6];

    expect(attempts.map((a) => retryDelayMillis(a, 1))).toEqual([
      500, 1000, 2000, 4000, 8000, 10000, 10000,
    ]);
  });

  it("is never shorter than half its ceiling, at every attempt", () => {
    const attempts = [0, 1, 2, 3, 4, 5, 6];

    expect(attempts.map((a) => retryDelayMillis(a, 0))).toEqual([
      250, 500, 1000, 2000, 4000, 5000, 5000,
    ]);
  });

  it("spends the jitter it is handed, differently at every attempt", () => {
    const attempts = [0, 1, 2, 3, 4, 5, 6];

    expect(attempts.map((a) => retryDelayMillis(a, 0.5))).toEqual([
      375, 750, 1500, 3000, 6000, 7500, 7500,
    ]);

    for (const attempt of attempts) {
      const spread = [0, 0.5, 1].map((j) => retryDelayMillis(attempt, j));
      expect(new Set(spread).size).toBe(3);
    }
  });

  it("stays inside its bounds however far the attempts run", () => {
    for (let attempt = 0; attempt <= 30; attempt++) {
      const delayAtMinJitter = retryDelayMillis(attempt, 0);
      const delayAtMaxJitter = retryDelayMillis(attempt, 0.999);

      expect(delayAtMinJitter).toBeGreaterThanOrEqual(250);
      expect(delayAtMinJitter).toBeLessThanOrEqual(LONGEST_RETRY_MILLIS);

      expect(delayAtMaxJitter).toBeGreaterThanOrEqual(250);
      expect(delayAtMaxJitter).toBeLessThanOrEqual(LONGEST_RETRY_MILLIS);
    }
  });
});
