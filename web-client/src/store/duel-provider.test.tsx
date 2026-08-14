import { render, screen } from "@testing-library/react";
import { act } from "react";
import { describe, it, expect, vi } from "vitest";
import { DuelProvider, useDuelState } from "./duel-provider";
import { createDuelStore, type DuelStore } from "./duel-store";
import type { ClientMessage } from "../protocol";
import type { ReactElement } from "react";

const ROOM_JOINED = { type: "RoomJoined", code: "ABCDEFGH", seat: 0 } as const;
const WELCOME = { type: "Welcome", deviceId: "d", protocolVersion: 2 } as const;

function RoomCode(): ReactElement {
  const state = useDuelState();
  return <p>{state.roomCode ?? "no room yet"}</p>;
}

function renderUnder(
  store: DuelStore,
  send: (message: ClientMessage) => void,
  child: ReactElement,
): void {
  render(
    <DuelProvider store={store} send={send}>
      {child}
    </DuelProvider>,
  );
}

describe("the duel provider", () => {
  it("hands a component the state the store holds", () => {
    const store = createDuelStore();
    const send: (message: ClientMessage) => void = vi.fn();
    store.apply(ROOM_JOINED);

    renderUnder(store, send, <RoomCode />);

    expect(screen.getByText("ABCDEFGH")).toBeDefined();
  });

  it("re-renders a component when a frame moves the state", () => {
    const store = createDuelStore();
    const send: (message: ClientMessage) => void = vi.fn();

    renderUnder(store, send, <RoomCode />);
    expect(screen.getByText("no room yet")).toBeDefined();

    act(() => {
      store.apply(ROOM_JOINED);
    });

    expect(screen.getByText("ABCDEFGH")).toBeDefined();
  });

  it("does not re-render when the reducer had no opinion", () => {
    const store = createDuelStore();
    const send: (message: ClientMessage) => void = vi.fn();
    store.apply(ROOM_JOINED);
    const rendered = vi.fn();

    function RoomCodeWithRender(): ReactElement {
      rendered();
      const state = useDuelState();
      return <p>{state.roomCode ?? "no room yet"}</p>;
    }

    renderUnder(store, send, <RoomCodeWithRender />);
    expect(rendered).toHaveBeenCalledOnce();

    act(() => {
      store.apply(WELCOME);
    });

    expect(rendered).toHaveBeenCalledOnce();
  });
});
