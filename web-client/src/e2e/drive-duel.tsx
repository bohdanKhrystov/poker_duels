import { act, render } from "@testing-library/react";
import { openConnection } from "../protocol";
import { FakeSocket } from "../protocol/fake-socket";
import { bootDuelClient } from "../store/boot";
import { DuelProvider } from "../store/duel-provider";
import { Lobby } from "../lobby/Lobby";
import { scriptedDuel } from "./scripted-duel";
import type { ScriptedSeat, ScriptStep } from "./scripted-duel";

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

/**
 * A `FakeSocket` that counts how many frames actually reached `receive`. The
 * count lives on the double itself, not on the replay loop below, so a bug
 * that fed the double a `"client"` step's frame would show up here even if
 * the loop's own bookkeeping insisted otherwise — the decoder silently
 * dropping an undecodable frame is not the same as it never having arrived.
 */
class CountingSocket extends FakeSocket {
  received = 0;

  override receive(data: unknown): void {
    this.received += 1;
    super.receive(data);
  }
}

/** One seat's replay through the real client: what it saw, and what it sent. */
export interface DuelRun {
  readonly seat: ScriptedSeat;
  readonly container: HTMLElement;
  readonly sent: readonly string[];
  /** How many frames the double's `receive` actually saw. */
  readonly receivedCount: number;
}

/**
 * Boots a real `DuelClient` and mounts the real `Lobby` over it, wired exactly
 * as `main.tsx` wires the app (`bootDuelClient` over `openConnection`), except
 * the socket `openConnection` wraps is a `FakeSocket` fed one seat's half of
 * the committed script. The recorded frames therefore drive the real store,
 * the real reducer and the real screens — not a hand-built stand-in for any
 * of them.
 *
 * Every `"server"` step reaches the client through `socket.receive`, the frame
 * string unmodified, so the client's own `decodeServerMessage` runs it exactly
 * as it would over a live connection. A `"client"` step is not replayed —
 * `TASK-031206` teaches the driver to act instead. `onStep` fires after both.
 */
export function driveScriptedDuel(options: {
  readonly viewerSeat: number;
  readonly onStep?: (
    step: ScriptStep,
    index: number,
    container: HTMLElement,
  ) => void;
}): DuelRun {
  const duel = scriptedDuel();
  const seat = duel.seats.find((s) => s.viewerSeat === options.viewerSeat);
  if (seat === undefined) {
    throw new Error(
      `driveScriptedDuel: the script carries no seat ${options.viewerSeat}`,
    );
  }

  const storage = inMemoryStorage();
  const socket = new CountingSocket();
  const client = bootDuelClient({
    connect: (onMessage) =>
      openConnection({ socket: socket.asWebSocket(), storage, onMessage }),
    joinRoomCode: duel.roomCode,
    storage,
  });

  const { container } = render(
    <DuelProvider store={client.store} send={client.send}>
      <Lobby />
    </DuelProvider>,
  );

  act(() => {
    socket.open();
  });

  seat.steps.forEach((step, index) => {
    if (step.from === "server") {
      act(() => {
        socket.receive(step.frame);
      });
    }
    options.onStep?.(step, index, container);
  });

  return {
    seat,
    container,
    sent: socket.sent,
    receivedCount: socket.received,
  };
}
