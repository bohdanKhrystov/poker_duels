import { describe, it, expect, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { HistoryScreen } from "./HistoryScreen";
import { aDuelLine } from "../profile/profile-fixture";
import type { DuelPageRead } from "../profile/duel-page";
import type { HistoryQuery } from "../profile/duels-query";
import {
  HISTORY_HEADING,
  LOADING_RECORD,
  NO_DUELS,
  NO_MATCH,
  READ_FAILED,
} from "./history-text";
import { finishedAtText } from "../profile/profile-text";

describe("the history screen", () => {
  it("renders the page it was handed, in the order it was handed", async () => {
    const rows = [
      aDuelLine({
        duelId: "xyz-1",
        opponentDisplayName: "Ada",
        outcome: "WON",
        coinDelta: 1,
        handsPlayed: 10,
        finishedAt: "2026-02-03T04:05:06Z",
      }),
      aDuelLine({
        duelId: "abc-2",
        opponentDisplayName: "Bob",
        outcome: "LOST",
        coinDelta: -2,
        handsPlayed: 23,
        finishedAt: "2026-03-04T05:06:07Z",
      }),
      aDuelLine({
        duelId: "mno-3",
        opponentDisplayName: "Charlie",
        outcome: "DREW",
        coinDelta: 0,
        handsPlayed: 5,
        finishedAt: "2026-01-02T03:04:05Z",
      }),
    ];

    const read = vi.fn<[HistoryQuery], Promise<DuelPageRead>>(
      async () =>
        ({
          kind: "page",
          duels: rows,
          nextCursor: null,
          restarted: false,
        }) as DuelPageRead,
    );

    render(<HistoryScreen read={read} />);

    const listItems = await waitFor(() => {
      const items = screen.getAllByRole("listitem");
      expect(items).toHaveLength(3);
      return items;
    });

    expect(listItems[0].textContent).toContain("Won");
    expect(listItems[0].textContent).toContain("+1");
    expect(listItems[0].textContent).toContain("10 hands");
    expect(listItems[0].textContent).toContain("Ada");
    expect(listItems[0].textContent).toContain(
      finishedAtText("2026-02-03T04:05:06Z"),
    );

    expect(listItems[1].textContent).toContain("Lost");
    expect(listItems[1].textContent).toContain("−2");
    expect(listItems[1].textContent).toContain("23 hands");
    expect(listItems[1].textContent).toContain("Bob");
    expect(listItems[1].textContent).toContain(
      finishedAtText("2026-03-04T05:06:07Z"),
    );

    expect(listItems[2].textContent).toContain("Drew");
    expect(listItems[2].textContent).toContain("0");
    expect(listItems[2].textContent).toContain("5 hands");
    expect(listItems[2].textContent).toContain("Charlie");
    expect(listItems[2].textContent).toContain(
      finishedAtText("2026-01-02T03:04:05Z"),
    );

    expect(screen.getByRole("region", { name: "your duels" })).toBeDefined();
    expect(screen.getByText(HISTORY_HEADING)).toBeDefined();
  });

  it("prints each row from the server, taking the outcome from the outcome and not the coin", async () => {
    const duel = aDuelLine({
      outcome: "WON",
      coinDelta: -1,
    });

    const read = vi.fn<[HistoryQuery], Promise<DuelPageRead>>(
      async () =>
        ({
          kind: "page",
          duels: [duel],
          nextCursor: null,
          restarted: false,
        }) as DuelPageRead,
    );

    render(<HistoryScreen read={read} />);

    const listItem = await waitFor(() => {
      const items = screen.getAllByRole("listitem");
      expect(items).toHaveLength(1);
      return items[0];
    });
    const text = listItem.textContent;

    expect(text).toContain("Won");
    expect(text).toContain("−1");
    expect(text).toContain("23 hands");
    expect(text).toContain(finishedAtText(duel.finishedAt));
  });

  it("names an opponent who has a name, and prints No name for one who has not", async () => {
    const rows = [
      aDuelLine({
        duelId: "duel-1",
        opponentDisplayName: "Ada",
      }),
      aDuelLine({
        duelId: "duel-2",
        opponentDisplayName: null,
      }),
    ];

    const read = vi.fn<[HistoryQuery], Promise<DuelPageRead>>(
      async () =>
        ({
          kind: "page",
          duels: rows,
          nextCursor: null,
          restarted: false,
        }) as DuelPageRead,
    );

    const { container } = render(<HistoryScreen read={read} />);

    const listItems = await waitFor(() => {
      const items = screen.getAllByRole("listitem");
      expect(items).toHaveLength(2);
      return items;
    });

    expect(listItems[0].textContent).toContain("Ada");
    expect(listItems[1].textContent).toContain("No name");
    expect(container.innerHTML).not.toContain("player-fixture");
  });

  it("asks for the first page once, with no cursor and no filter", async () => {
    const read = vi.fn<[HistoryQuery], Promise<DuelPageRead>>(
      async () =>
        ({
          kind: "page",
          duels: [aDuelLine()],
          nextCursor: null,
          restarted: false,
        }) as DuelPageRead,
    );

    render(<HistoryScreen read={read} />);

    await waitFor(() => {
      expect(screen.getAllByRole("listitem")).toHaveLength(1);
    });

    expect(read).toHaveBeenCalledTimes(1);
    expect(read).toHaveBeenCalledWith({
      outcome: null,
      opponent: "",
      after: null,
    });
  });

  it("says it is loading before the first page lands, and says nothing else", async () => {
    // A promise that never resolves, to keep the screen in loading state
    const neverResolves = new Promise<DuelPageRead>(() => {});

    const read = vi.fn<[HistoryQuery], Promise<DuelPageRead>>(
      async () => neverResolves,
    );

    render(<HistoryScreen read={read} />);

    // Wait for the loading sentence to appear
    await waitFor(() => {
      expect(screen.getByText(LOADING_RECORD)).toBeDefined();
    });

    // Verify that no list is shown
    const listItems = screen.queryAllByRole("listitem");
    expect(listItems).toHaveLength(0);

    // Verify that the other empty sentences are NOT shown
    expect(screen.queryByText(NO_DUELS)).toBeNull();
    expect(screen.queryByText(NO_MATCH)).toBeNull();
    expect(screen.queryByText(READ_FAILED)).toBeNull();
  });

  it("tells an empty record from a filter that matched nothing", async () => {
    // First render: empty page with no filter (should show NO_DUELS)
    const read = vi.fn<[HistoryQuery], Promise<DuelPageRead>>(
      async () =>
        ({
          kind: "page",
          duels: [],
          nextCursor: null,
          restarted: false,
        }) as DuelPageRead,
    );

    const { unmount } = render(<HistoryScreen read={read} />);

    // Wait for NO_DUELS to appear
    await waitFor(() => {
      expect(screen.getByText(NO_DUELS)).toBeDefined();
    });

    // Verify NO_MATCH is NOT shown
    expect(screen.queryByText(NO_MATCH)).toBeNull();

    // Clean up before second render
    unmount();

    // Second render: empty page with filter (should show NO_MATCH)
    const read2 = vi.fn<[HistoryQuery], Promise<DuelPageRead>>(
      async () =>
        ({
          kind: "page",
          duels: [],
          nextCursor: null,
          restarted: false,
        }) as DuelPageRead,
    );

    render(
      <HistoryScreen read={read2} filter={{ outcome: "WON", opponent: "" }} />,
    );

    // Wait for NO_MATCH to appear
    await waitFor(() => {
      expect(screen.getByText(NO_MATCH)).toBeDefined();
    });

    // Verify NO_DUELS is NOT shown
    expect(screen.queryByText(NO_DUELS)).toBeNull();
  });

  it("says the read failed, and keeps the pages already read", async () => {
    // Scenario 1: A first read that answers `unavailable` shows `READ_FAILED` and no list
    const read1 = vi.fn<[HistoryQuery], Promise<DuelPageRead>>(
      async () =>
        ({
          kind: "unavailable",
        }) as unknown as DuelPageRead,
    );

    render(<HistoryScreen read={read1} />);

    await waitFor(() => {
      expect(screen.getByText(READ_FAILED)).toBeDefined();
    });

    let listItems = screen.queryAllByRole("listitem");
    expect(listItems).toHaveLength(0);

    // Scenario 2: A first read that answers two rows, then a second read that
    // answers `unavailable`, shows `READ_FAILED` and keeps both rows on screen.
    // This simulates what happens when a "show more" request fails.
    const twoRows = [
      aDuelLine({
        duelId: "duel-1",
        opponentDisplayName: "Alice",
      }),
      aDuelLine({
        duelId: "duel-2",
        opponentDisplayName: "Bob",
      }),
    ];

    let resolveFirstRead: ((value: DuelPageRead) => void) | null = null;
    const firstReadPromise = new Promise<DuelPageRead>((resolve) => {
      resolveFirstRead = resolve;
    });

    const read2 = vi.fn<[HistoryQuery], Promise<DuelPageRead>>(
      async () => firstReadPromise,
    );

    const { rerender } = render(<HistoryScreen read={read2} />);

    // First read is pending, component shows loading. Resolve it with rows.
    if (resolveFirstRead) {
      (resolveFirstRead as (value: DuelPageRead) => void)({
        kind: "page",
        duels: twoRows,
        nextCursor: "cursor-123",
        restarted: false,
      });
    }

    // Wait for rows to appear
    await waitFor(() => {
      const items = screen.getAllByRole("listitem");
      expect(items).toHaveLength(2);
    });

    // Now simulate a second read (show more) failing by re-rendering with a
    // read that returns unavailable. This triggers the effect again with the
    // new read function.
    const read3 = vi.fn<[HistoryQuery], Promise<DuelPageRead>>(
      async () =>
        ({
          kind: "unavailable",
        }) as unknown as DuelPageRead,
    );

    rerender(<HistoryScreen read={read3} />);

    // Wait for the failure sentence to appear
    await waitFor(() => {
      expect(screen.getByText(READ_FAILED)).toBeDefined();
    });

    // Verify both rows are still displayed (not cleared by the failure)
    listItems = screen.queryAllByRole("listitem");
    expect(listItems).toHaveLength(2);
  });
});
