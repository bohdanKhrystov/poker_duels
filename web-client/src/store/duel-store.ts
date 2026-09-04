import type { ServerMessage } from "../protocol";
import {
  advanceReveal,
  applyServerMessage,
  initialState,
  tickClock,
  type DuelState,
} from "./duel-state";

/** The tab's one duel state, and the subscription a renderer reads it through. */
export interface DuelStore {
  getState(): DuelState;
  subscribe(listener: () => void): () => void;
  apply(message: ServerMessage): void;
}

/**
 * Arranges for `run` to happen once, after `delayMillis`, the same shape as `setTimeout` — the
 * store's only door onto a clock (`ADR-0102` §4), the class of seam this repository already
 * insists on wherever an outcome depends on time: the engine's injected `Rng`, and `ADR-0062`'s
 * injected `java.time.Clock`.
 */
export type Schedule = (run: () => void, delayMillis: number) => void;

export interface DuelStoreOptions {
  /** How long a reveal step stands before the next one takes over. `0` means synchronous:
   * released in the same turn, with `schedule` never called (`ADR-0102` §4). Absent means `0` —
   * every caller that does not ask for a runout's pacing gets today's behaviour unchanged. */
  readonly stepMillis?: number;
  /** Absent only where `stepMillis` is also absent, so this is never reached at `0`. */
  readonly schedule?: Schedule;
  /**
   * The monotonic reading the store anchors and ticks a turn clock against (`ADR-0113` §6).
   * Absent defaults to `performance.now()` itself, so the dozens of merged `createDuelStore()`
   * call sites that never pass this are untouched.
   */
  readonly now?: () => number;
  /**
   * How often a live turn clock re-arms, in milliseconds. Absent or `0` means the store never
   * arms a clock tick at all — today's behaviour for every caller that does not ask. This is
   * not `stepMillis`'s `0`: a reveal at `0` releases synchronously, a clock at `0` does not run.
   */
  readonly tickMillis?: number;
}

/** A fresh store at the reducer's initial state, with nobody listening yet. */
export function createDuelStore(options: DuelStoreOptions = {}): DuelStore {
  const stepMillis = options.stepMillis ?? 0;
  const schedule = options.schedule ?? ((run) => run());
  const now = options.now ?? (() => performance.now());
  const tickMillis = options.tickMillis ?? 0;
  let state = initialState();
  const listeners = new Set<() => void>();
  let clockPending = false;

  const notify = (): void => {
    for (const listener of listeners) listener();
  };

  // Ticks a reveal already in progress one step further, and — only while one still stands —
  // arranges the next tick. Never touches `schedule` at a step of `0`: recursing here keeps the
  // whole reveal inside the turn that started it (`ADR-0102` §4), which is what lets
  // `web-client/src/e2e/drive-duel.tsx` replay recorded frames with no clock in the way.
  const tick = (): void => {
    const next = advanceReveal(state);
    if (next === state) return;
    state = next;
    notify();
    if (state.reveal !== null) armTick();
    // A TurnClock queued behind this reveal (ADR-0102 §1) only reaches state.turnClock once
    // the queue drains here, so this is the other place a live clock can first appear.
    armClockTick();
  };

  const armTick = (): void => {
    if (stepMillis === 0) {
      tick();
      return;
    }
    schedule(tick, stepMillis);
  };

  // ADR-0113 §6, ADR-0102 §4's idiom applied to a server-stated deadline: one tick, re-armed
  // rather than repeated, so a missed or delayed tick costs an update rather than compounds.
  // `clockPending` keeps exactly one ever pending — the same guarantee `hadReveal` gives the
  // reveal below.
  const clockTick = (): void => {
    clockPending = false;
    const next = tickClock(state, now());
    if (next === state) return;
    state = next;
    notify();
    armClockTick();
  };

  const armClockTick = (): void => {
    if (tickMillis === 0 || clockPending || state.turnClock === null) return;
    clockPending = true;
    schedule(clockTick, tickMillis);
  };

  return {
    getState: () => state,
    subscribe: (listener) => {
      listeners.add(listener);
      return () => {
        listeners.delete(listener);
      };
    },
    apply: (message) => {
      // The reducer returns the state it was given for a frame it has no
      // opinion about. Notifying then would re-render every screen for nothing,
      // and would hand useSyncExternalStore a snapshot that never settles.
      const hadReveal = state.reveal !== null;
      // ADR-0113 §6: read through the injected clock here, at the socket seam, rather than
      // reach for the host's own — a monotonic reading, because a correction to the host's
      // clock must never stretch or collapse a countdown already anchored against it.
      const next = applyServerMessage(state, message, now());
      if (next === state) return;
      state = next;
      notify();
      // Arming here only when a reveal has just begun — not on every message a reveal in
      // progress merely queues — is what keeps exactly one reveal tick ever pending at a time.
      if (!hadReveal && state.reveal !== null) armTick();
      armClockTick();
    },
  };
}
