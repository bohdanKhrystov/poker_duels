import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
} from "@testing-library/react";
import { describe, it, expect, afterEach } from "vitest";
import { Lobby } from "../lobby/Lobby";
import { DuelProvider } from "../store/duel-provider";
import { createDuelStore, type DuelStore } from "../store/duel-store";
import { aLegalActions } from "./turn-fixture";
import { aView } from "./view-fixture";

/**
 * `ADR-0110` §3: before the first `Snapshot` the client holds a room code and
 * a seat number and nothing else, so the table states no stack, no blind, no
 * card, no pot, no dealer button and no action bar — none of those is a fact
 * the server has stated yet, and a client may never assert a game fact. This
 * file is that refusal made enforceable: a surface a later `EPIC-13` story
 * adds to the table either renders nothing while `view` is `null`, or says
 * here what it renders instead.
 *
 * The acting mark (`TASK-130303`) renders nothing here: the host-alone
 * screen is `WaitingTable`, which mounts no `SeatPlate` at all, and the mark
 * itself speaks no `aria-label` and no `title` on any screen it does reach —
 * so neither `spoken()` nor the digit sweep below changes shape to admit it.
 *
 * The last-act mark shows nothing here either, and belt and braces: `Lobby.tsx` mounts
 * `WaitingTable` — never `DuelTable`, so no `SeatPlate` at all — while `view` is `null`, and
 * `state.lastAct` itself stays `null` until an `Events` frame carries an act (`TASK-130403`):
 * the opening frame of a hand carries only `HandStarted`, `BlindPosted`, `HoleCardsDealt` and
 * `ActionOn`, and no act of its own. The mark speaks nothing on any screen it does reach either
 * — `TASK-130406` pins one `aria-label` and zero `title` on the plate — so neither `spoken()`
 * nor the digit sweep below changes shape to admit it.
 *
 * The typed total (`TASK-130505`, `aria-label="the total"`) renders nothing here for the usual
 * reason: `Lobby.tsx` mounts `WaitingTable`, never `ActionBar`, while `view` is `null`. But it is
 * the one surface on this page that does not appear the moment a view arrives either — the field
 * lives inside the bar's `Live`, which does not render until the server has named a turn, and
 * even then only when that turn carries a bet or a raise. The test below walks past the live
 * table the earlier tests already open, to prove that middle wait is real and not merely untested.
 *
 * The chip pile (`TASK-130605`–`TASK-130607`) shows nothing here either: `Lobby.tsx` mounts
 * `WaitingTable`, which mounts none of `SeatPlate`, `PotStrip` or `DuelTable` — the three surfaces
 * that draw one — while `view` is `null`. Unlike the typed total, it has no third state: it
 * appears the moment a `Snapshot` arrives and never later, exactly like the acting mark. And it
 * is `aria-hidden` with no text node, so neither `spoken()` nor the digit sweep below changes
 * shape to admit it.
 *
 * The turn clock — the acting seat's countdown and both seats' timebanks (`TASK-130910`) — renders
 * nothing here either, for the same structural reason as the acting mark and the last-act mark
 * above: `Lobby.tsx` mounts `WaitingTable` — never `DuelTable`, so no `SeatPlate` at all — while
 * `view` is `null`, and `state.turnClock` itself stays `null` until a `TurnClock` frame arrives,
 * which the server sends no sooner than the first `Snapshot`. Unlike those two marks, the figure
 * this surface would draw carries a digit and speaks no `aria-label` and no `title` of its own —
 * so it is the digit sweep below, not `spoken()`, that would redden were it ever drawn here, and
 * the test below closes both anyway: on the bank's own text, and on the class that colours the
 * figure.
 */

afterEach(() => {
  // A clipboard installed by one test must not leak into the next: it would
  // silently change what "Copy the link" does under the next test's nose.
  Reflect.deleteProperty(navigator, "clipboard");
});

