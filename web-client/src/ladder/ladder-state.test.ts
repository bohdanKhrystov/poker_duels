import { describe, expect, it } from "vitest";
import {
  initialLadder,
  ladderReducer,
  nextPageAfter,
  type LadderState,
} from "./ladder-state";
import type { LadderPage, LadderRow, SelfStanding } from "./ladder-page";

function row(rank: number): LadderRow {
  return {
    rank,
    playerId: `player-${rank}`,
    displayName: `Player ${rank}`,
    coins: 1_000 - rank,
  };
}

// `season` and `self` default to the walk's usual first-page values so the
// five tests written before either field existed keep producing the exact
// pages they always did; a test proving the season or self rule overrides
// one or both.
function page(
  ranks: readonly number[],
  nextCursor: string | null,
  overrides?: {
    readonly season?: string;
    readonly self?: SelfStanding | null;
  },
): LadderPage {
  return {
    season: overrides?.season ?? "2026-08",
    rows: ranks.map(row),
    nextCursor,
    self: overrides?.self ?? null,
  };
}

// A state already holding a page, standing in for "the screen has read
// something" — every test that must prove append from replace starts from
// rows the reducer did not just invent, per the two-non-empty-lists rule.
function heldState(
  ranks: readonly number[],
  nextCursor: string | null,
): LadderState {
  return {
    rows: ranks.map(row),
    nextCursor,
    askedWith: null,
    phase: "ready",
    season: "2026-08",
    self: null,
  };
}

// The state after the walk's first page has answered — the season and self
// standing this fixes are what the tests below prove a second page cannot
// move.
function walkedFirstPage(): LadderState {
  const asked = ladderReducer(initialLadder(), { type: "asked", after: null });
  return ladderReducer(asked, {
    type: "page",
    page: page([1], "c1", { season: "2026-08", self: { rank: 5, coins: 1 } }),
  });
}

describe("the ladder walk", () => {
  it("appends the next page under the rows it already holds", () => {
    const asked = ladderReducer(heldState([1, 1], "c1"), {
      type: "asked",
      after: "c1",
    });
    const next = ladderReducer(asked, {
      type: "page",
      page: page([1, 5], "c2"),
    });

    expect(next.rows.map((r) => r.rank)).toEqual([1, 1, 1, 5]);
  });

  it("replaces the rows when the page answers a request that carried no cursor", () => {
    const asked = ladderReducer(heldState([1, 1], "c1"), {
      type: "asked",
      after: null,
    });
    const next = ladderReducer(asked, {
      type: "page",
      page: page([2, 3], "c2"),
    });

    expect(next.rows.map((r) => r.rank)).toEqual([2, 3]);
  });

  it("un-reads nothing when a request fails", () => {
    const held = heldState([1, 2], "c1");

    const next = ladderReducer(held, { type: "failed" });

    expect(next.rows.map((r) => r.rank)).toEqual([1, 2]);
    expect(next.nextCursor).toBe("c1");
    expect(next.phase).toBe("failed");
    expect(next.askedWith).toBeNull();
  });

  it("offers no next page when the server named none", () => {
    expect(nextPageAfter(heldState([1], "c9"))).toBe("c9");
    expect(nextPageAfter(heldState([1], null))).toBeNull();
  });

  it("starts loading, with nothing read and nowhere to go", () => {
    const state = initialLadder();

    expect(state.rows).toEqual([]);
    expect(state.nextCursor).toBeNull();
    expect(state.askedWith).toBeNull();
    expect(state.phase).toBe("loading");
  });

  it("takes the season and the self standing of a page that carried no cursor", () => {
    const asked = ladderReducer(initialLadder(), {
      type: "asked",
      after: null,
    });
    const next = ladderReducer(asked, {
      type: "page",
      page: page([1], "c1", {
        season: "2026-08",
        self: { rank: 5, coins: 1 },
      }),
    });

    expect(next.season).toBe("2026-08");
    expect(next.self).toEqual({ rank: 5, coins: 1 });
  });

  it("keeps the season and the self standing the first page carried", () => {
    const asked = ladderReducer(walkedFirstPage(), {
      type: "asked",
      after: "c1",
    });
    const next = ladderReducer(asked, {
      type: "page",
      page: page([2], "c2", {
        season: "2026-09",
        self: { rank: 9, coins: -4 },
      }),
    });

    expect(next.season).toBe("2026-08");
    expect(next.self).toEqual({ rank: 5, coins: 1 });
    expect(next.rows.map((r) => r.rank)).toEqual([1, 2]);
  });

  it("does not move the self standing when a later page carries a different one", () => {
    const asked = ladderReducer(walkedFirstPage(), {
      type: "asked",
      after: "c1",
    });
    const next = ladderReducer(asked, {
      type: "page",
      page: page([2], "c2", { self: null }),
    });

    expect(next.self).toEqual({ rank: 5, coins: 1 });
  });
});
