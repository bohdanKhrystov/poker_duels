import type { ReactElement } from "react";
import type { SeatView, SeatPresence } from "../protocol";
import type { ActEvent } from "../store/duel-state";
import { ChipPile } from "./ChipPile";
import { lastActText } from "./action-text";
import { formatChips } from "./chips";
import { seatStatus } from "./seat-status";

// A non-breaking space, built at runtime rather than typed as a literal
// character — the mark's verb and figure sit on one pill exactly as the
// design card's own Call&nbsp;1,700 does.
const NBSP = String.fromCharCode(0xa0);

/**
 * A seat plate: who it is, what it is doing, its last act, the button, and
 * the stack.
 */
export function SeatPlate(props: {
  name: string;
  seat: SeatView;
  hasButton: boolean;
  isToAct: boolean;
  isViewer: boolean;
  presence?: SeatPresence | null;
  lastAct?: ActEvent | null;
}): ReactElement {
  const status = seatStatus(
    props.seat,
    props.isToAct,
    props.isViewer,
    props.presence ?? null,
  );
  const onTurn = status === "Your turn" || status === "Their turn";
  const act = props.lastAct ? lastActText(props.lastAct) : null;
  return (
    <div
      className={`flex items-center gap-4 rounded-medium border border-l-2 border-hairline bg-surface px-5 py-4 ${
        onTurn ? "border-l-accent acting-mark" : "border-l-transparent"
      }`}
    >
      <span className="min-w-0 flex-1">
        <span className="block truncate font-medium">{props.name}</span>
        <span
          className={`mt-1 block min-h-[1.5em] text-micro leading-body ${
            onTurn
              ? "font-medium tracking-caps text-accent uppercase"
              : "text-text-faint"
          }`}
        >
          {status}
        </span>
      </span>
      {act && (
        <span className="last-act">
          {act.amount === null
            ? act.verb
            : `${act.verb}${NBSP}${formatChips(act.amount)}`}
        </span>
      )}
      {props.hasButton && (
        <span
          aria-label="the button"
          className="rounded-pill border border-hairline px-3 py-1 font-mono text-micro text-text-muted"
        >
          D
        </span>
      )}
      {props.seat.stack > 0 && <ChipPile key={props.seat.stack} />}
      <span className="font-mono tabular-nums">
        {formatChips(props.seat.stack)}
      </span>
    </div>
  );
}
