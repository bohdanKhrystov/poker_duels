import { readDeviceId } from "./device-id";
import { encodeClientMessage } from "./frames";
import type {
  ClientMessage,
  ProtocolError,
  ServerMessage,
} from "./protocol.gen";
import { PROTOCOL_VERSION } from "./version";

export type ConnectionStatus =
  | { readonly kind: "connecting" }
  | { readonly kind: "ready"; readonly deviceId: string }
  | { readonly kind: "refused"; readonly error: ProtocolError }
  | { readonly kind: "outdated" };

export interface ConnectionOptions {
  readonly socket: WebSocket;
  readonly storage: Storage;
  readonly onMessage: (message: ServerMessage) => void;
}

export interface Connection {
  readonly status: ConnectionStatus;
  send(message: ClientMessage): void;
  close(): void;
}

/**
 * Wraps an already-constructed socket and says `Hello` the moment it opens,
 * carrying the device id this browser holds (or `null` on a first visit) and
 * the protocol version this client speaks. `openConnection` sets nothing
 * beyond `onopen` — inbound frames, reconnection and identity persistence
 * each belong to a later ticket.
 */
export function openConnection(options: ConnectionOptions): Connection {
  // TASK-030309 and TASK-030310 reassign this once `Ready`/`Refused`/
  // `Outdated` arrive; `prefer-const` cannot see across tickets.
  // eslint-disable-next-line prefer-const
  let status: ConnectionStatus = { kind: "connecting" };

  options.socket.onopen = (): void => {
    options.socket.send(
      encodeClientMessage({
        type: "Hello",
        deviceId: readDeviceId(options.storage),
        protocolVersion: PROTOCOL_VERSION,
      }),
    );
  };

  return {
    get status(): ConnectionStatus {
      return status;
    },
    send(message: ClientMessage): void {
      options.socket.send(encodeClientMessage(message));
    },
    close(): void {
      options.socket.close();
    },
  };
}
