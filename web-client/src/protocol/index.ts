import { type Connection } from "./connection";
import { socketUrl } from "./socket-url";
import type { ServerMessage } from "./protocol.gen";
import {
  openReconnectingConnection,
  type ReconnectingOptions,
} from "./reconnecting";

export type * from "./protocol.gen";
export { PROTOCOL_VERSION } from "./version";
export { DEVICE_ID_STORAGE_KEY, readDeviceId } from "./device-id";
export {
  ROOM_CODE_STORAGE_KEY,
  forgetRoomCode,
  readRoomCode,
  writeRoomCode,
} from "./room-memory";
export { socketUrl } from "./socket-url";
export { openConnection } from "./connection";
export type {
  Connection,
  ConnectionOptions,
  ConnectionStatus,
} from "./connection";
export { openReconnectingConnection };
export type { ReconnectingOptions };

/** Opens this client's one socket to the duel server. */
export function connectToDuelServer(
  onMessage: (message: ServerMessage) => void,
): Connection {
  return openReconnectingConnection({
    // Re-read on every attempt rather than captured once: a socket opened an
    // hour later should still go where this page came from.
    openSocket: () => new WebSocket(socketUrl(window.location)),
    storage: localStorage,
    onMessage,
  });
}
