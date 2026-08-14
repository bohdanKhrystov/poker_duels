import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { openConnection } from "./connection";
import { FakeSocket } from "./fake-socket";
import type { ServerMessage } from "./protocol.gen";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`.
 *
 * Node 24+ defines its own `localStorage` global which is present but inert
 * unless the process is started with `--localstorage-file`, and under Vitest it
 * shadows the one jsdom provides: `typeof localStorage` is `"undefined"` while
 * `sessionStorage` — which Node keeps in memory — works. Depending on that
 * global would make these tests a property of the Node version rather than of
 * this module, so `openConnection` is handed a `Storage` built here instead;
 * see `device-id.test.ts` for the same pattern.
 */
function inMemoryStorage(): Storage {
  const entries = new Map<string, string>();
  return {
    get length(): number {
      return entries.size;
    },
    clear(): void {
      entries.clear();
    },
    getItem(key: string): string | null {
      return entries.has(key) ? (entries.get(key) as string) : null;
    },
    key(index: number): string | null {
      return Array.from(entries.keys())[index] ?? null;
    },
    removeItem(key: string): void {
      entries.delete(key);
    },
    setItem(key: string, value: string): void {
      entries.set(key, value);
    },
  };
}

describe("the connection", () => {
  let socket: FakeSocket;
  let storage: Storage;

  beforeEach(() => {
    socket = new FakeSocket();
    storage = inMemoryStorage();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("starts out connecting", () => {
    const connection = openConnection({
      socket: socket.asWebSocket(),
      storage,
      onMessage: () => {},
    });

    expect(connection.status).toEqual({ kind: "connecting" });
  });

  it("sends nothing before the socket opens", () => {
    openConnection({
      socket: socket.asWebSocket(),
      storage,
      onMessage: () => {},
    });

    expect(socket.sent).toEqual([]);
  });

  it("says hello with no device id on a first visit", () => {
    openConnection({
      socket: socket.asWebSocket(),
      storage,
      onMessage: () => {},
    });

    socket.open();

    expect(JSON.parse(socket.sent[0])).toEqual({
      type: "Hello",
      deviceId: null,
      protocolVersion: 2,
    });
  });

  it("says hello with the device id it already holds", () => {
    storage.setItem("pd.deviceId", "d-1");
    openConnection({
      socket: socket.asWebSocket(),
      storage,
      onMessage: () => {},
    });

    socket.open();

    expect(JSON.parse(socket.sent[0]).deviceId).toBe("d-1");
  });

  it("writes a client message to the socket", () => {
    const connection = openConnection({
      socket: socket.asWebSocket(),
      storage,
      onMessage: () => {},
    });

    connection.send({ type: "CreateRoom" });

    expect(socket.sent).toEqual(['{"type":"CreateRoom"}']);
  });

  it("closes the underlying socket", () => {
    const connection = openConnection({
      socket: socket.asWebSocket(),
      storage,
      onMessage: () => {},
    });

    connection.close();

    expect(socket.closed).toBe(true);
  });

  it("hands a decoded frame to the listener", () => {
    const messages: ServerMessage[] = [];
    openConnection({
      socket: socket.asWebSocket(),
      storage,
      onMessage: (message) => messages.push(message),
    });

    socket.receive('{"type":"RoomJoined","code":"ABCD","seat":0}');

    expect(messages).toEqual([{ type: "RoomJoined", code: "ABCD", seat: 0 }]);
  });

  it("logs and drops a frame it cannot read", () => {
    const warn = vi.spyOn(console, "warn").mockImplementation(() => {});
    const messages: ServerMessage[] = [];
    openConnection({
      socket: socket.asWebSocket(),
      storage,
      onMessage: (message) => messages.push(message),
    });

    socket.receive("not json");

    expect(messages).toEqual([]);
    expect(warn).toHaveBeenCalledOnce();
  });

  it("drops a frame whose type it does not know", () => {
    const messages: ServerMessage[] = [];
    openConnection({
      socket: socket.asWebSocket(),
      storage,
      onMessage: (message) => messages.push(message),
    });

    socket.receive('{"type":"Nonsense"}');

    expect(messages).toEqual([]);
  });

  it("handles the next valid frame after a dropped one", () => {
    const messages: ServerMessage[] = [];
    openConnection({
      socket: socket.asWebSocket(),
      storage,
      onMessage: (message) => messages.push(message),
    });

    socket.receive("not json");
    socket.receive('{"type":"Nonsense"}');
    socket.receive('{"type":"RoomJoined","code":"ABCD","seat":0}');

    expect(messages).toEqual([{ type: "RoomJoined", code: "ABCD", seat: 0 }]);
  });

  it("remembers the device id the server issued", () => {
    const connection = openConnection({
      socket: socket.asWebSocket(),
      storage,
      onMessage: () => {},
    });

    socket.receive('{"type":"Welcome","deviceId":"d-7","protocolVersion":2}');

    expect(connection.status).toEqual({ kind: "ready", deviceId: "d-7" });
    expect(storage.getItem("pd.deviceId")).toBe("d-7");
  });

  it("hands the welcome to the listener too", () => {
    const messages: ServerMessage[] = [];
    openConnection({
      socket: socket.asWebSocket(),
      storage,
      onMessage: (message) => messages.push(message),
    });

    socket.receive('{"type":"Welcome","deviceId":"d-7","protocolVersion":2}');

    expect(messages).toEqual([
      { type: "Welcome", deviceId: "d-7", protocolVersion: 2 },
    ]);
  });

  it("sends the remembered device id on the next connection", () => {
    openConnection({
      socket: socket.asWebSocket(),
      storage,
      onMessage: () => {},
    });

    socket.receive('{"type":"Welcome","deviceId":"d-7","protocolVersion":2}');

    const secondSocket = new FakeSocket();
    openConnection({
      socket: secondSocket.asWebSocket(),
      storage,
      onMessage: () => {},
    });

    secondSocket.open();

    expect(JSON.parse(secondSocket.sent[0]).deviceId).toBe("d-7");
  });

  it("refuses to trust a welcome at another version", () => {
    const connection = openConnection({
      socket: socket.asWebSocket(),
      storage,
      onMessage: () => {},
    });

    socket.receive('{"type":"Welcome","deviceId":"d-7","protocolVersion":3}');

    expect(connection.status).toEqual({ kind: "outdated" });
    expect(storage.getItem("pd.deviceId")).toBeNull();
  });
});
