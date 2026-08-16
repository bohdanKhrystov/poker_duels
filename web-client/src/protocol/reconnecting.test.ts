import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
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
    deviceId,
    protocolVersion: PROTOCOL_VERSION,
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
}

/**
 * Wires `openReconnectingConnection` over `FakeSocket`s pushed onto `sockets`
 * as they are opened, with a `jitter` of `0` so every delay lands on the low
 * edge `retryDelayMillis` names: 250, 500, 1000, …
 */
function openOverFakeSockets(): Harness {
  const sockets: FakeSocket[] = [];
  const messages: ServerMessage[] = [];

  openReconnectingConnection({
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

  return { sockets, messages };
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
});
