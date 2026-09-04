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
  readonly forgetRoom: () => void;
  readonly roomAwaited: boolean;
}

const DuelContext = createContext<DuelClientContext | null>(null);

const NO_FORGET = (): void => {};

/** Puts the booted client's store and send within reach of every screen. */
export function DuelProvider(props: {
  store: DuelStore;
  send: (message: ClientMessage) => void;
  forgetRoom?: () => void;
  roomAwaited?: boolean;
  children: ReactNode;
}): ReactElement {
  const value = useMemo(
    () => ({
      store: props.store,
      send: props.send,
      forgetRoom: props.forgetRoom ?? NO_FORGET,
      roomAwaited: props.roomAwaited ?? false,
    }),
    [props.store, props.send, props.forgetRoom, props.roomAwaited],
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

/** The boot-created send. Screens call it from event handlers, never effects. */
export function useSend(): (message: ClientMessage) => void {
  return useDuelClient().send;
}

/** The boot-created forget. Screens call it from event handlers, never effects. */
export function useForgetRoom(): () => void {
  return useDuelClient().forgetRoom;
}

/**
 * `useRoomAwaited` — whether this tab is awaiting a room from the server (`ADR-0114` §5).
 */
export function useRoomAwaited(): boolean {
  return useDuelClient().roomAwaited;
}
