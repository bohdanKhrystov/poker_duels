import { useState, type ReactElement } from "react";
import { roomLink } from "../lobby/room-link";

/**
 * The invite is selectable text before it is anything else: the one interaction
 * this product depends on cannot need a working clipboard.
 */
export function InvitePanel(props: { readonly code: string }): ReactElement {
  const link = roomLink(window.location.origin, props.code);
  return (
    <>
      <p className="rounded-medium border border-hairline bg-surface px-5 py-4 text-center font-mono text-display tracking-[var(--pd-track-code)] text-text">
        {props.code}
      </p>
      <label htmlFor="invite-link">Invite link</label>
      <input
        autoFocus
        id="invite-link"
        className="rounded-medium border border-hairline bg-surface px-5 py-4 text-text"
        readOnly
        value={link}
        onFocus={(event) => event.currentTarget.select()}
      />
      <CopyLink link={link} />
    </>
  );
}

/** Absent where the clipboard API is: the box above is always the fallback. */
function CopyLink(props: { readonly link: string }): ReactElement | null {
  const [outcome, setOutcome] = useState<"none" | "copied" | "refused">("none");
  if (!navigator.clipboard) {
    return null;
  }
  return (
    <>
      <button
        type="button"
        className="rounded-medium border border-transparent bg-accent-fill px-5 py-4 leading-tight font-medium text-on-accent"
        onClick={() => {
          void navigator.clipboard.writeText(props.link).then(
            () => setOutcome("copied"),
            () => setOutcome("refused"),
          );
        }}
      >
        Copy the link
      </button>
      {outcome === "copied" && <p>Link copied.</p>}
      {outcome === "refused" && <p>Copy it from the box above.</p>}
    </>
  );
}
