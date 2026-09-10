import type { GameEvent } from "../protocol";
import type { CardStanding } from "./PlayingCard";
import { awardsForHand } from "./awards";

/**
 * The cards that took the pot of the hand `handNumber` names: the union of the
 * winning five each `PotAwarded` for that hand carries — one seat's five, or
 * both seats' at a split. Empty for a hand still in play, for a hand won on a
 * fold (an award with no hand), and for a hand this client never saw awarded.
 *
 * Nothing here evaluates a hand: the five are the engine's, sent on the award,
 * and the client only collects them.
 */
export function winningCards(
  narration: readonly GameEvent[],
  handNumber: number,
): ReadonlySet<string> {
  const marked = new Set<string>();
  for (const award of awardsForHand(narration, handNumber)) {
    for (const card of award.hand) marked.add(card);
  }
  return marked;
}

/** How one face-up card stands beside a mark — `plain` wherever there is none. */
export function standingOf(
  card: string,
  marked: ReadonlySet<string> | undefined,
): CardStanding {
  if (marked === undefined || marked.size === 0) return "plain";
  return marked.has(card) ? "marked" : "unmarked";
}
