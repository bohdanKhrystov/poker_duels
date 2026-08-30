import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { LadderPage, LadderRow } from "./ladder-page";
import type { LadderRead } from "./ladder-read";
import { LadderScreen } from "./LadderScreen";

/**
 * Builds a page carrying exactly the given rows. `nextCursor` is always
 * `null` here — the *Show more* control does not exist until `TASK-050312`,
 * so a fixture naming a next page would sprout a button that ticket has not
 * shipped yet.
 */
function buildPage(rows: readonly LadderRow[]): LadderPage {
  return {
    season: "2026-08",
    rows,
    nextCursor: null,
    self: null,
  };
}

/**
 * Builds a two-call `read`: the first call (cursor `null`) answers page
 * one; every later call answers page two, but only once the test itself
 * calls the returned `resolvePageTwo` — held open so a second press made
 * before page two answers can be observed rather than raced.
 *
 * Page one's ranks are `[1, 1, 1, 5]` and page two's are `[5, 5, 9, 9]` —
 * neither reads `1..n`, and the boundary rank `5` is deliberately printed
 * by both pages, the way a real tie spanning a page break reads
 * (`ADR-0064` §2). The two `self` standings differ (rank 5 vs. rank 9) so a
 * test reading the self line after the second page can tell the walk held
 * page one's standing from a port that merely happened to leave a
 * same-valued line alone.
 */
function buildTwoPageRead() {
  const pageOne: LadderPage = {
    season: "2026-08",
    rows: [
      { rank: 1, playerId: "p1", displayName: "P1", coins: 1 },
      { rank: 1, playerId: "p2", displayName: "P2", coins: 1 },
      { rank: 1, playerId: "p3", displayName: "P3", coins: 1 },
      { rank: 5, playerId: "p4", displayName: "P4", coins: 5 },
    ],
    nextCursor: "c1",
    self: { rank: 5, coins: 1 },
  };
  const pageTwo: LadderPage = {
    season: "2026-08",
    rows: [
      { rank: 5, playerId: "p5", displayName: "P5", coins: 5 },
      { rank: 5, playerId: "p6", displayName: "P6", coins: 5 },
      { rank: 9, playerId: "p7", displayName: "P7", coins: 9 },
      { rank: 9, playerId: "p8", displayName: "P8", coins: 9 },
    ],
    nextCursor: null,
    self: { rank: 9, coins: -2 },
  };

  let deliverPageTwo: (answer: LadderRead) => void = () => {};
  const pageTwoAnswer = new Promise<LadderRead>((resolve) => {
    deliverPageTwo = resolve;
  });

  const read = vi.fn((after: string | null): Promise<LadderRead> => {
    if (after === null) {
      return Promise.resolve({ kind: "page", page: pageOne });
    }
    return pageTwoAnswer;
  });

  return {
    read,
    resolvePageTwo: () => deliverPageTwo({ kind: "page", page: pageTwo }),
  };
}

/**
 * Builds a page whose four rows carry the given ranks and coins, under the
 * names ["Ada", "Bo", null, "Cy"], with `season: "2026-09"` and a self line
 * of rank 5 on 1 coin. TASK-050313's flat and spread fixtures share every
 * field but these two arrays, so a difference between their renders can only
 * be the numbers a reader compares — never the row count, the season, or
 * whether a self line is present at all.
 */
function buildRefusalPage(
  ranks: readonly number[],
  coins: readonly number[],
): LadderPage {
  const names: readonly (string | null)[] = ["Ada", "Bo", null, "Cy"];
  return {
    season: "2026-09",
    rows: names.map((name, index) => ({
      rank: ranks[index],
      playerId: name ?? `no-name-${index}`,
      displayName: name,
      coins: coins[index],
    })),
    nextCursor: null,
    self: { rank: 5, coins: 1 },
  };
}

