import type { ReactElement } from "react";
import { rematchStand } from "./rematch-stand";

/**
 * The rematch button and its companion states. One click calls the callback; a second press is harmless.
 *
 * Returns nothing for a client that holds no seat — one with no identity has
 * no authority to offer anything. A button that can only fail is not offered.
 *
 * No `disabled` state or in-flight `useState`: `ADR-0044` §3 makes `OfferRematch`
 * idempotent on the wire — a repeat is answered with the same `RematchOffered`,
 * never an error. A double press cannot produce an error state and needs no guard.
 * Unlike `ActionBar`'s `sent` lock, which exists because `Act` is not idempotent.
 */
export function RematchControl(props: {
  mySeat: number | null;
  onOffer: () => void;
  offers: readonly number[];
}): ReactElement | null {
  if (props.mySeat === null) {
    return null;
  }

  const { mine, theirs } = rematchStand(props.offers, props.mySeat);

  // When both mine and theirs are true, show the chip (this should not happen per ADR-0044 §4,
  // as the second offer starts the duel and sends no RematchOffered, but handle it for completeness).
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
          onClick={props.onOffer}
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
