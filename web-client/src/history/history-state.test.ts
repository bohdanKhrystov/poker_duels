import { describe, expect, it } from "vitest";

import { NO_FILTER } from "../profile/duels-query";
import { aDuelLine } from "../profile/profile-fixture";
import {
  firstPageQuery,
  historyReducer,
  initialHistory,
  nextPageQuery,
  type HistoryState,
} from "./history-state";

// Three rows that climb in no field and fall in no field: duelId, finishedAt,
// handsPlayed and coinDelta each zig-zag across this arrival order, so a
// reducer that silently sorted by any one of them would produce a
// detectably different array rather than passing by luck.
const rowMercury = aDuelLine({
  duelId: "duel-mercury",
  finishedAt: "2026-02-03T04:05:06Z",
  handsPlayed: 40,
  coinDelta: 5,
});
const rowVenus = aDuelLine({
  duelId: "duel-venus",
  finishedAt: "2026-01-01T00:00:00Z",
  handsPlayed: 10,
  coinDelta: -3,
});
const rowEarth = aDuelLine({
  duelId: "duel-earth",
  finishedAt: "2026-01-15T00:00:00Z",
  handsPlayed: 25,
  coinDelta: 2,
});

// A first page of two rows, held before a second page is asked for. Combined
// with the second-page rows below, the four-row arrival order is likewise
// non-monotonic in every field.
const rowJupiter = aDuelLine({
  duelId: "duel-jupiter",
  finishedAt: "2026-01-10T00:00:00Z",
  handsPlayed: 15,
  coinDelta: 2,
});
const rowMars = aDuelLine({
  duelId: "duel-mars",
  finishedAt: "2026-03-01T00:00:00Z",
  handsPlayed: 60,
  coinDelta: -5,
});

const heldTwoRows: HistoryState = {
  filter: NO_FILTER,
  rows: [rowJupiter, rowMars],
  nextCursor: "cur-1",
  askedWith: null,
  phase: "ready",
};

// The continuation of heldTwoRows's walk.
const rowSaturn = aDuelLine({
  duelId: "duel-saturn",
  finishedAt: "2026-02-15T00:00:00Z",
  handsPlayed: 8,
  coinDelta: 9,
});
const rowNeptune = aDuelLine({
  duelId: "duel-neptune",
  finishedAt: "2026-01-25T00:00:00Z",
  handsPlayed: 30,
  coinDelta: -1,
});

// A page answering a request asked with no cursor — must replace, not join.
const rowUranus = aDuelLine({
  duelId: "duel-uranus",
  finishedAt: "2026-04-05T00:00:00Z",
  handsPlayed: 12,
  coinDelta: 4,
});
const rowPluto = aDuelLine({
  duelId: "duel-pluto",
  finishedAt: "2026-01-20T00:00:00Z",
  handsPlayed: 55,
  coinDelta: -7,
});

// A page answering a restarted walk — must also replace, even though the
// request in flight carried a cursor. It carries rowJupiter again: a
// restart re-reads from the newest page, so the row the client already
// held is exactly the kind of row a wrongly-appending reducer would
// duplicate.
const rowIo = aDuelLine({
  duelId: "duel-io",
  finishedAt: "2026-05-01T00:00:00Z",
  handsPlayed: 3,
  coinDelta: 1,
});

describe("the history walk", () => {
  it("starts loading, with nothing read and nowhere to go", () => {
    expect(initialHistory()).toEqual({
      filter: NO_FILTER,
      rows: [],
      nextCursor: null,
      askedWith: null,
      phase: "loading",
    });
  });

  it("keeps the rows the server sent, in the order it sent them", () => {
    const state = historyReducer(initialHistory(), {
      type: "page",
      duels: [rowMercury, rowVenus, rowEarth],
      nextCursor: null,
      restarted: false,
    });

    expect(state.rows.map((row) => row.duelId)).toEqual([
      rowMercury.duelId,
      rowVenus.duelId,
      rowEarth.duelId,
    ]);
  });

  it("appends the next page under the rows already read", () => {
    const asked = historyReducer(heldTwoRows, {
      type: "asked",
      after: "cur-1",
    });
    const state = historyReducer(asked, {
      type: "page",
      duels: [rowSaturn, rowNeptune],
      nextCursor: "cur-2",
      restarted: false,
    });

    expect(state.rows).toEqual([rowJupiter, rowMars, rowSaturn, rowNeptune]);
  });

  it("replaces the rows when the page was asked for with no cursor", () => {
    const asked = historyReducer(heldTwoRows, { type: "asked", after: null });
    const state = historyReducer(asked, {
      type: "page",
      duels: [rowUranus, rowPluto],
      nextCursor: "cur-3",
      restarted: false,
    });

    expect(state.rows).toEqual([rowUranus, rowPluto]);
  });

  it("replaces the rows when the server restarted the walk", () => {
    const asked = historyReducer(heldTwoRows, {
      type: "asked",
      after: "stale",
    });
    const state = historyReducer(asked, {
      type: "page",
      duels: [rowJupiter, rowIo],
      nextCursor: "cur-4",
      restarted: true,
    });

    expect(state.rows).toEqual([rowJupiter, rowIo]);
  });

  it("offers a first query with no cursor, and a next query only while one is named", () => {
    expect(firstPageQuery({ outcome: "WON", opponent: "Ada" })).toEqual({
      outcome: "WON",
      opponent: "Ada",
      after: null,
    });

    const withCursor: HistoryState = {
      filter: { outcome: "LOST", opponent: "Bo" },
      rows: [],
      nextCursor: "cur-7",
      askedWith: null,
      phase: "ready",
    };
    expect(nextPageQuery(withCursor)).toEqual({
      outcome: "LOST",
      opponent: "Bo",
      after: "cur-7",
    });

    const atTheEnd: HistoryState = {
      filter: NO_FILTER,
      rows: [],
      nextCursor: null,
      askedWith: null,
      phase: "ready",
    };
    expect(nextPageQuery(atTheEnd)).toBeNull();
  });

  it("a failure keeps what was already read, and says so", () => {
    const held: HistoryState = {
      filter: NO_FILTER,
      rows: [rowMercury, rowVenus, rowEarth],
      nextCursor: "cur-9",
      askedWith: "cur-9",
      phase: "loading",
    };

    const state = historyReducer(held, { type: "failed" });

    expect(state.phase).toBe("failed");
    expect(state.rows).toEqual([rowMercury, rowVenus, rowEarth]);
    expect(state.nextCursor).toBe("cur-9");
    expect(state.askedWith).toBeNull();
  });
});
