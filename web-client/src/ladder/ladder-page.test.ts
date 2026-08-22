import { describe, it, expect } from "vitest";
import { parseLadderPage } from "./ladder-page";

describe("one page of the ladder, parsed", () => {
  it("keeps the rows in the order the body listed them, and renumbers nothing", () => {
    const page = parseLadderPage({
      season: "2026-08",
      rows: [
        {
          rank: 1,
          playerId: "d",
          displayName: "Alice",
          coins: 10,
        },
        {
          rank: 2,
          playerId: "a",
          displayName: "Bob",
          coins: 8,
        },
        {
          rank: 2,
          playerId: "c",
          displayName: "Charlie",
          coins: 8,
        },
        {
          rank: 4,
          playerId: "b",
          displayName: "David",
          coins: 5,
        },
      ],
      nextCursor: null,
      self: null,
    });

    expect(page).not.toBeNull();
    if (page !== null) {
      expect(page.season).toBe("2026-08");
      expect(page.nextCursor).toBe(null);
      expect(page.rows.map((r) => r.rank)).toEqual([1, 2, 2, 4]);
      expect(page.rows.map((r) => r.playerId)).toEqual(["d", "a", "c", "b"]);
    }
  });

  it("reads a rank, a player id, a name that may be null, and a standing", () => {
    const page = parseLadderPage({
      season: "2026-08",
      rows: [
        {
          rank: 1,
          playerId: "player1",
          displayName: "Named Player",
          coins: 3,
        },
        {
          rank: 2,
          playerId: "player2",
          displayName: null,
          coins: -2,
        },
      ],
      nextCursor: "cursor-string",
      self: null,
    });

    expect(page).not.toBeNull();
    if (page !== null) {
      expect(page.rows.length).toBe(2);
      expect(page.rows[0].displayName).toBe("Named Player");
      expect(page.rows[0].coins).toBe(3);
      expect(page.rows[1].displayName).toBeNull();
      expect(page.rows[1].coins).toBe(-2);
      expect(page.nextCursor).toBe("cursor-string");
    }
  });

  it("refuses a body with no cursor, a season that is not a month, or a row missing a field", () => {
    // Body with no nextCursor key
    const noCursor = parseLadderPage({
      season: "2026-08",
      rows: [
        {
          rank: 1,
          playerId: "player1",
          displayName: "Player",
          coins: 10,
        },
      ],
      self: null,
    });
    expect(noCursor).toBeNull();

    // Body with invalid season format (not YYYY-MM)
    const invalidSeason = parseLadderPage({
      season: "August",
      rows: [
        {
          rank: 1,
          playerId: "player1",
          displayName: "Player",
          coins: 10,
        },
      ],
      nextCursor: null,
      self: null,
    });
    expect(invalidSeason).toBeNull();

    // Row with no rank field
    const missingRank = parseLadderPage({
      season: "2026-08",
      rows: [
        {
          playerId: "player1",
          displayName: "Player",
          coins: 10,
        },
      ],
      nextCursor: null,
      self: null,
    });
    expect(missingRank).toBeNull();

    // Row with coins as a string instead of number
    const coinsAsString = parseLadderPage({
      season: "2026-08",
      rows: [
        {
          rank: 1,
          playerId: "player1",
          displayName: "Player",
          coins: "10",
        },
      ],
      nextCursor: null,
      self: null,
    });
    expect(coinsAsString).toBeNull();
  });

  it("reads a placed reader as a rank and a standing, and keeps no player id on it", () => {
    const page = parseLadderPage({
      season: "2026-08",
      rows: [
        {
          rank: 1,
          playerId: "player1",
          displayName: "Player",
          coins: 10,
        },
      ],
      nextCursor: null,
      self: {
        playerId: "me",
        rank: 5,
        coins: -1,
      },
    });

    expect(page).not.toBeNull();
    if (page !== null) {
      expect(page.self).not.toBeNull();
      if (page.self !== null) {
        expect(page.self.rank).toBe(5);
        expect(page.self.coins).toBe(-1);
        // Ensure playerId is not copied to SelfStanding
        expect(Object.keys(page.self)).toEqual(["rank", "coins"]);
      }
    }
  });

  it("reads a reader with no place as two nulls, never as a zero", () => {
    const page = parseLadderPage({
      season: "2026-08",
      rows: [
        {
          rank: 1,
          playerId: "player1",
          displayName: "Player",
          coins: 10,
        },
      ],
      nextCursor: null,
      self: {
        playerId: "me",
        rank: null,
        coins: null,
      },
    });

    expect(page).not.toBeNull();
    if (page !== null) {
      expect(page.self).not.toBeNull();
      if (page.self !== null) {
        expect(page.self.rank).toBeNull();
        expect(page.self.coins).toBeNull();
        expect(page.self.coins).not.toBe(0);
      }
    }
  });

  it("reads an absent reader as no self standing at all", () => {
    const page = parseLadderPage({
      season: "2026-08",
      rows: [
        {
          rank: 1,
          playerId: "player1",
          displayName: "Player",
          coins: 10,
        },
      ],
      nextCursor: null,
      self: null,
    });

    expect(page).not.toBeNull();
    if (page !== null) {
      expect(page.self).toBeNull();
    }
  });

  it("refuses a self object that has a rank but no standing", () => {
    // Body with rank as number and coins as null
    const rankButNoCoin = parseLadderPage({
      season: "2026-08",
      rows: [
        {
          rank: 1,
          playerId: "player1",
          displayName: "Player",
          coins: 10,
        },
      ],
      nextCursor: null,
      self: {
        rank: 5,
        coins: null,
      },
    });
    expect(rankButNoCoin).toBeNull();

    // Body with coins as number and rank as null
    const coinButNoRank = parseLadderPage({
      season: "2026-08",
      rows: [
        {
          rank: 1,
          playerId: "player1",
          displayName: "Player",
          coins: 10,
        },
      ],
      nextCursor: null,
      self: {
        rank: null,
        coins: 3,
      },
    });
    expect(coinButNoRank).toBeNull();
  });
});
