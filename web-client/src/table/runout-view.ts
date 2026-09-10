import type { GameEvent, PlayerView } from "../protocol";
import { awardsForHand } from "./awards";

/**
 * The table as it stood while the pot was still in the middle.
 *
 * A hand that ends all in arrives as one snapshot in which the pot has already
 * been paid out: the winner's stack holds it and the pot reads zero. Painted
 * under a flop, a turn and a river that are still being turned over, that
 * snapshot spoils the river before it lands — the stack knows who won before
 * the board does, and "Pot 0" sits over a ten-thousand-chip runout.
 *
 * So for every step but the last, the table is drawn from this instead: each
 * seat's award is taken back out of its stack and put back in the pot, and
 * nothing is left committed on the street. Every figure here is one the server
 * sent — an award amount, a final stack — combined by nothing more than
 * subtraction; the client still names no winner and evaluates no hand. The
 * hole cards are the snapshot's own: a runout turns both hands face up before
 * the first card is dealt, and that is exactly what the steps should show.
 *
 * When this client holds no award for the hand (a resume that landed on the
 * finished snapshot, `ADR-0102` §5), there is nothing to take back and the
 * snapshot is returned as it is.
 */
export function runoutView(
  view: PlayerView,
  narration: readonly GameEvent[],
): PlayerView {
  const awards = awardsForHand(narration, view.handNumber);
  if (awards.length === 0) return view;
  const awardedTo = new Map<number, number>();
  let potBeforeAward = view.pot;
  for (const award of awards) {
    awardedTo.set(award.seat, (awardedTo.get(award.seat) ?? 0) + award.amount);
    potBeforeAward += award.amount;
  }
  return {
    ...view,
    pot: potBeforeAward,
    seats: view.seats.map((seat) => ({
      ...seat,
      stack: seat.stack - (awardedTo.get(seat.index) ?? 0),
      committedThisStreet: 0,
    })),
  };
}