/**
 * Every text node under `root`, trimmed, empties dropped. Adapted from
 * `no-derivation.test.tsx` — not imported, because that file's helpers are
 * shaped around a `PlayerView` this screen does not have yet.
 */
function textNodes(root: HTMLElement): string[] {
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
  const texts: string[] = [];
  for (let node = walker.nextNode(); node !== null; node = walker.nextNode()) {
    const text = node.textContent?.trim() ?? "";
    if (text !== "") texts.push(text);
  }
  return texts;
}

/**
 * Every `aria-label` and every `title` attribute value under `root`. A figure
 * or a name the client worked out for itself reaches the player from either
 * of those just as surely as from print, so a text-only sweep is not enough
 * to stand as the whole guard: this file asserts the result closed to the
 * empty set on the null view, on top of folding it into `digitBearing`'s
 * figure sweep — a name spoken here is caught on its own, not only when it
 * happens to contain a digit.
 */
function spoken(root: HTMLElement): string[] {
  return [...root.querySelectorAll("[aria-label], [title]")]
    .flatMap((element) => [
      element.getAttribute("aria-label"),
      element.getAttribute("title"),
    ])
    .filter((value): value is string => value !== null);
}

/** Every string the screen prints or speaks that carries at least one digit. */
function digitBearing(root: HTMLElement): string[] {
  return [...textNodes(root), ...spoken(root)].filter((value) =>
    /\d/.test(value),
  );
}

function withClipboard(writeText: () => Promise<void>): void {
  Object.defineProperty(navigator, "clipboard", {
    value: { writeText },
    configurable: true,
  });
}

/**
 * Renders the screen a host sees the moment `CreateRoom` answers, before any
 * `Snapshot` has arrived — `RoomJoined` and nothing else. The whole screen,
 * not `WaitingTable` alone: only the screen can say whether a future story
 * draws a game fact *beside* the table rather than on it.
 */
function renderNullView(code: string): {
  container: HTMLElement;
  store: DuelStore;
} {
  const store = createDuelStore();
  store.apply({ type: "RoomJoined", code, seat: 0 });
  const { container } = render(
    <DuelProvider store={store} send={() => {}} forgetRoom={() => {}}>
      <Lobby />
    </DuelProvider>,
  );
  return { container, store };
}

