import { describe, it, expect, vi } from "vitest";
import { readFileSync } from "node:fs";
import { resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { act, render, screen, fireEvent } from "@testing-library/react";
import type { ReactNode } from "react";
import { App } from "./App";
import { DuelProvider } from "./store/duel-provider";
import { createDuelStore } from "./store/duel-store";
import { HistoryProvider, LadderProvider } from "./main";
import { HistoryScreen } from "./history/HistoryScreen";
import { LadderScreen } from "./ladder/LadderScreen";
import type { Snapshot, SeatView } from "./protocol";

const here = dirname(fileURLToPath(import.meta.url));

// main.tsx's HistoryProvider hands HistoryScreen the real readHistory, bound to
// window.fetch and localStorage. Node's own localStorage shadows jsdom's and is
// undefined, so mounting that real tree here throws inside HistoryScreen's mount
// effect and rejects a promise nothing in this file awaits. Every test below gets
// this fake standing in for the whole module instead, so no test reaches
// main.tsx's binding — the same reason the layering guard further down hands
// HistoryScreen a fake `read` directly, without going through main.tsx at all.
vi.mock("./main", () => {
  const fakeHistoryRead = vi.fn(async () => ({
    kind: "page" as const,
    duels: [],
    nextCursor: null,
    restarted: false,
  }));
  const fakeLadderRead = vi.fn(async () => ({
    kind: "page" as const,
    page: {
      season: "2026-08",
      rows: [],
      nextCursor: null,
      self: null,
    },
  }));
  return {
    HistoryProvider: (props: { children: ReactNode }): ReactNode =>
      props.children,
    useHistory: () => fakeHistoryRead,
    LadderProvider: (props: { children: ReactNode }): ReactNode =>
      props.children,
    useLadder: () => fakeLadderRead,
  };
});

function seatView(index: number): SeatView {
  return {
    index,
    stack: 500,
    committedThisStreet: 0,
    committedThisHand: 0,
    hasFolded: false,
    isAllIn: false,
    holeCards: [],
  };
}

function renderApp(): void {
  render(
    <HistoryProvider>
      <LadderProvider>
        <DuelProvider store={createDuelStore()} send={vi.fn()}>
          <App />
        </DuelProvider>
      </LadderProvider>
    </HistoryProvider>,
  );
}

describe("App", () => {
  it("renders the application heading", () => {
    renderApp();
    expect(screen.getByRole("heading").textContent).toBe("Poker Duels");
  });

  it("gives the heading a token-derived class", () => {
    renderApp();
    const heading = screen.getByRole("heading");
    expect(heading.className.split(" ")).toContain("text-title");
  });

  it("renders the lobby beneath the heading", () => {
    renderApp();
    expect(
      screen.getByRole("button", { name: "Create a duel room" }),
    ).toBeDefined();
  });

  it("binds the history read to the browser fetch and the browser storage", () => {
    // The binding in main.tsx is asserted by reading its source: it names
    // window.fetch and localStorage, and HistoryScreen.tsx and duel-page.ts
    // name neither. Fails against a component that reaches for a global.
    const mainSource = readFileSync(resolve(here, "main.tsx"), "utf-8");
    expect(mainSource).toMatch(/window\.fetch\(/);
    expect(mainSource).toMatch(/localStorage/);
    expect(mainSource).toMatch(/readDuelPage/);

    const historyScreenSource = readFileSync(
      resolve(here, "history/HistoryScreen.tsx"),
      "utf-8",
    );
    // HistoryScreen should not use window.fetch or localStorage
    expect(historyScreenSource).not.toMatch(/window\.fetch\(/);
    expect(historyScreenSource).not.toMatch(/localStorage\./);

    const duelPageSource = readFileSync(
      resolve(here, "profile/duel-page.ts"),
      "utf-8",
    );
    // duel-page should not use window.fetch or localStorage
    expect(duelPageSource).not.toMatch(/window\.fetch\(/);
    expect(duelPageSource).not.toMatch(/localStorage\./);
  });

  it("binds the ladder read to the browser fetch and the browser storage", () => {
    // The same source assertion as the history one above, for the ladder's
    // own binding: main.tsx names window.fetch, localStorage and
    // readLadderPage, and neither LadderScreen.tsx nor ladder-read.ts names
    // window.fetch or localStorage. Fails against a component that reaches
    // for a global.
    const mainSource = readFileSync(resolve(here, "main.tsx"), "utf-8");
    expect(mainSource).toMatch(/window\.fetch\(/);
    expect(mainSource).toMatch(/localStorage/);
    expect(mainSource).toMatch(/readLadderPage/);

    const ladderScreenSource = readFileSync(
      resolve(here, "ladder/LadderScreen.tsx"),
      "utf-8",
    );
    // LadderScreen should not use window.fetch or localStorage
    expect(ladderScreenSource).not.toMatch(/window\.fetch\(/);
    expect(ladderScreenSource).not.toMatch(/localStorage\./);

    const ladderReadSource = readFileSync(
      resolve(here, "ladder/ladder-read.ts"),
      "utf-8",
    );
    // ladder-read should not use window.fetch or localStorage
    expect(ladderReadSource).not.toMatch(/window\.fetch\(/);
    expect(ladderReadSource).not.toMatch(/localStorage\./);
  });

  it("leaves the lobby exactly as it was for a player who never opens the record", () => {
    // The three merged tests still find the heading, its class and the
    // *Create a duel room* button, and nothing from the history screen is on
    // the first screen a player sees.
    renderApp();

    // The app heading is still there
    expect(screen.getByRole("heading").textContent).toBe("Poker Duels");

    // The heading still has its token-derived class
    const heading = screen.getByRole("heading");
    expect(heading.className.split(" ")).toContain("text-title");

    // The create button is still there
    expect(
      screen.getByRole("button", { name: "Create a duel room" }),
    ).toBeDefined();

    // The history screen (aria-label="your duels") is not rendered
    const historyScreen = screen.queryByLabelText("your duels");
    expect(historyScreen).toBeNull();
  });

  it("leaves the first screen for the record, and comes back to it", () => {
    // The affordance: that a player can reach the record and get back.
    // This test renders App, activates *Your duels*, asserts the first
    // screen is gone, activates *Back*, and asserts it is there again.
    renderApp();

    // The lobby is showing
    expect(
      screen.getByRole("button", { name: "Create a duel room" }),
    ).toBeDefined();

    // Click "Your duels" button
    const yourDuelsButton = screen.getByRole("button", { name: "Your duels" });
    fireEvent.click(yourDuelsButton);

    // The create button is gone
    expect(
      screen.queryByRole("button", { name: "Create a duel room" }),
    ).toBeNull();

    // The history screen is shown
    const historyScreen = screen.getByLabelText("your duels");
    expect(historyScreen).toBeDefined();

    // Click "Back" button
    const backButton = screen.getByRole("button", { name: "Back" });
    fireEvent.click(backButton);

    // The create button is back
    expect(
      screen.getByRole("button", { name: "Create a duel room" }),
    ).toBeDefined();

    // The history screen is gone
    expect(screen.queryByLabelText("your duels")).toBeNull();
  });

  it("leaves the first screen for the ladder, and comes back to it", () => {
    // The same round trip as the record's test above, for the fifth control
    // ADR-0060 predicted the first screen would carry: the lobby is showing,
    // *Leaderboard* is clicked, *Create a duel room* is gone and the ladder
    // is on screen; *Back* is clicked, and the lobby is back.
    renderApp();

    // The lobby is showing
    expect(
      screen.getByRole("button", { name: "Create a duel room" }),
    ).toBeDefined();

    // Click "Leaderboard" button
    const leaderboardButton = screen.getByRole("button", {
      name: "Leaderboard",
    });
    fireEvent.click(leaderboardButton);

    // The create button is gone
    expect(
      screen.queryByRole("button", { name: "Create a duel room" }),
    ).toBeNull();

    // The ladder screen is shown
    const ladderScreen = screen.getByLabelText("leaderboard");
    expect(ladderScreen).toBeDefined();

    // Click "Back" button
    const backButton = screen.getByRole("button", { name: "Back" });
    fireEvent.click(backButton);

    // The create button is back
    expect(
      screen.getByRole("button", { name: "Create a duel room" }),
    ).toBeDefined();

    // The ladder screen is gone
    expect(screen.queryByLabelText("leaderboard")).toBeNull();
  });

  it("mounted history screen carries exactly one heading", () => {
    // Assert the HistoryScreen component contains exactly one heading total.
    // This test guards against adding extra headings inside HistoryScreen.
    renderApp();

    // Click "Your duels" to show the history screen
    const yourDuelsButton = screen.getByRole("button", { name: "Your duels" });
    fireEvent.click(yourDuelsButton);

    // Get the history screen section
    const historyScreen = screen.getByLabelText("your duels");

    // Count headings within the history screen
    const headingsInHistory = historyScreen.querySelectorAll(
      "h1, h2, h3, h4, h5, h6",
    );

    // Should be exactly one heading in the history screen
    expect(headingsInHistory).toHaveLength(1);
  });

  it("offers the door regardless of profile read failure", () => {
    // The door does not depend on the profile read. The control appears on the
    // first screen even when the profile read fails or is unavailable.
    renderApp();

    // The "Your duels" button should always be visible, even if profile failed
    const yourDuelsButton = screen.queryByRole("button", {
      name: "Your duels",
    });
    expect(yourDuelsButton).not.toBeNull();
  });

  it("does not offer the door while a duel is in progress", () => {
    // The door is offered only where a player is not in a duel. A duel
    // outranks the record. If the store ever moves into a duel while the
    // record is open, the duel takes the screen.
    const store = createDuelStore();
    const send = vi.fn();

    render(
      <HistoryProvider>
        <DuelProvider store={store} send={send}>
          <App />
        </DuelProvider>
      </HistoryProvider>,
    );

    // Initially in lobby, door should be available
    expect(screen.queryByRole("button", { name: "Your duels" })).not.toBeNull();

    // Simulate joining a room
    act(() => {
      store.apply({ type: "RoomJoined", code: "ABCDEFGH", seat: 0 });
    });

    // Simulate a duel starting with a Snapshot (view !== null)
    const snapshot: Snapshot = {
      type: "Snapshot",
      view: {
        viewerSeat: 0,
        handNumber: 1,
        buttonSeat: 0,
        street: "PREFLOP",
        board: { cards: [] },
        pot: 30,
        betToMatch: 20,
        minRaiseTo: 40,
        seatToAct: 0,
        smallBlind: 10,
        bigBlind: 20,
        seats: [seatView(0), seatView(1)],
      },
    };

    act(() => {
      store.apply(snapshot);
    });

    // Now the duel is in progress (view !== null), and the door should not be available
    // If the door button is shown (due to the bug I added), this assertion will fail
    expect(screen.queryByRole("button", { name: "Your duels" })).toBeNull();
  });

  it("HistoryScreen renders no Back button when mounted on its own", () => {
    // HistoryScreen is a pure presentation component that knows nothing about
    // navigation. The Back button is rendered by whatever renders the swap (Lobby),
    // not by HistoryScreen itself. This guard ensures HistoryScreen never takes on
    // navigation knowledge, which would violate ADR-0060 §4 layering.
    const mockRead = vi.fn(async () => ({
      kind: "page" as const,
      duels: [],
      nextCursor: null,
      restarted: false,
    }));

    render(<HistoryScreen read={mockRead} />);

    // HistoryScreen should not render a Back button
    const backButton = screen.queryByRole("button", { name: "Back" });
    expect(backButton).toBeNull();
  });

  it("renders no Back button when the ladder screen is mounted on its own", () => {
    // LadderScreen is a pure presentation component that knows nothing about
    // navigation, mirroring the HistoryScreen guard above. The Back button is
    // rendered by whatever renders the swap (Lobby), never by LadderScreen
    // itself — ADR-0060's layering.
    const mockRead = vi.fn(async () => ({
      kind: "page" as const,
      page: {
        season: "2026-08",
        rows: [],
        nextCursor: null,
        self: null,
      },
    }));

    render(<LadderScreen read={mockRead} />);

    // LadderScreen should not render a Back button
    const backButton = screen.queryByRole("button", { name: "Back" });
    expect(backButton).toBeNull();
  });
});
