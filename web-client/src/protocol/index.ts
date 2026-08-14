import { openConnection, type Connection } from "./connection";
import { socketUrl } from "./socket-url";
import type { ServerMessage } from "./protocol.gen";

export type * from "./protocol.gen";
export { PROTOCOL_VERSION } from "./version";
export { DEVICE_ID_STORAGE_KEY, readDeviceId } from "./device-id";
export { socketUrl } from "./socket-url";
export { openConnection } from "./connection";
export type {
  Connection,
  ConnectionOptions,
  ConnectionStatus,
} from "./connection";

/** Opens this client's one socket to the duel server. */
export function connectToDuelServer(
  onMessage: (message: ServerMessage) => void,
): Connection {
  return openConnection({
    socket: new WebSocket(socketUrl(window.location)),
    storage: localStorage,
    onMessage,
  });
}
