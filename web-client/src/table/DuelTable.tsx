import type { ReactElement } from "react";
import type { PlayerView, SeatPresence } from "../protocol";
import { BoardCards } from "./BoardCards";
import { PotStrip } from "./PotStrip";
import { SeatPlate } from "./SeatPlate";
import { Hand } from "./Hand";
import { formatChips } from "./chips";

/**
 * The duel table: one column, rival above, board between, you below.
 *
 * Everything on it is read off the `PlayerView` the server computed. Nothing is
 * worked out here — not the pot, not the street, not whose cards these are, and
 * not what anyone may do next.
 */
export function DuelTable(props: {
  view: PlayerView;
  rivalPresence?: SeatPresence | null;
}): ReactElement {
  const { view } = props;
  const you = view.seats.find((seat) => seat.index === view.viewerSeat);
  const rival = view.seats.find((seat) => seat.index !== view.viewerSeat);
  return (
    <div className="[container-type:inline-size] mx-auto flex max-w-[560px] flex-col gap-5">
      {rival !== undefined && (
        <div className="flex flex-col gap-2">
          <SeatPlate
            name="Your rival"
            seat={rival}
            hasButton={view.buttonSeat === rival.index}
            isToAct={view.seatToAct === rival.index}
            isViewer={false}
            presence={props.rivalPresence ?? null}
          />
          <div className="flex justify-center gap-2 [--w:40px]">
            <Hand
              cards={rival.holeCards}
              hiddenLabel="your rival's hidden hand"
            />
          </div>
          <BetLine committed={rival.committedThisStreet} />
        </div>
      )}
      <div className="flex flex-col items-center gap-4">
        <PotStrip view={view} />
        <BoardCards cards={view.board.cards} />
      </div>
      {you !== undefined && (
        <div className="flex flex-col gap-4">
          <div className="flex justify-center gap-3 [--w:96px]">
            <Hand cards={you.holeCards} hiddenLabel="your hidden hand" />
          </div>
          <SeatPlate
            name="You"
            seat={you}
            hasButton={view.buttonSeat === you.index}
            isToAct={view.seatToAct === you.index}
            isViewer
          />
        </div>
      )}
    </div>
  );
}

/**
 * The chips a seat has out on this street. The word is the field's, not an
 * action's: the view says how much is committed and never says whether it got
 * there by a blind, a call, a bet or a raise. The line keeps its height when
 * there is nothing to say, so nothing below it moves.
 */
function BetLine(props: { committed: number }): ReactElement {
  return (
    <p className="min-h-[calc(var(--pd-fs-small)*var(--pd-lh-body))] text-center text-small text-text-faint">
      {props.committed > 0 && (
        <>
          committed{" "}
          <span className="font-mono text-text tabular-nums">
            {formatChips(props.committed)}
          </span>
        </>
      )}
    </p>
  );
}
