import { act, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { bootDuelClient } from "./boot";
import { DuelProvider } from "./duel-provider";
import { FakeSocket } from "../protocol/fake-socket";
import { openReconnectingConnection } from "../protocol/reconnecting";
import { PROTOCOL_VERSION } from "../protocol";
import { Lobby } from "../lobby/Lobby";
import { aSeat, aView } from "../table/view-fixture";

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

/**
 * Boots a real `DuelClient` over `openReconnectingConnection`, wired exactly as
 * `main.tsx` wires it, so a test drives the sockets the retry loop itself opens
 * rather than a stand-in for them. `TASK-031012` adds rendering tests to this
 * same file and reuses this helper.
 *
 * One `storage` for the whole tab, exactly as the browser has one: the device
 * id the first `Welcome` writes is the device id the second `Hello` reads.
 */
function reconnectingClient(joinRoomCode: string | null = null) {
  const sockets: FakeSocket[] = [];
  const storage = inMemoryStorage();
  const client = bootDuelClient({
    connect: (onMessage) =>
      openReconnectingConnection({
        openSocket: () => {
          const socket = new FakeSocket();
          sockets.push(socket);
          return socket.asWebSocket();
        },
        storage,
        onMessage,
        jitter: () => 0,
      }),
    joinRoomCode,
    storage,
  });
  return { sockets, client, storage };
}

function sentFrames(socket: FakeSocket): unknown[] {
  return socket.sent.map((frame) => JSON.parse(frame));
}

/**
 * Mounts the real `Lobby` over a `reconnectingClient`'s store and send, wired
 * exactly as `main.tsx` wires the app, so these tests exercise the screen a
 * player actually sees rather than a store in isolation.
 */
function renderDuelScreen(client: ReturnType<typeof bootDuelClient>): void {
  render(
    <DuelProvider store={client.store} send={client.send}>
      <Lobby />
    </DuelProvider>,
  );
}

describe("a tab whose socket dropped", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("says Hello then JoinRoom, in that order and once each, on the socket it reopened", () => {
    const { sockets } = reconnectingClient("ABCDEFGH");

    sockets[0].open();
    sockets[0].receive(WELCOME);
    sockets[0].receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":1}');
    sockets[0].close();

    vi.advanceTimersByTime(250);
    sockets[1].open();
    sockets[1].receive(WELCOME);

    expect(sockets[1].sent.map((frame) => JSON.parse(frame))).toEqual([
      { type: "Hello", deviceId: "d-1", protocolVersion: PROTOCOL_VERSION },
      { type: "JoinRoom", code: "ABCDEFGH" },
    ]);

    // A second drop, one generation further on: "once each" is a claim about
    // the loop, not just the first reconnect, so it must hold again on the
    // socket that replaces the one just asserted on above.
    sockets[1].close();

    vi.advanceTimersByTime(250);
    sockets[2].open();
    sockets[2].receive(WELCOME);

    expect(sockets[2].sent.map((frame) => JSON.parse(frame))).toEqual([
      { type: "Hello", deviceId: "d-1", protocolVersion: PROTOCOL_VERSION },
      { type: "JoinRoom", code: "ABCDEFGH" },
    ]);
  });

  it("carries the device id the first Welcome issued into the second Hello", () => {
    const { sockets } = reconnectingClient("ABCDEFGH");

    sockets[0].open();
    expect(sentFrames(sockets[0])[0]).toEqual({
      type: "Hello",
      deviceId: null,
      protocolVersion: PROTOCOL_VERSION,
    });

    sockets[0].receive(
      JSON.stringify({
        type: "Welcome",
        deviceId: "d-7",
        protocolVersion: PROTOCOL_VERSION,
      }),
    );
    sockets[0].close();

    vi.advanceTimersByTime(250);
    sockets[1].open();

    expect(sentFrames(sockets[1])[0]).toEqual({
      type: "Hello",
      deviceId: "d-7",
      protocolVersion: PROTOCOL_VERSION,
    });
  });

  it("rejoins the room the server seated it in, though the tab was opened without a code", () => {
    const { sockets } = reconnectingClient(null);

    sockets[0].open();
    sockets[0].receive(WELCOME);
    sockets[0].receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":1}');
    sockets[0].close();

    vi.advanceTimersByTime(250);
    sockets[1].open();
    sockets[1].receive(WELCOME);

    expect(sentFrames(sockets[1])).toEqual([
      { type: "Hello", deviceId: "d-1", protocolVersion: PROTOCOL_VERSION },
      { type: "JoinRoom", code: "ABCDEFGH" },
    ]);
  });

  it("repaints from the snapshot that followed the resume, not the one it held", () => {
    const { sockets, client } = reconnectingClient("ABCDEFGH");
    renderDuelScreen(client);

    act(() => {
      sockets[0].open();
      sockets[0].receive(WELCOME);
      sockets[0].receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":0}');
      sockets[0].receive(JSON.stringify({ type: "Snapshot", view: aView() }));
      sockets[0].close();
    });

    expect(screen.getByText("Pot 30")).toBeDefined();

    act(() => {
      vi.advanceTimersByTime(250);
      sockets[1].open();
      sockets[1].receive(WELCOME);
      sockets[1].receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":0}');
      sockets[1].receive(
        JSON.stringify({
          type: "Snapshot",
          view: aView({
            pot: 260,
            handNumber: 4,
            street: "FLOP",
            seats: [
              aSeat({ index: 0, stack: 640 }),
              aSeat({ index: 1, stack: 360 }),
            ],
          }),
        }),
      );
    });

    // Several fields differ, not just pot: a reducer that carried one field
    // forward (or a table wired to a constant) would still pass a check that
    // named only the pot. Street and hand number come off PotStrip's one
    // `view` prop; the stacks come off each seat's own SeatPlate — a reducer
    // has to replace the whole view, not patch a field it happens to be
    // asked about, to get all of these right at once.
    expect(screen.getByText("Pot 260")).toBeDefined();
    expect(screen.getByText(/· Flop$/)).toBeDefined();
    expect(screen.getByText(/Hand 4 ·/)).toBeDefined();
    expect(screen.getByText("640")).toBeDefined();
    expect(screen.getByText("360")).toBeDefined();

    expect(screen.queryByText("Pot 30")).toBeNull();
    expect(screen.queryByText(/· Preflop$/)).toBeNull();
    expect(screen.queryByText(/Hand 1 ·/)).toBeNull();
    expect(screen.queryByText("500")).toBeNull();
  });

  it("shows the duel that ended while the socket was down", () => {
    const { sockets, client } = reconnectingClient("ABCDEFGH");
    renderDuelScreen(client);

    act(() => {
      sockets[0].open();
      sockets[0].receive(WELCOME);
      sockets[0].receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":0}');
      sockets[0].receive(JSON.stringify({ type: "Snapshot", view: aView() }));
      sockets[0].close();
    });

    act(() => {
      vi.advanceTimersByTime(250);
      sockets[1].open();
      sockets[1].receive(WELCOME);
      sockets[1].receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":0}');
      sockets[1].receive(
        '{"type":"DuelFinished","outcome":{"winner":0,"handsPlayed":9,"finalStacks":[1000,0]}}',
      );
    });

    expect(screen.getByRole("region", { name: "the result" })).toBeDefined();
    expect(screen.queryByText("Pot 30")).toBeNull();
  });
});
