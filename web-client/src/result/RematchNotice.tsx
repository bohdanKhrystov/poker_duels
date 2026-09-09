import { useState, type ReactElement } from "react";
import { useScreen } from "../routing/use-screen";
import { roomStanding, rulingOn } from "../routing/room-standing";
import { useDuelState, useRoomAwaited, useSend } from "../store/duel-provider";
import { rematchStand } from "./rematch-stand";
import {
  RIVAL_OFFERS,
  REMATCH_LABEL,
  DEALING_LEAD,
  DEALING_TAIL,
  ROOM_GONE,
} from "./rematch-text";

// ADR-0138 §4: copied, not imported — account-offer-text.ts's OFFER_DISMISS
// is a different surface's label, and importing it here would tie this
// panel's wording to that one.
const NOT_NOW = "Not now";

/**
 * The standing rematch offer, mounted beside `Lobby` rather than inside it
 * (`ADR-0138` §1), so its state survives every screen change instead of
 * dying on a return to `/`. Takes no props: it reads `useDuelState`,
 * `useRoomAwaited` and `useScreen` for itself, because being rendered by
 * `Lobby` is the one thing this component cannot be.
 *
 * Registers no effect of any kind — no `useEffect`, no `useLayoutEffect`,
 * no `setTimeout`, no `setInterval` — which is what makes `ADR-0123` §5's
 * "never retires itself on a timer" structural rather than remembered.
 *
 * Renders the rival's own standing offer and, once pressed, the dealing
 * span in its place (`RematchControl.tsx:32` holds the same span for the
 * same reason), or the gone-room sentence if `UNKNOWN_ROOM` retired the
 * accept (`TASK-141506`). `Not now` (`TASK-141507`) stands beside whichever
 * of those three the panel currently shows, since it asserts nothing about
 * the game and is the only way off.
 */
export function RematchNotice(): ReactElement | null {
  const state = useDuelState();
  const roomAwaited = useRoomAwaited();
  const { screen } = useScreen();
  const send = useSend();
  const [accepted, setAccepted] = useState(false);
  const [dismissed, setDismissed] = useState(false);

  // ADR-0138 §2: a second call of ADR-0114's one predicate, not a second
  // rule — the same two pure functions Lobby already calls, on the same
  // render's snapshot of the store and the address.
  const standing = roomStanding(state, roomAwaited);
  const shown = rulingOn(screen, standing) === "honour" ? screen : "first";
  const { theirs } = rematchStand(state.rematchOffers, state.mySeat);

  // ADR-0138 §3's clear, applied to the second boolean for §3's own reason: an
  // offer ends and another is made, and a flag that outlived its offer would show
  // the dealing sentence in place of the button on every later offer for the life
  // of the tab. Set during render, React's documented way to adjust state when
  // what it was derived from changes: it terminates (after the set the condition
  // is false), it is pure so StrictMode-safe, and it costs no effect — which
  // keeps §1's "no effect of any kind" true. Runs ahead of the screen-gated
  // early return below, because the offer can end on a render this component
  // would otherwise return null for, and the clear must not miss that render.
  if (accepted && !theirs) setAccepted(false);

  // ADR-0123 §7: a dismissal lasts as long as the offer it was about, and the
  // offer ends when the duel that answers it begins (Snapshot) or when the room
  // hands down another result (DuelFinished). Cleared here, in the render that
  // first sees the offer gone, so the next offer is not swallowed by the last
  // dismissal. Runs ahead of the early return below for the same reason as the
  // accepted-clear above: the offer can end on a render this component would
  // otherwise return null for, and the clear must not miss that render.
  if (dismissed && !theirs) setDismissed(false);

  if (shown === "first" || !theirs || dismissed) return null;

  return (
    <div
      role="status"
      className="fixed right-5 bottom-5 left-5 z-10 mx-auto flex max-w-md flex-col gap-4 rounded-medium bg-surface-raised p-5 shadow-pop"
    >
      {state.refusal === "UNKNOWN_ROOM" ? (
        <p className="rounded-small border border-accent bg-accent-subtle px-4 py-3 text-body">
          {ROOM_GONE}
        </p>
      ) : accepted ? (
        <p className="text-center text-text-muted">
          {DEALING_LEAD}
          <br />
          {DEALING_TAIL}
        </p>
      ) : (
        <>
          <p className="rounded-small border border-accent bg-accent-subtle px-4 py-3 text-body">
            {RIVAL_OFFERS}
          </p>
          <button
            type="button"
            onClick={() => {
              setAccepted(true);
              send({ type: "OfferRematch" });
            }}
            className="rounded-medium bg-accent-fill px-5 py-4 leading-tight font-medium text-on-accent"
          >
            {REMATCH_LABEL}
          </button>
        </>
      )}
      {/* ADR-0138 §4: the dismiss stands in all three states, because it
       * asserts nothing about the game — it is the only way off. So it sits
       * outside the branch above that swaps the accept for the dealing
       * sentence or for ROOM_GONE. */}
      <button
        type="button"
        onClick={() => setDismissed(true)}
        className="text-center text-text-muted underline"
      >
        {NOT_NOW}
      </button>
    </div>
  );
}
