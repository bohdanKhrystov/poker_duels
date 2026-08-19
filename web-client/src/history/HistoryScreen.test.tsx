import { describe, it, expect, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { HistoryScreen } from "./HistoryScreen";
import { aDuelLine } from "../profile/profile-fixture";
import type { DuelPageRead } from "../profile/duel-page";
import type { HistoryQuery } from "../profile/duels-query";
import { HISTORY_HEADING } from "./history-text";
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
});
