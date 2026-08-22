import { render, screen, waitFor, within } from "@testing-library/react";
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
});
