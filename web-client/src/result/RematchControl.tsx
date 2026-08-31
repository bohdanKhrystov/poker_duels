import { useState, type ReactElement } from "react";
import type { ProtocolError } from "../protocol/protocol.gen";
import { rematchStand } from "./rematch-stand";

/**
 * The rematch button and its companion states. One click calls the callback; a second press is harmless.
 *
 * Returns nothing for a client that holds no seat — one with no identity has
 * no authority to offer anything. A button that can only fail is not offered.
 *
 * `ADR-0044` §3 makes `OfferRematch` idempotent on the wire — a repeat is answered
 * with the same `RematchOffered`, never an error — so no `disabled` lock guards the
 * button, unlike `ActionBar`'s `sent` lock, which exists because `Act` is not
 * idempotent. The one `useState` below is not that kind of guard: it marks that
 * this seat has just matched a standing offer, which `ADR-0044` §4 answers with the
 * new hand's `Snapshot` directly and never a restated `RematchOffered` — no message
 * ever states "both sides now want it," so this is the only way the accepting
 * seat's own client can show the card's *"it begins"* frame
 * (`design/screens/rematch-states.html`) for the span between that click and the
 * `Snapshot` that ends it (`TASK-121102`).
 *
 * When the room is reaped, `refusal` becomes `UNKNOWN_ROOM`, the control retires
 * with a message, and no other refusal touches it — as per `ADR-0044` §6, this is
 * the frame that ends a rematch.
 */
export function RematchControl(props: {
  mySeat: number | null;
  onOffer: () => void;
  offers: readonly number[];
  refusal: ProtocolError | null;
}): ReactElement | null {
  const [accepted, setAccepted] = useState(false);

  if (props.mySeat === null) {
    return null;
  }

  // The room is gone: retire the control and state it plainly.
  if (props.refusal === "UNKNOWN_ROOM") {
    return (
      <div className="text-center text-small text-text-muted">
        That duel room is gone.
      </div>
    );
  }

  const { mine, theirs } = rematchStand(props.offers, props.mySeat);

  // Both sides want it and the new hand has not arrived: `accepted` is this seat's own click
  // matching a standing offer, and `mine && theirs` is the store carrying both seats (the
  // restatement case the old comment here called "should not happen per ADR-0044 §4" — now it
  // gets the same frame instead of the stale "waiting" chip below).
  if (accepted || (mine && theirs)) {
    return (
      <div className="text-center text-text-muted">
        Rematch. The button changes sides —<br />
        dealing hand 1…
      </div>
    );
  }

  if (mine) {
    return (
      <div className="rounded-medium border border-accent bg-accent-subtle px-5 py-4 text-center leading-tight font-medium text-text">
        Rematch offered — waiting for your rival
      </div>
    );
  }

  if (theirs) {
    return (
      <>
        <div className="text-center text-small font-medium text-accent">
          Your rival offers a rematch
        </div>
        <button
          type="button"
          onClick={() => {
            setAccepted(true);
            props.onOffer();
          }}
          className="rounded-medium bg-accent-fill px-5 py-4 leading-tight font-medium text-on-accent"
        >
          Rematch
        </button>
      </>
    );
  }

  return (
    <button
      type="button"
      onClick={props.onOffer}
      className="rounded-medium bg-accent-fill px-5 py-4 leading-tight font-medium text-on-accent"
    >
      Rematch
    </button>
  );
}
