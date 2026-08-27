import type { Verdict } from "./outcome-text";

export interface OfferInput {
  /** The verdict `verdictOf` read off the server's `DuelOutcome` and this client's seat. */
  readonly verdict: Verdict;
  /** Whether this browser holds a session token (`useSignedIn`). */
  readonly signedIn: boolean;
  /** Whether this offer has already been made and answered. Its source is `DEC-079`'s settlement. */
  readonly settled: boolean;
}

/**
 * Whether the account offer should be made to this player.
 *
 * The offer is made if all three conditions hold: the player just won a duel (ADR-0036
 * §Decision), they do not already hold a credential, and the offer has not been settled
 * (answered or permanently dismissed). Every condition is essential: a win alone offers
 * nothing if the browser already has a credential; a credential loss is not shown if the
 * offer was settled before; and a settled offer is never made again.
 */
export function offerAccount(input: OfferInput): boolean {
  return input.verdict === "win" && !input.signedIn && !input.settled;
}
