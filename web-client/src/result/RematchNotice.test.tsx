import { describe, it, expect, vi, beforeEach } from "vitest";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { DuelProvider } from "../store/duel-provider";
import { createDuelStore, type DuelStore } from "../store/duel-store";
import { RematchNotice } from "./RematchNotice";
import { RIVAL_OFFERS, REMATCH_LABEL } from "./rematch-text";

// The room fixture: RoomJoined seats this tab, DuelFinished ends the duel
// (clearing nothing an offer needs — duel-state.ts:350-361 — but establishing
// `outcome`, which is what a rematch offer arrives beside), then
// RematchOffered names the offering seat. The order is load-bearing.
function storeWith(mySeat: number, offeringSeat: number): DuelStore {
  const store = createDuelStore();
  store.apply({ type: "RoomJoined", code: "ABCDEF", seat: mySeat });
  store.apply({
    type: "DuelFinished",
    outcome: { winner: null, handsPlayed: 1, finalStacks: [0, 0] },
  });
  store.apply({ type: "RematchOffered", seat: offeringSeat });
  return store;
}

function renderNotice(store: DuelStore, send = vi.fn()) {
  return render(
    <DuelProvider store={store} send={send}>
      <RematchNotice />
    </DuelProvider>,
  );
}

describe("the rematch notice", () => {
  beforeEach(() => {
    window.location.hash = "";
  });

  it("stands over a chosen screen when the rival has offered", () => {
    window.location.hash = "#/account";
    const store = storeWith(1, 0);
    renderNotice(store);

    const line = screen.getByText(RIVAL_OFFERS);
    expect(line.closest('[role="status"]')).not.toBeNull();
  });

  it("says nothing on the room's own screen", () => {
    window.location.hash = "";
    const store = storeWith(1, 0);
    renderNotice(store);

    expect(screen.queryByText(RIVAL_OFFERS)).toBeNull();
  });

  it("follows only an incoming offer", () => {
    window.location.hash = "#/account";

    const rivalStore = storeWith(1, 0);
    const first = renderNotice(rivalStore);
    expect(within(first.container).getByText(RIVAL_OFFERS)).toBeDefined();
    first.unmount();

    const ownStore = storeWith(0, 0);
    const second = renderNotice(ownStore);
    expect(within(second.container).queryByText(RIVAL_OFFERS)).toBeNull();
    second.unmount();
  });

  it("says nothing to a store that has been told nothing", () => {
    window.location.hash = "#/account";
    const store = createDuelStore();
    renderNotice(store);

    expect(screen.queryByText(RIVAL_OFFERS)).toBeNull();
  });

  it("says nothing to a client that holds no seat", () => {
    window.location.hash = "#/account";
    const store = createDuelStore();
    store.apply({
      type: "DuelFinished",
      outcome: { winner: null, handsPlayed: 1, finalStacks: [0, 0] },
    });
    store.apply({ type: "RematchOffered", seat: 0 });
    renderNotice(store);

    expect(screen.queryByText(RIVAL_OFFERS)).toBeNull();
  });

  it("offers the rematch control beside the rival's line", () => {
    window.location.hash = "#/account";
    const store = storeWith(1, 0);
    renderNotice(store);

    const button = screen.getByRole("button", { name: REMATCH_LABEL });
    expect(button.closest('[role="status"]')).not.toBeNull();
  });

  it("one press sends exactly one OfferRematch and nothing else", () => {
    window.location.hash = "#/account";
    const store = storeWith(1, 0);
    const send = vi.fn();
    renderNotice(store, send);

    fireEvent.click(screen.getByRole("button", { name: REMATCH_LABEL }));

    expect(send).toHaveBeenCalledTimes(1);
    expect(send).toHaveBeenCalledWith({ type: "OfferRematch" });
  });

  it("the press opens the dealing span in place of the button", () => {
    window.location.hash = "#/account";
    const store = storeWith(1, 0);
    renderNotice(store);

    fireEvent.click(screen.getByRole("button", { name: REMATCH_LABEL }));

    expect(
      screen.getByText(/The button changes sides.*dealing hand 1…/),
    ).toBeDefined();
    expect(screen.queryByRole("button", { name: REMATCH_LABEL })).toBeNull();
  });

  it("the panel offers the button again after the offer that was accepted has gone", () => {
    window.location.hash = "#/account";
    const store = storeWith(1, 0);
    renderNotice(store);

    fireEvent.click(screen.getByRole("button", { name: REMATCH_LABEL }));
    expect(screen.queryByRole("button", { name: REMATCH_LABEL })).toBeNull();

    act(() => {
      store.apply({
        type: "Snapshot",
        view: {
          viewerSeat: 1,
          handNumber: 1,
          buttonSeat: 0,
          street: "PREFLOP",
          board: { cards: [] },
          pot: 0,
          betToMatch: 0,
          minRaiseTo: 0,
          seatToAct: 0,
          smallBlind: 1,
          bigBlind: 2,
          seats: [],
        },
      });
    });
    act(() => {
      store.apply({
        type: "DuelFinished",
        outcome: { winner: null, handsPlayed: 1, finalStacks: [0, 0] },
      });
    });
    act(() => {
      store.apply({ type: "RematchOffered", seat: 0 });
    });

    expect(screen.getByRole("button", { name: REMATCH_LABEL })).toBeDefined();
    expect(
      screen.queryByText(/The button changes sides.*dealing hand 1…/),
    ).toBeNull();
  });
});
