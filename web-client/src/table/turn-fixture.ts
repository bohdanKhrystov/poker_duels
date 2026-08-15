import type { LegalActions } from "../protocol";
import type { PendingTurn } from "../store/duel-state";

/**
 * A `LegalActions` carrying every field the wire declares, for a test to bend.
 *
 * The four amounts are mutually independent — no two of them add, subtract,
 * double or halve into a third — so a figure the bar works out for itself lands
 * outside the set instead of colliding with a legitimate one.
 */
export function aLegalActions(
  overrides: Partial<LegalActions> = {},
): LegalActions {
  return {
    seat: 0,
    allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
    callTo: 400,
    minBetTo: 175,
    minRaiseTo: 1200,
    allInTo: 13400,
    ...overrides,
  };
}

/** A `PendingTurn` the store would hold after one `YourTurn`, for a test to bend. */
export function aTurn(overrides: Partial<PendingTurn> = {}): PendingTurn {
  return {
    handNumber: 14,
    actionSequence: 27,
    legalActions: aLegalActions(),
    ...overrides,
  };
}
