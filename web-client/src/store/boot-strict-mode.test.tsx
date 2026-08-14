import { render } from "@testing-library/react";
import { StrictMode, type ReactElement } from "react";
import { describe, expect, it, vi } from "vitest";
import { bootDuelClient } from "./boot";
import { DuelProvider, useDuelState } from "./duel-provider";
import { FakeSocket } from "../protocol/fake-socket";
import {
  openConnection,
  PROTOCOL_VERSION,
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
  const client = bootDuelClient({
    connect: (onMessage: (message: ServerMessage) => void) =>
      openConnection({
        socket: socket.asWebSocket(),
        storage: inMemoryStorage(),
        onMessage,
      }),
    joinRoomCode,
  });
  return { socket, client };
}

function joinRoomCount(socket: FakeSocket): number {
  return socket.sent
    .map((frame) => JSON.parse(frame))
    .filter((frame) => frame.type === "JoinRoom").length;
}

describe("a screen mounted twice by StrictMode", () => {
  it("is really mounted twice", () => {
    const { client } = bootOverFakeSocket("ABCDEFGH");
    const rendered = vi.fn();

    function RoomCode(): ReactElement {
      const state = useDuelState();
      rendered();
      return <p>{state.roomCode ?? "no room yet"}</p>;
    }

    function mountUnderStrictMode(): void {
      render(
        <StrictMode>
          <DuelProvider store={client.store} send={client.send}>
            <RoomCode />
          </DuelProvider>
        </StrictMode>,
      );
    }

    rendered.mockClear();
    mountUnderStrictMode();
    expect(rendered.mock.calls.length).toBeGreaterThan(1);
  });

  it("sends no JoinRoom of its own after the reaction already fired", () => {
    const { socket, client } = bootOverFakeSocket("ABCDEFGH");
    const rendered = vi.fn();

    function RoomCode(): ReactElement {
      const state = useDuelState();
      rendered();
      return <p>{state.roomCode ?? "no room yet"}</p>;
    }

    function mountUnderStrictMode(): void {
      render(
        <StrictMode>
          <DuelProvider store={client.store} send={client.send}>
            <RoomCode />
          </DuelProvider>
        </StrictMode>,
      );
    }

    socket.open();
    socket.receive(WELCOME);
    mountUnderStrictMode();
    expect(joinRoomCount(socket)).toBe(1);
  });

  it("sends exactly one JoinRoom when Welcome arrives after it mounted", () => {
    const { socket, client } = bootOverFakeSocket("ABCDEFGH");
    const rendered = vi.fn();

    function RoomCode(): ReactElement {
      const state = useDuelState();
      rendered();
      return <p>{state.roomCode ?? "no room yet"}</p>;
    }

    function mountUnderStrictMode(): void {
      render(
        <StrictMode>
          <DuelProvider store={client.store} send={client.send}>
            <RoomCode />
          </DuelProvider>
        </StrictMode>,
      );
    }

    mountUnderStrictMode();
    socket.open();
    socket.receive(WELCOME);
    expect(joinRoomCount(socket)).toBe(1);
  });
});
