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
});
