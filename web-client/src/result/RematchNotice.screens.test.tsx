import { describe, it, expect, vi, beforeEach } from "vitest";
import {
  render,
  screen,
  fireEvent,
  waitFor,
  act,
  type RenderResult,
} from "@testing-library/react";
import type { ReactNode } from "react";
import { App } from "../App";
import { DuelProvider } from "../store/duel-provider";
import { createDuelStore, type DuelStore } from "../store/duel-store";
import { useSignedIn } from "../main";
import {
  AccountProvider,
  type AccountCalls,
} from "../account/account-provider";
import { RIVAL_OFFERS, NOT_NOW } from "./rematch-text";
import { HISTORY_HEADING } from "../history/history-text";
import { LADDER_HEADING } from "../ladder/ladder-text";
import { ACCOUNT_HEADING, SIGN_IN_HEADING } from "../account/account-text";
import { VERIFY_HEADING, RESET_HEADING } from "../account/recovery-text";
import { hashForScreen, type Screen } from "../routing/screen";
import type { Snapshot, SeatView } from "../protocol";

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

// TASK-141508: this file proves App.tsx's one addition — <RematchNotice />
// mounted beside <Lobby /> — actually follows onto every chosen screen
// rather than appearing on one. It copies App.test.tsx's vi.mock("./main")
// factory and provider stack rather than opening that file: App.test.tsx is
// gated at its own measured count, and this story opens none of its tests.
vi.mock("../main", () => {
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
    SignedInProvider: (props: { children: ReactNode }): ReactNode =>
      props.children,
    useSignedIn: vi.fn(() => false),
    nameAskSkippedHere: () => false,
    skipNameAskHere: vi.fn(),
  };
});

const fakeAccountCalls: AccountCalls = {
  signUp: vi.fn() as unknown as AccountCalls["signUp"],
  signIn: vi.fn() as unknown as AccountCalls["signIn"],
  signOut: vi.fn() as unknown as AccountCalls["signOut"],
  revokeThisDevice: vi.fn() as unknown as AccountCalls["revokeThisDevice"],
  attachRecoveryEmail:
    vi.fn() as unknown as AccountCalls["attachRecoveryEmail"],
  forgotPassword: vi.fn() as unknown as AccountCalls["forgotPassword"],
  verifyEmail: vi.fn() as unknown as AccountCalls["verifyEmail"],
  resetPassword: vi.fn() as unknown as AccountCalls["resetPassword"],
};

// The room fixture: RoomJoined seats this tab at seat 1, DuelFinished ends
// the duel, then RematchOffered from seat 0 — the rival — is the incoming
// offer RematchNotice follows. The order is load-bearing (RematchNotice.test.tsx).
function incomingOfferStore(): DuelStore {
  const store = createDuelStore();
  store.apply({ type: "RoomJoined", code: "ABCDEF", seat: 1 });
  store.apply({
    type: "DuelFinished",
    outcome: { winner: null, handsPlayed: 1, finalStacks: [0, 0] },
  });
  store.apply({ type: "RematchOffered", seat: 0 });
  return store;
}

function renderAppWith(store: DuelStore): RenderResult {
  return render(
    <AccountProvider calls={fakeAccountCalls}>
      <DuelProvider store={store} send={vi.fn()}>
        <App />
      </DuelProvider>
    </AccountProvider>,
  );
}

