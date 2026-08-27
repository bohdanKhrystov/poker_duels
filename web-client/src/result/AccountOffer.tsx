import type { ReactElement } from "react";
import {
  OFFER_ACCEPT,
  OFFER_BODY,
  OFFER_DISMISS,
  OFFER_HEADING,
} from "./account-offer-text";
import { hashForScreen } from "../routing/screen";

export function AccountOffer(props: {
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
      >
        {OFFER_ACCEPT}
      </a>
      <button type="button" onClick={props.onDismiss}>
        {OFFER_DISMISS}
      </button>
    </section>
  );
}
