import type { ReactElement } from "react";

/**
 * The rematch button. One click calls the callback; a second press is harmless.
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
}): ReactElement | null {
  if (props.mySeat === null) {
    return null;
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
