import { fireEvent, render, screen } from "@testing-library/react";
import { describe, it, expect, vi, afterEach } from "vitest";
import { Lobby } from "./Lobby";
import { DuelProvider } from "../store/duel-provider";
import { createDuelStore, type DuelStore } from "../store/duel-store";

const ROOM_JOINED = { type: "RoomJoined", code: "ABCDEFGH", seat: 0 } as const;

function withClipboard(writeText: () => Promise<void>): void {
  Object.defineProperty(navigator, "clipboard", {
    value: { writeText },
    configurable: true,
  });
}

afterEach(() => {
  Reflect.deleteProperty(navigator, "clipboard");
});

function renderLobby(store: DuelStore = createDuelStore()): {
  send: ReturnType<typeof vi.fn>;
} {
  const send = vi.fn();
  render(
    <DuelProvider store={store} send={send}>
      <Lobby />
    </DuelProvider>,
  );
  return { send };
}

function typeCode(value: string): void {
  fireEvent.change(screen.getByLabelText("Room code"), { target: { value } });
}

describe("the lobby", () => {
  it("asks the server for a room when the host clicks create", () => {
    const { send } = renderLobby();

    fireEvent.click(screen.getByRole("button", { name: "Create a duel room" }));

    expect(send).toHaveBeenCalledOnce();
    expect(send).toHaveBeenCalledWith({ type: "CreateRoom" });
  });

  it("sends a pasted code trimmed and upper-cased", () => {
    const { send } = renderLobby();

    typeCode("  abcdefgh  ");
    fireEvent.click(screen.getByRole("button", { name: "Join the duel" }));

    expect(send).toHaveBeenCalledOnce();
    expect(send).toHaveBeenCalledWith({ type: "JoinRoom", code: "ABCDEFGH" });
  });

  it("sends nothing when the code box holds only whitespace", () => {
    const { send } = renderLobby();

    typeCode("   ");
    fireEvent.click(screen.getByRole("button", { name: "Join the duel" }));

    expect(send).not.toHaveBeenCalled();
  });

  it("shows the room code the server named", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    renderLobby(store);

    expect(screen.getByText("ABCDEFGH")).toBeDefined();
    expect(
      screen.queryByRole("button", { name: "Create a duel room" }),
    ).toBeNull();
  });

  it("shows an invite link carrying that code", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    renderLobby(store);

    const inviteLink = screen.getByLabelText<HTMLInputElement>("Invite link");
    expect(inviteLink.value).toBe("http://localhost:3000/?room=ABCDEFGH");
  });

  it("leaves the invite link selectable and focused for a copy by hand", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    renderLobby(store);

    const inviteLink = screen.getByLabelText<HTMLInputElement>("Invite link");
    expect(inviteLink.readOnly).toBe(true);
    expect(document.activeElement).toBe(inviteLink);
  });

  it("copies the invite link when the browser has a clipboard", async () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    const writeText = vi.fn(() => Promise.resolve());
    withClipboard(writeText);
    renderLobby(store);

    fireEvent.click(screen.getByRole("button", { name: "Copy the link" }));

    expect(writeText).toHaveBeenCalledWith(
      "http://localhost:3000/?room=ABCDEFGH",
    );
    await screen.findByText("Link copied.");
  });

  it("offers no copy button when the browser has no clipboard", () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    renderLobby(store);

    expect(screen.queryByRole("button", { name: "Copy the link" })).toBeNull();
    const inviteLink = screen.getByLabelText<HTMLInputElement>("Invite link");
    expect(inviteLink.value).toBe("http://localhost:3000/?room=ABCDEFGH");
  });

  it("keeps the link in reach when the clipboard refuses", async () => {
    const store = createDuelStore();
    store.apply(ROOM_JOINED);
    withClipboard(() => Promise.reject(new Error("denied")));
    renderLobby(store);

    fireEvent.click(screen.getByRole("button", { name: "Copy the link" }));

    await screen.findByText("Copy it from the box above.");
    const inviteLink = screen.getByLabelText<HTMLInputElement>("Invite link");
    expect(inviteLink.value).toBe("http://localhost:3000/?room=ABCDEFGH");
  });
});
