import {
  createContext,
  useContext,
  useMemo,
  useSyncExternalStore,
  type ReactElement,
  type ReactNode,
} from "react";
import type { ClientMessage } from "../protocol";
import type { DuelState } from "./duel-state";
import type { DuelStore } from "./duel-store";

interface DuelClientContext {
  readonly store: DuelStore;
  readonly send: (message: ClientMessage) => void;
}

const DuelContext = createContext<DuelClientContext | null>(null);

/** Puts the booted client's store and send within reach of every screen. */
export function DuelProvider(props: {
  store: DuelStore;
  send: (message: ClientMessage) => void;
  children: ReactNode;
}): ReactElement {
  const value = useMemo(
    () => ({ store: props.store, send: props.send }),
    [props.store, props.send],
  );
  return (
    <DuelContext.Provider value={value}>{props.children}</DuelContext.Provider>
  );
}

function useDuelClient(): DuelClientContext {
  const client = useContext(DuelContext);
  if (client === null) {
    throw new Error("useDuelState and useSend need a DuelProvider above them");
  }
  return client;
}

/** The whole of the last state the server's frames folded into. */
export function useDuelState(): DuelState {
  const { store } = useDuelClient();
  return useSyncExternalStore(store.subscribe, store.getState);
}