describe("the rematch notice, mounted by App", () => {
  beforeEach(() => {
    window.location.hash = "";
    vi.mocked(useSignedIn).mockReturnValue(false);
  });

  it("follows onto every chosen screen", () => {
    // Six chosen screens, an enumeration rather than a sample (ADR-0123 §1's
    // no carve-out): a table of [hash, heading] pairs, each rendered and
    // unmounted in turn. The heading is the positive control — without it, a
    // test that only looks for RIVAL_OFFERS would pass on any hash the panel
    // happens to render for, including one where the chosen screen itself
    // did not render.
    //
    // Keyed by Screen, not a hand-written array: TypeScript requires every
    // member of the union as a key of this Record, so a screen added to
    // `screen.ts` tomorrow is a compile error here until someone decides
    // whether it belongs beside these six or beside "first". "first" is the
    // room's own screen, where the panel must not appear (ADR-0123 §1), so
    // it carries `null` here and is filtered out of the loop below rather
    // than iterated over.
    const headingByScreen: Record<Screen, string | null> = {
      first: null,
      duels: HISTORY_HEADING,
      leaderboard: LADDER_HEADING,
      account: ACCOUNT_HEADING,
      "sign-in": SIGN_IN_HEADING,
      verify: VERIFY_HEADING,
      reset: RESET_HEADING,
    };

    const screens: readonly [hash: string, heading: string][] = (
      Object.entries(headingByScreen) as [Screen, string | null][]
    )
      .filter((entry): entry is [Screen, string] => entry[0] !== "first")
      .map(([screenName, heading]) => [hashForScreen(screenName), heading]);

    for (const [hash, heading] of screens) {
      // The address is set before render(): assigning it inside act() does
      // not settle a useScreen consumer, since jsdom queues hashchange and
      // act's synchronous flush does not run it (measured at f20d07ed).
      window.location.hash = hash;

      const store = incomingOfferStore();
      const view = renderAppWith(store);

      expect(screen.getByRole("heading", { name: heading })).toBeDefined();

      const line = screen.getByText(RIVAL_OFFERS);
      expect(line.closest('[role="status"]')).not.toBeNull();

      view.unmount();
    }
  });

  it("one fact never has two live surfaces", () => {
    window.location.hash = "";
    const store = incomingOfferStore();
    renderAppWith(store);

    // RematchControl's line on the result screen, not the panel: a count,
    // not a presence check, because presence is what two live surfaces
    // would also satisfy.
    const matches = screen.getAllByText(RIVAL_OFFERS);
    expect(matches).toHaveLength(1);
    expect(matches[0].closest('[role="status"]')).toBeNull();
  });

  it("the dismissal survives a screen change and a trip through the lobby", async () => {
    // ADR-0138 §8's own discriminating assertion: a panel mounted inside
    // Lobby's cascade would pass the first half of this walk (dismiss on
    // #/duels, still hidden on #/account) and lose the dismissal the moment
    // the walk passes back through "/", because that leg unmounts every
    // branch Lobby renders. Mounting beside <Lobby />, as App.tsx does,
    // survives all four legs.
    window.location.hash = hashForScreen("duels");
    const store = incomingOfferStore();
    renderAppWith(store);

    // The positive control for the first leg: the destination screen's own
    // heading, not just the panel's presence.
    expect(
      screen.getByRole("heading", { name: HISTORY_HEADING }),
    ).toBeDefined();
    fireEvent.click(screen.getByText(NOT_NOW));

    // #/duels -> #/account. Assigning the hash alone does not settle a
    // useScreen consumer (jsdom queues hashchange; act's synchronous flush
    // does not run it, measured at f20d07ed), so the await on the
    // destination's own heading is the positive control that the address
    // actually moved.
    window.location.hash = hashForScreen("account");
    await waitFor(() =>
      expect(
        screen.getByRole("heading", { name: ACCOUNT_HEADING }),
      ).toBeDefined(),
    );
    expect(screen.queryByText(RIVAL_OFFERS)).toBeNull();

    // #/account -> "/", the leg that unmounts every branch Lobby renders.
    // The positive control here is not a heading but the result screen's
    // own line: getAllByText throws until RematchControl's line (not the
    // panel's) is on screen, so a walk that silently failed to reach "/"
    // fails here rather than passing on an absence that proves nothing.
    window.location.hash = hashForScreen("first");
    await waitFor(() => {
      const matches = screen.getAllByText(RIVAL_OFFERS);
      expect(matches).toHaveLength(1);
      expect(matches[0].closest('[role="status"]')).toBeNull();
    });

    // "/" -> #/account again: the dismissal must still hold after the trip
    // through the lobby, not merely across the first screen change.
    window.location.hash = hashForScreen("account");
    await waitFor(() =>
      expect(
        screen.getByRole("heading", { name: ACCOUNT_HEADING }),
      ).toBeDefined(),
    );
    expect(screen.queryByText(RIVAL_OFFERS)).toBeNull();
  });

  it("the agreement takes the screen with no panel over it", () => {
    // Offer standing at #/account: the panel is up before the agreement
    // arrives, so the assertion below proves the agreement took the screen
    // away from the panel rather than there never having been one to take.
    window.location.hash = hashForScreen("account");
    const store = incomingOfferStore();
    renderAppWith(store);

    expect(
      screen.getByRole("heading", { name: ACCOUNT_HEADING }),
    ).toBeDefined();
    expect(screen.getByText(RIVAL_OFFERS)).toBeDefined();

    // The agreeing Snapshot, applied in its own act(): the same fixture
    // App.test.tsx:967 builds for "shows the duel to a player reading the
    // account screen when a frame seats them", which asserts the same
    // table node and the same empty hash for the same restore.
    const snapshot: Snapshot = {
      type: "Snapshot",
      view: {
        viewerSeat: 1,
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

    // All three literals together: the table proves the Snapshot landed,
    // the absent status role proves no panel is over it, and the empty
    // hash proves ADR-0114 §3's layout restore ran in the same commit. Any
    // two alone would also pass a version where the panel flashed over the
    // table for one frame.
    expect(screen.getByText("You")).toBeDefined();
    expect(screen.queryByRole("status")).toBeNull();
    expect(window.location.hash).toBe("");
  });
});
