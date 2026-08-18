import { describe, it, expect, beforeEach } from "vitest";
import { readProfileStrip } from "./profile-strip";
import type { ApiFetch, ApiResponse } from "./api";
import { writeDeviceId } from "../protocol/device-id";
import { meBody, duelRowBody, aProfile, aDuelLine } from "./profile-fixture";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`.
 *
 * Node 24+ defines its own `localStorage` global which is present but inert
 * unless the process is started with `--localstorage-file`, and under Vitest it
 * shadows the one jsdom provides: `typeof localStorage` is `"undefined"` while
 * `sessionStorage` — which Node keeps in memory — works. Depending on that
 * global would make these tests a property of the Node version rather than of
 * this module. `readProfileStrip` takes the `Storage` it acts on as a parameter, so
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

/** Constructs a failed response with the given status code. */
function refusedWith(status: number): ApiResponse {
  return {
    status,
    json: async () => {
      throw new Error("json() should not be called on error responses");
    },
  };
}

describe("the profile strip read", () => {
  let storage: Storage;

  beforeEach(() => {
    storage = inMemoryStorage();
  });

  it("answers the profile and its duels when both reads land", async () => {
    // First case: balance of -1 and two duel rows
    writeDeviceId(storage, "d-1");
    const mock1 = answering(
      ok(meBody({ coinBalance: -1 })),
      ok({
        duels: [
          duelRowBody({
            duelId: "duel-1",
            outcome: "WON",
            coinDelta: 10,
            handsPlayed: 5,
            finishedAt: "2025-01-01T00:00:00Z",
          }),
          duelRowBody({
            duelId: "duel-2",
            outcome: "LOST",
            coinDelta: -5,
            handsPlayed: 3,
            finishedAt: "2025-01-02T00:00:00Z",
          }),
        ],
      }),
    );

    const result1 = await readProfileStrip({
      fetch: mock1.fetch,
      storage,
    });

    expect(result1).toEqual({
      kind: "profile",
      profile: aProfile({ coinBalance: -1 }),
      duels: [
        aDuelLine({
          duelId: "duel-1",
          outcome: "WON",
          coinDelta: 10,
          handsPlayed: 5,
          finishedAt: "2025-01-01T00:00:00Z",
        }),
        aDuelLine({
          duelId: "duel-2",
          outcome: "LOST",
          coinDelta: -5,
          handsPlayed: 3,
          finishedAt: "2025-01-02T00:00:00Z",
        }),
      ],
    });

    // Second case: balance of 7 and no duels
    storage = inMemoryStorage();
    writeDeviceId(storage, "d-2");
    const mock2 = answering(ok(meBody({ coinBalance: 7 })), ok({ duels: [] }));

    const result2 = await readProfileStrip({
      fetch: mock2.fetch,
      storage,
    });

    expect(result2).toEqual({
      kind: "profile",
      profile: aProfile({ coinBalance: 7 }),
      duels: [],
    });
  });

  it("asks both endpoints once each", async () => {
    writeDeviceId(storage, "d-1");
    const mock = answering(ok(meBody({ coinBalance: 5 })), ok({ duels: [] }));

    await readProfileStrip({
      fetch: mock.fetch,
      storage,
    });

    // Verify exactly two calls were made
    expect(mock.calls).toHaveLength(2);

    // Verify the paths, sorted
    const paths = mock.calls.map((call) => call.path).sort();
    expect(paths).toEqual(["/api/me", "/api/me/duels"]);
  });

  it("answers no-profile when either half says so", async () => {
    // First door: empty storage (no call is made at all)
    const mock1 = answering(ok(meBody({ coinBalance: 5 })), ok({ duels: [] }));

    const result1 = await readProfileStrip({
      fetch: mock1.fetch,
      storage,
    });

    expect(result1).toEqual({ kind: "no-profile" });
    // Verify no calls were made
    expect(mock1.calls).toHaveLength(0);

    // Second door: stored id but both endpoints answer 401
    storage = inMemoryStorage();
    writeDeviceId(storage, "d-1");
    const mock2 = answering(refusedWith(401), refusedWith(401));

    const result2 = await readProfileStrip({
      fetch: mock2.fetch,
      storage,
    });

    expect(result2).toEqual({ kind: "no-profile" });
    expect(mock2.calls).toHaveLength(2);
  });

  it("answers unavailable when either half fails", async () => {
    // First half: /api/me answers 500 while duels answer 200
    writeDeviceId(storage, "d-1");
    const mock1 = answering(refusedWith(500), ok({ duels: [] }));

    const result1 = await readProfileStrip({
      fetch: mock1.fetch,
      storage,
    });

    expect(result1).toEqual({ kind: "unavailable" });
    expect(mock1.calls).toHaveLength(2);

    // Second half: duels answer 500 while /api/me answers 200
    storage = inMemoryStorage();
    writeDeviceId(storage, "d-2");
    const mock2 = answering(ok(meBody({ coinBalance: 5 })), refusedWith(500));

    const result2 = await readProfileStrip({
      fetch: mock2.fetch,
      storage,
    });

    expect(result2).toEqual({ kind: "unavailable" });
    expect(mock2.calls).toHaveLength(2);

    // Mixed: profile says no-profile (401), duels says unavailable (500)
    // Precedence: no-profile > unavailable
    storage = inMemoryStorage();
    writeDeviceId(storage, "d-3");
    const mock3 = answering(refusedWith(401), refusedWith(500));

    const result3 = await readProfileStrip({
      fetch: mock3.fetch,
      storage,
    });

    expect(result3).toEqual({ kind: "no-profile" });
    expect(mock3.calls).toHaveLength(2);

    // Mixed: profile says unavailable (500), duels says no-profile (401)
    // Precedence: no-profile > unavailable
    storage = inMemoryStorage();
    writeDeviceId(storage, "d-4");
    const mock4 = answering(refusedWith(500), refusedWith(401));

    const result4 = await readProfileStrip({
      fetch: mock4.fetch,
      storage,
    });

    expect(result4).toEqual({ kind: "no-profile" });
    expect(mock4.calls).toHaveLength(2);
  });
});
