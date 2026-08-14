import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  connectToDuelServer,
  type Connection,
  type ServerMessage,
} from "./index";
import { FakeSocket } from "./fake-socket";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`.
 *
 * Node 24+ defines its own `localStorage` global which is present but inert
 * unless the process is started with `--localstorage-file`, and under Vitest it
 * shadows the one jsdom provides: `typeof localStorage` is `"undefined"` while
 * `sessionStorage` — which Node keeps in memory — works. `connectToDuelServer`
 * reads the real global (it must, in a browser), so these tests stub that
 * global with a store built here instead of relying on the Node one; see
 * `device-id.test.ts` and `connection.test.ts` for the same pattern.
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

describe("the duel server connection", () => {
  beforeEach(() => {
    vi.stubGlobal("localStorage", inMemoryStorage());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("opens the socket at /ws on this page", () => {
    const socket = new FakeSocket();
    const constructor = vi.fn(() => socket.asWebSocket());
    vi.stubGlobal("WebSocket", constructor);

    connectToDuelServer(() => {});

    expect(constructor).toHaveBeenCalledOnce();
    expect(constructor).toHaveBeenCalledWith(`ws://${window.location.host}/ws`);
  });

  it("hands the connection the storage the profile endpoint reads", () => {
    localStorage.setItem("pd.deviceId", "d-9");
    const socket = new FakeSocket();
    const constructor = vi.fn(() => socket.asWebSocket());
    vi.stubGlobal("WebSocket", constructor);

    connectToDuelServer(() => {});
    socket.open();

    expect(JSON.parse(socket.sent[0]).deviceId).toBe("d-9");
  });

  it("hands decoded frames to the listener it was given", () => {
    const socket = new FakeSocket();
    const constructor = vi.fn(() => socket.asWebSocket());
    vi.stubGlobal("WebSocket", constructor);

    const messages: ServerMessage[] = [];
    const connection: Connection = connectToDuelServer((message) => {
      messages.push(message);
    });

    socket.receive('{"type":"RoomJoined","code":"ABCD","seat":0}');

    expect(connection.status).toEqual({ kind: "connecting" });
    expect(messages).toEqual([{ type: "RoomJoined", code: "ABCD", seat: 0 }]);
  });
});
