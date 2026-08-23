import { fireEvent, render, screen } from "@testing-library/react";
import { act } from "react";
import { describe, it, expect, vi } from "vitest";
import {
  DuelProvider,
  useDuelState,
  useSend,
  useForgetRoom,
} from "./duel-provider";
import { createDuelStore, type DuelStore } from "./duel-store";
import type { ClientMessage } from "../protocol";
import type { ReactElement } from "react";

const ROOM_JOINED = { type: "RoomJoined", code: "ABCDEFGH", seat: 0 } as const;
const WELCOME = {
  type: "Welcome",
  playerId: "p-1",
  deviceId: "d",
  protocolVersion: 2,
} as const;

function RoomCode(): ReactElement {
  const state = useDuelState();
  return <p>{state.roomCode ?? "no room yet"}</p>;
}

function CreateButton(): ReactElement {
  const send = useSend();
  return (
    <button type="button" onClick={() => send({ type: "CreateRoom" })}>
      Create
    </button>
  );
}

function LeaveButton(): ReactElement {
  const forgetRoom = useForgetRoom();
  return (
    <button type="button" onClick={() => forgetRoom()}>
      Leave
    </button>
  );
}

function renderUnder(
  store: DuelStore,
  send: (message: ClientMessage) => void,
  child: ReactElement,
  forgetRoom?: () => void,
): void {
  render(
    <DuelProvider store={store} send={send} forgetRoom={forgetRoom}>
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

  it("hands a component the send the client was booted with", () => {
    const store = createDuelStore();
    const send: (message: ClientMessage) => void = vi.fn();

    renderUnder(store, send, <CreateButton />);

    const button = screen.getByText("Create");
    fireEvent.click(button);

    expect(send).toHaveBeenCalledOnce();
    expect(send).toHaveBeenCalledWith({ type: "CreateRoom" });
  });

  it("refuses to render a duel component with no provider above it", () => {
    expect(() => render(<RoomCode />)).toThrow(/DuelProvider/);
  });

  it("hands a component the forget the client was booted with", () => {
    const store = createDuelStore();
    const send: (message: ClientMessage) => void = vi.fn();
    const forgetRoom: () => void = vi.fn();

    renderUnder(store, send, <LeaveButton />, forgetRoom);

    const button = screen.getByText("Leave");
    fireEvent.click(button);

    expect(forgetRoom).toHaveBeenCalledOnce();
    expect(send).not.toHaveBeenCalled();
  });

  it("hands a component a forget that does nothing where none was given", () => {
    const store = createDuelStore();
    const send: (message: ClientMessage) => void = vi.fn();

    renderUnder(store, send, <LeaveButton />);

    const button = screen.getByText("Leave");
    expect(() => fireEvent.click(button)).not.toThrow();
    expect(send).not.toHaveBeenCalled();
  });
});
