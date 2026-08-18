import { describe, it, expect, beforeEach } from "vitest";
import { readProfile } from "./profile";
import { aProfile, meBody } from "./profile-fixture";
import type { ApiFetch, ApiResponse } from "./api";
import { DEVICE_ID_STORAGE_KEY, writeDeviceId } from "../protocol/device-id";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`.
 *
 * Node 24+ defines its own `localStorage` global which is present but inert
 * unless the process is started with `--localstorage-file`, and under Vitest it
 * shadows the one jsdom provides: `typeof localStorage` is `"undefined"` while
 * `sessionStorage` — which Node keeps in memory — works. Depending on that
 * global would make these tests a property of the Node version rather than of
 * this module. `readProfile` takes the `Storage` it acts on as a parameter, so
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

describe("the profile read", () => {
  let storage: Storage;

  beforeEach(() => {
    storage = inMemoryStorage();
  });

  it("asks /api/me with the device id from the key the socket module owns", async () => {
    // First call with device id "d-1"
    writeDeviceId(storage, "d-1");
    // Verify device id is stored under the correct key
    expect(storage.getItem(DEVICE_ID_STORAGE_KEY)).toBe("d-1");
    const mock1 = answering(ok(meBody({ coinBalance: 5 })));
    const result1 = await readProfile({
      fetch: mock1.fetch,
      storage,
    });

    expect(mock1.calls).toHaveLength(1);
    expect(mock1.calls[0]?.path).toBe("/api/me");
    expect(mock1.calls[0]?.headers["X-Device-Id"]).toBe("d-1");
    expect(result1).toEqual({
      kind: "profile",
      profile: aProfile({ coinBalance: 5 }),
    });

    // Second call with device id "d-2" to ensure it's not hardcoded
    storage = inMemoryStorage();
    writeDeviceId(storage, "d-2");
    // Verify device id is stored under the correct key
    expect(storage.getItem(DEVICE_ID_STORAGE_KEY)).toBe("d-2");
    const mock2 = answering(ok(meBody({ coinBalance: 10 })));
    const result2 = await readProfile({
      fetch: mock2.fetch,
      storage,
    });

    expect(mock2.calls).toHaveLength(1);
    expect(mock2.calls[0]?.path).toBe("/api/me");
    expect(mock2.calls[0]?.headers["X-Device-Id"]).toBe("d-2");
    expect(result2).toEqual({
      kind: "profile",
      profile: aProfile({ coinBalance: 10 }),
    });
  });

  it("takes the balance the server sent, sign and all", async () => {
    // First call with negative balance (-1)
    writeDeviceId(storage, "d-1");
    const mock1 = answering(ok(meBody({ coinBalance: -1 })));
    const result1 = await readProfile({
      fetch: mock1.fetch,
      storage,
    });

    expect(result1).toEqual({
      kind: "profile",
      profile: aProfile({ coinBalance: -1 }),
    });

    // Second call with positive balance (7) to ensure it's not hardcoded
    storage = inMemoryStorage();
    writeDeviceId(storage, "d-1");
    const mock2 = answering(ok(meBody({ coinBalance: 7 })));
    const result2 = await readProfile({
      fetch: mock2.fetch,
      storage,
    });

    expect(result2).toEqual({
      kind: "profile",
      profile: aProfile({ coinBalance: 7 }),
    });
  });

  it("answers no-profile from an empty browser and from a 401", async () => {
    // Case 1: empty browser (no device id in storage)
    const mock1 = answering(ok(meBody({})));
    const result1 = await readProfile({
      fetch: mock1.fetch,
      storage,
    });

    expect(result1).toEqual({ kind: "no-profile" });
    // Verify that no fetch call was made
    expect(mock1.calls).toHaveLength(0);

    // Case 2: device id in storage but server answers 401
    storage = inMemoryStorage();
    writeDeviceId(storage, "d-1");
    const mock2 = answering(refusedWith(401));
    const result2 = await readProfile({
      fetch: mock2.fetch,
      storage,
    });

    expect(result2).toEqual({ kind: "no-profile" });
    expect(mock2.calls).toHaveLength(1);
  });

  it("answers unavailable when the body is not a profile", async () => {
    // Case 1: empty object
    writeDeviceId(storage, "d-1");
    const mock1 = answering(ok({}));
    const result1 = await readProfile({
      fetch: mock1.fetch,
      storage,
    });

    expect(result1).toEqual({ kind: "unavailable" });

    // Case 2: playerId is not a string (it's a number) — stays literal to test wrong type
    storage = inMemoryStorage();
    writeDeviceId(storage, "d-1");
    const mock2 = answering(ok({ playerId: 1, coinBalance: 3 }));
    const result2 = await readProfile({
      fetch: mock2.fetch,
      storage,
    });

    expect(result2).toEqual({ kind: "unavailable" });

    // Case 3: coinBalance is not a number (it's a string) — stays literal to test wrong type
    storage = inMemoryStorage();
    writeDeviceId(storage, "d-1");
    const mock3 = answering(ok({ playerId: "p", coinBalance: "x" }));
    const result3 = await readProfile({
      fetch: mock3.fetch,
      storage,
    });

    expect(result3).toEqual({ kind: "unavailable" });
  });
});
