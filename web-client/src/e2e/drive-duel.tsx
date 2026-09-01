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
 * button the server's own recorded action names, presses the sizing row's
 * presets — the way a player reaches an amount, `ADR-0100` §1 — when the
 * action carries one that the button does not already print, then clicks.
 * Never `send`, `actFrame` or `socket.send` — the frame that reaches the
 * double is whatever the real bar built from a real click, not one handed to
 * it.
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
  const button = reachTheAmount(container, step, index, verb);
  fireEvent.click(button);
}

/**
 * Finds the action button the recorded step names and, for a `Bet` or
 * `Raise`, drives its printed total there first (`ADR-0100` §1):
 *
 * 1. Read the total the button already prints. If it already matches, this
 *    is the server's minimum — the bar opens there — so there is nothing to
 *    press.
 * 2. Otherwise press the sizing row's presets, one at a time in document
 *    order, re-querying the action button after every press because React
 *    has replaced it, and stop as soon as the printed total matches.
 *
 * @throws If no button's accessible name starts with `verb` (naming what was
 *   on screen instead), or if no preset ever makes the printed total match
 *   the recorded amount (naming every total the row reached) — a chip that
 *   computes wrongly presents exactly as a script that recorded oddly, so
 *   both sides are named.
 */
function reachTheAmount(
  container: HTMLElement,
  step: ClientStep,
  index: number,
  verb: string,
): HTMLElement {
  const { action } = step.act;
  const findActionButton = (): HTMLElement | null =>
    within(container).queryByRole("button", {
      name: (name) => name.startsWith(verb),
    });

  const first = findActionButton();
  if (first === null) {
    const onScreen = within(container)
      .queryAllByRole("button")
      .map((element) => element.textContent);
    throw new Error(
      `driveScriptedDuel: step ${index} (hand ${step.act.handNumber}) recorded ` +
        `"${action.type}", which needs a button starting with "${verb}", but the ` +
        `screen shows: ${onScreen.length > 0 ? onScreen.join(", ") : "(no buttons)"}`,
    );
  }

  if (action.type !== "Bet" && action.type !== "Raise") {
    return first;
  }
  if (totalOn(first) === action.to) {
    return first;
  }

  const reached = [totalOn(first)];
  const chips = within(
    within(container).getByRole("group", { name: "amount" }),
  ).queryAllByRole("button");

  for (const chip of chips) {
    fireEvent.click(chip);
    const button = findActionButton();
    if (button === null) continue;
    reached.push(totalOn(button));
    if (totalOn(button) === action.to) return button;
  }

  throw new Error(
    `driveScriptedDuel: step ${index} (hand ${step.act.handNumber}) recorded ` +
      `"${action.type}" to ${action.to}, but no sizing control reached it — ` +
      `the row reached: ${reached.join(", ")}`,
  );
}

/** The amount an action button prints, read the way a player reads it. */
function totalOn(button: HTMLElement): number {
  return Number(button.textContent?.replace(/\D+/g, "") ?? "");
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
    // ADR-0102 §4: synchronous, so a replayed frame settles inside the same
    // act() that delivered it — the four recorded-frame suites this driver
    // serves may not be edited, and none may gain a clock (ADR-0100 §3).
    stepMillis: 0,
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
      // Not act()-wrapped (`ADR-0100` §1): Testing Library's own fireEvent
      // calls inside actThroughTheBar are already act-wrapped and flush a
      // render after every press. Wrapping the whole step in one more act()
      // defers all of those renders to its end, so the action button never
      // updates between presses and every press reads the same stale total.
      actThroughTheBar(container, step, index);
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
