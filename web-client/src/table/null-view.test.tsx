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
});
