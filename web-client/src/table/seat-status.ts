import type { SeatView, SeatPresence } from "../protocol";

/**
 * What a seat is doing, in the view's own words.
 *
 * Read in this order because the earlier answers exclude the later ones: a seat
 * that folded is out of the hand, and a seat that is all in cannot be asked to
 * act. Every branch reads a field the server sent — none of it is inferred from
 * whether the seat is holding cards.
 */
export function seatStatus(
  seat: SeatView,
  isToAct: boolean,
  isViewer: boolean,
  presence: SeatPresence | null = null,
): string {
  if (seat.hasFolded) return "Folded";
  if (seat.isAllIn) return "All in";
  // ADR-0046 §1: presence outranks the turn, because `Their turn` on a seat nobody is
  // sitting at blames a pause on thinking — and never outranks a fact about the hand,
  // which stays true whoever is at the keyboard.
  if (presence === "AWAY") return "Away";
  if (presence === "ABSENT") return "Timed out";
  if (isToAct) return isViewer ? "Your turn" : "Their turn";
  return "";
}
