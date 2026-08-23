import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { Connection } from "./connection";
import { FakeSocket } from "./fake-socket";
import type { RoomJoined, ServerMessage, Welcome } from "./protocol.gen";
import { openReconnectingConnection } from "./reconnecting";
import { PROTOCOL_VERSION } from "./version";

/** A `Storage` that touches no browser: a `Map` behind the interface. */
function fakeStorage(): Storage {
  const data = new Map<string, string>();
  return {
    get length(): number {
      return data.size;
    },
    clear(): void {
      data.clear();
    },
    getItem(key: string): string | null {
      return data.get(key) ?? null;
    },
    key(index: number): string | null {
      return Array.from(data.keys())[index] ?? null;
    },
    removeItem(key: string): void {
      data.delete(key);
    },
    setItem(key: string, value: string): void {
      data.set(key, value);
    },
  };
}

function welcomeFrame(deviceId: string): string {
  return JSON.stringify({
    type: "Welcome",
    playerId: "player-1",
    deviceId,
    protocolVersion: PROTOCOL_VERSION,
  } satisfies Welcome);
}

function welcomeFrameWithDifferentVersion(deviceId: string): string {
  return JSON.stringify({
    type: "Welcome",
    playerId: "player-1",
    deviceId,
    protocolVersion: PROTOCOL_VERSION + 1,
  } satisfies Welcome);
}

function roomJoinedFrame(seat: number): string {
  return JSON.stringify({
    type: "RoomJoined",
    code: "ABC123",
    seat,
  } satisfies RoomJoined);
}

interface Harness {
  readonly sockets: FakeSocket[];
  readonly messages: ServerMessage[];
  readonly connection: Connection;
}

/**
 * Wires `openReconnectingConnection` over `FakeSocket`s pushed onto `sockets`
 * as they are opened, with a `jitter` of `0` so every delay lands on the low
 * edge `retryDelayMillis` names: 250, 500, 1000, …
 */
function openOverFakeSockets(): Harness {
  const sockets: FakeSocket[] = [];
  const messages: ServerMessage[] = [];

  const connection = openReconnectingConnection({
    openSocket: (): WebSocket => {
      const socket = new FakeSocket();
      sockets.push(socket);
      return socket.asWebSocket();
    },
    storage: fakeStorage(),
    onMessage: (message: ServerMessage): void => {
      messages.push(message);
    },
    jitter: (): number => 0,
  });

  return { sockets, messages, connection };
}

