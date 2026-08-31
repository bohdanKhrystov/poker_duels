import { describe, it, expect, beforeEach } from "vitest";
import { signUp, type SignUpOutcome } from "./sign-up";
import type { ApiFetch, ApiResponse } from "../profile/api";
import { writeDeviceId } from "../protocol/device-id";
import { readSessionToken } from "../protocol/session-token";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`.
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
  readonly method: string | undefined;
  readonly headers: Record<string, string>;
  readonly body: string | undefined;
}

/** The shape `ApiFetch`'s init parameter takes, mirrored here for the mocks below. */
type FetchInit = {
  readonly headers: Readonly<Record<string, string>>;
  readonly method?: string;
  readonly body?: string;
};

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
    fetch: async (path: string, init: FetchInit): Promise<ApiResponse> => {
      calls.push({
        path,
        method: init.method,
        headers: { ...init.headers },
        body: init.body,
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

/** Creates a mock fetch that records every call and always rejects, as a network failure would. */
function rejecting(): {
  readonly calls: FetchCall[];
  readonly fetch: ApiFetch;
} {
  const calls: FetchCall[] = [];

  return {
    calls,
    fetch: async (path: string, init: FetchInit): Promise<ApiResponse> => {
      calls.push({
        path,
        method: init.method,
        headers: { ...init.headers },
        body: init.body,
      });
      throw new Error("network unavailable");
    },
  };
}

/** Constructs a response with no body, for a status `docs/protocol.md` answers empty. */
function emptyBody(status: number): ApiResponse {
  return {
    status,
    json: async () => {
      throw new Error(`the body for status ${status} must not be read`);
    },
  };
}

/** Constructs a response with a JSON body, for a status `docs/protocol.md` answers one. */
function jsonBody(status: number, body: unknown): ApiResponse {
  return {
    status,
    json: async () => body,
  };
}

/** Parses a recorded call's JSON body, failing loudly if the call carried none. */
function parsedBody(call: FetchCall): unknown {
  if (call.body === undefined) {
    throw new Error("Expected a body");
  }
  return JSON.parse(call.body);
}

describe("signing up", () => {
  let storage: Storage;

  beforeEach(() => {
    storage = inMemoryStorage();
  });

  it("maps every status the endpoint documents to its own outcome", async () => {
    const statusCases: ReadonlyArray<{
      readonly status: number;
      readonly kind: SignUpOutcome["kind"];
    }> = [
      { status: 201, kind: "signed-up" },
      { status: 400, kind: "handle-refused" },
      { status: 409, kind: "unavailable-handle" },
      { status: 422, kind: "password-refused" },
      { status: 401, kind: "no-profile" },
      { status: 429, kind: "throttled" },
    ];

    const kinds: SignUpOutcome["kind"][] = [];

    for (const statusCase of statusCases) {
      const s = inMemoryStorage();
      writeDeviceId(s, "d-1");
      const mock = answering(emptyBody(statusCase.status));

      const outcome = await signUp({
        fetch: mock.fetch,
        storage: s,
        handle: "test",
        password: "password123",
      });

      kinds.push(outcome.kind);
      expect(outcome.kind).toBe(statusCase.kind);
    }

    // Assert that all six statuses map to distinct kinds
    expect(new Set(kinds).size).toBe(6);
  });

  it("keeps a throttled refusal apart from a broken server", async () => {
    // 429 should map to throttled
    const s1 = inMemoryStorage();
    writeDeviceId(s1, "d-1");
    const mock1 = answering(emptyBody(429));

    const outcome429 = await signUp({
      fetch: mock1.fetch,
      storage: s1,
      handle: "test",
      password: "password123",
    });

    expect(outcome429.kind).toBe("throttled");

    // 500 should map to failed
    const s2 = inMemoryStorage();
    writeDeviceId(s2, "d-1");
    const mock2 = answering(emptyBody(500));

    const outcome500 = await signUp({
      fetch: mock2.fetch,
      storage: s2,
      handle: "test",
      password: "password123",
    });

    expect(outcome500.kind).toBe("failed");

    // 503 should map to failed
    const s3 = inMemoryStorage();
    writeDeviceId(s3, "d-1");
    const mock3 = answering(emptyBody(503));

    const outcome503 = await signUp({
      fetch: mock3.fetch,
      storage: s3,
      handle: "test",
      password: "password123",
    });

    expect(outcome503.kind).toBe("failed");

    // fetch rejection should map to failed
    const s4 = inMemoryStorage();
    writeDeviceId(s4, "d-1");
    const mock4 = rejecting();

    const outcomeReject = await signUp({
      fetch: mock4.fetch,
      storage: s4,
      handle: "test",
      password: "password123",
    });

    expect(outcomeReject.kind).toBe("failed");

    // All four cases: 429 is throttled, the rest are failed
    expect([
      outcome429.kind,
      outcome500.kind,
      outcome503.kind,
      outcomeReject.kind,
    ]).toEqual(["throttled", "failed", "failed", "failed"]);
  });

  it("sends the handle and the password and nothing else", async () => {
    writeDeviceId(storage, "d-1");
    const mock = answering(emptyBody(201));

    await signUp({
      fetch: mock.fetch,
      storage,
      handle: "myhandle",
      password: "mypassword",
    });

    const [call] = mock.calls;
    const body = parsedBody(call) as Record<string, unknown>;
    const sortedKeys = Object.keys(body).sort();
    expect(sortedKeys).toEqual(["handle", "password"]);
  });

  it("sends what was typed, byte for byte", async () => {
    const cases = [
      { handle: "  Handle  ", password: "  Password  " },
      { handle: "MixedCase", password: "MixedCase" },
    ];

    for (const testCase of cases) {
      const s = inMemoryStorage();
      writeDeviceId(s, "d-1");
      const mock = answering(emptyBody(201));

      await signUp({
        fetch: mock.fetch,
        storage: s,
        handle: testCase.handle,
        password: testCase.password,
      });

      const [call] = mock.calls;
      const body = parsedBody(call) as Record<string, string>;
      expect(body.handle).toBe(testCase.handle);
      expect(body.password).toBe(testCase.password);
    }
  });

  it("authenticates as the device this browser holds", async () => {
    writeDeviceId(storage, "d-test-device");
    const mock = answering(emptyBody(201));

    await signUp({
      fetch: mock.fetch,
      storage,
      handle: "test",
      password: "password123",
    });

    const [call] = mock.calls;
    expect(call.headers["X-Device-Id"]).toBe("d-test-device");
    expect(call.headers["Authorization"]).toBeUndefined();
  });

  it("asks nothing of the server when this browser has no device id", async () => {
    const mock = answering();

    const outcome = await signUp({
      fetch: mock.fetch,
      storage,
      handle: "test",
      password: "password123",
    });

    expect(mock.calls).toHaveLength(0);
    expect(outcome.kind).toBe("no-profile");
  });

  it("sends one request and never a second", async () => {
    writeDeviceId(storage, "d-1");
    const mock = answering(emptyBody(429));

    await signUp({
      fetch: mock.fetch,
      storage,
      handle: "test",
      password: "password123",
    });

    expect(mock.calls).toHaveLength(1);
  });

  it("a successful claim leaves this browser holding a session", async () => {
    writeDeviceId(storage, "d-1");
    const mock = answering(
      emptyBody(201),
      jsonBody(200, { sessionToken: "session-abc" }),
    );

    const outcome = await signUp({
      fetch: mock.fetch,
      storage,
      handle: "winnerplayer",
      password: "password123",
    });

    expect(outcome.kind).toBe("signed-up");
    expect(mock.calls).toHaveLength(2);

    const signInCalls = mock.calls.filter(
      (call) => call.path === "/api/auth/sign-in",
    );
    expect(signInCalls).toHaveLength(1);
    expect(signInCalls[0].method).toBe("POST");
    const signInBody = parsedBody(signInCalls[0]) as Record<string, string>;
    expect(signInBody.handle).toBe("winnerplayer");
    expect(signInBody.password).toBe("password123");

    expect(readSessionToken(storage)).toBe("session-abc");
  });

  it("a claim the server refuses leaves no session behind", async () => {
    const refusals: ReadonlyArray<{
      readonly status: number;
      readonly kind: SignUpOutcome["kind"];
    }> = [
      { status: 409, kind: "unavailable-handle" },
      { status: 422, kind: "password-refused" },
    ];

    for (const refusal of refusals) {
      const s = inMemoryStorage();
      writeDeviceId(s, "d-1");
      const mock = answering(emptyBody(refusal.status));

      const outcome = await signUp({
        fetch: mock.fetch,
        storage: s,
        handle: "test",
        password: "password123",
      });

      expect(outcome.kind).toBe(refusal.kind);
      expect(mock.calls).toHaveLength(1);
      expect(readSessionToken(s)).toBeNull();
    }
  });

  it("a claim whose follow-up sign-in fails is still a claim", async () => {
    const followUpStatuses = [401, 429];

    for (const status of followUpStatuses) {
      const s = inMemoryStorage();
      writeDeviceId(s, "d-1");
      const mock = answering(emptyBody(201), emptyBody(status));

      const outcome = await signUp({
        fetch: mock.fetch,
        storage: s,
        handle: "test",
        password: "password123",
      });

      expect(outcome.kind).toBe("signed-up");
      expect(readSessionToken(s)).toBeNull();

      // The follow-up was attempted and failed — not skipped. Without this, a
      // module that never attempts the follow-up at all would pass here too,
      // proving nothing about the failure path this test names.
      expect(mock.calls).toHaveLength(2);
      expect(mock.calls[1].path).toBe("/api/auth/sign-in");
    }
  });
});
