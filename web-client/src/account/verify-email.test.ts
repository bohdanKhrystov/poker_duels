import { describe, expect, it, vi } from "vitest";
import { verifyEmail } from "./verify-email";
import type { ApiFetch, ApiResponse } from "../profile/api";

const TOKEN = "zqx-verify-token-zqx";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`: under
 * Vitest in Node, `localStorage` is shadowed and inert, so a fake that takes
 * no dependency on it is what makes these tests a property of this module
 * rather than of the Node version running them.
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

type RecordedCall = {
  readonly path: string;
  readonly init: Parameters<ApiFetch>[1];
};

/** A recording `ApiFetch` double that answers every call the same way. */
function recordingFetch(respond: () => Promise<ApiResponse>): {
  readonly fetch: ApiFetch;
  readonly calls: RecordedCall[];
} {
  const calls: RecordedCall[] = [];
  const fetch: ApiFetch = async (path, init) => {
    calls.push({ path, init });
    return respond();
  };
  return { fetch, calls };
}

/** A recording `ApiFetch` double that returns different responses for each call. */
function recordingFetchSequence(responses: ApiResponse[]): {
  readonly fetch: ApiFetch;
  readonly calls: RecordedCall[];
} {
  const calls: RecordedCall[] = [];
  let callIndex = 0;
  const fetch: ApiFetch = async (path, init) => {
    calls.push({ path, init });
    const response = responses[callIndex];
    callIndex += 1;
    return response;
  };
  return { fetch, calls };
}

/** A recording `ApiFetch` double whose call always rejects. */
function rejectingFetch(): {
  readonly fetch: ApiFetch;
  readonly calls: RecordedCall[];
} {
  const calls: RecordedCall[] = [];
  const fetch: ApiFetch = async (path, init) => {
    calls.push({ path, init });
    throw new Error("the network is unavailable");
  };
  return { fetch, calls };
}

function responseWithStatus(status: number): ApiResponse {
  return {
    status,
    json: async () => ({}),
  };
}

function responseWithRejectedJson(status: number): ApiResponse {
  return {
    status,
    json: async () => {
      throw new Error("json parsing failed");
    },
  };
}

describe("verifying an email", () => {
  it("puts the token in a body and in no path, header or query", async () => {
    const { fetch, calls } = recordingFetch(async () =>
      responseWithStatus(204),
    );

    const outcome = await verifyEmail({
      fetch,
      token: TOKEN,
    });

    expect(outcome).toEqual({ kind: "verified" });
    expect(calls.length).toBe(1);

    const call = calls[0];
    // Body assertion runs first (presence before absence)
    const body = JSON.parse(call.init.body ?? "{}") as Record<string, unknown>;
    expect(body).toEqual({ token: TOKEN });

    // Path is exactly as specified, with no token and no question mark
    expect(call.path).toBe("/api/auth/verify-email");
    expect(call.path).not.toContain(TOKEN);
    expect(call.path).not.toContain("?");

    // No header value contains the token
    const headerValues = Object.values(call.init.headers);
    for (const value of headerValues) {
      expect(value).not.toContain(TOKEN);
    }
  });

  it("maps every status the endpoint documents to its own outcome", async () => {
    const verified = await verifyEmail({
      fetch: recordingFetch(async () => responseWithStatus(204)).fetch,
      token: TOKEN,
    });
    expect(verified).toEqual({ kind: "verified" });

    const linkDead = await verifyEmail({
      fetch: recordingFetch(async () => responseWithStatus(400)).fetch,
      token: TOKEN,
    });
    expect(linkDead).toEqual({ kind: "link-dead" });

    const addressTaken = await verifyEmail({
      fetch: recordingFetch(async () => responseWithStatus(409)).fetch,
      token: TOKEN,
    });
    expect(addressTaken).toEqual({ kind: "address-taken" });

    const failed = await verifyEmail({
      fetch: recordingFetch(async () => responseWithStatus(500)).fetch,
      token: TOKEN,
    });
    expect(failed).toEqual({ kind: "failed" });
  });

  it("writes the token to no storage and to no log", async () => {
    const storage = inMemoryStorage();

    // Create spies for console methods
    const logSpy = vi.spyOn(console, "log");
    const warnSpy = vi.spyOn(console, "warn");
    const errorSpy = vi.spyOn(console, "error");

    // Prove the spies are live by logging a sentinel before the call
    const sentinel = "spy-is-active";
    console.log(sentinel);

    const logOutput = logSpy.mock.calls.map((call) => call.join(" ")).join(" ");
    expect(logOutput).toContain(sentinel);

    // Clear the spies after the sentinel check
    logSpy.mockClear();
    warnSpy.mockClear();
    errorSpy.mockClear();

    // Call verifyEmail (which must not touch storage or console)
    const { fetch } = recordingFetch(async () => responseWithStatus(204));
    await verifyEmail({
      fetch,
      token: TOKEN,
    });

    // Verify storage is untouched
    expect(storage.length).toBe(0);

    // Verify no token was logged
    const allLogOutput = [
      ...logSpy.mock.calls.map((call) => call.join(" ")),
      ...warnSpy.mock.calls.map((call) => call.join(" ")),
      ...errorSpy.mock.calls.map((call) => call.join(" ")),
    ].join(" ");

    expect(allLogOutput).not.toContain(TOKEN);

    // Clean up spies
    logSpy.mockRestore();
    warnSpy.mockRestore();
    errorSpy.mockRestore();
  });

  it("sends one request and never a second", async () => {
    const responses = [responseWithStatus(400), responseWithStatus(204)];
    const { fetch, calls } = recordingFetchSequence(responses);

    // First call with 400 response
    const outcome1 = await verifyEmail({
      fetch,
      token: TOKEN,
    });
    expect(outcome1).toEqual({ kind: "link-dead" });
    expect(calls.length).toBe(1);

    // Second call with 204 response
    const outcome2 = await verifyEmail({
      fetch,
      token: TOKEN,
    });
    expect(outcome2).toEqual({ kind: "verified" });
    expect(calls.length).toBe(2);

    // Each invocation sent exactly one request
    expect(calls.filter((_, i) => i === 0).length).toBe(1);
    expect(calls.filter((_, i) => i === 1).length).toBe(1);
  });

  it("answers verified without reading a body, and failed without throwing", async () => {
    // A 204 whose json() rejects still answers verified
    const rejectedJson = responseWithRejectedJson(204);
    const outcome1 = await verifyEmail({
      fetch: recordingFetch(async () => rejectedJson).fetch,
      token: TOKEN,
    });
    expect(outcome1).toEqual({ kind: "verified" });

    // A fetch that rejects answers failed and doesn't throw
    const rejected = rejectingFetch();
    const outcome2 = await verifyEmail({
      fetch: rejected.fetch,
      token: TOKEN,
    });
    expect(outcome2).toEqual({ kind: "failed" });
  });
});
