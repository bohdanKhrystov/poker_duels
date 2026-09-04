import { forgetRoomCode, readRoomCode, writeRoomCode } from "../protocol";
import type { ClientMessage, Connection, ServerMessage } from "../protocol";
import { createDuelStore, type DuelStore } from "./duel-store";

/**
 * How long a runout's step stands (`ADR-0102` §4), named once so nowhere else in the client
 * spells out the number. A runout from preflop therefore takes four of these; an ordinary hand's
 * ending takes one.
 */
export const REVEAL_STEP_MS = 600;

/**
 * How often a live turn clock re-arms (`ADR-0113` §6), named once so nowhere else in the client
 * spells out the number — `ADR-0108` §5's "ticking once per second".
 */
export const CLOCK_TICK_MS = 1000;

/** The tab's store, and the one way out to the server. */
export interface DuelClient {
  readonly store: DuelStore;
  readonly send: (message: ClientMessage) => void;
  /**
   * Forgets the room this tab remembers, so no socket opened after this one
   * rejoins it. It tells the server nothing — there is no leave on the wire —
   * and the socket that is open keeps its seat: the memory is about the next
   * socket, never the current one.
   */
  readonly forgetRoom: () => void;
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
  /**
   * How long a reveal step stands, in milliseconds, or absent for `REVEAL_STEP_MS` — the
   * production seam `ADR-0102` §4 fixes this at, not a test-only door. `0` means synchronous,
   * which is what lets `web-client/src/e2e/drive-duel.tsx` replay a recorded frame log with no
   * clock in the way.
   */
  readonly stepMillis?: number;
  /**
   * How often a live turn clock re-arms, in milliseconds, or absent for `CLOCK_TICK_MS` — the
   * production seam `ADR-0113` §6 fixes this at, not a test-only door.
   */
  readonly tickMillis?: number;
}

/**
 * Wires this tab's one connection to its one store, outside React's tree:
 * `main.tsx` calls this once, before rendering (`ADR-0032`). Nothing closes the
 * connection — closing the tab is the close.
 */
export function bootDuelClient(options: BootOptions): DuelClient {
  const store = createDuelStore({
    stepMillis: options.stepMillis ?? REVEAL_STEP_MS,
    tickMillis: options.tickMillis ?? CLOCK_TICK_MS,
    now: () => performance.now(),
    schedule: (run, delayMillis) => {
      setTimeout(run, delayMillis);
    },
  });
  // Whether the JoinRoom this reaction sent is still unanswered. A refusal is
  // only about our room when it is answering our rejoin — the same UNKNOWN_ROOM
  // reaches a player who mistyped a code in the lobby, and that must not throw
  // away a room this tab is seated in.
  let rejoining = false;
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
      if (code !== null) {
        rejoining = true;
        connection.send({ type: "JoinRoom", code });
      }
    }
    if (message.type === "RoomJoined") {
      rejoining = false;
      if (options.storage) {
        writeRoomCode(options.storage, message.code);
      }
    }
    if (
      message.type === "Failure" &&
      message.error === "UNKNOWN_ROOM" &&
      rejoining &&
      options.storage
    ) {
      // UNKNOWN_ROOM in answer to our own rejoin means the room the tab
      // remembered has been reaped: the resume ends here so no later socket
      // asks for it again. The connection stays open — only VERSION_MISMATCH
      // ends the retry loop, because that refusal repeats forever
      // (TASK-030513, TASK-031005); this one just means the room is gone.
      rejoining = false;
      forgetRoomCode(options.storage);
    }
  });

  return {
    store,
    send: (message) => {
      connection.send(message);
    },
    forgetRoom: () => {
      if (options.storage) {
        forgetRoomCode(options.storage);
      }
    },
  };
}
