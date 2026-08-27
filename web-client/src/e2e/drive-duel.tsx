import { act, fireEvent, render, within } from "@testing-library/react";
import { openConnection } from "../protocol";
import type { ActionType, PlayerAction } from "../protocol";
import { FakeSocket } from "../protocol/fake-socket";
import { bootDuelClient } from "../store/boot";
import { DuelProvider } from "../store/duel-provider";
import { Lobby } from "../lobby/Lobby";
import { actionVerb } from "../table/action-text";
import { scriptedDuel } from "./scripted-duel";
import type { ClientStep, ScriptedSeat, ScriptStep } from "./scripted-duel";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`.
 *
 * Node 24+ defines its own `localStorage` global which is present but inert
 * unless the process is started with `--localstorage-file`, and under Vitest it
 * shadows the one jsdom provides.
 */
export function inMemoryStorage(): Storage {
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
 * A `FakeSocket` that records every frame that actually reached `receive`, in
 * the order it arrived. Both the count and the order live on the double
 * itself, not on the replay loop below, so a bug that fed the double a
 * `"client"` step's frame, or fed two `"server"` steps out of sequence, would
 * show up here even if the loop's own bookkeeping insisted otherwise — the
 * decoder silently dropping an undecodable frame is not the same as it never
 * having arrived.
 */
class CountingSocket extends FakeSocket {
  readonly receivedFrames: string[] = [];

  override receive(data: unknown): void {
    this.receivedFrames.push(data as string);
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
  /** Every frame the double's `receive` actually saw, in the order it saw them. */
  readonly receivedFrames: readonly string[];
  /** The storage the run actually used, whether handed in or built. */
  readonly storage: Storage;
}

/**
 * The `ActionType` `actionVerb` prints, keyed by the discriminator the
 * server's own recording gives a `PlayerAction`. The two vocabularies name the
 * same six actions, spelled differently — this is the one place that bridges
 * them, so the driver can ask the production code for the word a player would
 * read on the button rather than guessing it again.
 */
const ACTION_TYPE_OF: Record<PlayerAction["type"], ActionType> = {
  Fold: "FOLD",
  Check: "CHECK",
  Call: "CALL",
  Bet: "BET",
  Raise: "RAISE",
  AllIn: "ALL_IN",
};

/**
 * Answers one recorded `"client"` step through the real action bar: finds the
 * button the server's own recorded action names, dials in the amount first
 * when the action carries one, then clicks. Never `send`, `actFrame` or
 * `socket.send` — the frame that reaches the double is whatever the real bar
 * built from a real click, not one handed to it.
 *
 * @throws If no button's accessible name starts with the recorded action's
 *   verb, naming the step, the hand and what was on screen instead — a driver
 *   that silently did nothing here would make every downstream assertion pass
 *   by having skipped the turn.
 */
function actThroughTheBar(
  container: HTMLElement,
  step: ClientStep,
  index: number,
): void {
  const { action } = step.act;
  const verb = actionVerb(ACTION_TYPE_OF[action.type]);
  const button = within(container).queryByRole("button", {
    name: (name) => name.startsWith(verb),
  });

  if (button === null) {
    const onScreen = within(container)
      .queryAllByRole("button")
      .map((element) => element.textContent);
    throw new Error(
      `driveScriptedDuel: step ${index} (hand ${step.act.handNumber}) recorded ` +
        `"${action.type}", which needs a button starting with "${verb}", but the ` +
        `screen shows: ${onScreen.length > 0 ? onScreen.join(", ") : "(no buttons)"}`,
    );
  }

  if (action.type === "Bet" || action.type === "Raise") {
    fireEvent.change(within(container).getByRole("slider"), {
      target: { value: String(action.to) },
    });
  }

  fireEvent.click(button);
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
 * as it would over a live connection. Every `"client"` step is answered
 * through the real action bar instead of being replayed: see
 * `actThroughTheBar`. `onStep` fires after both.
 */
export function driveScriptedDuel(options: {
  readonly viewerSeat: number;
  readonly onStep?: (
    step: ScriptStep,
    index: number,
    container: HTMLElement,
  ) => void;
  readonly storage?: Storage;
}): DuelRun {
  const duel = scriptedDuel();
  const seat = duel.seats.find((s) => s.viewerSeat === options.viewerSeat);
  if (seat === undefined) {
    throw new Error(
      `driveScriptedDuel: the script carries no seat ${options.viewerSeat}`,
    );
  }

  const storage = options.storage ?? inMemoryStorage();
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
    } else {
      act(() => {
        actThroughTheBar(container, step, index);
      });
    }
    options.onStep?.(step, index, container);
  });

  return {
    seat,
    container,
    sent: socket.sent,
    receivedCount: socket.receivedFrames.length,
    receivedFrames: socket.receivedFrames,
    storage,
  };
}