describe("the ladder screen", () => {
  it("asks for the first page once, with no cursor", async () => {
    const rows: readonly LadderRow[] = [
      { rank: 3, playerId: "ada", displayName: "Ada", coins: 3 },
    ];
    const read = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: buildPage(rows),
    }));

    render(<LadderScreen read={read} />);

    await waitFor(() => {
      expect(
        within(screen.getByRole("list")).getAllByRole("listitem"),
      ).toHaveLength(1);
    });

    expect(read).toHaveBeenCalledTimes(1);
    expect(read).toHaveBeenCalledWith(null);
  });

  it("prints the rows in the order the server sent them, and not in coin order", async () => {
    // Coins are deliberately not descending — no real ladder page is, and a
    // client that sorts by coins before rendering would put row three first.
    const rows: readonly LadderRow[] = [
      { rank: 3, playerId: "ada", displayName: "Ada", coins: 3 },
      { rank: 3, playerId: "no-name", displayName: null, coins: 1 },
      { rank: 5, playerId: "bo", displayName: "Bo", coins: 5 },
      { rank: 9, playerId: "cy", displayName: "Cy", coins: 0 },
    ];
    const read = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: buildPage(rows),
    }));

    render(<LadderScreen read={read} />);

    const items = await waitFor(() => {
      const found = within(screen.getByRole("list")).getAllByRole("listitem");
      expect(found).toHaveLength(4);
      return found;
    });

    expect(items.map((item) => item.textContent)).toEqual([
      "3 Ada 3",
      "3 No name 1",
      "5 Bo 5",
      "9 Cy 0",
    ]);
  });

  it("prints the rank the response carried, on a page that does not start at one", async () => {
    // Ranks repeat (3, 3) and skip (5 to 9) the way a real season's do — a
    // client that renumbers by position, or folds the repeated 3 into one
    // row, has nowhere to hide against this fixture.
    const rows: readonly LadderRow[] = [
      { rank: 3, playerId: "ada", displayName: "Ada", coins: 3 },
      { rank: 3, playerId: "no-name", displayName: null, coins: 1 },
      { rank: 5, playerId: "bo", displayName: "Bo", coins: 5 },
      { rank: 9, playerId: "cy", displayName: "Cy", coins: 0 },
    ];
    const read = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: buildPage(rows),
    }));

    render(<LadderScreen read={read} />);

    const items = await waitFor(() => {
      const found = within(screen.getByRole("list")).getAllByRole("listitem");
      expect(found).toHaveLength(4);
      return found;
    });

    expect(items.map((item) => item.textContent)).toEqual([
      "3 Ada 3",
      "3 No name 1",
      "5 Bo 5",
      "9 Cy 0",
    ]);
  });

  it("renders every row it was sent, including the two a client might drop", async () => {
    // This fixture holds a row with no name, a row with a negative standing,
    // and a row with both — the three a client tempted to filter would skip.
    // The list must hold all five, in the wire order, or filtering has crept in.
    const rows: readonly LadderRow[] = [
      { rank: 1, playerId: "ada", displayName: "Ada", coins: 5 },
      { rank: 1, playerId: "no-name-1", displayName: null, coins: 1 },
      { rank: 3, playerId: "bo", displayName: "Bo", coins: -2 },
      { rank: 4, playerId: "no-name-2", displayName: null, coins: -2 },
      { rank: 4, playerId: "cy", displayName: "Cy", coins: 0 },
    ];
    const read = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: buildPage(rows),
    }));

    render(<LadderScreen read={read} />);

    const items = await waitFor(() => {
      const found = within(screen.getByRole("list")).getAllByRole("listitem");
      expect(found).toHaveLength(5);
      return found;
    });

    expect(items.map((item) => item.textContent)).toEqual([
      "1 Ada 5",
      "1 No name 1",
      "3 Bo −2",
      "4 No name −2",
      "4 Cy 0",
    ]);
  });

  it("prints a negative standing with its sign, in the position the server gave it", async () => {
    // A player below zero has a standing of −2 (U+2212), not absolute value 2.
    // The row holding both a null name and negative coins reads "4 No name −2"
    // and sits in the fourth <li>, and a named row with the same coins shows
    // its own text (not clamped or adjusted).
    const rows: readonly LadderRow[] = [
      { rank: 1, playerId: "ada", displayName: "Ada", coins: 5 },
      { rank: 1, playerId: "no-name-1", displayName: null, coins: 1 },
      { rank: 3, playerId: "bo", displayName: "Bo", coins: -2 },
      { rank: 4, playerId: "no-name-2", displayName: null, coins: -2 },
      { rank: 4, playerId: "cy", displayName: "Cy", coins: 0 },
    ];
    const read = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: buildPage(rows),
    }));

    render(<LadderScreen read={read} />);

    const items = await waitFor(() => {
      const found = within(screen.getByRole("list")).getAllByRole("listitem");
      expect(found).toHaveLength(5);
      return found;
    });

    // The fourth <li> carries both null displayName and negative coins.
    expect(items[3].textContent).toBe("4 No name −2");

    // The third <li> is a named player with the same negative coins.
    expect(items[2].textContent).toBe("3 Bo −2");
  });

  it("names the season the response carried, and a different one for a different response", async () => {
    // First render: 2026-08 should display as "August 2026"
    const rows1: readonly LadderRow[] = [
      { rank: 1, playerId: "ada", displayName: "Ada", coins: 5 },
    ];
    const read1 = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: {
        season: "2026-08",
        rows: rows1,
        nextCursor: null,
        self: null,
      },
    }));

    const { unmount } = render(<LadderScreen read={read1} />);

    await waitFor(() => {
      expect(screen.getByText("August 2026")).toBeTruthy();
    });
    expect(screen.queryByText("2026-08")).toBeNull();

    unmount();

    // Second render: 2019-02 should display as "February 2019"
    const rows2: readonly LadderRow[] = [
      { rank: 1, playerId: "bo", displayName: "Bo", coins: 3 },
    ];
    const read2 = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: {
        season: "2019-02",
        rows: rows2,
        nextCursor: null,
        self: null,
      },
    }));

    render(<LadderScreen read={read2} />);

    await waitFor(() => {
      expect(screen.getByText("February 2019")).toBeTruthy();
    });
    expect(screen.queryByText("2019-02")).toBeNull();
  });

  it("renders an empty ladder as an empty ladder that still names its season", async () => {
    // An empty ladder with season should show the season name and EMPTY_LADDER sentence
    const read = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: {
        season: "2026-09",
        rows: [],
        nextCursor: null,
        self: null,
      },
    }));

    render(<LadderScreen read={read} />);

    await waitFor(() => {
      expect(screen.getByText("September 2026")).toBeTruthy();
    });

    // Verify the heading is present
    expect(screen.getByRole("heading")).toBeTruthy();

    // Verify the list is empty
    const list = screen.getByRole("list");
    expect(within(list).queryAllByRole("listitem")).toHaveLength(0);

    // Verify EMPTY_LADDER is shown
    expect(
      screen.getByText("No duels have finished this season yet."),
    ).toBeTruthy();

    // Verify LADDER_FAILED is not shown
    expect(
      screen.queryByText(
        "The leaderboard did not load. Reload the page to try again.",
      ),
    ).toBeNull();
  });

  it("says the ladder is loading before the first page answers", async () => {
    // A pending read should show LOADING_LADDER
    const read = vi.fn(
      () =>
        new Promise<LadderRead>(() => {
          /* never resolves */
        }),
    );

    render(<LadderScreen read={read} />);

    // LOADING_LADDER should appear
    await waitFor(() => {
      expect(screen.getByText("Loading the leaderboard…")).toBeTruthy();
    });

    // EMPTY_LADDER should not appear
    expect(
      screen.queryByText("No duels have finished this season yet."),
    ).toBeNull();

    // No season should be named yet
    expect(screen.queryByText(/2026-08|August 2026/)).toBeNull();
  });

  it("tells a read that failed from a ladder that is empty", async () => {
    // A failed read should show LADDER_FAILED
    const read = vi.fn(async (): Promise<LadderRead> => ({
      kind: "unavailable",
    }));

    render(<LadderScreen read={read} />);

    await waitFor(() => {
      expect(
        screen.getByText(
          "The leaderboard did not load. Reload the page to try again.",
        ),
      ).toBeTruthy();
    });

    // EMPTY_LADDER should not appear
    expect(
      screen.queryByText("No duels have finished this season yet."),
    ).toBeNull();
  });

  it("states a standing for a player whose row is on no page drawn", async () => {
    // The rows carry ranks [1, 1, 3, 4] and coins [2, 2, 1, 0]. Neither
    // fixture's self rank — 215, then 7 — nor its self coins — −1, then 4 —
    // is among them, so a line that echoed a row on screen could never
    // produce either sentence below.
    const rows: readonly LadderRow[] = [
      { rank: 1, playerId: "ada", displayName: "Ada", coins: 2 },
      { rank: 1, playerId: "bo", displayName: "Bo", coins: 2 },
      { rank: 3, playerId: "cy", displayName: "Cy", coins: 1 },
      { rank: 4, playerId: "dee", displayName: "Dee", coins: 0 },
    ];

    const readFirst = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: {
        season: "2026-08",
        rows,
        nextCursor: null,
        self: { rank: 215, coins: -1 },
      },
    }));

    const { unmount } = render(<LadderScreen read={readFirst} />);

    const firstItems = await waitFor(() => {
      const found = within(screen.getByRole("list")).getAllByRole("listitem");
      expect(found).toHaveLength(4);
      return found;
    });

    expect(
      screen.getByText("You are rank 215 this season, on −1 duel coins."),
    ).toBeTruthy();
    for (const item of firstItems) {
      expect(item.textContent).not.toContain("215");
    }

    unmount();

    const readSecond = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: {
        season: "2026-08",
        rows,
        nextCursor: null,
        self: { rank: 7, coins: 4 },
      },
    }));

    render(<LadderScreen read={readSecond} />);

    const secondItems = await waitFor(() => {
      const found = within(screen.getByRole("list")).getAllByRole("listitem");
      expect(found).toHaveLength(4);
      return found;
    });

    expect(
      screen.getByText("You are rank 7 this season, on 4 duel coins."),
    ).toBeTruthy();
    for (const item of secondItems) {
      expect(item.textContent).not.toContain("7");
    }
  });

  it("puts the self line under the season name and over the first row", async () => {
    const rows: readonly LadderRow[] = [
      { rank: 1, playerId: "ada", displayName: "Ada", coins: 2 },
      { rank: 1, playerId: "bo", displayName: "Bo", coins: 2 },
      { rank: 3, playerId: "cy", displayName: "Cy", coins: 1 },
      { rank: 4, playerId: "dee", displayName: "Dee", coins: 0 },
    ];

    const read = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: {
        season: "2026-08",
        rows,
        nextCursor: null,
        self: { rank: 215, coins: -1 },
      },
    }));

    const { container } = render(<LadderScreen read={read} />);

    await waitFor(() => {
      expect(
        within(screen.getByRole("list")).getAllByRole("listitem"),
      ).toHaveLength(4);
    });

    const text = container.textContent ?? "";
    const seasonIndex = text.indexOf("August 2026");
    const selfIndex = text.indexOf(
      "You are rank 215 this season, on −1 duel coins.",
    );
    const firstRowIndex = text.indexOf("1 Ada 2");

    expect(seasonIndex).toBeGreaterThanOrEqual(0);
    expect(selfIndex).toBeGreaterThan(seasonIndex);
    expect(firstRowIndex).toBeGreaterThan(selfIndex);
  });

  it("renders no self line for a response that carried none, and the ladder renders anyway", async () => {
    const rows: readonly LadderRow[] = [
      { rank: 1, playerId: "ada", displayName: "Ada", coins: 2 },
      { rank: 1, playerId: "bo", displayName: "Bo", coins: 2 },
      { rank: 3, playerId: "cy", displayName: "Cy", coins: 1 },
      { rank: 4, playerId: "dee", displayName: "Dee", coins: 0 },
    ];

    // First render: the response carried no self field at all — the
    // ordinary state of a first visit, not an error and not a spinner.
    const readWithNoSelf = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: {
        season: "2026-08",
        rows,
        nextCursor: null,
        self: null,
      },
    }));

    const { unmount } = render(<LadderScreen read={readWithNoSelf} />);

    await waitFor(() => {
      expect(
        within(screen.getByRole("list")).getAllByRole("listitem"),
      ).toHaveLength(4);
    });

    expect(screen.getByRole("heading")).toBeTruthy();
    expect(screen.getByText("August 2026")).toBeTruthy();
    expect(
      screen.queryByText("You have no place on this season's leaderboard."),
    ).toBeNull();
    expect(screen.queryByText(/^You are rank/)).toBeNull();
    expect(
      within(screen.getByRole("list"))
        .getAllByRole("listitem")
        .map((item) => item.textContent),
    ).toEqual(["1 Ada 2", "1 Bo 2", "3 Cy 1", "4 Dee 0"]);

    unmount();

    // Second render: the response named a self field of two nulls — a
    // standing that holds no place, not an absent standing.
    const readWithNoPlace = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: {
        season: "2026-08",
        rows,
        nextCursor: null,
        self: { rank: null, coins: null },
      },
    }));

    render(<LadderScreen read={readWithNoPlace} />);

    await waitFor(() => {
      expect(
        screen.getByText("You have no place on this season's leaderboard."),
      ).toBeTruthy();
    });

    expect(
      within(screen.getByRole("list"))
        .getAllByRole("listitem")
        .map((item) => item.textContent),
    ).toEqual(["1 Ada 2", "1 Bo 2", "3 Cy 1", "4 Dee 0"]);
  });

  it("appends the next page under the first, and repeats the rank the boundary repeated", async () => {
    const { read, resolvePageTwo } = buildTwoPageRead();

    render(<LadderScreen read={read} />);

    await waitFor(() => {
      expect(
        within(screen.getByRole("list")).getAllByRole("listitem"),
      ).toHaveLength(4);
    });

    const moreButton = screen.getByRole("button", { name: "Show more" });
    fireEvent.click(moreButton);

    // A second press while the first is still outstanding must not start a
    // second read: TASK-050305 demonstrated, with real output, that a late
    // answer to a superseded request is misclassified as a first page and
    // the held rows vanish. Only a request count can show the press was
    // refused — the visible outcome is identical either way.
    fireEvent.click(moreButton);
    expect(read).toHaveBeenCalledTimes(2);
    expect(read).toHaveBeenNthCalledWith(2, "c1");

    resolvePageTwo();

    const items = await waitFor(() => {
      const found = within(screen.getByRole("list")).getAllByRole("listitem");
      expect(found).toHaveLength(8);
      return found;
    });

    expect(items.map((item) => item.textContent)).toEqual([
      "1 P1 1",
      "1 P2 1",
      "1 P3 1",
      "5 P4 5",
      "5 P5 5",
      "5 P6 5",
      "9 P7 9",
      "9 P8 9",
    ]);

    // The double press cost nothing: still exactly the first call and the
    // one call for "c1", never a third.
    expect(read).toHaveBeenCalledTimes(2);
  });

  it("stops offering another page at the end of the ladder", async () => {
    const { read, resolvePageTwo } = buildTwoPageRead();

    render(<LadderScreen read={read} />);

    await waitFor(() => {
      expect(
        within(screen.getByRole("list")).getAllByRole("listitem"),
      ).toHaveLength(4);
    });

    // Before the click: page one named a next cursor, so the walk offers
    // another page.
    expect(screen.getByRole("button", { name: "Show more" })).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "Show more" }));
    resolvePageTwo();

    await waitFor(() => {
      expect(
        within(screen.getByRole("list")).getAllByRole("listitem"),
      ).toHaveLength(8);
    });

    // After the click: page two named no next cursor, so there is nowhere
    // left for the walk to go.
    expect(screen.queryByRole("button", { name: "Show more" })).toBeNull();
    expect(read).toHaveBeenCalledTimes(2);
  });

  it("leaves the self line where it was when a second page arrives", async () => {
    const { read, resolvePageTwo } = buildTwoPageRead();

    render(<LadderScreen read={read} />);

    await waitFor(() => {
      expect(
        within(screen.getByRole("list")).getAllByRole("listitem"),
      ).toHaveLength(4);
    });

    expect(
      screen.getByText("You are rank 5 this season, on 1 duel coin."),
    ).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "Show more" }));
    // Page two's self standing is rank 9, not rank 5 — a different value
    // than page one's, so the assertion below can tell the walk held page
    // one's line from a port that merely happened to leave a same-valued
    // line alone.
    resolvePageTwo();

    await waitFor(() => {
      expect(
        within(screen.getByRole("list")).getAllByRole("listitem"),
      ).toHaveLength(8);
    });

    // The self line still reads page one's standing, never page two's.
    expect(
      screen.getByText("You are rank 5 this season, on 1 duel coin."),
    ).toBeTruthy();
    expect(screen.queryByText(/rank 9/)).toBeNull();
    expect(read).toHaveBeenCalledTimes(2);
  });

  it("renders a nearly flat ladder exactly as it renders a spread one", async () => {
    // A day-two season, every rank tied at 5, is the moment ADR-0064 §6
    // refuses to soften with a "the season is still young" affordance — so
    // its markup, stripped of words, must be indistinguishable from a
    // spread ladder's. A banner, a grouping wrapper or an extra class on a
    // tied row exists in one skeleton and not the other, and reddens here.
    const flatRead = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: buildRefusalPage([5, 5, 5, 5], [1, 1, 1, 1]),
    }));

    const { container: flatContainer, unmount } = render(
      <LadderScreen read={flatRead} />,
    );

    await waitFor(() => {
      expect(
        within(screen.getByRole("list")).getAllByRole("listitem"),
      ).toHaveLength(4);
    });

    const flatSection = flatContainer.querySelector("section");
    if (flatSection === null) {
      throw new Error("expected the ladder to render inside a <section>");
    }
    const flatSkeleton = flatSection.innerHTML.replace(/>[^<]*</g, "><");

    unmount();

    const spreadRead = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: buildRefusalPage([2, 3, 4, 6], [4, 3, 2, 1]),
    }));

    const { container: spreadContainer } = render(
      <LadderScreen read={spreadRead} />,
    );

    await waitFor(() => {
      expect(
        within(screen.getByRole("list")).getAllByRole("listitem"),
      ).toHaveLength(4);
    });

    const spreadSection = spreadContainer.querySelector("section");
    if (spreadSection === null) {
      throw new Error("expected the ladder to render inside a <section>");
    }
    const spreadSkeleton = spreadSection.innerHTML.replace(/>[^<]*</g, "><");

    expect(flatSkeleton).toBe(spreadSkeleton);
  });

  it("marks no row on a page of equal ranks, and none as the one the reader stands on", async () => {
    // Every row here carries the self line's own rank and standing.
    // ADR-0065 §5 refuses a marker, a jump control and a scroll-to-my-row;
    // §6 says the reader appearing twice (once in the self line, once in
    // its row) is correct as is. Nothing may set any one of the four rows
    // apart from the rest, and none may be dropped for repeating the self
    // line.
    const read = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: buildRefusalPage([5, 5, 5, 5], [1, 1, 1, 1]),
    }));

    render(<LadderScreen read={read} />);

    const items = await waitFor(() => {
      const found = within(screen.getByRole("list")).getAllByRole("listitem");
      expect(found).toHaveLength(4);
      return found;
    });

    const [firstClassName] = items.map((item) => item.className);
    for (const item of items) {
      expect(item.className).toBe(firstClassName);
    }

    expect(screen.getByRole("list").textContent).toBe(
      "5 Ada 15 Bo 15 No name 15 Cy 1",
    );
  });

  it("renders the heading, the season, the self line and the rows, and nothing else", async () => {
    // ADR-0065 §7 refuses a ladder total, a movement line, a streak and a
    // tie count. DEC-057 — whether a row ever leads anywhere — is open, so
    // today's answer is that a row is text: no link, no exposed button
    // beyond the *Show more* control, which stays mounted and `hidden`
    // (never conditionally unmounted, so a second press can land on a node
    // the guard can still refuse — see LadderScreen's own comment). `hidden`
    // drops a node from the accessibility tree, which is why the role
    // queries below still find zero buttons, but not from `textContent`,
    // which is why its trailing words are part of the section's exact text
    // rather than absent from it.
    const read = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: buildRefusalPage([5, 5, 5, 5], [1, 1, 1, 1]),
    }));

    const { container } = render(<LadderScreen read={read} />);

    await waitFor(() => {
      expect(
        within(screen.getByRole("list")).getAllByRole("listitem"),
      ).toHaveLength(4);
    });

    const section = container.querySelector("section");
    if (section === null) {
      throw new Error("expected the ladder to render inside a <section>");
    }

    expect(section.textContent).toBe(
      "Leaderboard" +
        "September 2026" +
        "You are rank 5 this season, on 1 duel coin." +
        "5 Ada 15 Bo 15 No name 15 Cy 1" +
        "Show more",
    );

    expect(within(section).queryAllByRole("link")).toHaveLength(0);
    expect(section.querySelectorAll("a")).toHaveLength(0);
    expect(within(section).queryAllByRole("button")).toHaveLength(0);
  });

  it("a ladder row states its rank, its name and its coins in three elements", async () => {
    // Rank and coins are deliberately the same integer — a row rendered as
    // one flat string and a row rendered as three elements read alike to a
    // test that only checks the text is present. Only distinct elements can
    // tell "5 P4 5" apart without counting words.
    const rows: readonly LadderRow[] = [
      { rank: 5, playerId: "p4", displayName: "P4", coins: 5 },
    ];
    const read = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: buildPage(rows),
    }));

    render(<LadderScreen read={read} />);

    const item = await waitFor(() => {
      const found = within(screen.getByRole("list")).getAllByRole("listitem");
      expect(found).toHaveLength(1);
      return found[0];
    });

    const cells = Array.from(item.children).map((child) => child.textContent);
    expect(cells).toEqual(["5", "P4", "5"]);
  });

  it("a row's rank and coins are mono figures, and the rank is the muted one", async () => {
    // One row with positive coins and one with negative — a treatment that
    // only reaches the positive case, or that mutes every span rather than
    // just the rank, has nowhere to hide against both rows checked below.
    const rows: readonly LadderRow[] = [
      { rank: 1, playerId: "ada", displayName: "Ada", coins: 5 },
      { rank: 3, playerId: "bo", displayName: "Bo", coins: -2 },
    ];
    const read = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: buildPage(rows),
    }));

    render(<LadderScreen read={read} />);

    const items = await waitFor(() => {
      const found = within(screen.getByRole("list")).getAllByRole("listitem");
      expect(found).toHaveLength(2);
      return found;
    });

    for (const item of items) {
      const [rank, , coins] = Array.from(item.children) as HTMLElement[];

      expect(rank.className).toContain("font-mono");
      expect(rank.className).toContain("tabular-nums");
      expect(rank.className).toContain("text-text-muted");

      expect(coins.className).toContain("font-mono");
      expect(coins.className).toContain("tabular-nums");
      expect(coins.className).not.toContain("text-text-muted");
    }
  });

  it("the self line is the highlighted box the card draws", async () => {
    // A border alone can sit flush with the body colour — the box the card
    // draws carries both the tinted fill and the accent border.
    const rows: readonly LadderRow[] = [
      { rank: 1, playerId: "ada", displayName: "Ada", coins: 5 },
    ];
    const read = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: {
        season: "2026-08",
        rows,
        nextCursor: null,
        self: { rank: 1, coins: 1 },
      },
    }));

    render(<LadderScreen read={read} />);

    const sentence = await waitFor(() =>
      screen.getByText("You are rank 1 this season, on 1 duel coin."),
    );
    const box = sentence.closest("p");
    if (box === null) {
      throw new Error("expected the self line to sit inside a <p>");
    }

    expect(box.className).toContain("bg-accent-subtle");
    expect(box.className).toContain("border-accent");
  });

  it("Show more is dressed, not bare", async () => {
    // nextCursor must be non-null here, or the button stays `hidden` and
    // out of the accessibility tree — the class list would never be reached.
    const read = vi.fn(async (): Promise<LadderRead> => ({
      kind: "page",
      page: {
        season: "2026-08",
        rows: [{ rank: 1, playerId: "ada", displayName: "Ada", coins: 5 }],
        nextCursor: "c1",
        self: null,
      },
    }));

    render(<LadderScreen read={read} />);

    const button = await waitFor(() =>
      screen.getByRole("button", { name: "Show more" }),
    );

    expect(button.className).toContain("bg-accent-fill");
    expect(button.className).toContain("text-on-accent");
  });
});
