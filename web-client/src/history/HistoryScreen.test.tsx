import { describe, it, expect, vi } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { HistoryScreen } from "./HistoryScreen";
import { aDuelLine } from "../profile/profile-fixture";
import type { DuelPageRead } from "../profile/duel-page";
import type { HistoryQuery } from "../profile/duels-query";
import { duelsPath } from "../profile/duels-query";
import {
  HISTORY_HEADING,
  LOADING_RECORD,
  NO_DUELS,
  NO_MATCH,
  READ_FAILED,
  EVERY_OUTCOME,
  OPPONENT_LABEL,
  SEARCH,
  MORE,
} from "./history-text";
import { finishedAtText, outcomeWord } from "../profile/profile-text";

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

  // Unpadded base64url holding both "-" and "_", so a trim, a re-encode or a
  // reconstruction of the cursor shows up as a mismatch rather than passing by luck.
  const NEXT_CURSOR = "MjAy-Ni0w_Mw";

  it("offers another page while the server names one", async () => {
    const read = vi.fn<[HistoryQuery], Promise<DuelPageRead>>(
      async () =>
        ({
          kind: "page",
          duels: [aDuelLine()],
          nextCursor: NEXT_CURSOR,
          restarted: false,
        }) as DuelPageRead,
    );

    render(<HistoryScreen read={read} />);

    // No control before the first page has landed.
    expect(screen.queryByRole("button", { name: MORE })).toBeNull();

    await waitFor(() => {
      expect(screen.getAllByRole("listitem")).toHaveLength(1);
    });

    const buttons = screen.getAllByRole("button", { name: MORE });
    expect(buttons).toHaveLength(1);
    expect(buttons[0].textContent).toBe("Show more");
  });

  it("offers another page in the words the copy module holds", async () => {
    const read = vi.fn<[HistoryQuery], Promise<DuelPageRead>>(
      async () =>
        ({
          kind: "page",
          duels: [aDuelLine()],
          nextCursor: "cursor-123",
          restarted: false,
        }) as DuelPageRead,
    );

    render(<HistoryScreen read={read} />);

    await waitFor(() => {
      expect(screen.getAllByRole("listitem")).toHaveLength(1);
    });

    expect(screen.getByRole("button", { name: "Show more" })).toBeDefined();
  });

  it("stops offering on the last page, and asks for nothing more", async () => {
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

    expect(screen.queryByRole("button", { name: MORE })).toBeNull();
    expect(read).toHaveBeenCalledTimes(1);
  });

  it("asks with the cursor the server sent, byte for byte", async () => {
    const read = vi
      .fn<[HistoryQuery], Promise<DuelPageRead>>()
      .mockResolvedValueOnce({
        kind: "page",
        duels: [aDuelLine({ duelId: "duel-1" })],
        nextCursor: NEXT_CURSOR,
        restarted: false,
      } as DuelPageRead)
      .mockResolvedValueOnce({
        kind: "page",
        duels: [aDuelLine({ duelId: "duel-2" })],
        nextCursor: null,
        restarted: false,
      } as DuelPageRead);

    render(
      <HistoryScreen
        read={read}
        filter={{ outcome: "WON", opponent: "Halvard" }}
      />,
    );

    const button = await waitFor(() =>
      screen.getByRole("button", { name: MORE }),
    );
    fireEvent.click(button);

    // That click put a request for NEXT_CURSOR in flight. `nextCursor` does
    // not move until it answers, so a second click now would send the exact
    // query already outstanding — the loop TASK-041306 found. Firing it
    // anyway must not grow the request count.
    fireEvent.click(button);

    await waitFor(() => {
      expect(read).toHaveBeenCalledTimes(2);
    });

    const firstCall = read.mock.calls[0][0];
    const secondCall = read.mock.calls[1][0];
    expect(secondCall.after).toBe(NEXT_CURSOR);
    expect(secondCall.outcome).toBe(firstCall.outcome);
    expect(secondCall.opponent).toBe(firstCall.opponent);
  });

  it("appends the second page under the first, in the order both arrived", async () => {
    const firstPage = [
      aDuelLine({ duelId: "duel-1", opponentDisplayName: "Ada" }),
      aDuelLine({ duelId: "duel-2", opponentDisplayName: "Bob" }),
    ];
    const secondPage = [
      aDuelLine({ duelId: "duel-3", opponentDisplayName: "Charlie" }),
      aDuelLine({ duelId: "duel-4", opponentDisplayName: "Dana" }),
    ];
    const opponentOrder = ["Ada", "Bob", "Charlie", "Dana"];

    const read = vi
      .fn<[HistoryQuery], Promise<DuelPageRead>>()
      .mockResolvedValueOnce({
        kind: "page",
        duels: firstPage,
        nextCursor: NEXT_CURSOR,
        restarted: false,
      } as DuelPageRead)
      .mockResolvedValueOnce({
        kind: "page",
        duels: secondPage,
        nextCursor: null,
        restarted: false,
      } as DuelPageRead);

    render(<HistoryScreen read={read} />);

    const button = await waitFor(() =>
      screen.getByRole("button", { name: MORE }),
    );
    fireEvent.click(button);

    const listItems = await waitFor(() => {
      const items = screen.getAllByRole("listitem");
      expect(items).toHaveLength(4);
      return items;
    });

    const renderedOrder = listItems.map(
      (item) =>
        opponentOrder.find((name) => item.textContent?.includes(name)) ?? null,
    );
    expect(renderedOrder).toEqual(opponentOrder);
  });

  it("offers all and the three outcomes, in the words a row already uses", async () => {
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

    // Check that all four radios are present and labeled correctly
    const allRadio = screen.getByLabelText(EVERY_OUTCOME) as HTMLInputElement;
    expect(allRadio).toBeDefined();
    expect(allRadio.checked).toBe(true);

    const wonRadio = screen.getByLabelText(
      outcomeWord("WON"),
    ) as HTMLInputElement;
    expect(wonRadio).toBeDefined();
    expect(wonRadio.checked).toBe(false);

    const lostRadio = screen.getByLabelText(
      outcomeWord("LOST"),
    ) as HTMLInputElement;
    expect(lostRadio).toBeDefined();
    expect(lostRadio.checked).toBe(false);

    const drewRadio = screen.getByLabelText(
      outcomeWord("DREW"),
    ) as HTMLInputElement;
    expect(drewRadio).toBeDefined();
    expect(drewRadio.checked).toBe(false);
  });

  it("asks for the first page of the outcome chosen, dropping the cursor", async () => {
    const read = vi
      .fn<[HistoryQuery], Promise<DuelPageRead>>()
      .mockResolvedValueOnce({
        kind: "page",
        duels: [aDuelLine({ duelId: "duel-1" })],
        nextCursor: "cursor-123",
        restarted: false,
      } as DuelPageRead)
      .mockResolvedValueOnce({
        kind: "page",
        duels: [aDuelLine({ duelId: "duel-2" })],
        nextCursor: null,
        restarted: false,
      } as DuelPageRead);

    render(<HistoryScreen read={read} />);

    await waitFor(() => {
      expect(screen.getAllByRole("listitem")).toHaveLength(1);
    });

    const lostRadio = screen.getByLabelText(outcomeWord("LOST"));
    fireEvent.click(lostRadio);

    await waitFor(() => {
      expect(read).toHaveBeenCalledTimes(2);
    });

    const secondCall = read.mock.calls[1][0];
    expect(secondCall).toEqual({
      outcome: "LOST",
      opponent: "",
      after: null,
    });
  });

  it("replaces the rows of the filter just left", async () => {
    const read = vi
      .fn<[HistoryQuery], Promise<DuelPageRead>>()
      .mockResolvedValueOnce({
        kind: "page",
        duels: [
          aDuelLine({ duelId: "duel-1", opponentDisplayName: "Alice" }),
          aDuelLine({ duelId: "duel-2", opponentDisplayName: "Bob" }),
        ],
        nextCursor: null,
        restarted: false,
      } as DuelPageRead)
      .mockResolvedValueOnce({
        kind: "page",
        duels: [
          aDuelLine({ duelId: "duel-3", opponentDisplayName: "Charlie" }),
        ],
        nextCursor: null,
        restarted: false,
      } as DuelPageRead);

    render(<HistoryScreen read={read} />);

    let listItems = await waitFor(() => {
      const items = screen.getAllByRole("listitem");
      expect(items).toHaveLength(2);
      return items;
    });

    expect(listItems[0].textContent).toContain("Alice");
    expect(listItems[1].textContent).toContain("Bob");

    const wonRadio = screen.getByLabelText(outcomeWord("WON"));
    fireEvent.click(wonRadio);

    listItems = await waitFor(() => {
      const items = screen.getAllByRole("listitem");
      expect(items).toHaveLength(1);
      return items;
    });

    expect(listItems[0].textContent).toContain("Charlie");
  });

  it("does not keep rows from the old filter when changing outcome, even with a cursor", async () => {
    // FALSIFICATION (a): This test fails if "filtered" is not dispatched before ask(),
    // because rows from the old filter would be appended to rows from the new filter.
    // The first read returns WON outcomes with a cursor, then clicking LOST should
    // not show the WON rows mixed with LOST rows.
    const read = vi
      .fn<[HistoryQuery], Promise<DuelPageRead>>()
      .mockResolvedValueOnce({
        kind: "page",
        duels: [
          aDuelLine({
            duelId: "duel-1",
            opponentDisplayName: "Alice",
            outcome: "WON",
          }),
          aDuelLine({
            duelId: "duel-2",
            opponentDisplayName: "Bob",
            outcome: "WON",
          }),
        ],
        nextCursor: "cursor-with-won",
        restarted: false,
      } as DuelPageRead)
      .mockResolvedValueOnce({
        kind: "page",
        duels: [
          aDuelLine({
            duelId: "duel-3",
            opponentDisplayName: "Charlie",
            outcome: "LOST",
          }),
        ],
        nextCursor: null,
        restarted: false,
      } as DuelPageRead);

    render(<HistoryScreen read={read} />);

    let listItems = await waitFor(() => {
      const items = screen.getAllByRole("listitem");
      expect(items).toHaveLength(2);
      return items;
    });

    // Verify initial rows are WON outcomes
    expect(listItems[0].textContent).toContain("Won");
    expect(listItems[0].textContent).toContain("Alice");
    expect(listItems[1].textContent).toContain("Won");
    expect(listItems[1].textContent).toContain("Bob");

    // Click to filter by LOST
    const lostRadio = screen.getByLabelText(outcomeWord("LOST"));
    fireEvent.click(lostRadio);

    // After clicking, should have only the LOST row (Charlie), not the WON rows appended
    listItems = await waitFor(() => {
      const items = screen.getAllByRole("listitem");
      expect(items).toHaveLength(1);
      return items;
    });

    expect(listItems[0].textContent).toContain("Lost");
    expect(listItems[0].textContent).toContain("Charlie");
    // Verify Alice and Bob (WON rows) are NOT present
    expect(screen.queryByText("Alice")).toBeNull();
    expect(screen.queryByText("Bob")).toBeNull();
  });

  it("drops the cursor from the old filter when changing outcome", async () => {
    // FALSIFICATION (d): This test fails if ask() is called before dispatching "filtered",
    // because the stale cursor would be sent in the request. The request should have
    // after: null, not the cursor from the WON filter.
    const read = vi
      .fn<[HistoryQuery], Promise<DuelPageRead>>()
      .mockResolvedValueOnce({
        kind: "page",
        duels: [aDuelLine({ duelId: "duel-1", outcome: "WON" })],
        nextCursor: "cursor-with-won",
        restarted: false,
      } as DuelPageRead)
      .mockResolvedValueOnce({
        kind: "page",
        duels: [aDuelLine({ duelId: "duel-2", outcome: "LOST" })],
        nextCursor: null,
        restarted: false,
      } as DuelPageRead);

    render(<HistoryScreen read={read} />);

    await waitFor(() => {
      expect(screen.getAllByRole("listitem")).toHaveLength(1);
    });

    // Verify first call had no cursor
    expect(read.mock.calls[0][0]).toEqual({
      outcome: null,
      opponent: "",
      after: null,
    });

    // Click to filter by LOST
    const lostRadio = screen.getByLabelText(outcomeWord("LOST"));
    fireEvent.click(lostRadio);

    await waitFor(() => {
      expect(read).toHaveBeenCalledTimes(2);
    });

    // The second call must have after: null, not the cursor from the first filter
    const secondCall = read.mock.calls[1][0];
    expect(secondCall).toEqual({
      outcome: "LOST",
      opponent: "",
      after: null,
    });
  });

  it("uses the updated filter when showing more, not the stale one", async () => {
    // FALSIFICATION (a): This test fails if "filtered" is not dispatched,
    // because state.filter won't be updated. The next "show more" request
    // will use nextPageQuery(state), which builds from state.filter.
    // If state.filter is still null, the request will have outcome: null
    // instead of outcome: "LOST".
    const read = vi
      .fn<[HistoryQuery], Promise<DuelPageRead>>()
      .mockResolvedValueOnce({
        kind: "page",
        duels: [aDuelLine({ duelId: "duel-1", outcome: "WON" })],
        nextCursor: "cursor-for-all",
        restarted: false,
      } as DuelPageRead)
      .mockResolvedValueOnce({
        kind: "page",
        duels: [aDuelLine({ duelId: "duel-2", outcome: "LOST" })],
        nextCursor: "cursor-for-lost",
        restarted: false,
      } as DuelPageRead)
      .mockResolvedValueOnce({
        kind: "page",
        duels: [aDuelLine({ duelId: "duel-3", outcome: "LOST" })],
        nextCursor: null,
        restarted: false,
      } as DuelPageRead);

    render(<HistoryScreen read={read} />);

    // First read: no filter, one WON row with a cursor
    await waitFor(() => {
      expect(screen.getAllByRole("listitem")).toHaveLength(1);
    });
    expect(screen.getByText("Won")).toBeDefined();

    // Change filter to LOST
    const lostRadio = screen.getByLabelText(outcomeWord("LOST"));
    fireEvent.click(lostRadio);

    // Second read: LOST filter, one LOST row with a cursor
    await waitFor(() => {
      expect(read).toHaveBeenCalledTimes(2);
    });

    await waitFor(() => {
      const items = screen.getAllByRole("listitem");
      expect(items).toHaveLength(1);
      expect(items[0].textContent).toContain("Lost");
    });

    // Click show more
    const moreButton = screen.getByRole("button", { name: MORE });
    fireEvent.click(moreButton);

    // Third read should use the LOST filter, not the old null filter
    await waitFor(() => {
      expect(read).toHaveBeenCalledTimes(3);
    });

    const thirdCall = read.mock.calls[2][0];
    expect(thirdCall).toEqual({
      outcome: "LOST",
      opponent: "",
      after: "cursor-for-lost",
    });
  });

  it("sends the term the player typed, unmodified", async () => {
    const read = vi.fn<[HistoryQuery], Promise<DuelPageRead>>(
      async () =>
        ({
          kind: "page",
          duels: [],
          nextCursor: null,
          restarted: false,
        }) as DuelPageRead,
    );

    render(<HistoryScreen read={read} />);

    await waitFor(() => {
      expect(read).toHaveBeenCalledTimes(1);
    });

    const input = screen.getByLabelText(OPPONENT_LABEL);
    const searchButton = screen.getByRole("button", { name: SEARCH });

    const firstTerm = "  ada  ";
    fireEvent.change(input, { target: { value: firstTerm } });
    fireEvent.click(searchButton);

    await waitFor(() => {
      expect(read).toHaveBeenCalledTimes(2);
    });
    expect(read.mock.calls[1][0].opponent).toBe(firstTerm);

    const secondTerm = "100%Sure";
    fireEvent.change(input, { target: { value: secondTerm } });
    fireEvent.click(searchButton);

    await waitFor(() => {
      expect(read).toHaveBeenCalledTimes(3);
    });
    expect(read.mock.calls[2][0].opponent).toBe(secondTerm);
  });

  it("sends no opponent parameter once the box is emptied", async () => {
    const read = vi.fn<[HistoryQuery], Promise<DuelPageRead>>(
      async () =>
        ({
          kind: "page",
          duels: [],
          nextCursor: null,
          restarted: false,
        }) as DuelPageRead,
    );

    render(<HistoryScreen read={read} />);

    await waitFor(() => {
      expect(read).toHaveBeenCalledTimes(1);
    });

    const input = screen.getByLabelText(OPPONENT_LABEL);
    const searchButton = screen.getByRole("button", { name: SEARCH });

    fireEvent.change(input, { target: { value: "Ada" } });
    fireEvent.click(searchButton);

    await waitFor(() => {
      expect(read).toHaveBeenCalledTimes(2);
    });
    expect(read.mock.calls[1][0].opponent).toBe("Ada");

    fireEvent.change(input, { target: { value: "" } });
    fireEvent.click(searchButton);

    await waitFor(() => {
      expect(read).toHaveBeenCalledTimes(3);
    });

    const thirdQuery = read.mock.calls[2][0];
    expect(thirdQuery.opponent).toBe("");
    expect(duelsPath(thirdQuery)).toBe("/api/me/duels");
  });

  it("drops the cursor and the rows on a search, and keeps the outcome chosen", async () => {
    const read = vi
      .fn<[HistoryQuery], Promise<DuelPageRead>>()
      .mockResolvedValueOnce({
        kind: "page",
        duels: [
          aDuelLine({ duelId: "duel-1", opponentDisplayName: "Alice" }),
          aDuelLine({ duelId: "duel-2", opponentDisplayName: "Bob" }),
        ],
        nextCursor: "cursor-123",
        restarted: false,
      } as DuelPageRead)
      .mockResolvedValueOnce({
        kind: "page",
        duels: [aDuelLine({ duelId: "duel-3", opponentDisplayName: "Ada" })],
        nextCursor: null,
        restarted: false,
      } as DuelPageRead);

    render(
      <HistoryScreen read={read} filter={{ outcome: "WON", opponent: "" }} />,
    );

    await waitFor(() => {
      expect(screen.getAllByRole("listitem")).toHaveLength(2);
    });

    const input = screen.getByLabelText(OPPONENT_LABEL);
    const searchButton = screen.getByRole("button", { name: SEARCH });

    fireEvent.change(input, { target: { value: "Ada" } });
    fireEvent.click(searchButton);

    await waitFor(() => {
      expect(read).toHaveBeenCalledTimes(2);
    });

    expect(read.mock.calls[1][0]).toEqual({
      outcome: "WON",
      opponent: "Ada",
      after: null,
    });

    const listItems = await waitFor(() => {
      const items = screen.getAllByRole("listitem");
      expect(items).toHaveLength(1);
      return items;
    });
    expect(listItems[0].textContent).toContain("Ada");
    expect(screen.queryByText("Alice")).toBeNull();
    expect(screen.queryByText("Bob")).toBeNull();
  });

  it("asks nothing while the player types, and once when the search is submitted", async () => {
    const read = vi.fn<[HistoryQuery], Promise<DuelPageRead>>(
      async () =>
        ({
          kind: "page",
          duels: [],
          nextCursor: null,
          restarted: false,
        }) as DuelPageRead,
    );

    render(<HistoryScreen read={read} />);

    await waitFor(() => {
      expect(read).toHaveBeenCalledTimes(1);
    });

    const input = screen.getByLabelText(OPPONENT_LABEL) as HTMLInputElement;
    const term = "Halvard";

    for (const char of term) {
      fireEvent.change(input, { target: { value: input.value + char } });
    }

    // Every keystroke landed on the box and none of them reached the transport.
    expect(input.value).toBe(term);
    expect(read).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByRole("button", { name: SEARCH }));

    await waitFor(() => {
      expect(read).toHaveBeenCalledTimes(2);
    });
    expect(read.mock.calls[1][0].opponent).toBe(term);
  });

  it("uses the term just searched when showing more, not the stale one", async () => {
    // Mirrors TASK-041311's "uses the updated filter when showing more, not
    // the stale one" for the opponent axis: a search dispatches `filtered`
    // before it asks, so a later MORE click must read the searched term, not
    // the one the screen opened with.
    const read = vi
      .fn<[HistoryQuery], Promise<DuelPageRead>>()
      .mockResolvedValueOnce({
        kind: "page",
        duels: [aDuelLine({ duelId: "duel-1" })],
        nextCursor: null,
        restarted: false,
      } as DuelPageRead)
      .mockResolvedValueOnce({
        kind: "page",
        duels: [aDuelLine({ duelId: "duel-2" })],
        nextCursor: "cursor-for-ada",
        restarted: false,
      } as DuelPageRead)
      .mockResolvedValueOnce({
        kind: "page",
        duels: [aDuelLine({ duelId: "duel-3" })],
        nextCursor: null,
        restarted: false,
      } as DuelPageRead);

    render(
      <HistoryScreen read={read} filter={{ outcome: null, opponent: "Bob" }} />,
    );

    await waitFor(() => {
      expect(read).toHaveBeenCalledTimes(1);
    });

    const input = screen.getByLabelText(OPPONENT_LABEL);
    const searchButton = screen.getByRole("button", { name: SEARCH });

    fireEvent.change(input, { target: { value: "Ada" } });
    fireEvent.click(searchButton);

    await waitFor(() => {
      expect(read).toHaveBeenCalledTimes(2);
    });

    const moreButton = await waitFor(() =>
      screen.getByRole("button", { name: MORE }),
    );
    fireEvent.click(moreButton);

    await waitFor(() => {
      expect(read).toHaveBeenCalledTimes(3);
    });

    const thirdCall = read.mock.calls[2][0];
    expect(thirdCall).toEqual({
      outcome: null,
      opponent: "Ada",
      after: "cursor-for-ada",
    });
  });
});
