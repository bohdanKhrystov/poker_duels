import type { ReactElement } from "react";
import type { PlayerView } from "../protocol";
import { BoardCards } from "./BoardCards";
import { PotStrip } from "./PotStrip";
import { SeatPlate } from "./SeatPlate";
import { Hand } from "./Hand";

/**
 * The duel table: one column, rival above, board between, you below.
 *
 * Everything on it is read off the `PlayerView` the server computed. Nothing is
 * worked out here — not the pot, not the street, not whose cards these are, and
 * not what anyone may do next.
 */
export function DuelTable(props: { view: PlayerView }): ReactElement {
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
          />
          <div className="flex justify-center gap-2 [--w:40px]">
            <Hand
              cards={rival.holeCards}
              hiddenLabel="your rival's hidden hand"
            />
          </div>
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
