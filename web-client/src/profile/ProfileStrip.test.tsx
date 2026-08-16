import { describe, it, expect } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";
import { ProfileStrip } from "./ProfileStrip";

describe("the profile strip", () => {
  it("states the balance the server sent", () => {
    render(
      <ProfileStrip
        state={{
          kind: "profile",
          profile: { playerId: "p1", coinBalance: 7 },
          duels: [],
        }}
      />,
    );

    expect(screen.getByText("7 Duel coins")).toBeDefined();

    // Test negative balance: −1 (U+2212 MINUS SIGN)
    cleanup();
    render(
      <ProfileStrip
        state={{
          kind: "profile",
          profile: { playerId: "p2", coinBalance: -1 },
          duels: [],
        }}
      />,
    );

    expect(screen.getByText("−1 Duel coins")).toBeDefined();
  });

  it("says there is no profile yet, and raises no alarm", () => {
    render(<ProfileStrip state={{ kind: "no-profile" }} />);

    expect(screen.getByText("No profile yet.")).toBeDefined();
    expect(screen.queryAllByRole("alert")).toHaveLength(0);
    expect(
      screen.getByText("No profile yet.").textContent?.toLowerCase(),
    ).not.toContain("error");
  });

  it("renders nothing at all when the read did not land", () => {
    const { container } = render(
      <ProfileStrip state={{ kind: "unavailable" }} />,
    );

    expect(container.innerHTML).toBe("");
  });

  it("shows one line per duel, with its outcome, coin, hands and time", () => {
    render(
      <ProfileStrip
        state={{
          kind: "profile",
          profile: { playerId: "p1", coinBalance: 5 },
          duels: [
            {
              duelId: "d1",
              outcome: "WON",
              coinDelta: 1,
              handsPlayed: 1,
              finishedAt: "2026-01-15T10:30:00Z",
            },
            {
              duelId: "d2",
              outcome: "LOST",
              coinDelta: -1,
              handsPlayed: 9,
              finishedAt: "2025-12-20T14:45:00Z",
            },
          ],
        }}
      />,
    );

    const listItems = screen.getAllByRole("listitem");
    expect(listItems).toHaveLength(2);

    const firstItem = listItems[0].textContent;
    expect(firstItem).toContain("Won");
    expect(firstItem).toContain("+1");
    expect(firstItem).toContain("1 hand");
    expect(firstItem).toContain("2026");

    const secondItem = listItems[1].textContent;
    expect(secondItem).toContain("Lost");
    expect(secondItem).toContain("−1");
    expect(secondItem).toContain("9 hands");
    expect(secondItem).toContain("2025");
  });

  it("says there are no duels yet when the list is empty", () => {
    render(
      <ProfileStrip
        state={{
          kind: "profile",
          profile: { playerId: "p1", coinBalance: 5 },
          duels: [],
        }}
      />,
    );

    expect(screen.getByText("No duels yet.")).toBeDefined();
    expect(screen.queryAllByRole("listitem")).toHaveLength(0);
  });

  it("lists the duels in the order they arrived, not one it chose", () => {
    render(
      <ProfileStrip
        state={{
          kind: "profile",
          profile: { playerId: "p1", coinBalance: 5 },
          duels: [
            {
              duelId: "duel-b",
              outcome: "DREW",
              coinDelta: 0,
              handsPlayed: 12,
              finishedAt: "2026-03-02T09:00:00Z",
            },
            {
              duelId: "duel-a",
              outcome: "WON",
              coinDelta: 1,
              handsPlayed: 41,
              finishedAt: "2026-05-14T18:20:00Z",
            },
            {
              duelId: "duel-c",
              outcome: "LOST",
              coinDelta: -1,
              handsPlayed: 7,
              finishedAt: "2026-01-09T22:05:00Z",
            },
          ],
        }}
      />,
    );

    const listItems = screen.getAllByRole("listitem");
    expect(listItems).toHaveLength(3);

    // Assert hands played appear in the correct order: 12, 41, 7
    const handsPlayed = listItems.map((item) => {
      const match = item.textContent?.match(/(\d+)\s+(hands?)/);
      return match ? `${match[1]} ${match[2]}` : null;
    });
    expect(handsPlayed).toEqual(["12 hands", "41 hands", "7 hands"]);

    // Assert outcomes appear in the correct order: Drew, Won, Lost
    const outcomes = listItems.map((item) => {
      const text = item.textContent || "";
      if (text.startsWith("Drew")) return "Drew";
      if (text.startsWith("Won")) return "Won";
      if (text.startsWith("Lost")) return "Lost";
      return null;
    });
    expect(outcomes).toEqual(["Drew", "Won", "Lost"]);
  });
});
