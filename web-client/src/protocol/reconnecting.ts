import { openConnection } from "./connection";
import type { Connection, ConnectionStatus } from "./connection";
import type { ClientMessage, ServerMessage } from "./protocol.gen";
import { retryDelayMillis } from "./retry-delay";

export interface ReconnectingOptions {
  /** Constructs a fresh socket. Called once per attempt, never reused. */
  readonly openSocket: () => WebSocket;
  readonly storage: Storage;
  readonly onMessage: (message: ServerMessage) => void;
  /** A number in [0, 1) per attempt. Injected so a test hands it a value. */
  readonly jitter?: () => number;
}

/**
 * A `Connection` that outlives its socket: when the socket closes it opens
 * another, after the delay `retryDelayMillis` names, and forwards whatever
 * the live socket reports.
 *
 * Each attempt is a fresh `openConnection`, so the `Hello`, the device id and
 * the frame codec stay exactly where `STORY-0303` put them — this module
 * adds only the socket lifecycle around them.
 */
export function openReconnectingConnection(
  options: ReconnectingOptions,
): Connection {
  const jitter = options.jitter ?? Math.random;

  let stopped = false;
  let live = false;
  let failures = 0;

  function forward(message: ServerMessage): void {
    // A handshake that completed is a connection that worked, so the next
    // drop deserves a fast retry rather than yesterday's ceiling.
    if (message.type === "Welcome") {
      failures = 0;
    }
    options.onMessage(message);
  }

  function attach(): Connection {
    const socket = options.openSocket();
    live = true;
    // `openConnection` sets `onopen` and `onmessage` and nothing else, so
    // there is no existing handler on `onclose` to preserve.
    socket.onclose = (): void => {
      live = false;
      if (stopped) return;
      const delay = retryDelayMillis(failures, jitter());
      failures += 1;
      setTimeout(() => {
        current = attach();
      }, delay);
    };
    return openConnection({
      socket,
      storage: options.storage,
      onMessage: forward,
    });
  }

  let current: Connection = attach();

  return {
    get status(): ConnectionStatus {
      return current.status;
    },
    send(message: ClientMessage): void {
      // An action taken while disconnected is not an action.
      if (!live) return;
      current.send(message);
    },
    close(): void {
      // Set before delegating, so the close it causes schedules nothing.
      stopped = true;
      current.close();
    },
  };
}
