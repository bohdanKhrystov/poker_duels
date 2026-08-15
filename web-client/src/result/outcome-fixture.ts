import type { DuelOutcome } from "../protocol";

/**
 * A `DuelOutcome` carrying every field the wire declares, for a test to bend.
 *
 * Its three numbers — the hand count, both final stacks — are mutually
 * independent, and so is the `1` the coin moves by: no two of them add,
 * subtract, double or halve into a third. A figure the result screen worked
 * out for itself therefore lands outside the set instead of colliding with a
 * legitimate one.
 */
export function anOutcome(overrides: Partial<DuelOutcome> = {}): DuelOutcome {
  return {
    winner: 0,
    handsPlayed: 17,
    finalStacks: [19400, 4600],
    ...overrides,
  };
}
