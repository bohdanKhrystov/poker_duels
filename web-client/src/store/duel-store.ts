import type { ServerMessage } from "../protocol";
import { applyServerMessage, initialState, type DuelState } from "./duel-state";

/** The tab's one duel state, and the subscription a renderer reads it through. */
export interface DuelStore {
  getState(): DuelState;
  subscribe(listener: () => void): () => void;
  apply(message: ServerMessage): void;
}

/** A fresh store at the reducer's initial state, with nobody listening yet. */
export function createDuelStore(): DuelStore {
  let state = initialState();
  const listeners = new Set<() => void>();

  return {
    getState: () => state,
    subscribe: (listener) => {
      listeners.add(listener);
      return () => {
        listeners.delete(listener);
      };
    },
    apply: (message) => {
      const next = applyServerMessage(state, message);
      // The reducer returns the state it was given for a frame it has no
      // opinion about. Notifying then would re-render every screen for nothing,
      // and would hand useSyncExternalStore a snapshot that never settles.
      if (next === state) return;
      state = next;
      for (const listener of listeners) listener();
    },
  };
}
