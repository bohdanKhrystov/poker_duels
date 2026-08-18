import { describe, it, expect } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";
import { ProfileStrip } from "./ProfileStrip";
import { aProfile, aDuelLine } from "./profile-fixture";

describe("the profile strip", () => {
  it("states the balance the server sent", () => {
    render(
      <ProfileStrip
        state={{
          kind: "profile",
          profile: aProfile({ coinBalance: 7 }),
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
          profile: aProfile({ coinBalance: -1 }),
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
          profile: aProfile(),
          duels: [
            aDuelLine({
              duelId: "duel-1", // distinctness: two rows, two keys
              handsPlayed: 1,
              finishedAt: "2026-01-15T10:30:00Z",
            }),
            aDuelLine({
              duelId: "duel-2", // distinctness: two rows, two keys
              outcome: "LOST",
              coinDelta: -1,
              handsPlayed: 9,
              finishedAt: "2025-12-20T14:45:00Z",
            }),
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
          profile: aProfile(),
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
          profile: aProfile(),
          duels: [
            aDuelLine({
              duelId: "duel-b", // distinctness: three rows, three keys
              outcome: "DREW",
              coinDelta: 0,
              handsPlayed: 12,
              finishedAt: "2026-03-02T09:00:00Z",
            }),
            aDuelLine({
              duelId: "duel-a", // distinctness: three rows, three keys
              handsPlayed: 41,
              finishedAt: "2026-05-14T18:20:00Z",
            }),
            aDuelLine({
              duelId: "duel-c", // distinctness: three rows, three keys
              outcome: "LOST",
              coinDelta: -1,
              handsPlayed: 7,
              finishedAt: "2026-01-09T22:05:00Z",
            }),
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

  it("names the player, and stands in for a player with no name, in one render", () => {
    const { rerender } = render(
      <ProfileStrip
        state={{
          kind: "profile",
          profile: aProfile({ displayName: "Ada", coinBalance: 5 }),
          duels: [],
        }}
      />,
    );

    expect(screen.getByText("Ada")).toBeDefined();
    expect(screen.queryByText("No name")).toBeNull();

    rerender(
      <ProfileStrip
        state={{
          kind: "profile",
          profile: aProfile({ displayName: null, coinBalance: 5 }),
          duels: [],
        }}
      />,
    );

    expect(screen.getByText("No name")).toBeDefined();
    expect(screen.queryByText("Ada")).toBeNull();
  });

  it("says nothing different about a name that was removed", () => {
    const { container: containerRemoved } = render(
      <ProfileStrip
        state={{
          kind: "profile",
          profile: aProfile({
            displayName: null,
            displayNameRemoved: true,
            coinBalance: 5,
          }),
          duels: [],
        }}
      />,
    );

    const removedMarkup = containerRemoved.innerHTML;

    cleanup();

    const { container: containerNotRemoved } = render(
      <ProfileStrip
        state={{
          kind: "profile",
          profile: aProfile({
            displayName: null,
            displayNameRemoved: false,
            coinBalance: 5,
          }),
          duels: [],
        }}
      />,
    );

    const notRemovedMarkup = containerNotRemoved.innerHTML;

    expect(removedMarkup).toBe(notRemovedMarkup);
  });

  it("names a rival who has a name and stands in for one who does not, on two lines of one list", () => {
    render(
      <ProfileStrip
        state={{
          kind: "profile",
          profile: aProfile(),
          duels: [
            aDuelLine({ duelId: "d-1", opponentDisplayName: "Ada" }),
            aDuelLine({ duelId: "d-2", opponentDisplayName: null }),
          ],
        }}
      />,
    );

    const listItems = screen.getAllByRole("listitem");
    expect(listItems).toHaveLength(2);

    const firstItem = listItems[0].textContent;
    const secondItem = listItems[1].textContent;

    // First line contains the opponent's name "Ada"
    expect(firstItem).toContain("Ada");
    // First line does not contain "No name"
    expect(firstItem).not.toContain("No name");

    // Second line contains the treatment for no name
    expect(secondItem).toContain("No name");
  });

  it("keeps the rest of the line exactly as it was", () => {
    render(
      <ProfileStrip
        state={{
          kind: "profile",
          profile: aProfile(),
          duels: [
            aDuelLine({
              duelId: "d-1",
              opponentDisplayName: "Ada",
              outcome: "WON",
              coinDelta: 1,
              handsPlayed: 23,
              finishedAt: "2026-02-03T04:05:06Z",
            }),
            aDuelLine({
              duelId: "d-2",
              opponentDisplayName: null,
              outcome: "LOST",
              coinDelta: -2,
              handsPlayed: 5,
              finishedAt: "2026-01-15T14:30:00Z",
            }),
          ],
        }}
      />,
    );

    const listItems = screen.getAllByRole("listitem");
    expect(listItems).toHaveLength(2);

    const firstItem = listItems[0].textContent;
    expect(firstItem).toContain("Won");
    expect(firstItem).toContain("+1");
    expect(firstItem).toContain("23 hands");
    expect(firstItem).toContain("2026");

    const secondItem = listItems[1].textContent;
    expect(secondItem).toContain("Lost");
    expect(secondItem).toContain("−2");
    expect(secondItem).toContain("5 hands");
    expect(secondItem).toContain("2026");
  });
});
