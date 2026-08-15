import type { SeatView } from "../protocol";

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
): string {
  if (seat.hasFolded) return "Folded";
  if (seat.isAllIn) return "All in";
  if (isToAct) return isViewer ? "Your turn" : "Their turn";
  return "";
}
