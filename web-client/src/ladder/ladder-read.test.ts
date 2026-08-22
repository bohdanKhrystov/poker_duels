import { describe, it, expect } from "vitest";
import { readLadderPage } from "./ladder-read";
import type { ApiFetch, ApiResponse } from "../profile/api";
import { writeDeviceId } from "../protocol/device-id";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`.
 *
 * Node's own `localStorage` shadows jsdom's under Vitest and is `undefined`
 * unless the process is started with `--localstorage-file`; `sessionStorage`
 * works. Depending on the global would make these tests a property of the Node
 * version rather than of this module. `readLadderPage` takes the `Storage` it
 * acts on as a parameter, so the tests hand it one and rely on no global at all.
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

/** Constructs a response with a given status code. */
function response(status: number): ApiResponse {
  return {
    status,
    json: async () => ({}),
  };
}

/** A minimal valid ladder page body. */
function validLadderBody(): Record<string, unknown> {
  return {
    season: "2026-08",
    rows: [
      {
        rank: 1,
        playerId: "p-1",
        displayName: "Alice",
        coins: 100,
      },
    ],
    nextCursor: null,
    self: null,
  };
}

describe("reading one page of the ladder", () => {
  it("asks for the first page with no cursor, and for the next with the one it was given", async () => {
    // Two reads: one with after: null (first page) and one with after: "cur/1+2" (next page)
    // Proves that paths are built correctly and the cursor is URL-encoded
    const storage = inMemoryStorage();
    const mock = answering(ok(validLadderBody()), ok(validLadderBody()));

    // First read with no cursor
    const result1 = await readLadderPage({
      fetch: mock.fetch,
      storage,
      after: null,
    });

    expect(result1.kind).toBe("page");
    expect(mock.calls[0].path).toBe("/api/standings");

    // Second read with cursor
    const result2 = await readLadderPage({
      fetch: mock.fetch,
      storage,
      after: "cur/1+2",
    });

    expect(result2.kind).toBe("page");
    expect(mock.calls[1].path).toBe("/api/standings?after=cur%2F1%2B2");
  });

  it("sends the device id when the browser holds one", async () => {
    // Proves that when a device id exists in storage, it is sent in the X-Device-Id header
    const storage = inMemoryStorage();
    writeDeviceId(storage, "dev-1");
    const mock = answering(ok(validLadderBody()));

    await readLadderPage({
      fetch: mock.fetch,
      storage,
      after: null,
    });

    expect(mock.calls[0].headers["X-Device-Id"]).toBe("dev-1");
  });

  it("asks anyway when the browser holds no device id, and sends no device header", async () => {
    // Proves that when no device id exists, the request is still made
    // (unlike duel-page.ts which returns early), and no X-Device-Id header is sent.
    // This is the critical test: copying duel-page.ts's early return reddens it.
    const storage = inMemoryStorage();
    const mock = answering(ok(validLadderBody()));

    const result = await readLadderPage({
      fetch: mock.fetch,
      storage,
      after: null,
    });

    // Exactly one request made
    expect(mock.calls).toHaveLength(1);

    // No X-Device-Id header
    expect(mock.calls[0].headers["X-Device-Id"]).toBeUndefined();

    // Answer is a page, not unavailable or any no-profile state
    expect(result.kind).toBe("page");
  });

  it("answers unavailable for a refusal, a server error, and a fetch that throws", async () => {
    // Proves that 400, 500, fetch rejection, and unparseable 200 all map to unavailable
    const storage = inMemoryStorage();

    // Test 1: 400 status
    const mock400 = answering(response(400));
    const result400 = await readLadderPage({
      fetch: mock400.fetch,
      storage,
      after: null,
    });
    expect(result400.kind).toBe("unavailable");

    // Test 2: 500 status
    const mock500 = answering(response(500));
    const result500 = await readLadderPage({
      fetch: mock500.fetch,
      storage,
      after: null,
    });
    expect(result500.kind).toBe("unavailable");

    // Test 3: fetch that rejects
    const mockReject = {
      calls: [] as FetchCall[],
      fetch: async (): Promise<ApiResponse> => {
        throw new Error("Network error");
      },
    };
    const resultReject = await readLadderPage({
      fetch: mockReject.fetch,
      storage,
      after: null,
    });
    expect(resultReject.kind).toBe("unavailable");

    // Test 4: 200 with unparseable body (missing required fields)
    const mockInvalid = answering(ok({ invalid: "body" }));
    const resultInvalid = await readLadderPage({
      fetch: mockInvalid.fetch,
      storage,
      after: null,
    });
    expect(resultInvalid.kind).toBe("unavailable");

    // Verify none of them threw
    expect(result400).toBeDefined();
    expect(result500).toBeDefined();
    expect(resultReject).toBeDefined();
    expect(resultInvalid).toBeDefined();
  });
});
