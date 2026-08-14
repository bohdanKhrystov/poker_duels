import { useState, type ReactElement } from "react";
import type { ProtocolError } from "../protocol";
import { useDuelState, useSend } from "../store/duel-provider";
import { normalizeRoomCode, roomLink } from "./room-link";

/** The first screen: open a duel room, or join one by the code on the invite. */
export function Lobby(): ReactElement {
  const state = useDuelState();
  const send = useSend();
  const [typedCode, setTypedCode] = useState("");
  const code = normalizeRoomCode(typedCode);

  if (state.roomCode !== null) {
    return <WaitingForRival code={state.roomCode} />;
  }

  return (
    <section>
      {state.refusal !== null && <p>{refusalMessage(state.refusal)}</p>}
      <button type="button" onClick={() => send({ type: "CreateRoom" })}>
        Create a duel room
      </button>
      <form
        onSubmit={(event) => {
          event.preventDefault();
          // An empty code would spend one of the ten failed joins ADR-0022
          // budgets this player every minute, and tell them nothing.
          if (code === "") return;
          send({ type: "JoinRoom", code });
        }}
      >
        <label htmlFor="room-code">Room code</label>
        <input
          id="room-code"
          value={typedCode}
          onChange={(event) => setTypedCode(event.target.value)}
        />
        <button type="submit">Join the duel</button>
      </form>
    </section>
  );
}

/**
 * The client shows the refusal and stops. Retrying on the player's behalf would
 * spend the ten failed joins a minute `ADR-0022` budgets them.
 */
function refusalMessage(error: ProtocolError): string {
  switch (error) {
    case "UNKNOWN_ROOM":
      return "No duel room has that code.";
    case "ROOM_FULL":
      return "That duel room already has a rival in it.";
    default:
      return "The server refused that.";
  }
}

/**
 * The invite is selectable text before it is anything else: the one interaction
 * this product depends on cannot need a working clipboard.
 */
function WaitingForRival(props: { code: string }): ReactElement {
  const link = roomLink(window.location.origin, props.code);
  return (
    <section>
      <h2>Waiting for your rival</h2>
      <p>{props.code}</p>
      <label htmlFor="invite-link">Invite link</label>
      <input
        autoFocus
        id="invite-link"
        readOnly
        value={link}
        onFocus={(event) => event.currentTarget.select()}
      />
      <CopyLink link={link} />
    </section>
  );
}

/** Absent where the clipboard API is: the box above is always the fallback. */
function CopyLink(props: { link: string }): ReactElement | null {
  const [outcome, setOutcome] = useState<"none" | "copied" | "refused">("none");
  if (!navigator.clipboard) {
    return null;
  }
  return (
    <>
      <button
        type="button"
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
