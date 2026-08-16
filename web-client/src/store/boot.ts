import { forgetRoomCode, readRoomCode, writeRoomCode } from "../protocol";
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
  /**
   * Where this tab remembers the room it is seated in, so a reopened socket
   * knows what to ask for. Optional because a client that cannot remember is
   * still a working client — a test that is about something else need not
   * invent a Storage — but `main.tsx` always passes one.
   */
  readonly storage?: Storage;
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
    if (message.type === "Welcome") {
      // The invite wins over the memory: a player who has just followed a link to
      // a new room means that room, whatever this browser was in last. The memory
      // is what answers for the host, whose URL never carried a code, and for any
      // tab whose socket has been reopened under it.
      const remembered = options.storage ? readRoomCode(options.storage) : null;
      const code = options.joinRoomCode ?? remembered;
      if (code !== null) connection.send({ type: "JoinRoom", code });
    }
    if (message.type === "RoomJoined" && options.storage) {
      writeRoomCode(options.storage, message.code);
    }
    if (message.type === "DuelFinished" && options.storage) {
      // The way on from the result is a reload (TASK-030807). A tab that still
      // remembered this room would rejoin it and be handed the same DuelFinished
      // back, and the lobby would be unreachable.
      forgetRoomCode(options.storage);
    }
  });

  return {
    store,
    send: (message) => {
      connection.send(message);
    },
  };
}
