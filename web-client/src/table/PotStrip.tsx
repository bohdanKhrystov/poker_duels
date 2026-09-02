import type { ReactElement } from "react";
import type { GameEvent, PlayerView, PotAwarded, Street } from "../protocol";
import { formatChips } from "./chips";

// The street the view names, written out. A side table rather than a switch so
// `tsc` fails the day the wire grows a street this screen has no word for.
const STREET_NAMES: Record<Street, string> = {
  PREFLOP: "Preflop",
  FLOP: "Flop",
  TURN: "Turn",
  RIVER: "River",
  SHOWDOWN: "Showdown",
  COMPLETE: "Hand complete",
};

function potCommittedToTheHand(view: PlayerView): number {
  return view.seats.reduce(
    (sum, seat) => sum + seat.committedThisStreet,
    view.pot,
  );
}

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
 *
 * `Array.prototype.findLastIndex` is ES2023; this project targets ES2022, so
 * the start is found with a backward loop, and the end boundary stays the
 * forward loop that stops at the next `HandStarted`.
 */
function awardsForHand(
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

/**
 * The banner line for a hand that just ended, or `null` when the ordinary
 * `Pot N` line should stand instead — mid-hand, or when this client never
 * received the ended hand's award (`ADR-0095` §4). Every number is a
 * `PotAwarded.amount` this client actually received, never a total the client
 * works out, and a split states only the viewer's own share (`ADR-0095` §2).
 */
function awardLineFor(
  view: PlayerView,
  narration: readonly GameEvent[],
): string | null {
  if (view.street !== "COMPLETE") return null;
  const awards = awardsForHand(narration, view.handNumber);
  if (awards.length === 0) return null;
  if (awards.length === 1) {
    const [award] = awards;
    return award.seat === view.viewerSeat
      ? `You win ${formatChips(award.amount)}`
      : `Your rival wins ${formatChips(award.amount)}`;
  }
  const own = awards.find((award) => award.seat === view.viewerSeat);
  return own === undefined
    ? null
    : `Split pot — you win ${formatChips(own.amount)}`;
}

/**
 * The pot and the hand's standing facts, most of them read straight off the
 * view: the pot is the sum of `view.pot` and both seats' `committedThisStreet`
 * (`ADR-0107` §1), and the street is `view.street` unless `props.street` names
 * a different one — a runout's own step, held in its own prop rather than
 * folded into `view`, because a doctored view would be the client assembling a
 * fact the server never sent as a unit (`ADR-0102` §§2–3).
 *
 * When the street in effect is `COMPLETE` and this client saw the hand's
 * award, the amount slot states who took the pot instead (`ADR-0095`); every
 * other tick it reads `Pot N` as it always has. A step's own street is never
 * `COMPLETE`, so the award line is held back for exactly as long as one
 * stands (`ADR-0102` §2).
 */
export function PotStrip(props: {
  view: PlayerView;
  narration?: readonly GameEvent[];
  street?: Street;
}): ReactElement {
  const { view, narration = [] } = props;
  const street = props.street ?? view.street;
  const awardLine =
    street === "COMPLETE" ? awardLineFor(view, narration) : null;
  return (
    <div className="flex items-baseline gap-4 px-2 py-3">
      <span className="font-mono text-large tabular-nums">
        {awardLine ?? <>Pot&nbsp;{formatChips(potCommittedToTheHand(view))}</>}
      </span>
      <span className="text-small text-text-muted">
        Blinds {formatChips(view.smallBlind)}/{formatChips(view.bigBlind)} ·
        Hand {view.handNumber} · {STREET_NAMES[street]}
      </span>
    </div>
  );
}
