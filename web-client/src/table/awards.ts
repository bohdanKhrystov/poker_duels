import type { GameEvent, PotAwarded } from "../protocol";

/**
 * The `PotAwarded` events of the hand `handNumber` names: those after its
 * `HandStarted` and up to the next one. Keyed to the view's hand number and
 * not to "the last `HandStarted` seen" — the `Events` frame that starts the
 * next hand can arrive before the `Snapshot` that moves the view onto it, and
 * a window keyed to the last start would blink out for that tick.
 *
 * The window opens at the *last* `HandStarted` carrying that hand number, not
 * the first: `narration` is never cleared on a rematch, so a room's second
 * duel can hold two of them, and the first belongs to a hand a previous duel
 * already finished.
 */
export function awardsForHand(
  narration: readonly GameEvent[],
  handNumber: number,
): readonly PotAwarded[] {
  let start = -1;
  for (let i = narration.length - 1; i >= 0; i--) {
    const event = narration[i];
    if (event.type === "HandStarted" && event.handNumber === handNumber) {
      start = i;
      break;
    }
  }
  if (start === -1) return [];
  const awards: PotAwarded[] = [];
  for (let i = start + 1; i < narration.length; i++) {
    const event = narration[i];
    if (event.type === "HandStarted") break;
    if (event.type === "PotAwarded") awards.push(event);
  }
  return awards;
}
