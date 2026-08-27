import type { ReactElement } from "react";
import {
  OFFER_ACCEPT,
  OFFER_BODY,
  OFFER_DISMISS,
  OFFER_HEADING,
} from "./account-offer-text";
import { hashForScreen } from "../routing/screen";

export function AccountOffer(props: {
  /**
   * Called on the accept control's click, before the browser loads the account
   * screen (`ADR-0086` §6). The link stays an `<a href="/#/account">`, so the
   * handler runs and navigation stays the browser's. Storage operations are
   * synchronous, so a handler that forgets has finished before the page leaves.
   */
  readonly onAccept: () => void;
  readonly onDismiss: () => void;
}): ReactElement {
  return (
    <section
      aria-label="the offer"
      className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 rounded-medium border border-hairline bg-surface px-5 py-7 text-center"
    >
      <p className="text-display leading-tight font-bold text-text">
        {OFFER_HEADING}
      </p>
      <p className="text-small text-text-muted">{OFFER_BODY}</p>
      <a
        className="rounded-medium border border-hairline px-5 py-4 leading-tight font-medium text-text"
        href={`/${hashForScreen("account")}`}
        onClick={props.onAccept}
      >
        {OFFER_ACCEPT}
      </a>
      <button type="button" onClick={props.onDismiss}>
        {OFFER_DISMISS}
      </button>
    </section>
  );
}
