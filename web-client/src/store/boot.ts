import type { ClientMessage, Connection, ServerMessage } from "../protocol";
import { createDuelStore, type DuelStore } from "./duel-store";

/** The tab's store, and the one way out to the server. */
export interface DuelClient {
  readonly store: DuelStore;
  readonly send: (message: ClientMessage) => void;
}

export interface BootOptions {
  readonly connect: (onMessage: (message: ServerMessage) => void) => Connection;
  /** The code this tab's URL carried, or `null` when it carried none. */
  readonly joinRoomCode: string | null;
}

/**
 * Wires this tab's one connection to its one store, outside React's tree:
 * `main.tsx` calls this once, before rendering (`ADR-0032`). Nothing closes the
 * connection — closing the tab is the close.
 */
export function bootDuelClient(options: BootOptions): DuelClient {
  const store = createDuelStore();
  const connection = options.connect((message) => {
    store.apply(message);
    // A message-triggered send is a boot reaction, never a screen effect: one
    // boot per tab and one Welcome per socket is the whole of "exactly once",
    // with no ref, no guard and no cleanup anywhere (ADR-0032).
    if (message.type === "Welcome" && options.joinRoomCode !== null) {
      connection.send({ type: "JoinRoom", code: options.joinRoomCode });
    }
  });

  return {
    store,
    send: (message) => {
      connection.send(message);
    },
  };
}
