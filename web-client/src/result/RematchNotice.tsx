import type { ReactElement } from "react";
import { useScreen } from "../routing/use-screen";
import { roomStanding, rulingOn } from "../routing/room-standing";
import { useDuelState, useRoomAwaited } from "../store/duel-provider";
import { rematchStand } from "./rematch-stand";
import { RIVAL_OFFERS } from "./rematch-text";

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
 * Renders the rival's own standing offer and nothing else: the accept
 * button, the dealing span, the gone-room sentence and the dismissal are
 * later tickets' work (`TASK-141505`–`TASK-141507`).
 */
export function RematchNotice(): ReactElement | null {
  const state = useDuelState();
  const roomAwaited = useRoomAwaited();
  const { screen } = useScreen();

  // ADR-0138 §2: a second call of ADR-0114's one predicate, not a second
  // rule — the same two pure functions Lobby already calls, on the same
  // render's snapshot of the store and the address.
  const standing = roomStanding(state, roomAwaited);
  const shown = rulingOn(screen, standing) === "honour" ? screen : "first";
  const { theirs } = rematchStand(state.rematchOffers, state.mySeat);
  if (shown === "first" || !theirs) return null;

  return (
    <div
      role="status"
      className="fixed right-5 bottom-5 left-5 z-10 mx-auto flex max-w-md flex-col gap-4 rounded-medium bg-surface-raised p-5 shadow-pop"
    >
      <p className="rounded-small border border-accent bg-accent-subtle px-4 py-3 text-body">
        {RIVAL_OFFERS}
      </p>
    </div>
  );
}
