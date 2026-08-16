import { describe, it, expect } from "vitest";
import { readRecentDuels } from "./recent-duels";
import type { ApiFetch, ApiResponse } from "./api";
import { DEVICE_ID_STORAGE_KEY } from "../protocol/device-id";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`.
 *
 * Node 24+ defines its own `localStorage` global which is present but inert
 * unless the process is started with `--localstorage-file`, and under Vitest it
 * shadows the one jsdom provides: `typeof localStorage` is `"undefined"` while
 * `sessionStorage` — which Node keeps in memory — works. Depending on that
 * global would make these tests a property of the Node version rather than of
 * this module. `readRecentDuels` takes the `Storage` it acts on as a parameter, so
 * the tests hand it one and rely on no global at all.
 */
function inMemoryStorage(): Storage {
  const entries = new Map<string, string>();
  return {
    get length(): number {
      return entries.size;
    },
    clear(): void {
      entries.clear();
    },
    getItem(key: string): string | null {
      return entries.has(key) ? (entries.get(key) as string) : null;
    },
    key(index: number): string | null {
      return Array.from(entries.keys())[index] ?? null;
    },
    removeItem(key: string): void {
      entries.delete(key);
    },
    setItem(key: string, value: string): void {
      entries.set(key, value);
    },
  };
}

/** Records the details of each fetch call made. */
interface FetchCall {
  readonly path: string;
  readonly headers: Record<string, string>;
}

/**
 * Creates a mock fetch that records every call and answers them in order.
 * No network, no globals — purely synchronous recording and predetermined answers.
 */
function answering(...answers: readonly ApiResponse[]): {
  readonly calls: FetchCall[];
  readonly fetch: ApiFetch;
} {
  const calls: FetchCall[] = [];
  let answerIndex = 0;

  return {
    calls,
    fetch: async (
      path: string,
      init: { readonly headers: Readonly<Record<string, string>> },
    ): Promise<ApiResponse> => {
      calls.push({
        path,
        headers: { ...init.headers },
      });
      if (answerIndex >= answers.length) {
        throw new Error(
          `No more answers available (called ${answerIndex + 1} times)`,
        );
      }
      return answers[answerIndex++];
    },
  };
}

/** Constructs a successful response with the given body. */
function ok(body: unknown): ApiResponse {
  return {
    status: 200,
    json: async () => body,
  };
}

function storageHolding(deviceId: string): Storage {
  const storage = inMemoryStorage();
  storage.setItem(DEVICE_ID_STORAGE_KEY, deviceId);
  return storage;
}

describe("the recent duels read", () => {
  it("asks /api/me/duels with no limit of its own", async () => {
    const { fetch, calls } = answering(
      ok({
        duels: [],
      }),
    );

    await readRecentDuels({ fetch, storage: storageHolding("d-1") });

    expect(calls).toHaveLength(1);
    expect(calls[0]?.path).toBe("/api/me/duels");
    expect(calls[0]?.path).not.toContain("?");
  });

  it("keeps every field a row carries except the opponent", async () => {
    const { fetch } = answering(
      ok({
        duels: [
          {
            duelId: "duel-1",
            opponentPlayerId: "player-77",
            outcome: "WON",
            coinDelta: 1,
            handsPlayed: 9,
            finishedAt: "2026-08-14T21:03:05Z",
          },
        ],
      }),
    );

    const read = await readRecentDuels({
      fetch,
      storage: storageHolding("d-1"),
    });

    expect(read).toEqual({
      kind: "duels",
      duels: [
        {
          duelId: "duel-1",
          outcome: "WON",
          coinDelta: 1,
          handsPlayed: 9,
          finishedAt: "2026-08-14T21:03:05Z",
        },
      ],
    });
    expect(JSON.stringify(read)).not.toContain("player-77");
  });

  it("answers an empty list for a player who has never duelled", async () => {
    const { fetch } = answering(
      ok({
        duels: [],
      }),
    );

    const read = await readRecentDuels({
      fetch,
      storage: storageHolding("d-1"),
    });

    expect(read).toEqual({
      kind: "duels",
      duels: [],
    });
  });

  it("reads every outcome the server can send", async () => {
    const { fetch } = answering(
      ok({
        duels: [
          {
            duelId: "duel-w",
            outcome: "WON",
            coinDelta: 1,
            handsPlayed: 5,
            finishedAt: "2026-08-14T21:00:00Z",
          },
          {
            duelId: "duel-l",
            outcome: "LOST",
            coinDelta: -1,
            handsPlayed: 3,
            finishedAt: "2026-08-14T20:00:00Z",
          },
          {
            duelId: "duel-d",
            outcome: "DREW",
            coinDelta: 0,
            handsPlayed: 7,
            finishedAt: "2026-08-14T19:00:00Z",
          },
        ],
      }),
    );

    const read = await readRecentDuels({
      fetch,
      storage: storageHolding("d-1"),
    });

    expect(read).toEqual({
      kind: "duels",
      duels: [
        {
          duelId: "duel-w",
          outcome: "WON",
          coinDelta: 1,
          handsPlayed: 5,
          finishedAt: "2026-08-14T21:00:00Z",
        },
        {
          duelId: "duel-l",
          outcome: "LOST",
          coinDelta: -1,
          handsPlayed: 3,
          finishedAt: "2026-08-14T20:00:00Z",
        },
        {
          duelId: "duel-d",
          outcome: "DREW",
          coinDelta: 0,
          handsPlayed: 7,
          finishedAt: "2026-08-14T19:00:00Z",
        },
      ],
    });
  });

  it("takes the coin delta signed, including the zero of a draw", async () => {
    const { fetch } = answering(
      ok({
        duels: [
          {
            duelId: "duel-1",
            outcome: "WON",
            coinDelta: 1,
            handsPlayed: 5,
            finishedAt: "2026-08-14T21:00:00Z",
          },
          {
            duelId: "duel-2",
            outcome: "LOST",
            coinDelta: -1,
            handsPlayed: 3,
            finishedAt: "2026-08-14T20:00:00Z",
          },
          {
            duelId: "duel-3",
            outcome: "DREW",
            coinDelta: 0,
            handsPlayed: 7,
            finishedAt: "2026-08-14T19:00:00Z",
          },
        ],
      }),
    );

    const read = await readRecentDuels({
      fetch,
      storage: storageHolding("d-1"),
    });

    expect(read).toEqual({
      kind: "duels",
      duels: [
        {
          duelId: "duel-1",
          outcome: "WON",
          coinDelta: 1,
          handsPlayed: 5,
          finishedAt: "2026-08-14T21:00:00Z",
        },
        {
          duelId: "duel-2",
          outcome: "LOST",
          coinDelta: -1,
          handsPlayed: 3,
          finishedAt: "2026-08-14T20:00:00Z",
        },
        {
          duelId: "duel-3",
          outcome: "DREW",
          coinDelta: 0,
          handsPlayed: 7,
          finishedAt: "2026-08-14T19:00:00Z",
        },
      ],
    });
  });

  it("answers unavailable when a row is not a duel", async () => {
    // Test 1: missing outcome field
    const { fetch: fetch1 } = answering(
      ok({
        duels: [
          {
            duelId: "duel-1",
            coinDelta: 1,
            handsPlayed: 5,
            finishedAt: "2026-08-14T21:00:00Z",
          },
        ],
      }),
    );

    const read1 = await readRecentDuels({
      fetch: fetch1,
      storage: storageHolding("d-1"),
    });

    expect(read1).toEqual({ kind: "unavailable" });

    // Test 2: invalid outcome word
    const { fetch: fetch2 } = answering(
      ok({
        duels: [
          {
            duelId: "duel-1",
            outcome: "TIED",
            coinDelta: 1,
            handsPlayed: 5,
            finishedAt: "2026-08-14T21:00:00Z",
          },
        ],
      }),
    );

    const read2 = await readRecentDuels({
      fetch: fetch2,
      storage: storageHolding("d-2"),
    });

    expect(read2).toEqual({ kind: "unavailable" });

    // Test 3: coinDelta is a string instead of number
    const { fetch: fetch3 } = answering(
      ok({
        duels: [
          {
            duelId: "duel-1",
            outcome: "WON",
            coinDelta: "one",
            handsPlayed: 5,
            finishedAt: "2026-08-14T21:00:00Z",
          },
        ],
      }),
    );

    const read3 = await readRecentDuels({
      fetch: fetch3,
      storage: storageHolding("d-3"),
    });

    expect(read3).toEqual({ kind: "unavailable" });

    // Test 4: duels is not an array
    const { fetch: fetch4 } = answering(
      ok({
        duels: "not-an-array",
      }),
    );

    const read4 = await readRecentDuels({
      fetch: fetch4,
      storage: storageHolding("d-4"),
    });

    expect(read4).toEqual({ kind: "unavailable" });
  });
});
