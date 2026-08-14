import type { ClientMessage, Connection, ServerMessage } from "../protocol";
import { createDuelStore, type DuelStore } from "./duel-store";

/** The tab's store, and the one way out to the server. */
export interface DuelClient {
  readonly store: DuelStore;
  readonly send: (message: ClientMessage) => void;
}

export interface BootOptions {
  readonly connect: (onMessage: (message: ServerMessage) => void) => Connection;
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
  });

  return {
    store,
    send: (message) => {
      connection.send(message);
    },
  };
}