describe("the reconnecting connection", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("opens one socket and no more while it stays open", () => {
    const { sockets } = openOverFakeSockets();

    sockets[0].open();
    sockets[0].receive(welcomeFrame("device-1"));

    vi.advanceTimersByTime(60 * 60 * 1000);

    expect(sockets).toHaveLength(1);
  });

  it("opens nothing until the backoff delay has elapsed", () => {
    const { sockets } = openOverFakeSockets();

    sockets[0].close();

    vi.advanceTimersByTime(249);
    expect(sockets).toHaveLength(1);

    vi.advanceTimersByTime(1);
    expect(sockets).toHaveLength(2);
  });

  it("waits longer after each close, at the delays the backoff names", () => {
    const { sockets } = openOverFakeSockets();

    sockets[0].close();
    vi.advanceTimersByTime(249);
    expect(sockets).toHaveLength(1);
    vi.advanceTimersByTime(1);
    expect(sockets).toHaveLength(2);

    sockets[1].close();
    vi.advanceTimersByTime(499);
    expect(sockets).toHaveLength(2);
    vi.advanceTimersByTime(1);
    expect(sockets).toHaveLength(3);

    sockets[2].close();
    vi.advanceTimersByTime(999);
    expect(sockets).toHaveLength(3);
    vi.advanceTimersByTime(1);
    expect(sockets).toHaveLength(4);
  });

  it("starts the backoff over once a Welcome arrives", () => {
    const { sockets } = openOverFakeSockets();

    sockets[0].close();
    vi.advanceTimersByTime(250);
    expect(sockets).toHaveLength(2);

    sockets[1].receive(welcomeFrame("device-1"));
    sockets[1].close();

    vi.advanceTimersByTime(249);
    expect(sockets).toHaveLength(2);

    vi.advanceTimersByTime(1);
    expect(sockets).toHaveLength(3);
  });

  it("forwards the frames of whichever socket is live", () => {
    const { sockets, messages } = openOverFakeSockets();

    sockets[0].receive(roomJoinedFrame(0));

    sockets[0].close();
    vi.advanceTimersByTime(250);

    sockets[1].receive(roomJoinedFrame(1));

    expect(sockets).toHaveLength(2);
    expect(messages).toEqual([
      { type: "RoomJoined", code: "ABC123", seat: 0 },
      { type: "RoomJoined", code: "ABC123", seat: 1 },
    ]);
  });

  it("ignores the close of a socket it has already replaced", () => {
    const { sockets } = openOverFakeSockets();

    sockets[0].close();
    vi.advanceTimersByTime(250);
    expect(sockets).toHaveLength(2);

    // ADR-0018: the server closes the socket the new one adopted the seat from.
    sockets[0].close();
    vi.advanceTimersByTime(60_000);

    expect(sockets).toHaveLength(2);

    // The tab reconnects again, so socket 1 is now two generations behind —
    // a 1-bit "was this the immediately-previous attempt" flag would wrap
    // around and wrongly accept its echo below.
    sockets[1].close();
    vi.advanceTimersByTime(500);
    expect(sockets).toHaveLength(3);

    // The same stale echo as above, arriving after that second reconnect.
    sockets[0].close();
    vi.advanceTimersByTime(60_000);

    expect(sockets).toHaveLength(3);
  });

  it("counts the failures of the live socket, not of the one it replaced", () => {
    const { sockets } = openOverFakeSockets();

    sockets[0].close();
    vi.advanceTimersByTime(250);
    expect(sockets).toHaveLength(2);

    // The same stale echo as above — it must not spend a retry attempt that
    // the live socket's own close will spend again.
    sockets[0].close();

    sockets[1].close();
    vi.advanceTimersByTime(499);
    expect(sockets).toHaveLength(2);
    vi.advanceTimersByTime(1);
    expect(sockets).toHaveLength(3);
  });

  it("writes to the socket it opened last", () => {
    const { sockets, connection } = openOverFakeSockets();

    sockets[0].open();
    sockets[0].close();
    vi.advanceTimersByTime(250);
    expect(sockets).toHaveLength(2);

    sockets[1].open();
    connection.send({ type: "CreateRoom" });

    expect(
      sockets[1].sent.map(
        (frame) => (JSON.parse(frame) as { type: string }).type,
      ),
    ).toEqual(["Hello", "CreateRoom"]);
    expect(sockets[0].sent).toHaveLength(1);
  });

  it("drops a send made while no socket is open", () => {
    const { sockets, connection } = openOverFakeSockets();

    sockets[0].close();

    expect(() => connection.send({ type: "CreateRoom" })).not.toThrow();

    vi.advanceTimersByTime(250);
    expect(sockets).toHaveLength(2);

    sockets[1].open();
    expect(
      sockets[1].sent.map(
        (frame) => (JSON.parse(frame) as { type: string }).type,
      ),
    ).toEqual(["Hello"]);
  });

  it("opens no further socket once the Welcome named another protocol version", () => {
    const { sockets } = openOverFakeSockets();

    sockets[0].receive(welcomeFrameWithDifferentVersion("device-1"));
    sockets[0].close();
    vi.advanceTimersByTime(60 * 60 * 1000);

    expect(sockets).toHaveLength(1);

    // Even if the socket closes again after the long advance, no new socket opens.
    sockets[0].close();
    vi.advanceTimersByTime(250 + 500 + 1000);

    expect(sockets).toHaveLength(1);
  });

  it("opens no further socket after a VERSION_MISMATCH failure", () => {
    const { sockets } = openOverFakeSockets();

    sockets[0].receive('{"type":"Failure","error":"VERSION_MISMATCH"}');
    sockets[0].close();
    vi.advanceTimersByTime(60 * 60 * 1000);

    expect(sockets).toHaveLength(1);

    // Even if the socket closes again after the long advance, no new socket opens.
    sockets[0].close();
    vi.advanceTimersByTime(250 + 500 + 1000);

    expect(sockets).toHaveLength(1);
  });

  it("keeps retrying after a failure that is not a version mismatch", () => {
    const { sockets } = openOverFakeSockets();

    sockets[0].receive('{"type":"Failure","error":"UNKNOWN_ROOM"}');
    sockets[0].close();
    vi.advanceTimersByTime(250);

    expect(sockets).toHaveLength(2);
  });

  it("writes nothing to the socket that has closed", () => {
    const { sockets, connection } = openOverFakeSockets();

    sockets[0].open(); // the Hello goes out
    sockets[0].close(); // and the connection is no longer live

    connection.send({ type: "CreateRoom" });

    // The socket that just closed is the one an ungated send reaches, so it is
    // the one that has to be read. Nothing has been retried yet, so it is still
    // the only socket there is.
    expect(
      sockets[0].sent.map(
        (frame) => (JSON.parse(frame) as { type: string }).type,
      ),
    ).toEqual(["Hello"]);
    expect(sockets).toHaveLength(1);
  });

  it("closes the socket it is holding and opens no other", () => {
    const { sockets, connection } = openOverFakeSockets();

    sockets[0].open();
    connection.close();

    expect(sockets[0].closed).toBe(true);

    vi.advanceTimersByTime(60 * 60 * 1000);
    expect(sockets).toHaveLength(1);
  });

  it("reports the status of the socket it opened last", () => {
    const { sockets, connection } = openOverFakeSockets();

    sockets[0].open();
    sockets[0].receive(welcomeFrame("device-1"));
    expect(connection.status).toEqual({ kind: "ready", deviceId: "device-1" });

    sockets[0].close();
    vi.advanceTimersByTime(250);
    expect(sockets).toHaveLength(2);

    sockets[1].open();
    sockets[1].receive(welcomeFrame("device-2"));
    expect(connection.status).toEqual({ kind: "ready", deviceId: "device-2" });
  });
});
