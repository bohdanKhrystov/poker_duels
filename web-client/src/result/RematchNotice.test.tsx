import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, within } from "@testing-library/react";
import { DuelProvider } from "../store/duel-provider";
import { createDuelStore, type DuelStore } from "../store/duel-store";
import { RematchNotice } from "./RematchNotice";
import { RIVAL_OFFERS } from "./rematch-text";

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

function renderNotice(store: DuelStore) {
  return render(
    <DuelProvider store={store} send={vi.fn()}>
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
});
