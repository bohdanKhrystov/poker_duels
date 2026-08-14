import { describe, expect, it, vi } from "vitest";
import { bootDuelClient } from "./boot";
import { FakeSocket } from "../protocol/fake-socket";
import { openConnection, type ServerMessage } from "../protocol";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`.
 *
 * Node 24+ defines its own `localStorage` global which is present but inert
 * unless the process is started with `--localstorage-file`, and under Vitest it
 * shadows the one jsdom provides.
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

function bootOverFakeSocket() {
  const socket = new FakeSocket();
  const connect = vi.fn((onMessage: (message: ServerMessage) => void) =>
    openConnection({
      socket: socket.asWebSocket(),
      storage: inMemoryStorage(),
      onMessage,
    }),
  );
  const client = bootDuelClient({ connect });
  return { socket, client, connect };
}

function sentFrames(socket: FakeSocket): { type: string; code?: string }[] {
  return socket.sent.map((frame) => JSON.parse(frame));
}

describe("booting the duel client", () => {
  it("opens the tab's one connection", () => {
    const { connect } = bootOverFakeSocket();
    expect(connect).toHaveBeenCalledOnce();
  });

  it("folds every frame the server sends into the store", () => {
    const { socket, client } = bootOverFakeSocket();
    socket.receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":1}');
    const state = client.store.getState();
    expect(state.roomCode).toBe("ABCDEFGH");
    expect(state.mySeat).toBe(1);
  });

  it("sends through the connection it opened", () => {
    const { socket, client } = bootOverFakeSocket();
    socket.open();
    client.send({ type: "CreateRoom" });
    const frames = sentFrames(socket);
    expect(frames).toHaveLength(2);
    expect(frames[1]).toEqual({ type: "CreateRoom" });
  });
});
