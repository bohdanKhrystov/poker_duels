import { describe, expect, it, vi } from "vitest";
import { bootDuelClient } from "./boot";
import { FakeSocket } from "../protocol/fake-socket";
import {
  openConnection,
  PROTOCOL_VERSION,
  readRoomCode,
  writeRoomCode,
  type ServerMessage,
} from "../protocol";

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

const WELCOME = JSON.stringify({
  type: "Welcome",
  deviceId: "d-1",
  protocolVersion: PROTOCOL_VERSION,
});

function bootOverFakeSocket(joinRoomCode: string | null = null) {
  const socket = new FakeSocket();
  const storage = inMemoryStorage();
  const connect = vi.fn((onMessage: (message: ServerMessage) => void) =>
    openConnection({
      socket: socket.asWebSocket(),
      storage,
      onMessage,
    }),
  );
  const client = bootDuelClient({ connect, joinRoomCode, storage });
  return { socket, client, connect, storage };
}

function sentFrames(socket: FakeSocket): { type: string; code?: string }[] {
  return socket.sent.map((frame) => JSON.parse(frame));
}

function sentJoinRooms(socket: FakeSocket): { type: string; code?: string }[] {
  return sentFrames(socket).filter((frame) => frame.type === "JoinRoom");
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

  it("sends exactly one JoinRoom when Welcome arrives with a code in hand", () => {
    const { socket } = bootOverFakeSocket("ABCDEFGH");
    socket.open();
    socket.receive(WELCOME);
    expect(sentJoinRooms(socket)).toEqual([
      { type: "JoinRoom", code: "ABCDEFGH" },
    ]);
  });

  it("sends no JoinRoom when the tab opened without a code", () => {
    const { socket } = bootOverFakeSocket(null);
    socket.open();
    socket.receive(WELCOME);
    expect(sentJoinRooms(socket)).toEqual([]);
  });

  it("rejoins the remembered room when the tab carried no code", () => {
    const { socket, storage } = bootOverFakeSocket(null);
    writeRoomCode(storage, "ZYXWVUTS");

    socket.open();
    socket.receive(WELCOME);

    expect(sentJoinRooms(socket)).toEqual([
      { type: "JoinRoom", code: "ZYXWVUTS" },
    ]);
  });

  it("prefers the code the tab was opened with over the one it remembers", () => {
    const { socket, storage } = bootOverFakeSocket("ABCDEFGH");
    writeRoomCode(storage, "ZYXWVUTS");

    socket.open();
    socket.receive(WELCOME);

    expect(sentJoinRooms(socket)).toEqual([
      { type: "JoinRoom", code: "ABCDEFGH" },
    ]);
  });

  it("sends one JoinRoom on every Welcome, at the code it now holds", () => {
    const { socket } = bootOverFakeSocket(null);
    socket.open();
    socket.receive(WELCOME);
    expect(sentJoinRooms(socket)).toEqual([]);

    socket.receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":1}');
    socket.receive(WELCOME);

    expect(sentJoinRooms(socket)).toEqual([
      { type: "JoinRoom", code: "ABCDEFGH" },
    ]);
  });

  it("sends nothing but Hello before Welcome", () => {
    const { socket } = bootOverFakeSocket("ABCDEFGH");
    socket.open();
    expect(sentFrames(socket).map((frame) => frame.type)).toEqual(["Hello"]);
  });

  it("leaves the socket open and quiet after a refusal", () => {
    const { socket, client } = bootOverFakeSocket("ABCDEFGH");
    socket.open();
    socket.receive(WELCOME);
    socket.receive('{"type":"Failure","error":"UNKNOWN_ROOM"}');
    expect(sentFrames(socket).map((frame) => frame.type)).toEqual([
      "Hello",
      "JoinRoom",
    ]);
    expect(socket.closed).toBe(false);
    expect(client.store.getState().refusal).toBe("UNKNOWN_ROOM");
  });

  it("remembers each room the server seats it in", () => {
    const { socket, storage } = bootOverFakeSocket();

    socket.receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":1}');
    expect(readRoomCode(storage)).toBe("ABCDEFGH");

    // Mixed case and padded with whitespace: the decoder validates nothing
    // beyond the discriminator (`decodeServerMessage`), so this is a code the
    // wire permits. Storing it byte-identical proves the write is verbatim —
    // a `.trim()` or `.toUpperCase()` at the write site would corrupt it.
    socket.receive('{"type":"RoomJoined","code":" zYxwVUts ","seat":0}');
    expect(readRoomCode(storage)).toBe(" zYxwVUts ");
  });

  it("remembers no room until the server names one", () => {
    const { socket, storage } = bootOverFakeSocket();
    socket.open();
    socket.receive(WELCOME);
    expect(readRoomCode(storage)).toBeNull();
  });

  it("remembers nothing when it was given nowhere to remember it", () => {
    const socket = new FakeSocket();
    const client = bootDuelClient({
      connect: (onMessage) =>
        openConnection({
          socket: socket.asWebSocket(),
          storage: inMemoryStorage(),
          onMessage,
        }),
      joinRoomCode: null,
    });

    expect(() =>
      socket.receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":1}'),
    ).not.toThrow();
    const state = client.store.getState();
    expect(state.mySeat).toBe(1);
    expect(state.roomCode).toBe("ABCDEFGH");
  });

  it("forgets the room once the duel has finished", () => {
    const { socket, storage } = bootOverFakeSocket();

    socket.receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":0}');
    expect(readRoomCode(storage)).toBe("ABCDEFGH");

    socket.receive(
      '{"type":"DuelFinished","outcome":{"winner":0,"handsPlayed":3,"finalStacks":[2000,0]}}',
    );
    expect(readRoomCode(storage)).toBeNull();
  });

  it("sends no JoinRoom on the Welcome after a duel has finished", () => {
    const { socket } = bootOverFakeSocket(null);

    socket.open();
    socket.receive(WELCOME);
    socket.receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":0}');
    socket.receive(
      '{"type":"DuelFinished","outcome":{"winner":0,"handsPlayed":3,"finalStacks":[2000,0]}}',
    );
    socket.receive(WELCOME);

    expect(sentJoinRooms(socket)).toEqual([]);
  });
});
