import type { ReactElement } from "react";
import type { SeatView } from "../protocol";
import { formatChips } from "./chips";
import { seatStatus } from "./seat-status";

/** A seat plate: who it is, what it is doing, the button, and the stack. */
export function SeatPlate(props: {
  name: string;
  seat: SeatView;
  hasButton: boolean;
  isToAct: boolean;
  isViewer: boolean;
}): ReactElement {
  const status = seatStatus(props.seat, props.isToAct, props.isViewer);
  const onTurn = status === "Your turn" || status === "Their turn";
  return (
    <div
      className={`flex items-center gap-4 rounded-medium border border-l-2 border-hairline bg-surface px-5 py-4 ${
        onTurn ? "border-l-accent" : "border-l-transparent"
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
      {props.hasButton && (
        <span
          aria-label="the button"
          className="rounded-pill border border-hairline px-3 py-1 font-mono text-micro text-text-muted"
        >
          D
        </span>
      )}
      <span className="font-mono tabular-nums">
        {formatChips(props.seat.stack)}
      </span>
    </div>
  );
}
