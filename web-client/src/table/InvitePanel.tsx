import { useRef, useState, type ReactElement, type RefObject } from "react";
import { roomLink } from "../lobby/room-link";

/**
 * The invite is selectable text before it is anything else: the one interaction
 * this product depends on cannot need a working clipboard.
 */
export function InvitePanel(props: { readonly code: string }): ReactElement {
  const link = roomLink(window.location.origin, props.code);
  const box = useRef<HTMLInputElement>(null);
  return (
    <>
      <p className="text-center text-small text-text-muted">Room code</p>
      <p className="rounded-medium border border-hairline bg-surface px-5 py-4 text-center font-mono text-display tracking-[var(--pd-track-code)] text-text">
        {props.code}
      </p>
      <label
        htmlFor="invite-link"
        className="mt-2 text-center text-small text-text-muted"
      >
        Invite link
      </label>
      <input
        autoFocus
        ref={box}
        id="invite-link"
        className="w-full rounded-medium border border-hairline bg-surface px-5 py-4 text-center font-mono text-small text-text"
        readOnly
        value={link}
        onFocus={(event) => event.currentTarget.select()}
      />
      <CopyLink link={link} box={box} />
    </>
  );
}

/** ADR-0128 §1: never absent. §3: a press that cannot copy hands over the selection. */
function CopyLink(props: {
  readonly link: string;
  readonly box: RefObject<HTMLInputElement>;
}): ReactElement {
  const [outcome, setOutcome] = useState<"none" | "copied" | "refused">("none");
  const handOver = (): void => {
    props.box.current?.focus();
    props.box.current?.select();
    setOutcome("refused");
  };
  return (
    <>
      <button
        type="button"
        className="rounded-medium border border-transparent bg-accent-fill px-5 py-4 leading-tight font-medium text-on-accent"
        onClick={() => {
          if (!navigator.clipboard) {
            handOver();
            return;
          }
          void navigator.clipboard
            .writeText(props.link)
            .then(() => setOutcome("copied"), handOver);
        }}
      >
        Copy the link
      </button>
      {/* Reserved whether or not there is anything to say, so saying it moves nothing. */}
      <p className="min-h-[calc(var(--pd-fs-small)*var(--pd-lh-body))] text-center text-small text-text-muted">
        {outcome === "copied" && "Link copied."}
        {outcome === "refused" && "Copy it from the box above."}
      </p>
    </>
  );
}