describe("what the table shows when there is no view", () => {
  it("states no figure the server never stated", () => {
    // Two inputs, not one: a literal compiled into a component would still
    // pass a single-code check by coincidence. The second code carries no
    // digit at all, so it tells an echoed code apart from a hard-coded one.
    const withDigits = renderNullView("7Q4M9K2T");
    expect(digitBearing(withDigits.container)).toEqual(["7Q4M9K2T"]);
    // Closed on its own, not only through the digit filter above: a fact
    // gated on the code's own content (a digit-bearing code, say) could
    // otherwise reach `spoken()` in this render alone and nowhere else this
    // file renders, and escape undetected.
    expect(spoken(withDigits.container)).toEqual([]);
    cleanup();

    const withoutDigits = renderNullView("ABCDEFGH");
    expect(digitBearing(withoutDigits.container)).toEqual([]);
    expect(spoken(withoutDigits.container)).toEqual([]);
  });

  it("deals no card, draws no button, offers no bar and names no pot", () => {
    const { container } = renderNullView("ABCDEFGH");

    expect(screen.queryAllByRole("img")).toHaveLength(0);
    expect(screen.queryByLabelText("the button")).toBeNull();
    expect(screen.queryByLabelText("your move")).toBeNull();
    expect(screen.queryByText(/^Pot/)).toBeNull();
    // Closed, not filtered: `digitBearing` only ever asks `spoken()` for its
    // digits, so a *name* — a card, a street, a made hand — spoken through
    // `aria-label` or `title` would pass every assertion above it in this
    // file undetected. Nothing is spoken here at all.
    expect(spoken(container)).toEqual([]);
  });

  it("finds all four of those on the live table", () => {
    // The guard on the guard: if any of these four probes ever found nothing
    // on a table that plainly has one, the previous test would be four
    // assertions about probes that never work, passing for the wrong reason
    // forever. Never a count here — a later story changing the live table
    // must not redden this for a reason unrelated to the contract.
    const { container, store } = renderNullView("ABCDEFGH");

    act(() => {
      store.apply({ type: "Snapshot", view: aView() });
    });

    expect(screen.queryAllByRole("img").length).toBeGreaterThan(0);
    expect(screen.queryByLabelText("the button")).not.toBeNull();
    expect(screen.queryByLabelText("your move")).not.toBeNull();
    expect(screen.queryByText(/^Pot/)).not.toBeNull();
    expect(digitBearing(container).length).toBeGreaterThan(0);
  });

  it("the copy control's feedback adds one named string and no other", async () => {
    // ADR-0110 §6's exhaustive six. Sorted comparison, per its own note: the
    // order these render in is the card's and the human's (ADR-0024 §3), not
    // this test's.
    const BASELINE = [
      "Waiting for your rival",
      "ABCDEFGH",
      "Invite link",
      "You",
      "Back to the lobby",
      "The room stays open. That link still works for your rival, and it brings you back.",
    ];

    withClipboard(() => Promise.resolve());
    const resolved = renderNullView("ABCDEFGH");
    expect(textNodes(resolved.container).sort()).toEqual(
      [...BASELINE, "Copy the link"].sort(),
    );
    expect(spoken(resolved.container)).toEqual([]);

    fireEvent.click(screen.getByRole("button", { name: "Copy the link" }));
    await screen.findByText("Link copied.");
    expect(textNodes(resolved.container).sort()).toEqual(
      [...BASELINE, "Copy the link", "Link copied."].sort(),
    );
    expect(spoken(resolved.container)).toEqual([]);

    cleanup();
    Reflect.deleteProperty(navigator, "clipboard");
    withClipboard(() => Promise.reject(new Error("denied")));
    const rejected = renderNullView("ABCDEFGH");
    fireEvent.click(screen.getByRole("button", { name: "Copy the link" }));
    await screen.findByText("Copy it from the box above.");
    expect(textNodes(rejected.container).sort()).toEqual(
      [...BASELINE, "Copy the link", "Copy it from the box above."].sort(),
    );
    expect(spoken(rejected.container)).toEqual([]);
  });

  it("marks no acting seat before the server has named one", () => {
    // The positive half is the guard on the guard, exactly as this file's
    // third test already does it for the four ADR-0110 probes: without it,
    // ".acting-mark" is a selector that could match nothing anywhere in this
    // app and the refusal below would pass forever for the wrong reason.
    const { container, store } = renderNullView("ABCDEFGH");

    expect(container.querySelectorAll(".acting-mark")).toHaveLength(0);
    expect(spoken(container)).toEqual([]);

    act(() => {
      store.apply({ type: "Snapshot", view: aView() });
    });

    expect(container.querySelectorAll(".acting-mark").length).toBeGreaterThan(
      0,
    );
  });

  it("marks no last act before the server has named one", () => {
    // The positive half is the guard on the guard, exactly as this file's
    // third test already does it for the four ADR-0110 probes: without it,
    // ".last-act" is a selector that could match nothing anywhere in this
    // app and the refusal below would pass forever for the wrong reason. It
    // is also the only proof that `Lobby.tsx` passes the field: delete that
    // one attribute and this half goes red.
    const { container, store } = renderNullView("ABCDEFGH");

    expect(container.querySelectorAll(".last-act")).toHaveLength(0);
    expect(spoken(container)).toEqual([]);

    act(() => {
      store.apply({
        type: "Events",
        events: [{ type: "PlayerBet", sequence: 4, seat: 1, to: 950 }],
      });
      store.apply({ type: "Snapshot", view: aView() });
    });

    expect(container.querySelectorAll(".last-act").length).toBeGreaterThan(0);
  });

  it("draws no chip before the server has named a stack", () => {
    // The positive half is the guard on the guard, exactly as this file's
    // third test already does it for the four ADR-0110 probes: without it,
    // ".chip-pile" is a selector that could match nothing anywhere in this
    // app and the refusal below would pass forever for the wrong reason.
    const { container, store } = renderNullView("ABCDEFGH");

    expect(container.querySelectorAll(".chip-pile")).toHaveLength(0);
    // Closed, not filtered, the same reason the second test in this file
    // insists on it: a name spoken through `aria-label` alone would pass
    // every other assertion here undetected.
    expect(spoken(container)).toEqual([]);

    act(() => {
      store.apply({ type: "Snapshot", view: aView() });
    });

    expect(container.querySelectorAll(".chip-pile").length).toBeGreaterThan(0);
    // The surface-scoped closure `STORY-1304` recorded as the right shape: a
    // statement about this surface's own silence, not the file's general
    // `spoken()`, so it cannot redden the day another surface earns a label
    // of its own, and it does redden the day a pile starts speaking one.
    expect(
      container.querySelectorAll(
        ".chip-pile [aria-label], .chip-pile[aria-label], .chip-pile [title], .chip-pile[title]",
      ),
    ).toHaveLength(0);
  });

  it("offers no typed total before the server has named a turn", () => {
    // Every other surface this file covers appears the moment a view
    // arrives; this one does not, so the walk needs a third state past the
    // Snapshot the earlier tests stop at, or the middle assertion below
    // would be checking the wrong boundary.
    const { container, store } = renderNullView("ABCDEFGH");

    expect(screen.queryByLabelText("the total")).toBeNull();
    // Closed, not filtered, the same reason the second test in this file
    // insists on it: a name spoken through `aria-label` alone would pass
    // every other assertion here undetected.
    expect(spoken(container)).toEqual([]);

    act(() => {
      store.apply({ type: "Snapshot", view: aView() });
    });
    // The bar is on screen now (the third test above proves
    // `queryByLabelText("your move")` non-null here) and the field still is
    // not: a Snapshot alone is not enough.
    expect(screen.queryByLabelText("the total")).toBeNull();

    act(() => {
      store.apply({
        type: "YourTurn",
        handNumber: 1,
        actionSequence: 1,
        legalActions: aLegalActions(),
      });
    });
    // The guard on the guard: without this, the two refusals above would be
    // assertions about a probe that never matches anything in this app.
    expect(screen.queryByLabelText("the total")).not.toBeNull();
  });

  it("draws no countdown and no bank before the server has named a turn", () => {
    const { container, store } = renderNullView("ABCDEFGH");

    expect(screen.queryByText(/Timebank/)).toBeNull();
    // `text-warn` is `SeatPlate`'s own colour for a countdown running out
    // (`turn-clock.ts`'s `CLOCK_COLOUR`), and nothing else in this app wears
    // it — the guard on the guard below is what proves that.
    expect(container.querySelectorAll(".text-warn")).toHaveLength(0);
    expect(spoken(container)).toEqual([]);

    act(() => {
      store.apply({ type: "Snapshot", view: aView() });
      store.apply({
        type: "TurnClock",
        seat: 0,
        handNumber: 1,
        actionSequence: 1,
        turnRemainingMillis: 5_000,
        bankRemainingMillis: [60_000, 60_000],
      });
    });

    // `queryAllByText`, not `queryByText`: both plates show a bank once the
    // table is live, and a single-element query throws on the second match
    // rather than confirming it.
    expect(screen.queryAllByText(/Timebank/).length).toBeGreaterThan(0);
    expect(container.querySelectorAll(".text-warn").length).toBeGreaterThan(0);
  });
});
