import { readDeviceId, writeDeviceId } from "./device-id";
import { decodeServerMessage, encodeClientMessage } from "./frames";
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
 * the protocol version this client speaks. It also forwards every inbound
 * frame the codec can decode to `options.onMessage` and drops the rest —
 * reconnection still belongs to a later ticket.
 */
export function openConnection(options: ConnectionOptions): Connection {
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

  options.socket.onmessage = (event) => {
    const message = decodeServerMessage(event.data);
    if (message === null) {
      // A frame we cannot read is not an error to raise into a render: the next
      // Snapshot re-establishes the truth, which is why the server sends one
      // after every transition.
      console.warn("protocol: dropped an unreadable frame", event.data);
      return;
    }
    if (message.type === "Welcome") {
      if (message.protocolVersion === PROTOCOL_VERSION) {
        writeDeviceId(options.storage, message.deviceId);
        status = { kind: "ready", deviceId: message.deviceId };
      } else {
        status = { kind: "outdated" };
      }
    }
    options.onMessage(message);
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
