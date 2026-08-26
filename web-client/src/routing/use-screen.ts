import { useSyncExternalStore } from "react";
import { hashForScreen, screenFromHash, type Screen } from "./screen";

// Every useScreen() caller shares one subscriber set and one window
// listener, created once at module scope — the duel-store.ts
// notify-and-cache contract, applied to the address bar instead of a socket.
const listeners = new Set<() => void>();

function notify(): void {
  for (const listener of listeners) listener();
}

function subscribe(listener: () => void): () => void {
  // window discards a duplicate identical listener on its own, but removing
  // it must wait for the last caller to leave: an earlier caller's
  // unsubscribe would otherwise silence a caller still mounted.
  if (listeners.size === 0) {
    window.addEventListener("hashchange", notify);
  }
  listeners.add(listener);

  return () => {
    listeners.delete(listener);
    if (listeners.size === 0) {
      window.removeEventListener("hashchange", notify);
    }
  };
}

// The snapshot is the Screen string itself, never a fresh object: a new
// reference on every call sends useSyncExternalStore into an infinite
// render loop, and a string needs no cache because it compares by value.
function getSnapshot(): Screen {
  return screenFromHash(window.location.hash);
}

/**
 * The screen the address currently names, and the two navigations of
 * `ADR-0076` §6 that change it.
 */
export function useScreen(): {
  readonly screen: Screen;
  readonly open: (screen: Exclude<Screen, "first">) => void;
  readonly leave: () => void;
} {
  const screen = useSyncExternalStore(subscribe, getSnapshot);

  return {
    screen,
    open: (next) => {
      // Assigning location.hash fires hashchange itself, which is what
      // notifies every subscriber — the whole reason this module listens
      // for hashchange rather than for popstate (ADR-0076 §5).
      window.location.hash = hashForScreen(next);
    },
    leave: () => {
      // history.replaceState fires *neither* popstate nor hashchange
      // (ADR-0076 §5), so nothing re-checks the snapshot unless this module
      // notifies its own subscribers itself. The one bug here no type
      // checker catches.
      window.history.replaceState(null, "", hashForScreen("first"));
      notify();
    },
  };
}
