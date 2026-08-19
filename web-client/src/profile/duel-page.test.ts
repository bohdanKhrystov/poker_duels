import { describe, it, expect } from "vitest";
import { readDuelPage } from "./duel-page";
import type { ApiFetch, ApiResponse } from "./api";
import { DEVICE_ID_STORAGE_KEY } from "../protocol/device-id";
import { WHOLE_RECORD, type HistoryQuery } from "./duels-query";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`.
 *
 * Node 24+ defines its own `localStorage` global which is present but inert
 * unless the process is started with `--localstorage-file`, and under Vitest it
 * shadows the one jsdom provides: `typeof localStorage` is `"undefined"` while
 * `sessionStorage` — which Node keeps in memory — works. Depending on that
 * global would make these tests a property of the Node version rather than of
 * this module. `readDuelPage` takes the `Storage` it acts on as a parameter, so
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

/** Constructs a response with the given status code. */
function withStatus(code: number): ApiResponse {
  return {
    status: code,
    json: async () => {
      throw new Error("json() should not be called for non-200 status");
    },
  };
}

function storageHolding(deviceId: string): Storage {
  const storage = inMemoryStorage();
  storage.setItem(DEVICE_ID_STORAGE_KEY, deviceId);
  return storage;
}

/** Builds a duel row body to use in responses. */
function duelRowBody(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    duelId: "duel-1",
    opponentPlayerId: "player-1",
    opponentDisplayName: "Alice",
    outcome: "WON",
    coinDelta: 1,
    handsPlayed: 5,
    finishedAt: "2026-08-14T21:00:00Z",
    ...overrides,
  };
}

describe("the duel page read", () => {
  it("asks the path the query names, and sends the device id", async () => {
    const { fetch, calls } = answering(
      ok({
        duels: [],
        nextCursor: null,
      }),
      ok({
        duels: [],
        nextCursor: null,
      }),
    );

    // First read: WHOLE_RECORD should produce /api/me/duels with no query
    await readDuelPage({
      fetch,
      storage: storageHolding("d-1"),
      query: WHOLE_RECORD,
    });

    expect(calls).toHaveLength(1);
    expect(calls[0]?.path).toBe("/api/me/duels");
    expect(calls[0]?.headers["X-Device-Id"]).toBe("d-1");

    // Second read: filtered query should produce /api/me/duels with query parameters
    const filtered: HistoryQuery = {
      outcome: "LOST",
      opponent: "Ada",
      after: "cur-1",
    };

    await readDuelPage({
      fetch,
      storage: storageHolding("d-1"),
      query: filtered,
    });

    expect(calls).toHaveLength(2);
    expect(calls[1]?.path).toContain("/api/me/duels?");
    expect(calls[1]?.path).toContain("outcome=LOST");
    expect(calls[1]?.path).toContain("opponent=Ada");
    expect(calls[1]?.path).toContain("after=cur-1");
    expect(calls[1]?.headers["X-Device-Id"]).toBe("d-1");
  });

  it("answers the rows and the cursor that names the next page", async () => {
    const { fetch } = answering(
      ok({
        duels: [duelRowBody()],
        nextCursor: "cur-9",
      }),
      ok({
        duels: [duelRowBody()],
        nextCursor: null,
      }),
    );

    const read1 = await readDuelPage({
      fetch,
      storage: storageHolding("d-1"),
      query: WHOLE_RECORD,
    });

    expect(read1).toEqual({
      kind: "page",
      duels: [
        {
          duelId: "duel-1",
          opponentDisplayName: "Alice",
          outcome: "WON",
          coinDelta: 1,
          handsPlayed: 5,
          finishedAt: "2026-08-14T21:00:00Z",
        },
      ],
      nextCursor: "cur-9",
    });

    const read2 = await readDuelPage({
      fetch,
      storage: storageHolding("d-1"),
      query: WHOLE_RECORD,
    });

    expect(read2).toEqual({
      kind: "page",
      duels: [
        {
          duelId: "duel-1",
          opponentDisplayName: "Alice",
          outcome: "WON",
          coinDelta: 1,
          handsPlayed: 5,
          finishedAt: "2026-08-14T21:00:00Z",
        },
      ],
      nextCursor: null,
    });
  });

  it("answers unavailable when the body names no cursor", async () => {
    const { fetch: fetch1 } = answering(
      ok({
        duels: [],
        // Missing nextCursor key
      }),
    );

    const read1 = await readDuelPage({
      fetch: fetch1,
      storage: storageHolding("d-1"),
      query: WHOLE_RECORD,
    });

    expect(read1).toEqual({ kind: "unavailable" });

    const { fetch: fetch2 } = answering(
      ok({
        duels: [],
        nextCursor: 7, // Wrong type: number instead of string or null
      }),
    );

    const read2 = await readDuelPage({
      fetch: fetch2,
      storage: storageHolding("d-1"),
      query: WHOLE_RECORD,
    });

    expect(read2).toEqual({ kind: "unavailable" });
  });

  it("asks nothing when the browser holds no device id", async () => {
    const { fetch, calls } =
      answering(
        // No response needed because no fetch should be made
      );

    const read = await readDuelPage({
      fetch,
      storage: inMemoryStorage(), // Empty storage, no device id
      query: WHOLE_RECORD,
    });

    expect(read).toEqual({ kind: "no-profile" });
    expect(calls).toHaveLength(0);
  });

  it("reads a 401 as no profile and a 500 as unavailable", async () => {
    const { fetch: fetch1 } = answering(withStatus(401));

    const read1 = await readDuelPage({
      fetch: fetch1,
      storage: storageHolding("d-1"),
      query: WHOLE_RECORD,
    });

    expect(read1).toEqual({ kind: "no-profile" });

    const { fetch: fetch2 } = answering(withStatus(500));

    const read2 = await readDuelPage({
      fetch: fetch2,
      storage: storageHolding("d-1"),
      query: WHOLE_RECORD,
    });

    expect(read2).toEqual({ kind: "unavailable" });
  });

  it("drops the opponent id from every row it parses", async () => {
    const { fetch } = answering(
      ok({
        duels: [duelRowBody({ opponentPlayerId: "player-77" })],
        nextCursor: null,
      }),
    );

    const read = await readDuelPage({
      fetch,
      storage: storageHolding("d-1"),
      query: WHOLE_RECORD,
    });

    expect(read).toEqual({
      kind: "page",
      duels: [
        {
          duelId: "duel-1",
          opponentDisplayName: "Alice",
          outcome: "WON",
          coinDelta: 1,
          handsPlayed: 5,
          finishedAt: "2026-08-14T21:00:00Z",
        },
      ],
      nextCursor: null,
    });

    expect(JSON.stringify(read)).not.toContain("player-77");
  });
});
