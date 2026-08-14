import { useState, type ReactElement } from "react";
import { useSend } from "../store/duel-provider";
import { normalizeRoomCode } from "./room-link";

/** The first screen: open a duel room, or join one by the code on the invite. */
export function Lobby(): ReactElement {
  const send = useSend();
  const [typedCode, setTypedCode] = useState("");
  const code = normalizeRoomCode(typedCode);

  return (
    <section>
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
