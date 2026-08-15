import type { ReactElement } from "react";
import type { PlayerView, Street } from "../protocol";
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

/**
 * The pot and the hand's standing facts, every one of them read straight off
 * the view: the pot is `view.pot` and not a sum of what the seats put in, and
 * the street is `view.street` and not a count of board cards — those two
 * disagree at exactly the moments that matter.
 */
export function PotStrip(props: { view: PlayerView }): ReactElement {
  const { view } = props;
  return (
    <div className="flex items-baseline gap-4 px-2 py-3">
      <span className="font-mono text-large tabular-nums">
        Pot&nbsp;{formatChips(view.pot)}
      </span>
      <span className="text-small text-text-muted">
        Blinds {formatChips(view.smallBlind)}/{formatChips(view.bigBlind)} ·
        Hand {view.handNumber} · {STREET_NAMES[view.street]}
      </span>
    </div>
  );
}
