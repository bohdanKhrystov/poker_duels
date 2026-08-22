import { describe, expect, it } from "vitest";
import {
  initialLadder,
  ladderReducer,
  nextPageAfter,
  type LadderState,
} from "./ladder-state";
import type { LadderPage, LadderRow } from "./ladder-page";

function row(rank: number): LadderRow {
  return {
    rank,
    playerId: `player-${rank}`,
    displayName: `Player ${rank}`,
    coins: 1_000 - rank,
  };
}

function page(ranks: readonly number[], nextCursor: string | null): LadderPage {
  return {
    season: "2026-08",
    rows: ranks.map(row),
    nextCursor,
    self: null,
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
  };
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
});
