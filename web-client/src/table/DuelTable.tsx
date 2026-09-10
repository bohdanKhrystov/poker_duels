import type { ReactElement } from "react";
import type { GameEvent, PlayerView, SeatPresence, Street } from "../protocol";
import type { ActEvent } from "../store/duel-state";
import type { ClockReading } from "./turn-clock";
import { BoardCards } from "./BoardCards";
import { PotStrip } from "./PotStrip";
import { SeatPlate } from "./SeatPlate";
import { Hand } from "./Hand";
import { formatChips } from "./chips";
import { ChipPile } from "./ChipPile";
import { seatClock } from "./turn-clock";
import { runoutView } from "./runout-view";
import { winningCards } from "./winning-cards";

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
  /**
   * The clock the store anchored and the reading to draw it against — never re-derived here
   * (`ADR-0113` §6). Absent before a `TurnClock` has arrived, which is the merged behaviour
   * every existing caller and every existing test still gets.
   */
  clock?: ClockReading | null;
  /**
   * The two seats' display names, in seat order, as the server stated them in `SeatNames`
   * — `null` for a seat with no name. The rival's plate carries their name when there is
   * one and "Your rival" otherwise; the hero's plate always says "You", since across a
   * heads-up table there is nobody else it could mean.
   */
  names?: readonly (string | null)[];
}): ReactElement {
  const board = props.revealStep?.board ?? props.view.board.cards;
  // A runout step that is not the hand's last beat draws the seats and the
  // pot as they stood before the award (`runout-view.ts`): the snapshot behind
  // it has already paid the pot out, and a stack that knows the winner before
  // the river is turned spoils the river. The last beat — the step whose street
  // is the snapshot's own `COMPLETE` — draws the snapshot as it is.
  const lagging =
    props.revealStep !== null &&
    props.revealStep !== undefined &&
    props.revealStep.street !== "COMPLETE";
  const view = lagging
    ? runoutView(props.view, props.narration ?? [])
    : props.view;
  const you = view.seats.find((seat) => seat.index === view.viewerSeat);
  const rival = view.seats.find((seat) => seat.index !== view.viewerSeat);
  // The five that took the pot, marked only on the hand's last beat — never
  // while a runout is still turning cards, and never mid-hand, where the set
  // is empty anyway.
  const marked =
    !lagging && view.street === "COMPLETE"
      ? winningCards(props.narration ?? [], view.handNumber)
      : undefined;
  return (
    <>
      {rival !== undefined && (
        <div className="flex flex-col gap-2">
          <SeatPlate
            name={props.names?.[rival.index] ?? "Your rival"}
            seat={rival}
            hasButton={view.buttonSeat === rival.index}
            isToAct={view.seatToAct === rival.index}
            isViewer={false}
            presence={props.rivalPresence ?? null}
            lastAct={props.lastAct?.seat === rival.index ? props.lastAct : null}
            clock={seatClock(
              props.clock ?? null,
              rival.index,
              view.seatToAct,
              view.handNumber,
            )}
          />
          {/* ADR-0103 §3.2: the rival's face-down hand narrows furthest of
              anything on the table — her name, her stack, her button and
              whose turn it is are on the plate directly above it. */}
          <div
            className={`flex justify-center gap-2 ${
              rival.holeCards.length === 0
                ? "[--w:clamp(24px,calc((100cqi-135px)/10.625),40px)]"
                : // Face up, the rival's hand is read, not counted: it takes
                  // the hero's own card width so a shown hand is as legible
                  // as the one below it.
                  "gap-3 [--w:clamp(clamp(48px,calc((100cqi-64px)/5),72px),calc((100cqi-40px)/5),96px)]"
            }`}
          >
            <Hand
              cards={rival.holeCards}
              hiddenLabel="your rival's hidden hand"
              marked={marked}
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
        <BoardCards cards={board} marked={marked} />
      </div>
      {you !== undefined && (
        <div className="flex flex-col gap-4">
          {/* ADR-0103 §3.3: the hero's own hole cards narrow too, floored at
              the board's own card width — `clamp(48px,calc((100cqi-64px)/5),72px)`
              is `BoardCards.tsx`'s own `--w`, repeated as the floor rather than
              shared through a variable, so this block never depends on a name
              declared outside it. A table that drew the shared five larger
              than the private two would invert the game's own emphasis.
              The hero's own bet line sits beside the cards, in the row's own
              height, so it costs the phone shape no vertical room — the
              column is measured to the pixel at 390 × 664 (ADR-0121). */}
          <div className="relative flex justify-center gap-3 [--w:clamp(clamp(48px,calc((100cqi-64px)/5),72px),calc((100cqi-40px)/5),96px)]">
            <div className="absolute top-[50%] left-[0px] -translate-y-[50%]">
              <BetLine committed={you.committedThisStreet} />
            </div>
            <Hand
              cards={you.holeCards}
              hiddenLabel="your hidden hand"
              marked={marked}
            />
          </div>
          <SeatPlate
            name="You"
            seat={you}
            hasButton={view.buttonSeat === you.index}
            isToAct={view.seatToAct === you.index}
            isViewer
            lastAct={props.lastAct?.seat === you.index ? props.lastAct : null}
            clock={seatClock(
              props.clock ?? null,
              you.index,
              view.seatToAct,
              view.handNumber,
            )}
          />
        </div>
      )}
    </>
  );
}

/**
 * The chips a seat has out on this street — drawn for both seats, so a player
 * can see what they themselves have put in front of them as well as what
 * their rival has. The word is the field's, not an action's: the view says how
 * much is committed and never says whether it got there by a blind, a call, a
 * bet or a raise. The line keeps its height when there is nothing to say, so
 * nothing below it moves.
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
