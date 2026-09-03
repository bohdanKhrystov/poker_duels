import type { ReactElement } from "react";
import type { GameEvent, PlayerView, SeatPresence, Street } from "../protocol";
import type { ActEvent } from "../store/duel-state";
import { BoardCards } from "./BoardCards";
import { PotStrip } from "./PotStrip";
import { SeatPlate } from "./SeatPlate";
import { Hand } from "./Hand";
import { formatChips } from "./chips";
import { ChipPile } from "./ChipPile";

/**
 * The duel table: one column, rival above, board between, you below.
 *
 * Everything on it is read off the `PlayerView` the server computed. Nothing is
 * worked out here — not the pot, not the street, not whose cards these are, and
 * not what anyone may do next.
 *
 * Renders no wrapping column of its own (`ADR-0103` §5): the container-query
 * context, the width cap and the height budget live once, on the screen that
 * assembles this with the action bar below it — a second copy of that cap
 * here was the duplicate column the ADR names, and duplicating it left the
 * centre block's `flex-1` with no slack to claim.
 */
export function DuelTable(props: {
  view: PlayerView;
  rivalPresence?: SeatPresence | null;
  narration?: readonly GameEvent[];
  /**
   * The board and street a hand's ending is currently standing on, or absent for ordinary play
   * (`ADR-0102` §2). The only two fields a runout ever lags — every seat plate below is still
   * the snapshot's own, unlagged.
   */
  revealStep?: { board: readonly string[]; street: Street } | null;
  /**
   * The most recent act of the hand on screen (`ADR-0109` §1), or absent before the hand has
   * made one. The store's own `lastAct` field, never worked out here — and never handed to both
   * plates: exactly one `SeatPlate` below receives it, chosen by `lastAct.seat` alone.
   */
  lastAct?: ActEvent | null;
}): ReactElement {
  const { view } = props;
  const board = props.revealStep?.board ?? view.board.cards;
  const you = view.seats.find((seat) => seat.index === view.viewerSeat);
  const rival = view.seats.find((seat) => seat.index !== view.viewerSeat);
  return (
    <>
      {rival !== undefined && (
        <div className="flex flex-col gap-2">
          <SeatPlate
            name="Your rival"
            seat={rival}
            hasButton={view.buttonSeat === rival.index}
            isToAct={view.seatToAct === rival.index}
            isViewer={false}
            presence={props.rivalPresence ?? null}
            lastAct={props.lastAct?.seat === rival.index ? props.lastAct : null}
          />
          {/* ADR-0103 §3.2: the rival's face-down hand narrows furthest of
              anything on the table — her name, her stack, her button and
              whose turn it is are on the plate directly above it. */}
          <div className="flex justify-center gap-2 [--w:clamp(24px,calc((100cqi-135px)/10.625),40px)]">
            <Hand
              cards={rival.holeCards}
              hiddenLabel="your rival's hidden hand"
            />
          </div>
          <BetLine committed={rival.committedThisStreet} />
        </div>
      )}
      {/* ADR-0103 §1: the centre block is the one that claims the column's
          slack (`flex-1`) — it can only do that as a direct child of the
          `min-h-[100dvh]` column the screen above provides. */}
      <div className="flex flex-1 flex-col items-center justify-center gap-4">
        <PotStrip
          view={view}
          narration={props.narration}
          street={props.revealStep?.street}
        />
        <BoardCards cards={board} />
      </div>
      {you !== undefined && (
        <div className="flex flex-col gap-4">
          {/* ADR-0103 §3.3: the hero's own hole cards narrow too, floored at
              the board's own card width — `clamp(48px,calc((100cqi-64px)/5),72px)`
              is `BoardCards.tsx`'s own `--w`, repeated as the floor rather than
              shared through a variable, so this block never depends on a name
              declared outside it. A table that drew the shared five larger
              than the private two would invert the game's own emphasis. */}
          <div className="flex justify-center gap-3 [--w:clamp(clamp(48px,calc((100cqi-64px)/5),72px),calc((100cqi-40px)/5),96px)]">
            <Hand cards={you.holeCards} hiddenLabel="your hidden hand" />
          </div>
          <SeatPlate
            name="You"
            seat={you}
            hasButton={view.buttonSeat === you.index}
            isToAct={view.seatToAct === you.index}
            isViewer
            lastAct={props.lastAct?.seat === you.index ? props.lastAct : null}
          />
        </div>
      )}
    </>
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
          <ChipPile key={props.committed} />
          committed{" "}
          <span className="font-mono text-text tabular-nums">
            {formatChips(props.committed)}
          </span>
        </>
      )}
    </p>
  );
}
